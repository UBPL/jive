package edu.buffalo.cse.jive.model.slicing;

import java.util.BitSet;
import java.util.Set;

import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IEventModel.IExceptionThrowEvent;
import edu.buffalo.cse.jive.model.IEventModel.IFieldAssignEvent;
import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodEnteredEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodReturnedEvent;
import edu.buffalo.cse.jive.model.IEventModel.ITerminatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IVarAssignEvent;
import edu.buffalo.cse.jive.model.IEventModel.IVarDeleteEvent;
import edu.buffalo.cse.jive.model.IModel.IMethodContourReference;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IMethodDependenceGraph;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedLine;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;

/**
 * A method slice M is slice relevant if it satisfies any of the following:
 * 
 * <pre>
 *   a) M is the initial method slice in the computation OR 
 * 
 *   b) the result of the method call represented by M is relevant 
 *      in an assignment or control predicate in the caller line of 
 *      a slice relevant method slice (including method calls in a 
 *      qualifier position) OR 
 *      
 *   c) M modifies a field chased by the program OR 
 *   
 *   d) M is an ancestor, in the call stack, of a slice relevant method slice OR
 * 
 *   e) M is terminated by means of a relevant exception OR 
 *   
 *   f) M is the constructor of a relevant context OR 
 *   
 *   g) M is the class initializer of a relevant context
 * </pre>
 * 
 * TODO: reduce the cyclomatic complexity across and nested block depth.
 */
final class MethodSlice
{
  /**
   * Help debug method slices.
   */
  static final boolean DEBUG_MODE = false;
  /**
   * Flag to turn normal flow processing on/off.
   */
  private static final boolean NORMAL_FLOW_ON = false;
  /**
   * Arguments ever relevant to the slice. This is an append-only set derived as follows-- every
   * time a variable is added to the set of chase variables, it is added to the this set if it is an
   * argument. While variables may be removed from the set of chase variables they are never removed
   * from this set.
   */
  private final Set<IContourMember> arguments;
  /**
   * Variables currently relevant to the slice. An assignment to a variable in this set results in
   * the removal of the variable from the set. The fields used in the assignment are added to the
   * global slice and the local variables to this set.
   */
  private final Set<IContourMember> chaseVariables;
  /**
   * Members that should be processed in case a delete event is encountered.
   */
  private final Set<IContourMember> deletePending;
  /**
   * Events that effectively belong to the slice.
   */
  private final BitSet events;
  /**
   * initial relevance
   */
  private boolean isInitialRelevant;
  /**
   * result relevance
   */
  private boolean isResultRelevant;
  /**
   * field relevance
   */
  private boolean isFieldDefRelevant;
  /**
   * ancestor relevance
   */
  private boolean isAncestorRelevant;
  /**
   * exception relevance
   */
  private boolean isExceptionRelevant;
  /**
   * Determines whether this method slice *could* but does not alter the normal flow of execution.
   * The three ways in which this could happen are: (1) declaring an exception and terminating
   * normally, (2) having a System.exit() call in the method body, and (3) calling a normal flow
   * relevant method. In every case, normal termination of the method call is flow-relevant.
   */
  private final boolean isNormalFlowRelevant;
  /**
   * Initial event on this method slice.
   */
  private final IJiveEvent initialEvent;
  /**
   * Manages source line resolution for this method slice.
   */
  private final LineSlice lineSlice;
  /**
   * The method contour which identifies this method slice.
   */
  private final IMethodContour method;
  /**
   * Line that is never executed but contains formal parameter definitions.
   */
  private final IResolvedLine methodDeclLine;
  /**
   * Method slice at the top of the call stack when this slice was created.
   */
  private final MethodSlice parent;
  /**
   * Program slice-- every method slice instantiated during a program slice computation shares an
   * instance of this class.
   */
  private final ProgramSlice program;

  MethodSlice(final IJiveEvent initialEvent, final ProgramSlice program, final MethodSlice parent,
      final IMethodContour method)
  {
    this.arguments = TypeTools.newLinkedHashSet();
    this.chaseVariables = TypeTools.newLinkedHashSet();
    this.deletePending = TypeTools.newLinkedHashSet();
    this.events = new BitSet();
    this.method = method;
    this.isInitialRelevant = false;
    this.isResultRelevant = false;
    this.isFieldDefRelevant = false;
    this.isAncestorRelevant = false;
    this.isExceptionRelevant = false;
    this.initialEvent = initialEvent;
    this.parent = parent;
    this.program = program;
    this.lineSlice = new LineSlice(this);
    this.isNormalFlowRelevant = (mdg() != null && mdg().hasSystemExit())
        || (!method.schema().thrownExceptions().isEmpty() && !(initialEvent.parent().terminator() instanceof IExceptionThrowEvent));
    // determine the first line number on this method
    IResolvedLine methodDecl = null;
    if (mdg() != null)
    {
      // the key set must be traversed in increasing line number
      for (final Integer key : mdg().dependenceMap().keySet())
      {
        // method declaration
        if (methodDecl == null)
        {
          methodDecl = mdg().dependenceMap().get(key);
          break;
        }
      }
    }
    this.methodDeclLine = methodDecl;
  }

  @Override
  public String toString()
  {
    final StringBuffer buffer = new StringBuffer("");
    buffer.append("\n").append(method.toString()).append("\n");
    buffer.append("  isAncestorRelevant.......: ").append(isAncestorRelevant).append("\n");
    buffer.append("  isClinitRelevant.........: ").append(isClinitRelevant()).append("\n");
    buffer.append("  isConstructorRelevant....: ").append(isConstructorRelevant()).append("\n");
    buffer.append("  isExceptionRelevant......: ").append(isExceptionRelevant).append("\n");
    buffer.append("  isFieldDefRelevant.......: ").append(isFieldDefRelevant).append("\n");
    buffer.append("  isInitialRelevant........: ").append(isInitialRelevant).append("\n");
    buffer.append("  isNormalFlowRelevant.....: ").append(isNormalFlowRelevant()).append("\n");
    buffer.append("  isResultRelevant.........: ").append(isResultRelevant).append("\n");
    buffer.append("  relevant arguments.......: ").append(arguments.toString()).append("\n");
    return buffer.toString();
  }

  private IMethodCallEvent getNonSyntheticParent(final IMethodCallEvent call)
  {
    // in-model
    if (call.target() instanceof IMethodContourReference)
    {
      return call;
    }
    // out-of-model, but can be resolved
    if (call.target().toString().contains(".access$")
        && call.target().toString().contains("SYNTHETIC")
        && call.parent() instanceof IMethodCallEvent)
    {
      System.err.println("SYNTHETIC_ACCESSOR_RESOLVED_FOR_METHOD_SLICE");
      return getNonSyntheticParent((IMethodCallEvent) call.parent());
    }
    // unresolved out-of-model
    return null;
  }

  /**
   * Determines whether this is a relevant class initialization method.
   */
  private boolean isClinitRelevant()
  {
    return isClinit() && program.isRelevantContext(method.parent());
  }

  private boolean isInModelResultRelevant()
  {
    // if this was called from in-model, the method result must be relevant to the caller
    return !isClinit() && !(initialEvent instanceof IExceptionThrowEvent) && isUseOrDefRelevant();
  }

  private boolean isScaffolding(final IJiveEvent event)
  {
    return event instanceof IInitiatorEvent || event instanceof ITerminatorEvent
        || event instanceof IMethodEnteredEvent || event instanceof IMethodReturnedEvent;
  }

  /**
   * The parent line is use or def relevant.
   */
  private boolean isUseOrDefRelevant()
  {
    // the line from which the method was called.
    final LineSlice parentLine = parent == null ? null : parent.lineSlice();
    return parentLine != null && (parentLine.isUseRelevant() || parentLine.isDefRelevant());
  }

  /**
   * Method dependence graph for this slice.
   */
  private final IMethodDependenceGraph mdg()
  {
    return method == null ? null : method.schema().getDependenceGraph();
  }

  /**
   * Marks this method slice as relevant due to its presence in the call stack when a relevant
   * method slice has been computed.
   */
  private void setAncestorRelevant()
  {
    this.isAncestorRelevant = true;
  }

  Set<IContourMember> arguments()
  {
    return this.arguments;
  }

  /**
   * Called when this method slice is popped from the stack.
   */
  void done(final IMethodCallEvent event)
  {
    if (parent != null)
    {
      if (parent.lineSlice() != null)
      {
        parent.lineSlice().nestedCallCompleted(event, this);
      }
      if (isRelevant())
      {
        parent.setAncestorRelevant();
      }
    }
    if (MethodSlice.DEBUG_MODE /* && isNormalFlowRelevant */)
    {
      System.out.println("\nCOMPLETED:\n" + toString());
      if (parent != null && parent.lineSlice() != null)
      {
        System.out.println("\nPARENT LINE:\n" + parent.lineSlice().toString());
      }
    }
  }

  BitSet events()
  {
    return this.events;
  }

  void handleArgumentUse(final IContourMember member)
  {
    if (member != null && member.schema().modifiers().contains(NodeModifier.NM_ARGUMENT))
    {
      // chain this argument use
      program.chainValueUse(member);
      arguments.add(member);
    }
  }

  void handleVariableDef(final IVarAssignEvent event)
  {
    // chain this variable def
    program.chainValueDef(event);
    chaseVariables.remove(event.member());
    // a delete event for this member should no longer be processed
    deletePending.remove(event.member());
  }

  void handleVariableDel(final IVarDeleteEvent event)
  {
    program.chainValueDel(event);
    // a delete event for this member should no longer be processed
    deletePending.remove(event.member());
  }

  public boolean hasPendingDelete(final IVarDeleteEvent event)
  {
    return deletePending.contains(event.member());
  }

  /**
   * Called when a variable use is relevant to the ongoing computation of the relevant uses of a
   * field or variable assignment.
   */
  void handleVariableUse(final IContourMember member)
  {
    // chase variable
    if (member.schema().kind() == NodeKind.NK_VARIABLE)
    {
      // chain this argument use
      program.chainValueUse(member);
      // add to the chase set
      chaseVariables.add(member);
      // track used arguments
      handleArgumentUse(member);
      // record within the program
      program.addMember(member);
      // a delete event for this member should be processed
      deletePending.add(member);
    }
  }

  boolean hasChaseVariable(final IContourMember member)
  {
    return chaseVariables.contains(member);
  }

  IJiveEvent initialEvent()
  {
    return initialEvent;
  }

  boolean isAncestorRelevant()
  {
    return this.isAncestorRelevant;
  }

  boolean isClinit()
  {
    return method.schema().modifiers().contains(NodeModifier.NM_TYPE_INITIALIZER);
  }

  boolean isConstructor()
  {
    return method.schema().modifiers().contains(NodeModifier.NM_CONSTRUCTOR);
  }

  boolean isConstructorRelevant()
  {
    return isConstructor() && program.isRelevantContext(method.parent());
  }

  /**
   * The parent line is control relevant or this method has been reached by structured exception
   * handling (i.e., the exception is already being chased by the program).
   */
  boolean isExceptionRelevant()
  {
    if (!(initialEvent instanceof IExceptionThrowEvent)
        || !((IExceptionThrowEvent) initialEvent).framePopped())
    {
      return false;
    }
    /**
     * This is the throw of a relevant exception. Propagate the exception's relevance backwards to
     * the original throw site.
     */
    if (program.hasException(((IExceptionThrowEvent) initialEvent).exception()))
    {
      return true;
    }
    final LineSlice parentLine = parent == null ? null : parent.lineSlice();
    /**
     * An exception must always be initially caught in a catch block. Only if the catch block is the
     * entry point in a relevant method slice do we add this exception to the relevant set. All
     * other times this exception is relevant, it must be because it is being chased already.
     */
    return parentLine != null
        && (/* parentLine.isEntryPoint() && */parentLine.isCatchLine() && parent != null && parent
            .isRelevant());
    // return parentLine != null
    // && (parentLine.isUseRelevant()
    // || parentLine.isDefRelevant()
    // || (parentLine.isEntryPoint() && parentLine.isCatchLine() && parent != null && parent
    // .isRelevant()) || parentLine.isCatchRelevant());
  }

  boolean isFieldDefRelevant()
  {
    return this.isFieldDefRelevant;
  }

  boolean isInitialRelevant()
  {
    return this.isInitialRelevant;
  }

  /**
   * If a method declares that it throws exceptions but terminates normally, it is flow relevant in
   * that the relevant input arguments do not cause an exception to be thrown.
   */
  boolean isNormalFlowRelevant()
  {
    return this.isNormalFlowRelevant && MethodSlice.NORMAL_FLOW_ON;
  }

  boolean isRelevant()
  {
    return isInitialRelevant || isResultRelevant || isFieldDefRelevant || isAncestorRelevant
        || isExceptionRelevant || isConstructorRelevant() || isClinitRelevant()
        || isNormalFlowRelevant();
  }

  boolean isResultRelevant()
  {
    return this.isResultRelevant;
  }

  boolean isSuperConstructor(final MethodSlice superConstructor)
  {
    if (isConstructor() && superConstructor.isConstructor())
    {
      final ITypeNode superType = superConstructor.method.parent().schema();
      final ITypeNode thisType = method.parent().schema();
      return thisType != null && thisType.superClass() == superType;
    }
    return false;
  }

  LineSlice lineSlice()
  {
    return this.lineSlice;
  }

  IMethodContour method()
  {
    return method;
  }

  IResolvedLine methodDeclLine()
  {
    return this.methodDeclLine;
  }

  void processEvent(final IJiveEvent event)
  {
    // update the resolved line
    lineSlice.updateLine(event);
    // if the line slice handles the event successfully, process it here as well
    if (!isScaffolding(event) && lineSlice.handleEvent(event))
    {
      events.set((int) event.eventId());
    }
    // update the global relevance of this method slice
    isFieldDefRelevant = lineSlice.isFieldDefRelevant() || isFieldDefRelevant;
  }

  ProgramSlice program()
  {
    return this.program;
  }

  void setFieldDefRelevant()
  {
    isFieldDefRelevant = true;
  }

  /**
   * Initiates the computation of this method slice, starting from the given assign event. This is
   * the event corresponding to the slicing criterion.
   */
  void sliceInitiated()
  {
    if (initialEvent instanceof IVarAssignEvent)
    {
      // initialize the set of chased variables with the assginment's variable
      handleVariableUse(((IVarAssignEvent) initialEvent).member());
    }
    else if (initialEvent instanceof IFieldAssignEvent)
    {
      // initialize the set of chased fields with the assginment's field
      program.handleFieldUse((IFieldAssignEvent) initialEvent);
      isFieldDefRelevant = true;
    }
    // this method is slice relevant
    isInitialRelevant = true;
    // the line slice is entry point relevant
    lineSlice.setEntryPoint();
    // process the initial event
    processEvent(initialEvent);
  }

  /**
   * Initiates the computation of this method slice, starting from the given event. The completed
   * slice is the slice that has was last popped from the stack. If the out-of-model flag is true,
   * then the given completed slice is a pending method slice, meaning, it was called from an
   * out-of-model caller. If the out-of-model call is non-null, this is an in/out+/in call chain but
   * if the out-of-model call is null, this is an in/out+/in/return/out+/in chain, so the
   * out-of-model call is irrelevant (the completed slice and this slice are siblings called from
   * out-of-model).
   */
  void sliceInitiated(final MethodSlice completed, final IMethodCallEvent outOfModelCall,
      final boolean isOutOfModel)
  {
    // propagate relevance from the completed slice
    isInitialRelevant = parent == null && completed != null
        && (completed.isInitialRelevant() || (completed.isRelevant() && outOfModelCall == null));
    // the method can be result relevant if called from either in-model or out-of-model
    isResultRelevant = isInModelResultRelevant();
    if (isResultRelevant)
    {
      /**
       * A constructor "returns" a new instance, therefore, if the method is result relevant, the
       * context corresponding to the created instance is relevant.
       */
      if (isConstructor())
      {
        program.addContext(initialEvent.parent().executionContext());
      }
      /**
       * The result variable is relevant if the method returns a value and is result relevant.
       */
      else
      {
        handleVariableUse(method().lookupResultMember());
      }
    }
    /**
     * If this method was terminated by a thrown exception, it is relevant if it interrupted a
     * relevant computation (use or def), if it to the slice if the exception is currently being
     * chased.
     */
    isExceptionRelevant = isExceptionRelevant();
    /**
     * A slice initiated without a corresponding caller slice.
     */
    if (parent == null)
    {
      // this line is an entry point
      lineSlice.setEntryPoint();
      // update the resolved line
      lineSlice.updateLine(initialEvent);
      if (isOutOfModel && outOfModelCall == null)
      {
        // process the out-of-model call
        lineSlice.nestedOutOfModelCallCompleted(completed);
      }
      else if (completed != null)
      {
        // process the in-model call
        lineSlice.nestedCallCompleted(isOutOfModel ? outOfModelCall
            : getNonSyntheticParent((IMethodCallEvent) completed.initialEvent().parent()),
            completed);
      }
    }
    /**
     * A relevant slice initiated by a thrown exception or a slice on which the normal flow of
     * execution depends.
     */
    else if (isExceptionRelevant)
    {
      lineSlice.setEntryPoint();
    }
    // process the initial event
    processEvent(initialEvent);
  }
}