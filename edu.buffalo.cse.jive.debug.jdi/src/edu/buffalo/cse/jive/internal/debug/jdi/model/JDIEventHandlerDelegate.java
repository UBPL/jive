package edu.buffalo.cse.jive.internal.debug.jdi.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.AccessWatchpointEvent;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.ModificationWatchpointEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadDeathEvent;

import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.debug.JiveDebugPlugin;
import edu.buffalo.cse.jive.debug.jdi.model.IExecutionState;
import edu.buffalo.cse.jive.debug.jdi.model.IJDIManager;
import edu.buffalo.cse.jive.debug.jdi.model.IJiveEventDispatcher;
import edu.buffalo.cse.jive.debug.jdi.model.IModelFilter;
import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IContourModel.IObjectContour;
import edu.buffalo.cse.jive.model.IEventModel.IMethodExitEvent;
import edu.buffalo.cse.jive.model.IEventModel.ITerminatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IThreadStartEvent;
import edu.buffalo.cse.jive.model.IJiveProject;
import edu.buffalo.cse.jive.model.IModel.IContourReference;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNodeRef;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.model.factory.IContourFactory;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory.IStaticModelDelegate;

class JDIEventHandlerDelegate
{
  private static final String VOID_TYPE_NAME = "void";
  private final IJiveDebugTarget owner;
  private final Set<ReferenceType> noInfoTypes;
  private final Set<ArrayReference> pendingCellWrites;
  private IStaticModelDelegate adapterForAST;

  JDIEventHandlerDelegate(final IJiveDebugTarget owner)
  {
    this.owner = owner;
    this.noInfoTypes = Collections.newSetFromMap(new ConcurrentHashMap<ReferenceType, Boolean>());
    this.pendingCellWrites = TypeTools.newHashSet();
  }

  private IStaticModelDelegate adapterForAST(final VirtualMachine vm)
  {
    if (this.adapterForAST == null)
    {
      // non-null project must exist
      final IJiveProject project = owner.getProject();
      // to eliminate the dependence between JDI and AST, we construct delegates separately
      this.adapterForAST = JiveDebugPlugin.createStaticModelDelegate(manager().executionModel(),
          project, vm, eventFilter());
    }
    return this.adapterForAST;
  }

  /**
   * Returns whether the given thread contains any in-model frames (i.e., whether there is a frame
   * on the stack with a corresponding contour) between the top frame and the frame matching the
   * supplied location.
   */
  private boolean containsInModelFrames(final ThreadReference thread, final Location catchLocation)
      throws IncompatibleThreadStateException
  {
    @SuppressWarnings("unchecked")
    final List<StackFrame> stack = thread.frames();
    for (final StackFrame frame : stack)
    {
      final ReferenceType type = frame.location().declaringType();
      if (eventFilter().acceptsType(type))
      {
        return true;
      }
      if (catchLocation != null && frame.location().method().equals(catchLocation.method()))
      {
        return false;
      }
    }
    return false;
  }

  private IContourFactory contourFactory()
  {
    return owner.model().contourFactory();
  }

  /**
   * Determines the correct stack frame for the {@code LocatableEvent}. The method associated with
   * the event is normally the top frame (number 0) on the thread's stack. In some situations this
   * is not the case. This has been observed when a {@code CallEvent} for an {@code Enum}'s
   * {@code <clinit>} is generated. In this case, additional stack frames exist. Therefore, the
   * correct frame needs to be determined.
   * <p>
   * This method is also responsible for generating throw and catch events as well as out-of-model
   * call and return events.
   * 
   * TODO: reduce the cyclomatic complexity.
   */
  private StackFrame determineStackFrame(final LocatableEvent event)
      throws IncompatibleThreadStateException, AbsentInformationException
  {
    // make sure a thread value exists in the model
    if (!executionState().containsThread(event.thread())
        && manager().modelFilter().acceptsThread(event.thread()))
    {
      final IThreadValue thread = owner.model().valueFactory()
          .createThread(event.thread().uniqueID(), event.thread().name());
      executionState().observedThread(thread);
    }
    // The stack size according to JIVE should be the same as reported by the JVM
    final ThreadReference jdiThread = event.thread();
    final int jdiStackSize = jdiThread.frameCount();
    final int jiveStackSize = executionState().frameCount(jdiThread.uniqueID());
    // Except when a method is being called it will be one-off
    final int modifier = event instanceof MethodEntryEvent ? 1 : 0;
    int index = jdiStackSize - jiveStackSize - modifier; // 0 == top frame
    // Special handling is needed if an exception has occurred on the thread
    boolean exceptionOccurred = executionState().containsException(jdiThread.uniqueID());
    int framesPopped = 0;
    // Find the the "top" stack frame
    int top = 0;
    StackFrame frame = jdiThread.frame(top);
    // method entry
    if (modifier == 1)
    {
      // find the position of the event's frame within JIVE's thread stack
      while (!event.location().method().equals(frame.location().method()))
      {
        top++;
        frame = jdiThread.frame(top);
      }
    }
    /**
     * Index is 0 or 1; top is any number greater or equal to 0. Pop frames from the JIVE thread in
     * order to synchronize the stacks.
     */
    while (index < top)
    {
      index++;
      if (exceptionOccurred)
      {
        final ExceptionEvent exception = outstandingException(jdiThread);
        dispatcher().dispatchThrowEvent(exception, true);
        framesPopped++;
      }
      else
      {
        dispatcher().dispatchMethodExitEvent(jdiThread);
      }
    }
    // post-condition: index == top
    /**
     * Check for out-of-model frames on the JVM stack.
     */
    if (modifier == 1)
    {
      while (executionState().hasFrames(jdiThread.uniqueID()))
      {
        // Stop at an in-model frame
        final StackFrame topFrame = executionState().framePeek(jdiThread.uniqueID());
        if (executionState().containsInModelFrame(topFrame))
        {
          break;
        }
        // Stop once a matching frame is found
        final Method topMethod = topFrame.location().method();
        if (topMethod.equals(jdiThread.frame(index + 1).location().method()))
        {
          break;
        }
        // Generate the appropriate events
        index++;
        if (exceptionOccurred)
        {
          final Location catchLocation = outstandingException(jdiThread).catchLocation();
          if (catchLocation != null && topMethod.equals(catchLocation.method()))
          {
            removeException(jdiThread);
            exceptionOccurred = false;
            dispatcher().dispatchMethodExitEvent(jdiThread);
          }
          else
          {
            final ExceptionEvent exception = outstandingException(jdiThread);
            dispatcher().dispatchThrowEvent(exception, true);
            framesPopped++;
          }
        }
        else
        {
          dispatcher().dispatchMethodExitEvent(jdiThread);
        }
      }
    }
    // post-condition: index >= top
    frame = (index == top) ? frame : jdiThread.frame(index - 1);
    // More frames on the JVM stack
    while (index > top)
    {
      // resolve the method
      resolveMethod(frame, frame.location().method());
      dispatcher().dispatchOutOfModelCallEvent(frame);
      index--;
      frame = jdiThread.frame(index);
    }
    // Generate exception-related events if necessary
    if (exceptionOccurred)
    {
      final StackFrame catchFrame = jdiThread.frame(index + modifier);
      // Only generate a catch event if caught in-model
      final ReferenceType type = catchFrame.location().method().declaringType();
      if (eventFilter().acceptsType(type))
      {
        // Generate a throw event if the exception was caught where it was thrown
        if (framesPopped == 0)
        {
          final ExceptionEvent exception = outstandingException(jdiThread);
          dispatcher().dispatchThrowEvent(exception, false);
        }
        final ExceptionEvent exception = outstandingException(catchFrame.thread());
        dispatcher().dispatchCatchEvent(catchFrame, exception);
      }
      removeException(jdiThread);
    }
    // make sure we return a consistent frame
    assert frame.location().method().equals(event.location().method());
    return frame;
  }

  private IJiveEventDispatcher dispatcher()
  {
    return manager().jiveDispatcher();
  }

  private IModelFilter eventFilter()
  {
    return manager().modelFilter();
  }

  private IExecutionState executionState()
  {
    return manager().executionState();
  }

  /**
   * This is used for generating non-JDI events, therefore, it has no information on whether the
   * source method has debug information. Thus, we do the necessary processing and optimizations in
   * here.
   */
  private List<?> getVariables(final Method m)
  {
    try
    {
      if (!noInfoTypes.contains(m.declaringType()))
      {
        return m.variables();
      }
    }
    catch (final AbsentInformationException e)
    {
      noInfoTypes.add(m.declaringType());
      System.err.println(m.declaringType());
    }
    return Collections.emptyList();
  }

  private void handleClassLoad(final ClassType classType, final ThreadReference thread)
  {
    // do not proceed if this part of the hierarchy has been processed
    if (contourFactory().lookupStaticContour(classType.name()) == null)
    {
      // update the meta-data for this type and all other types in its file
      resolveType(classType);
      // handle the super class
      final ClassType superClass = classType.superclass();
      // if a class is in-model, all its ancestors must be in-model so that the proper static
      // contours and their members exist
      if (superClass != null)
      {
        handleClassLoad(superClass, thread);
      }
      // Only static and top-level types have static contours, *including* nested static types.
      if (classType.isStatic() || !isNestedType(classType))
      {
        // creates and dispatches a type load event for this type
        manager().jiveDispatcher().dispatchLoadEvent(classType, thread);
      }
    }
  }

  private void handleInterfaceLoad(final InterfaceType intfType, final ThreadReference thread)
  {
    // do not proceed if this part of the hierarchy has been processed
    if (contourFactory().lookupStaticContour(intfType.name()) == null)
    {
      // update the meta-data for this type and all other types in its file
      resolveType(intfType);
      // recursively create the schemas, as necessary
      final List<?> superTypes = intfType.superinterfaces();
      for (final Object o : superTypes)
      {
        final InterfaceType superType = (InterfaceType) o;
        handleInterfaceLoad(superType, thread);
      }
      // all interfaces are either top-level or static;
      // we only create a contour if the interface declares fields
      if (intfType.fields().size() > 0)
      {
        // creates and dispatches a type load event for this type
        manager().jiveDispatcher().dispatchLoadEvent(intfType, thread);
      }
    }
  }

  private boolean handlePendingReturned(final Location location, final StackFrame frame,
      final ThreadReference thread)
  {
    final ITerminatorEvent te = executionState().getPendingMethodReturned(thread.uniqueID());
    if (te != null)
    {
      dispatcher().dispatchMethodReturnedEvent(thread);
      //
      // From this point on, we're trying to detect modifications to some cell of
      // the array returned by this method
      //
      if (manager().generateArrayEvents() && location != null && frame != null
          && te instanceof IMethodExitEvent)
      {
        final IMethodExitEvent mee = (IMethodExitEvent) te;
        //
        // Return value of the method is an object
        //
        if (mee.returnValue() instanceof IContourReference)
        {
          final IContourReference cr = (IContourReference) mee.returnValue();
          //
          // Return value of the method is an array
          //
          if (cr.contour().schema().kind() == NodeKind.NK_ARRAY)
          {
            final IObjectContour oc = (IObjectContour) cr.contour();
            executionState().observedArrayResult(thread, oc);
          }
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Check if the provided type is nested.
   */
  private boolean isNestedType(final ReferenceType type)
  {
    // Class names with a dollar sign in the name. However, classes starting with a dollar sign
    // are synthetic proxy classes (e.g., $Proxy0).
    return type.name().contains("$") && !type.name().startsWith("$");
  }

  private IJDIManager manager()
  {
    return JiveDebugPlugin.getDefault().jdiManager(owner);
  }

  private ExceptionEvent outstandingException(final ThreadReference thread)
  {
    final ExceptionEvent result = executionState().lookupException(thread.uniqueID());
    if (result == null)
    {
      throw new IllegalStateException("An exception has not occurred on the thread.");
    }
    return result;
  }

  private void processArrayCellWrite(final Location location, final StackFrame frame,
      final IObjectContour oc)
  {
    if (!manager().generateArrayEvents() || oc == null)
    {
      return;
    }
    final Object jvm = owner.getJVM();
    if (jvm != null)
    {
      final VirtualMachine vm = (VirtualMachine) jvm;
      // traverse loaded classes to find the array
      for (final Object c : vm.allClasses())
      {
        // skip non-arrays
        if (!(c instanceof ArrayType))
        {
          continue;
        }
        // array type
        final ArrayType refType = (ArrayType) c;
        final ITypeNodeRef type = staticModelFactory().lookupTypeNode(refType.signature());
        // skip if not the same type as the array contour
        if (type == null || type.node() != oc.schema())
        {
          continue;
        }
        // create all instances but do not assign field values
        for (final Object o : refType.instances(0))
        {
          if (o instanceof ArrayReference)
          {
            final ArrayReference ar = (ArrayReference) o;
            if (ar.uniqueID() == oc.oid())
            {
              if (visitArrayCells(location, frame, ar, oc))
              {
                System.err
                    .println("Processed cell assginment to array: returned from method or invisible local variable.");
              }
            }
          }
        }
      }
    }
  }

  private void removeException(final ThreadReference thread)
  {
    executionState().removeException(thread.uniqueID());
  }

  private void removeLocals(final StackFrame frame) throws AbsentInformationException
  {
    final Location loc = frame.location();
    final Method m = loc.method();
    if (m.isAbstract() || m.isNative())
    {
      return;
    }
    final IMethodContour method = executionState().retrieveTopContour(frame.thread().uniqueID());
    for (final Object o : getVariables(m))
    {
      final LocalVariable var = (LocalVariable) o;
      IContourMember varInstance = method.lookupMember(m.variables().indexOf(var));
      if (varInstance == null || !varInstance.schema().name().equals(var.name()))
      {
        varInstance = method.lookupMember(var.name(), loc.lineNumber());
      }
      if (varInstance == null)
      {
        /**
         * This should only happen when a variable is declared final but not initialized in the
         * source. The compiler apparently creates two instances of the variable with different
         * scopes. Thus, if the lines don't match here, it's because the variable is not in scope
         * anyway, so missing this event is fine.
         */
        JiveDebugPlugin.info(String.format(
            "JDI says a variable is visible but it really is not: '%s' not found at %s:%d.",
            var.toString(), loc.sourceName(), loc.lineNumber()));
      }
      else
      {
        executionState().removeVariable(varInstance, frame.thread());
      }
    }
    executionState().removeArguments(method);
  }

  /**
   * Creates a node for this type, if one does not exist.
   */
  private ITypeNodeRef resolveType(final ReferenceType type)
  {
    ITypeNodeRef result = staticModelFactory().lookupTypeNode(type.signature());
    if (result == null)
    {
      // the adapter creates static model nodes using AST when possible and JDI otherwise
      final IStaticModelDelegate adapter = adapterForAST(type.virtualMachine());
      // create the node for this type and related types, recursively
      result = adapter.resolveType(type.signature(), type.name());
    }
    return result;
  }

  private IStaticModelFactory staticModelFactory()
  {
    return owner.model().staticModelFactory();
  }

  private void visitArguments(final LocatableEvent event, final StackFrame frame)
      throws AbsentInformationException
  {
    final Location loc = frame.location();
    final Method m = loc.method();
    if (m.isAbstract() || m.isNative() || m.isSynthetic())
    {
      return;
    }
    final IMethodContour method = executionState().retrieveTopContour(frame.thread().uniqueID());
    final Map<LocalVariable, Value> vars = frame.getValues(frame.visibleVariables());
    for (final LocalVariable var : vars.keySet())
    {
      // every argument should be created since it's the first time these variables are seen
      if (var.isArgument())
      {
        final Value val = vars.get(var);
        final IContourMember varInstance = method.lookupMember(m.variables().indexOf(var));
        if (executionState().observedVariable(varInstance, val, frame.thread(), loc.lineNumber()))
        {
          /**
           * Dispatch a new array event, if appropriate. When an array is instantiated as part of an
           * argument expression, this is the first time the array it can be detected. Conceptually,
           * it is as if it had been created upon entering the called method. This will also capture
           * small arrays instantiated in out-of-model methods and passed to in-model methods.
           */
          if (manager().generateArrayEvents() && val instanceof ArrayReference)
          {
            // dispatch a new array event and the respective cell assignments
            handleNewArray((ArrayReference) val, event != null ? event.location() : loc, frame);
          }
          /**
           * No array event support for method invocation with array instantiations since we cannot
           * determine the correct order in which an array is created in the argument expressions of
           * the method invocation.
           */
          dispatcher().dispatchVarAssignEvent(event, frame, val, varInstance, var.typeName());
        }
      }
    }
  }

  private boolean visitArrayCells(final Location location, final StackFrame frame,
      final ArrayReference arrayRef)
  {
    // only process small arrays
    if (!manager().generateArrayEvents() || arrayRef == null
        || arrayRef.length() > EventFactoryAdapter.SMALL_ARRAY_SIZE)
    {
      return false;
    }
    // retrieve the array reference type
    final ArrayType at = (ArrayType) arrayRef.type();
    // retrieve the array contour
    final IContextContour array = contourFactory().lookupInstanceContour(at.name(),
        arrayRef.uniqueID());
    // make sure the respective array contour exists
    if (array == null)
    {
      return false;
    }
    return visitArrayCells(location, frame, arrayRef, array);
  }

  private boolean visitArrayCells(final Location location, final StackFrame frame,
      final ArrayReference arrayRef, final IContextContour array)
  {
    boolean modified = false;
    // retrieve the array values
    final List<Value> values = arrayRef.length() > 0 ? arrayRef.getValues() : null;
    // check for modifications on each of the array cells
    for (int i = 0; values != null && i < values.size(); i++)
    {
      final IContourMember cell = array.lookupMember(i);
      final Value cellValue = values.get(i);
      // recursively process the array reference value
      if (cellValue instanceof ArrayReference)
      {
        if (!handleNewArray((ArrayReference) cellValue, location, frame)
            && EventFactoryAdapter.PROCESS_MULTI_ARRAY)
        {
          final ArrayReference innerArray = (ArrayReference) cellValue;
          // retrieve the array reference type
          final ArrayType at = (ArrayType) cellValue.type();
          // retrieve the array contour
          final IContextContour innerContour = contourFactory().lookupInstanceContour(at.name(),
              innerArray.uniqueID());
          visitArrayCells(location, frame, innerArray, innerContour);
        }
      }
      // true if the variable was newly observed or its value changed
      if (executionState().observedVariable(cell, cellValue, frame.thread(), location.lineNumber()))
      {
        // dispatch the assignment to the modified cell
        dispatcher().dispatchArrayCellWriteEvent(location, frame.thread(), array, cell, cellValue,
            ((ArrayType) arrayRef.type()).componentTypeName());
        modified = true;
      }
    }
    return modified;
  }

  /**
   * The name of a local variable must not duplicate the name of another local variable defined in a
   * parent block or in the same block (in a tree of blocks, the list of self-or-ancestor blocks
   * must not contain a local variable with the same name). This means that no local variable names
   * conflict in nested scopes.
   * 
   * @see {@code org.eclipse.jdi.internal.LocalVariableImpl}
   */
  private void visitLocals(final LocatableEvent event, final StackFrame frame, final Location loc)
      throws AbsentInformationException
  {
    final Method m = loc.method();
    if (m.isAbstract() || m.isNative() || m.isSynthetic())
    {
      return;
    }
    final ThreadReference thread = frame.thread();
    final IMethodContour method = executionState().retrieveTopContour(thread.uniqueID());
    for (final Object o : getVariables(m))
    {
      final LocalVariable var = (LocalVariable) o;
      IContourMember varInstance = method.lookupMember(m.variables().indexOf(var));
      if (varInstance == null || !varInstance.schema().name().equals(var.name()))
      {
        varInstance = method.lookupMember(var.name(), loc.lineNumber());
      }
      if (varInstance == null)
      {
        /**
         * This should only happen when a variable is declared final but not initialized in the
         * source. The compiler apparently creates two instances of the variable with different
         * scopes. Thus, if the lines don't match here, it's because the variable is not in scope
         * anyway, so missing this event is fine.
         */
      }
      else if (var.isVisible(frame))
      {
        final Value val = frame.getValue(var);
        // true if the variable was newly observed, its value changed, or it is a multi-array
        if (executionState().observedVariable(varInstance, val, thread, loc.lineNumber()))
        {
          if (manager().generateArrayEvents() && val instanceof ArrayReference)
          {
            // try to dispatch a new array event with the respective cell assignments
            if (!handleNewArray((ArrayReference) val, event != null ? event.location() : loc, frame))
            {
              // if it doesn't work, process source lines that contain some array cell modification
              if (executionState().isArrayCellWriteLine(varInstance, thread, false))
              {
                visitArrayCells(event != null ? event.location() : loc, frame, (ArrayReference) val);
              }
            }
          }
          // dispatch the assignment if the variable is NOT an array whose cells is updated
          if (!executionState().isArrayCellWriteLine(varInstance, thread, true))
          {
            dispatcher().dispatchVarAssignEvent(event, frame, val, varInstance, var.typeName());
          }
        }
      }
      else
      {
        // process source array cell modification for the variable
        try
        {
          if (manager().generateArrayEvents() && var.type() instanceof ArrayType
              && varInstance.name().equals(var.name())
              && varInstance.value() instanceof IContourReference)
          {
            IContourReference cr = (IContourReference) varInstance.value();
            if (cr.contour() instanceof IObjectContour)
            {
              if (executionState().isArrayCellWriteLine(varInstance, thread, true))
              {
                processArrayCellWrite(loc, frame, (IObjectContour) cr.contour());
              }
            }
          }
        }
        catch (ClassNotLoadedException e)
        {
        }
        // true if the variable existed in the frame
        if (executionState().removeVariable(varInstance, thread))
        {
          dispatcher().dispatchVarDeleteEvent(event, frame, varInstance);
        }
      }
    }
    // process pending array field cell changes
    if (manager().generateArrayEvents() && !pendingCellWrites.isEmpty()
        && executionState().isArrayCellWriteLine(method, null, thread))
    {
      final Set<ArrayReference> toRemove = TypeTools.newHashSet();
      for (final ArrayReference arrayRef : pendingCellWrites)
      {
        if (visitArrayCells(event != null ? event.location() : loc, frame, arrayRef))
        {
          toRemove.add(arrayRef);
        }
      }
      pendingCellWrites.removeAll(toRemove);
    }
  }

  void handleExceptionThrown(final ExceptionEvent event) throws IncompatibleThreadStateException
  {
    boolean mapException = false;
    // Check if the exception was thrown in-model
    final ReferenceType throwingType = event.location().declaringType();
    if (eventFilter().acceptsType(throwingType))
    {
      mapException = true;
    }
    else
    {
      // Check if the exception wasn't caught
      final Location catchLocation = event.catchLocation();
      if (catchLocation == null)
      {
        // Check if there are any in-model frames on the stack
        if (containsInModelFrames(event.thread(), null))
        {
          mapException = true;
        }
      }
      else
      {
        // Check if the exception was caught in-model
        final ReferenceType catchingType = catchLocation.declaringType();
        if (eventFilter().acceptsType(catchingType))
        {
          mapException = true;
        }
        else
        {
          // Check if there are any in-model frames between the
          // throw and catch locations
          if (containsInModelFrames(event.thread(), catchLocation))
          {
            mapException = true;
          }
        }
      }
    }
    if (mapException)
    {
      executionState().observedException(event.thread().uniqueID(), event);
    }
  }

  /**
   * Field reads/writes are registered only for non-filtered classes . TODO: reduce the cyclomatic
   * complexity and nested block depth.
   */
  void handleFieldRead(final AccessWatchpointEvent event, final boolean generateLocals)
      throws IncompatibleThreadStateException, AbsentInformationException
  {
    // field reads from out-of-model are not interesting
    if (!eventFilter().acceptsMethod(event.location().method(), event.thread()))
    {
      return;
    }
    final StackFrame frame = determineStackFrame(event);
    // handle a pending returned event if necessary-- enum field reads is one case
    handlePendingReturned(event.location(), frame, event.thread());
    handleNewObject(frame, event.thread());
    dispatcher().dispatchFieldReadEvent(event);
    //
    // <clinit> field read in an enum type
    //
    final IMethodContour mc = executionState().retrieveTopContour(frame.thread().uniqueID());
    if (mc != null && mc.schema().modifiers().contains(NodeModifier.NM_TYPE_INITIALIZER)
        && mc.parent().schema().kind() == NodeKind.NK_ENUM)
    {
      // System.err.println("FIELD_READ_IN_ENUM_CLINIT");
      if (generateLocals)
      {
        handleLocals(null, frame, event.location());
      }
      // record the step
      dispatcher().dispatchStepEvent(event.location(), frame);
    }
    /**
     * A field read is issued prior to writing to the cell of an array field.
     */
    if (manager().generateArrayEvents() && event.object() != null)
    {
      // the field being read is an array reference
      final Value fieldValue = event.object().getValue(event.field());
      if (fieldValue instanceof ArrayReference)
      {
        // retrieve this field's object contour
        final IContextContour oc = contourFactory().lookupInstanceContour(
            event.object().type().name(), event.object().uniqueID());
        if (oc != null)
        {
          final IMethodContour method = executionState().retrieveTopContour(
              frame.thread().uniqueID());
          final IContourMember varInstance = oc.lookupMember(event.field().name());
          // the source line contains some array cell modification
          if (varInstance != null
              && executionState().isArrayCellWriteLine(method, varInstance, event.thread()))
          {
            final ArrayReference arrayRef = (ArrayReference) fieldValue;
            // TODO we may need to conservatively guarantee the existence of the array contour here
            // handleNewArray(arrayRef, event.thread());
            if (arrayRef != null && arrayRef.length() <= EventFactoryAdapter.SMALL_ARRAY_SIZE)
            {
              pendingCellWrites.add(arrayRef);
            }
          }
        }
      }
    }
  }

  /**
   * Field reads/writes are registered only for non-filtered classes.
   */
  void handleFieldWrite(final ModificationWatchpointEvent event)
      throws IncompatibleThreadStateException, AbsentInformationException
  {
    final StackFrame frame = determineStackFrame(event);
    handleNewObject(frame, event.thread());
    // dispatch a new object for the array value, if appropriate
    if (manager().generateArrayEvents() && event.valueToBe() instanceof ArrayReference)
    {
      // dispatch a new array event and the respective cell assignments
      handleNewArray((ArrayReference) event.valueToBe(), event.location(), frame);
    }
    dispatcher().dispatchFieldWriteEvent(event);
  }

  void handleLocals(final LocatableEvent event, final StackFrame frame, final Location location)
      throws AbsentInformationException
  {
    // checks for modified and deleted (out-of-scope) local variables
    if (!location.method().isNative()
        && executionState().containsTopFrameContour(frame.thread().uniqueID()))
    {
      try
      {
        final IMethodContour method = executionState()
            .retrieveTopContour(frame.thread().uniqueID());
        // arguments are visited on the first line of the method
        if (!executionState().visitedArguments(method))
        {
          visitArguments(event, frame);
        }
        else
        {
          visitLocals(event, frame, location);
        }
      }
      catch (final AbsentInformationException aie)
      {
        System.err.println("missing locals information for " + location.method().toString());
        throw aie;
      }
    }
  }

  /**
   * Filtered method calls/returns are captured lazily by determineStackFrame(event).
   */
  void handleMethodEntry(final MethodEntryEvent event, final boolean generateLocals)
      throws IncompatibleThreadStateException, AbsentInformationException
  {
    if (!eventFilter().acceptsMethod(event.method(), event.thread()))
    {
      return;
    }
    // adjust the stack frames if necessary
    final StackFrame frame = determineStackFrame(event);
    // handle a pending returned event if necessary
    handlePendingReturned(event.location(), frame, event.thread());
    // record newly observed types and objects if necessary
    handleNewObject(frame, event.thread());
    // resolve the method
    resolveMethod(frame, event.method());
    // record the method call
    dispatcher().dispatchInModelCallEvent(event, frame);
    // handle locals if necessary
    if (generateLocals)
    {
      handleLocals(null, frame, event.location());
    }
  }

  /**
   * Filtered method calls/returns are captured lazily by determineStackFrame(event).
   */
  void handleMethodExit(final MethodExitEvent event, final boolean generateLocals)
      throws IncompatibleThreadStateException, AbsentInformationException
  {
    if (!eventFilter().acceptsMethod(event.method(), event.thread()))
    {
      return;
    }
    final StackFrame frame = determineStackFrame(event);
    // local variable writes on the return line could be missed without this
    if (generateLocals)
    {
      handleLocals(null, frame, event.location());
    }
    // handle a pending returned event if necessary
    handlePendingReturned(event.location(), frame, event.thread());
    if (!event.method().returnTypeName().equalsIgnoreCase(JDIEventHandlerDelegate.VOID_TYPE_NAME))
    {
      final Value result = event.returnValue();
      if (manager().generateArrayEvents() && result instanceof ArrayReference)
      {
        // dispatch a new array event and the respective cell assignments
        handleNewArray((ArrayReference) result, event.location(), frame);
      }
      // dispatch a result event
      dispatcher().dispatchMethodResultEvent(event);
    }
    // unregister this contour's local variables
    if (generateLocals && !frame.location().method().isNative())
    {
      removeLocals(frame);
    }
    // dispatch the event
    dispatcher().dispatchMethodExitEvent(event);
  }

  boolean handleNewArray(final ArrayReference array, final Location location, final StackFrame frame)
  {
    if (array == null || !manager().generateArrayEvents())
    {
      return false;
    }
    // handle the instantiation of of small arrays
    if (array != null && array.length() <= EventFactoryAdapter.SMALL_ARRAY_SIZE
        && contourFactory().lookupInstanceContour(array.type().name(), array.uniqueID()) == null)
    {
      handleTypeLoad((ReferenceType) array.type(), frame.thread());
      // a specialized new event handles the immutable length of the array
      manager().jiveDispatcher().dispatchNewEvent(array, frame.thread(), array.length());
      visitArrayCells(location, frame, array);
      return true;
    }
    return false;
  }

  boolean handleNewObject(final ObjectReference object, final ThreadReference thread)
  {
    if (object == null)
    {
      return false;
    }
    if (contourFactory().lookupInstanceContour(object.type().name(), object.uniqueID()) == null)
    {
      manager().jiveDispatcher().dispatchNewEvent(object, thread, -1);
      return true;
    }
    return false;
  }

  /**
   * Instance processing creates all the necessary static and instance schemas, dispatches any
   * number of type load events followed by at most one new object event, if necessary.
   */
  void handleNewObject(final StackFrame frame, final ThreadReference thread)
  {
    final Location location = frame.location();
    if (location == null)
    {
      return;
    }
    final Method method = location.method();
    if (method == null)
    {
      return;
    }
    handleTypeLoad(method.declaringType(), thread);
    final ObjectReference object = frame.thisObject();
    handleNewObject(object, thread);
  }

  /**
   * No steps from filtered methods.
   */
  void handleStep(final StepEvent event, final boolean generateLocals)
      throws IncompatibleThreadStateException, AbsentInformationException
  {
    if (!eventFilter().acceptsStep(event))
    {
      return;
    }
    // adjust the stack frames if necessary
    final StackFrame frame = determineStackFrame(event);
    // no support for out-of-model step events
    if (!eventFilter().acceptsMethod(frame.location().method(), frame.thread()))
    {
      return;
    }
    // handle a pending array result
    if (manager().generateArrayEvents()
        && executionState().lookupArrayResult(frame.thread()) != null)
    {
      processArrayCellWrite(event.location(), frame,
          executionState().lookupArrayResult(frame.thread()));
      executionState().removeArrayResult(frame.thread());
    }
    // handle a pending returned event if necessary
    final boolean withPendingReturn = handlePendingReturned(event.location(), frame, event.thread());
    // handle locals if necessary-- a write is inferred *after* it has been performed
    if (generateLocals && !withPendingReturn) 
    {
      /**
       * if a pending return was processed, VarAssign/VarDelete events should rely on the locatable
       * event for their source location.
       */
      handleLocals(withPendingReturn ? event : null, frame, event.location());
    }
    // record the step
    dispatcher().dispatchStepEvent(event.location(), frame);
  }

  void handleThreadDeath(final ThreadDeathEvent event)
  {
    final ThreadReference thread = event.thread();
    /**
     * If a thread dies with outstanding frames, then all frames are popped with an exception. An
     * unexpected termination is gracefully handled by the execution model.
     */
    while (executionState().frameCount(thread.uniqueID()) != 0)
    {
      // if by any chance the VM generated a throw event
      if (executionState().containsException(thread.uniqueID()))
      {
        final ExceptionEvent exception = outstandingException(thread);
        dispatcher().dispatchThrowEvent(exception, true);
      }
      else
      {
        dispatcher().dispatchThrowEvent(thread, true);
      }
    }
    if (executionState().containsException(thread.uniqueID()))
    {
      removeException(thread);
    }
    handlePendingReturned(null, null, event.thread());
    final IThreadValue threadValue = owner.model().valueFactory()
        .createThread(thread.uniqueID(), thread.name());
    // avoid duplicate thread termination
    final IThreadStartEvent threadStart = owner.model().lookupThread(threadValue);
    if (threadStart != null && threadStart.terminator() == null)
    {
      dispatcher().dispatchThreadDeath(event.thread());
    }
  }

  /**
   * Class processing creates all the necessary static and instance schemas, and dispatches any
   * necessary class load events, from the most general to the most specific class.
   */
  void handleTypeLoad(final ReferenceType type, final ThreadReference thread)
  {
    if (type instanceof ClassType)
    {
      handleClassLoad((ClassType) type, thread);
    }
    else if (type instanceof InterfaceType)
    {
      handleInterfaceLoad((InterfaceType) type, thread);
    }
    // resolve the array's type
    else if (manager().generateArrayEvents() && type instanceof ArrayType)
    {
      final ArrayType at = (ArrayType) type;
      try
      {
        if (at.componentType() instanceof ReferenceType)
        {
          handleTypeLoad(((ReferenceType) at.componentType()), thread);
        }
        resolveType(type);
      }
      catch (final ClassNotLoadedException e)
      {
        System.err.println("TYPE_NOT_LOADED_FOR_ARRAY[" + type.signature() + "]");
        // e.printStackTrace();
        String componentTypeName = type.signature();
        componentTypeName = componentTypeName.substring(componentTypeName.lastIndexOf('[') + 1);
        final ITypeNodeRef result = staticModelFactory().lookupTypeNode(componentTypeName);
        if (result != null)
        {
          System.err.println("TYPE_LOADED_FOR_ARRAY[" + type.signature() + "]");
          resolveType(type);
        }
        else
        {
          System.err.println("IGNORING_COMPONENT_TYPE_FOR_ARRAY[" + type.signature() + "]");
          resolveType(type);
        }
      }
    }
  }

  boolean isInModel(final ReferenceType classType)
  {
    return manager().modelFilter().acceptsType(classType);
  }

  void reset()
  {
    adapterForAST = null;
    noInfoTypes.clear();
  }

  /**
   * Creates a node for this method, if one does not exist.
   */
  IMethodNode resolveMethod(final StackFrame frame, final Method method)
  {
    final String methodKey = executionState().methodKey(method);
    // resolve the declaring class for the method
    final ITypeNode type = resolveType(method.declaringType()).node();
    // lookup the method
    final IMethodNode result = staticModelFactory().lookupMethodNode(methodKey);
    // null is returned only for JDI methods
    if (result != null)
    {
      return result;
    }
    // the adapter creates static model nodes using AST when possible and JDI otherwise
    final IStaticModelDelegate adapter = adapterForAST(method.virtualMachine());
    // create and return the missing method node
    return adapter.resolveMethod(type, method, methodKey);
  }
}