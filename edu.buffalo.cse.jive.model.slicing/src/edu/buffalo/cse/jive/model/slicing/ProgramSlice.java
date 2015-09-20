package edu.buffalo.cse.jive.model.slicing;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import edu.buffalo.cse.jive.lib.StringTools;
import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IEventModel.IAssignEvent;
import edu.buffalo.cse.jive.model.IEventModel.IDestroyObjectEvent;
import edu.buffalo.cse.jive.model.IEventModel.IFieldAssignEvent;
import edu.buffalo.cse.jive.model.IEventModel.IFieldReadEvent;
import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodEnteredEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodReturnedEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodTerminatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.INewObjectEvent;
import edu.buffalo.cse.jive.model.IEventModel.ISystemStartEvent;
import edu.buffalo.cse.jive.model.IEventModel.ITerminatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IThreadStartEvent;
import edu.buffalo.cse.jive.model.IEventModel.ITypeLoadEvent;
import edu.buffalo.cse.jive.model.IEventModel.IVarDeleteEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IExecutionModel.IProgramSlice;
import edu.buffalo.cse.jive.model.IModel.IContourReference;
import edu.buffalo.cse.jive.model.IModel.IMethodContourReference;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;
import edu.buffalo.cse.jive.model.IModel.IValue;
import edu.buffalo.cse.jive.model.IStaticModel.IDataNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.model.IStaticModel.NodeOrigin;

/**
 * One instance of this class is shared across all method slices. This guarantees that the same set
 * of fields is tracked by all method slices and, therefore, the effects of every method slice on
 * the currently used static and instance fields is visible across all method slices.
 * 
 * TODO: reduce the cyclomatic complexity across and nested block depth.
 */
public final class ProgramSlice implements IProgramSlice
{
  /**
   * Exceptions currently relevant to the slice. This set is important for exceptions propagating
   * across method slices.
   */
  private final Set<IValue> chaseExceptions;
  /**
   * Fields currently relevant to the slice. An assignment to a field in this set results in the
   * removal of the field from the set. The fields used in the assignment must be added to this set;
   * local variables used in the assignment must be added to the variable set of the respective
   * method slice.
   * 
   * A field assignment can occur on any method of any thread. Hence, the entire trace must be
   * traversed-- no method call should be skipped even if it appears otherwise irrelevant to a
   * particular branch of the slice computation.
   */
  private final Set<IContourMember> chaseFields;
  /**
   * Static and instance contours, each corresponding to the execution context of some method call
   * or field used in the slice. This set is used to determine if a given constructor or class
   * initializer is relevant to the slice.
   */
  private final Set<IContextContour> contexts;
  /**
   * Maps each MethodCallEvent to the respective MethodEnteredEvent in order to construct a
   * consistent reduced execution model.
   */
  private final Map<IJiveEvent, IJiveEvent> enteredEvents;
  /**
   * Keeps track of the enums that have been seen and processed. The purpose of this data structure
   * is to support correct processing of the spurious enum reads after the initialization of all
   * enum constants.
   */
  private final Map<ITypeNode, Set<IDataNode>> enums;
  /**
   * Set of event identifiers that actually belong to the slice.
   */
  private final BitSet eventSet;
  /**
   * Determines if this slice should include the snapshot thread.
   */
  private boolean hasSnapshot;
  /**
   * Fields and variables ever used in the slice. This is an append-only set derived as follows--
   * every time a field is added to the set of chase fields, it is also added to the this set.
   * However, while fields may be removed from the set of chase fields they are never removed from
   * this set. This set also includes local variables and arguments that were relevant at any point
   * in the slice computation.
   */
  private final Set<IContourMember> members;
  /**
   * The slicing criterion must be an assign event.
   */
  private final IAssignEvent initial;
  /**
   * <pre>
   * Method contours corresponding to method calls relevant to the program slice.  A method call is 
   * relevant if at least one of the following holds: 
   * 
   *   a) it appears on the right hand side (RHS) of a relevant assignment AND 
   *   
   *      1) the call is top-level OR
   * 
   *      2) the call appears in a relevant argument position of a relevant method call OR
   *      
   *      3) the call is a qualifier of a relevant method call, field use, or field def;
   *      
   *   b) the field chase set is modified by the respective method slice
   *   
   *   c) it is the caller of a relevant method call (relevance for scaffolding purposes)
   * </pre>
   */
  private final Set<IMethodContour> methods;
  /**
   * A map of threads to completed method slices that returned to out-of-model. An out-of-model
   * context cannot process a completed slice, hence this map allows delayed processing of this
   * slice.
   */
  private final Map<IThreadValue, MethodSlice> pendingCompleted;
  /**
   * A map of threads to out-of-model calls (from in-model). This allows for processing delayed
   * completed slices.
   */
  private final Map<IThreadValue, IMethodCallEvent> pendingOutOfModel;
  /**
   * Maps each MethodTerminatorEvent to the respective MethodReturnedEvent in order to construct a
   * consistent reduced execution model.
   */
  private final Map<IJiveEvent, IJiveEvent> returnedEvents;
  /**
   * Method slices are stacked similarly to method calls during execution. The stack of method
   * slices contains all currently active slice computations, mirroring the call stacks at every
   * instant of execution. The method slice being currently processed for a thread is always the
   * slice at the top of the corresponding stack.
   */
  private final Map<IThreadValue, Stack<MethodSlice>> slices;
  /**
   * Value chaining for relevant slice members. This is necessary because contour members will be
   * updated in a sliced execution using sliced events only. These events must, thus, refer to the
   * values used in the slice.
   */
  private final Map<IContourMember, IAssignEvent> valueChain;

  public ProgramSlice(final IAssignEvent initial)
  {
    this.chaseExceptions = TypeTools.newHashSet();
    this.chaseFields = TypeTools.newHashSet();
    this.contexts = TypeTools.newHashSet();
    this.enteredEvents = TypeTools.newHashMap();
    this.enums = TypeTools.newHashMap();
    this.eventSet = new BitSet();
    this.hasSnapshot = false;
    this.initial = initial;
    this.members = TypeTools.newHashSet();
    this.methods = TypeTools.newHashSet();
    this.pendingCompleted = TypeTools.newHashMap();
    this.pendingOutOfModel = TypeTools.newHashMap();
    this.returnedEvents = TypeTools.newHashMap();
    this.slices = TypeTools.newLinkedHashMap();
    this.valueChain = TypeTools.newHashMap();
  }

  /**
   * When a *relevant* assignment is detected, it is associated with the respective contour member
   * as its last assignment.
   */
  public void chainValueDef(final IAssignEvent event)
  {
    final IContourMember member = event.member();
    if (valueChain.containsKey(member))
    {
      final IAssignEvent value = valueChain.get(member);
      // last operation on this member was a DEF
      if (value != null)
      {
        // associate the original assignment with this member (old value)
        valueChain.get(member).setLastAssignment(event);
      }
      // last operation on this member was a USE
      else
      {
        // associate the member with the assignment
        valueChain.put(member, event);
        // local variables referencing contours
        if (event.newValue().isContourReference() && member.schema().kind() == NodeKind.NK_VARIABLE)
        {
          final IContextContour context = (IContextContour) ((IContourReference) event.newValue())
              .contour();
          if (context != null)
          {
            // System.err.println("CONTEXT_FOR_LOCAL[" + context + "]");
            addContext(context);
          }
        }
      }
    }
  }

  /**
   * When a *relevant* delete is detected, it is associated with the respective contour member as
   * its last assignment. A delete is like assigning the uninitialized value to a local.
   */
  void chainValueDel(final IVarDeleteEvent event)
  {
    final IContourMember member = event.member();
    if (valueChain.containsKey(member))
    {
      IAssignEvent ae = valueChain.get(member);
      if (ae != null)
      {
        // associate an uninitialized value with the member's assign event
        ae.setLastAssignment(event);
        System.err.println("Setting unassigned value for: " + member.name());
      }
    }
  }

  public void chainValueUse(final IContourMember member)
  {
    // unconditionally add the member so that it can keep track of assignments
    valueChain.put(member, null);
  }

  @Override
  public Set<IContextContour> contexts()
  {
    return this.contexts;
  }

  @Override
  public List<IJiveEvent> events()
  {
    final List<IJiveEvent> result = TypeTools.newArrayList(eventSet.cardinality());
    for (int i = eventSet.nextSetBit(0); i >= 0; i = eventSet.nextSetBit(i + 1))
    {
      /**
       * DO NOT lookup-- get the actual event!
       */
      final IJiveEvent e = initial.model().store().lookupRawEvent(i);
      result.add(e);
    }
    return result;
  }

  public IAssignEvent initialEvent()
  {
    return initial;
  }

  @Override
  public Set<IContourMember> members()
  {
    return this.members;
  }

  @Override
  public Set<IMethodContour> methods()
  {
    return this.methods;
  }

  @Override
  public IExecutionModel model()
  {
    return initial.model();
  }

  @Override
  public String toString()
  {
    return StringTools.sliceToString(this);
  }

  /**
   * Adds the given method call event to the slice, will all relevant structural events-- entered,
   * terminator, and returned. Additionally, adds all methods in the call stack that supports the
   * given call event.
   */
  private void addMethod(final IMethodCallEvent event)
  {
    // add the method call if it is in the slice's context
    if (event.eventId() < initial.eventId())
    {
      // initiator
      IInitiatorEvent start = event;
      // scaffolding
      while (start != null && !eventSet.get((int) start.eventId()))
      {
        // System.err.println("(+) " + start.toString());
        // relevant initiator
        eventSet.set((int) start.eventId());
        // method call
        if (start instanceof IMethodCallEvent
            && (isInModel((IMethodCallEvent) start) || (hasSnapshot && isSnapshotEvent(start)) || (start
                .executionContext() != null && start.execution().schema().modifiers()
                .contains(NodeModifier.NM_CONSTRUCTOR))))
        {
          // method contour and the respective context contour
          if (start.execution() != null)
          {
            methods.add(start.execution());
            addContext(start.execution().parent());
          }
          // relevant method entered
          final IJiveEvent entered = enteredEvents.remove(start);
          if (entered != null && entered.eventId() < initial.eventId())
          {
            // System.err.println("(+) " + entered.toString());
            // relevant method entered
            eventSet.set((int) entered.eventId());
          }
        }
        // terminator
        final ITerminatorEvent terminator = start.terminator();
        if (terminator != null && terminator.eventId() < initial.eventId())
        {
          // System.err.println("(+) " + terminator.toString());
          // relevant terminator
          eventSet.set((int) terminator.eventId());
          // method returned
          final IJiveEvent returned = returnedEvents.remove(terminator);
          if (returned != null && returned.eventId() < initial.eventId())
          {
            // System.err.println("(+) " + returned.toString());
            // relevant method returned
            eventSet.set((int) returned.eventId());
          }
        }
        // traverse back
        start = start.parent();
      }
    }
  }

  private MethodSlice createSlice(final Stack<MethodSlice> stack, final IJiveEvent event,
      final MethodSlice completed)
  {
    if (event == initial)
    {
      final MethodSlice slice = new MethodSlice(event, this, null, getMethod(
          (IMethodCallEvent) event.parent(), true));
      // push the new method slice onto the proper stack
      stack.push(slice);
      slice.sliceInitiated();
      return slice;
    }
    // parent slice (may not correspond to the caller method)
    final MethodSlice parent = stack.isEmpty() ? null : stack.peek();
    // create a slice only for in-model targets
    if (event.parent() instanceof IMethodCallEvent && isInModel((IMethodCallEvent) event.parent()))
    {
      final MethodSlice slice = new MethodSlice(event, this, parent, getMethod(
          (IMethodCallEvent) event.parent(), false));
      // push the new method slice onto the proper stack
      stack.push(slice);
      /**
       * A non-null pending out-of-model call indicates that the completed slice is not relevant to
       * the new slice.
       */
      final MethodSlice pending = pendingCompleted.remove(event.thread());
      final IMethodCallEvent outOfModelCall = pendingOutOfModel.remove(event.thread());
      if (pending != null && completed != null)
      {
        throw new IllegalStateException(
            "Cannot have both a pending completed method slice and a newly completed method slice.");
      }
      if (pending != null)
      {
        // if (outOfModelCall == null) {
        // System.err.println("in --> out (return with pending slice) --> out+ --> in.");
        // }
        // check whether this is an in-model sibling or ancestor
        IJiveEvent pendingEvent = pending.initialEvent();
        while (pendingEvent != null && pendingEvent != event)
        {
          pendingEvent = pendingEvent.parent();
        }
        final boolean isSibling = (pendingEvent != event);
        if (isSibling)
        {
          System.err
              .println("in (1) --> out+/(pending slice)/out+ --> in (2), (1) and (2) are siblings.");
        }
        else
        {
          System.err
              .println("in (1) --> out+/(pending slice)/out+ --> in (2), (1) and (2) are ancestor/descendant.");
        }
        System.err.println("(*) Initiating with a pending method slice and out-of-model call!");
        slice.sliceInitiated(isSibling ? null : pending, outOfModelCall, true);
      }
      else
      {
        slice.sliceInitiated(completed, null, false);
      }
      return slice;
    }
    /**
     * An out-of-model target leaves the completed method slice orphaned. Instead of dropping it, we
     * record the slice for later use.
     */
    else
    {
      if (completed != null)
      {
        pendingCompleted.put(event.thread(), completed);
      }
      // pending in-model to out-of-model call-- the first out-of-model target and last out-of-model
      // caller are known
      if (event.parent() instanceof IMethodCallEvent
          && event.parent().parent() instanceof IMethodCallEvent
          && isInModel((IMethodCallEvent) event.parent().parent()))
      {
        pendingOutOfModel.put(event.thread(), (IMethodCallEvent) event.parent());
      }
    }
    return null;
  }

  private IMethodContour getMethod(final IMethodCallEvent call, final boolean isInitial)
  {
    if (call.target() instanceof IMethodContourReference)
    {
      return ((IMethodContourReference) call.target()).contour();
    }
    if (isInitial && call.target().toString().contains("access$")
        && call.target().toString().contains("SYNTHETIC")
        && call.parent() instanceof IMethodCallEvent)
    {
      // System.err.println("SYNTHETIC_ACCESSOR_RESOLVED_FOR_PROGRAM_SLICE");
      return getMethod((IMethodCallEvent) call.parent(), true);
    }
    return null;
  }

  private void handleMethodCall(final Stack<MethodSlice> stack, final IMethodCallEvent event)
  {
    // the top of the stack must contain the completed method slice
    final MethodSlice completed = stack.pop();
    // allow the slice to complete its computation
    completed.done(event);
    // retrieve the terminator
    final IMethodTerminatorEvent terminator = event.terminator();
    // update the program slice with the completed method slice
    if (completed.isRelevant())
    {
      // add the method context, its call stack, and all relevant contexts
      addMethod(event);
      // add all events in the completed method slice
      eventSet.or(completed.events());
    }
    else
    {
      enteredEvents.remove(event);
      if (terminator != null)
      {
        returnedEvents.remove(terminator);
      }
    }
    // if this was the last method slice on the stack, create a slice for the caller method
    if (stack.isEmpty())
    {
      createSlice(stack, event, completed);
    }
  }

  private void handleSnapshot(final IJiveEvent event)
  {
    // chased field
    if (event instanceof IFieldAssignEvent && hasChaseField((IFieldAssignEvent) event))
    {
      handleFieldDef((IFieldAssignEvent) event);
      hasSnapshot = true;
    }
    // the snapshot has become relevant
    else if (hasSnapshot && event instanceof IMethodCallEvent)
    {
      addMethod((IMethodCallEvent) event);
    }
  }

  /**
   * Type load and new events are handled by the structural handler.
   */
  private void handleStructural(final IJiveEvent event)
  {
    // the event is relevant if the the new contour is one of the slice's contexts
    if (event instanceof INewObjectEvent
        && contexts.contains(((INewObjectEvent) event).newContour()))
    {
      // System.err.println("(+) " + event.toString());
      eventSet.set((int) event.eventId());
      // make sure the snapshot is included, if relevant
      hasSnapshot = hasSnapshot || isSnapshotEvent(event);
      /**
       * If a New Object is relevant, its destroy event might also be relevant-- namely, it should
       * be in the slice if the destruction happens before the initial event. Undoing a destroy is
       * recreating the object in its final state whereas undoing a create is removing the object in
       * its initial state.
       */
      final long oid = ((INewObjectEvent) event).newContour().oid();
      final IJiveEvent de = model().store().lookupDestroyEvent(oid);
      if (de != null && de.eventId() < initial.eventId())
      {
        eventSet.set((int) de.eventId());
      }
    }
    // the event is relevant if the the new contour is one of the slice's contexts
    else if (event instanceof ITypeLoadEvent
        && contexts.contains(((ITypeLoadEvent) event).newContour()))
    {
      // System.err.println("(+) " + event.toString());
      eventSet.set((int) event.eventId());
      // make sure the snapshot is included, if relevant
      hasSnapshot = hasSnapshot || isSnapshotEvent(event);
    }
  }

  private boolean isInModel(final IMethodCallEvent event)
  {
    return event.inModel() && event.execution().schema().origin() == NodeOrigin.NO_AST;
  }

  private boolean isSnapshotEvent(final IJiveEvent event)
  {
    return event.thread().id() == -1 && event.thread().name().contains("JIVE Snapshot");
  }

  private boolean isStructuralEvent(final IJiveEvent event)
  {
    return event instanceof INewObjectEvent || event instanceof IDestroyObjectEvent
        || event instanceof ITypeLoadEvent || event instanceof ISystemStartEvent
        || event instanceof IThreadStartEvent;
  }

  /**
   * Returns a non-null, possibly empty stack of method slices associated with the event's thread.
   */
  private Stack<MethodSlice> lookupSliceStack(final IJiveEvent event)
  {
    if (slices.get(event.thread()) == null)
    {
      slices.put(event.thread(), new Stack<MethodSlice>());
    }
    return slices.get(event.thread());
  }

  /**
   * Returns the prior event in the trace.
   * 
   * POSSIBLE IMPROVEMENT: If the set of currently chased fields is empty, this method could skip
   * events from threads with no outstanding method slice, since such threads cannot possibly affect
   * the slice computation and threads do not share local variables.
   */
  private IJiveEvent priorEvent(final IJiveEvent event)
  {
    // not an event
    if (event == null)
    {
      return null;
    }
    // no prior event
    if (event.eventId() <= 1)
    {
      return null;
    }
    // return the prior event
    final IJiveEvent prior = event.prior();
    initial.model().temporalState().rollback();
    // ((ExecutionModel) initial.model()).transactionLog().rollback();
    return prior;
  }

  /**
   * Adds the context to the set of relevant contexts. For instance contours, all inherited contours
   * are included and so are the respective static contours.
   */
  void addContext(final IContextContour context)
  {
    // System.err.println("ADD_CONTEXT[" + context + "]");
    IContextContour contour = context;
    while (contour != null && !contexts.contains(contour))
    {
      contexts.add(contour);
      contour = contour.parent();
    }
    // for instance contours, add the respective static contours
    if (context != null && !context.isStatic())
    {
      addContext(context.schema().lookupStaticContour());
    }
  }

  /**
   * Adds the exception to the set of chase exceptions.
   */
  void addException(final IValue exception)
  {
    chaseExceptions.add(exception);
  }

  /**
   * Adds the contour member to the set of relevant members. For members that reference contours,
   * the respective contour is also added as a relevant context.
   */
  void addMember(final IContourMember member)
  {
    if (member != null)
    {
      members.add(member);
      // if the member value is a contour reference, it is relevant
      if (member.value().isContourReference())
      {
        addContext((IContextContour) ((IContourReference) member.value()).contour());
      }
    }
  }

  /**
   * For each event in the trace, processes the event in the the appropriate method slice. If no
   * method slice exists yet for the current event, a new one is created and push it onto the stack
   * of method slices for the corresponding thread.
   * 
   * The computation of the program slice is split across the program slicer, the method slicer, and
   * the line slicer.
   */
  public void computeSlice()
  {
    // the initial event to process
    IJiveEvent event = initial;
    // while there are events to process
    while (event != null)
    {
      // structural events are processed separately
      if (isStructuralEvent(event))
      {
        handleStructural(event);
      }
      else if (isSnapshotEvent(event))
      {
        handleSnapshot(event);
      }
      else
      {
        // always handle returned events
        if (event instanceof IMethodReturnedEvent)
        {
          returnedEvents.put(((IMethodReturnedEvent) event).terminator(), event);
        }
        else if (event instanceof IMethodEnteredEvent)
        {
          // the initiator has already been marked as relevant
          if (event.parent() != null && eventSet.get((int) event.parent().eventId()))
          {
            eventSet.set((int) event.eventId());
          }
          else
          {
            enteredEvents.put(event.parent(), event);
          }
        }
        // retrieve the current event's slice stack (creates an empty one if necessary)
        final Stack<MethodSlice> stack = lookupSliceStack(event);
        if (stack.isEmpty())
        {
          // the slice initiated method processes the event
          final MethodSlice top = createSlice(stack, event, null);
          // could not create a top method slice, move on to the prior event
          if (top == null)
          {
            event = priorEvent(event);
            continue;
          }
        }
        // a method call event marks the completion of the computation of a method slice
        else if (event instanceof IMethodCallEvent)
        {
          if (isInModel((IMethodCallEvent) event))
          {
            handleMethodCall(stack, (IMethodCallEvent) event);
          }
        }
        // first ever event on this method must be a method terminator event (exit or throw)
        else if (event instanceof IMethodTerminatorEvent
            && ((IMethodTerminatorEvent) event).framePopped())
        {
          // terminator of out-of-model method calls-- no slice to create
          if (!isInModel(((IMethodTerminatorEvent) event).parent()))
          {
            final IMethodContour c = ((IMethodTerminatorEvent) event).parent().execution();
            if (c != null && c.schema().modifiers().contains(NodeModifier.NM_CONSTRUCTOR))
            {
              if (contexts.contains(c.parent()))
              {
                addMethod((IMethodCallEvent) event.parent());
              }
            }
            event = priorEvent(event);
            continue;
          }
          // create a method slice for the called method
          createSlice(stack, event, null);
        }
        // event on an existing method slice
        else
        {
          // retrieve the method slice corresponding to the event
          final MethodSlice slice = stack.peek();
          // delegate event processing to the slice
          slice.processEvent(event);
        }
      }
      // move to the prior event
      event = priorEvent(event);
    }
  }

  public BitSet eventSet()
  {
    return eventSet;
  }

  /**
   * Tries to remove the enum constant from the appropriate set in the enums map.
   */
  boolean handleEnumConstant(final IDataNode enumConstant)
  {
    final Set<IDataNode> constants = enums.get(enumConstant.parent());
    return constants != null && constants.remove(enumConstant);
  }

  /**
   * Creates a populated entry for the given enum in the enums map.
   */
  void handleEnumDeclaration(final ITypeNode enumNode)
  {
    if (!enums.containsKey(enumNode))
    {
      final Set<IDataNode> constants = TypeTools.newHashSet();
      for (final IDataNode c : enumNode.dataMembers().values())
      {
        if (c.modifiers().contains(NodeModifier.NM_ENUM_CONSTANT))
        {
          constants.add(c);
        }
      }
      enums.put(enumNode, constants);
    }
  }

  /**
   * Called when a field definition is found. A field definition always removes the field from the
   * chase set. The fields used in this assignment (directly or transitively) must be added to the
   * uses set. Once the method return the field is no longer on the chase set. The method returns
   * true only if the field was in the chased field set.
   */
  boolean handleFieldDef(final IFieldAssignEvent def)
  {
    if (chaseFields.remove(def.member())
        || (def.contour().schema().kind() == NodeKind.NK_ARRAY && isRelevantContext(def.contour())))
    // if (chaseFields.remove(def.member()))
    {
      // chain this field def
      chainValueDef(def);
      // add the event to the slice
      eventSet.set((int) def.eventId());
      // add the field to the model
      members.add(def.member());
      // add the field's context contour to the model
      addContext(def.contour());
      // if the new value is a contour reference, it is relevant
      if (def.newValue().isContourReference())
      {
        addContext((IContextContour) ((IContourReference) def.newValue()).contour());
      }
      // the definition is relevant
      return true;
    }
    // the definition is not relevant
    return false;
  }

  /**
   * Called when a member is used in a definition.
   */
  void handleFieldUse(final IContextContour context, final IContourMember member)
  {
    // chain this field use
    chainValueUse(member);
    // chase the field use
    chaseFields.add(member);
    if (member.name().equals("rank"))
    {
      System.err.println(context.signature() + ".rank::" + member.hashCode());
    }
    // add this member to the model
    members.add(member);
    // add the field's context contour to the model
    addContext(context);
  }

  /**
   * Called when a field assignment is used to define a slicing criterion.
   */
  void handleFieldUse(final IFieldAssignEvent use)
  {
    // chain this field use
    chainValueUse(use.member());
    // add the event to the slice
    eventSet.set((int) use.eventId());
    // chase the field use
    chaseFields.add(use.member());
    if (use.member().name().equals("rank"))
    {
      System.err.println(use.contour().signature() + ".rank::" + use.member().hashCode());
    }
    // add this member to the model
    members.add(use.member());
    // add the field's context contour to the model
    addContext(use.contour());
  }

  /**
   * Called when a field use is relevant to the ongoing computation of the relevant uses of a field
   * or variable assignment.
   */
  void handleFieldUse(final IFieldReadEvent use)
  {
    // chain this field use
    chainValueUse(use.member());
    // add the event to the slice
    eventSet.set((int) use.eventId());
    // chase the field use
    chaseFields.add(use.member());
    // add the field to the model
    addMember(use.member());
    // add the field's context contour to the model
    addContext(use.contour());
  }

  boolean hasChaseField(final IFieldAssignEvent event)
  {
    //final IContextContour context = event.contour();
    final IContourMember member = event.member();
    return chaseFields.contains(member);
    // return chaseFields.contains(member)
    // || (context.schema().kind() == NodeKind.NK_ARRAY && isRelevantContext(context));
  }

  boolean hasException(final IValue exception)
  {
    return chaseExceptions.contains(exception);
  }

  boolean isRelevantContext(final IContextContour context)
  {
    return context != null && contexts.contains(context);
  }
}