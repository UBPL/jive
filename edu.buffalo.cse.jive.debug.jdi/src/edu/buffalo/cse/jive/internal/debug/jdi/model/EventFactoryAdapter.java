package edu.buffalo.cse.jive.internal.debug.jdi.model;

import java.util.ArrayList;
import java.util.List;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.CharType;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.AccessWatchpointEvent;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.ModificationWatchpointEvent;

import edu.buffalo.cse.jive.debug.JiveDebugPlugin;
import edu.buffalo.cse.jive.debug.jdi.model.IExecutionState;
import edu.buffalo.cse.jive.debug.jdi.model.IJDIManager;
import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IContourModel.IObjectContour;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodExitEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodTerminatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.LockOperation;
import edu.buffalo.cse.jive.model.IModel.ILineValue;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;
import edu.buffalo.cse.jive.model.IModel.IValue;
import edu.buffalo.cse.jive.model.IStaticModel.IDataNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.model.factory.IContourFactory;
import edu.buffalo.cse.jive.model.factory.IEventFactory;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory;
import edu.buffalo.cse.jive.model.factory.IValueFactory;

/**
 * This adapter users JDI model elements to create Jive events that do not depend on the JDI model.
 */
final class EventFactoryAdapter
{
  private static final String VOID_TYPE_NAME = "VOID";
  public static final int SMALL_ARRAY_SIZE = 50;
  public static final boolean PROCESS_MULTI_ARRAY = true;
  private final IJiveDebugTarget owner;

  EventFactoryAdapter(final IJiveDebugTarget owner)
  {
    this.owner = owner;
  }

  public IJiveEvent createDestroyEvent(final ThreadReference thread, final long oid)
  {
    final IThreadValue threadId = resolveThread(thread);
    final IObjectContour contour = executionState().deleteObject(oid);
    return eventFactory().createDestroyEvent(threadId, valueFactory().createUnavailableLine(),
        contour);
  }

  private IContourFactory contourFactory()
  {
    return manager().executionModel().contourFactory();
  }

  private IValue createCaller(final ThreadReference thread)
  {
    if (executionState().frameCount(thread.uniqueID()) == 0)
    {
      return valueFactory().createSystemCaller();
    }
    final StackFrame frame = executionState().framePeek(thread.uniqueID());
    // in-model caller
    if (executionState().containsInModelFrame(frame))
    {
      // encapsulate the method contour in a reference
      return valueFactory().createReference(executionState().lookupContour(frame));
    }
    // out-of-model caller; provide a reference to the top in-model method frame
    else
    {
      final String description = createDescription(frame);
      // find the top in-model frame on the stack
      final StackFrame top = executionState().framePeekInModel(frame.thread().uniqueID());
      // find the corresponding method contour identifier
      if (top != null)
      {
        final IMethodContour method = executionState().lookupContour(top);
        // encapsulate the description and top in-model method contour in a reference
        return valueFactory().createOutOfModelMethodReference(description, method);
      }
      final String methodKey = executionState().methodKey(frame.location().method());
      // out-of-model caller has a simple textual description
      return valueFactory().createOutOfModelMethodKeyReference(description, methodKey);
    }
  }

  private String createDescription(final StackFrame frame)
  {
    final Method method = frame.location().method();
    return executionState().methodName(method);
  }

  private IValue createSyntheticTarget(final StackFrame frame)
  {
    // synthetic thread class schema
    final ITypeNode schema = staticModelFactory().lookupSnapshotType();
    // synthetic thread context
    final IContextContour context = contourFactory().lookupStaticContour(schema.name());
    // run method node
    final IMethodNode node = schema.methodMembers().get(0);
    // Jive thread
    final IThreadValue threadId = resolveThread(frame);
    // create the method contour
    final IMethodContour method = context.createMethodContour(node, threadId);
    // create the in-target call
    return valueFactory().createReference(method);
  }

  private IValue createTarget(final StackFrame frame, final boolean inModel,
      final boolean createLocals)
  {
    // push the frame
    final StackFrame f = executionState().framePush(frame.thread().uniqueID(), frame, inModel);
    // Jive thread
    final IThreadValue threadId = resolveThread(f);
    // calling an in-model method
    if (executionState().containsInModelFrame(f))
    {
      // JDI method
      final Method m = f.location().method();
      // calling context
      final IContextContour context = executionState().retrieveContext(f);
      // retrieve the method node
      final IMethodNode node = staticModelFactory().lookupMethodNode(executionState().methodKey(m));
      // create the method contour
      final IMethodContour method = context.createMethodContour(node, threadId);
      // push the frame onto the stack to associate it with the method contour
      executionState().observedMethod(f, method);
      // create the in-target call
      return valueFactory().createReference(method);
    }
    // calling an out-of-model method
    else
    {
      final String methodKey = executionState().methodKey(f.location().method());
      // synthetic accessors-- keep them on the current line
      if (methodKey.contains(".access$") && f.location().method().isSynthetic())
      {
        // System.err.println("SYNTHETIC_ACCESSOR_METHOD_CALL[" + methodKey + "]");
        // push the accessor onto the stack to associate it with the method exit
        executionState().observedSyntheticAccessor(methodKey,
            executionState().currentLine(threadId));
      }
      // out-of-model target method has a description of the out-of-model RPDL
      return valueFactory().createOutOfModelMethodKeyReference(createDescription(f), methodKey);
    }
  }

  private IValue createThrower(final StackFrame frame)
  {
    if (executionState().containsInModelFrame(frame))
    {
      return valueFactory().createReference(executionState().lookupContour(frame));
    }
    final String methodKey = executionState().methodKey(frame.location().method());
    // out-of-model thrower has a description of the out-of-model method
    return valueFactory().createOutOfModelMethodKeyReference(createDescription(frame), methodKey);
  }

  /**
   * TODO: reduce the cyclomatic complexity and nested block depth.
   */
  private IValue createValue(final ThreadReference thread, final ObjectReference object,
      final String typeName)
  {
    if (object.type() instanceof ClassType)
    {
      final ClassSummary summary = new ClassSummary(typeName, object);
      if (summary.isResolved())
      {
        String value;
        // Enum type is resolved to its name
        if (summary.isEnum)
        {
          Value val = null;
          try
          {
            // do not call this during a snapshot
            if (thread.uniqueID() > 0)
            {
              final Method toString = ((ClassType) object.type()).methodsByName("name").get(0);
              val = object.invokeMethod(thread, toString, new ArrayList(),
                  ObjectReference.INVOKE_SINGLE_THREADED);
            }
          }
          catch (final Exception e)
          {
            val = null;
          }
          if (val != null)
          {
            value = val.toString();
            value = value.substring(1, value.length() - 1);
          }
          else
          {
            value = object.toString();
          }
        }
        // String is resolved and escaped
        else if (summary.isString)
        {
          value = object.toString();
        }
        // Date is resolved
        else if (summary.isDate)
        {
          Value val = null;
          try
          {
            // do not call this during a snapshot
            if (thread.uniqueID() > 0)
            {
              final Method toString = ((ClassType) object.type()).methodsByName("toString").get(0);
              val = object.invokeMethod(thread, toString, new ArrayList(),
                  ObjectReference.INVOKE_SINGLE_THREADED);
            }
          }
          catch (final Exception e)
          {
            val = null;
          }
          if (val != null)
          {
            value = val.toString();
          }
          else
          {
            value = object.toString();
          }
        }
        else
        {
          // retrieve the primitive field "private final XX value" of boxed types
          // Boolean, Byte, Char, Double, Float, Integer, Long, Short
          value = object.getValue(((ClassType) object.type()).fieldByName("value")).toString();
        }
        // escaped values
        if (summary.isEscaped)
        {
          value = escapeStringValue(value);
        }
        // declared and actual types match
        if (summary.matchingTypes)
        {
          return valueFactory().createResolvedValue(value, "");
        }
        // declared and actual types do not match
        return valueFactory().createResolvedValue(value, summary.actualTypeName);
      }
    }
    // else if (type instanceof ArrayType) {
    // final ArrayType arrayType = (ArrayType) type;
    // try {
    // boolean isResolved = false;
    // if (arrayType.componentType() instanceof PrimitiveType) {
    // isResolved = true;
    // }
    // else if (arrayType.componentType() instanceof ClassType) {
    // final ComponentSummary summary = new ComponentSummary(
    // (ClassType) arrayType.componentType());
    // isResolved = summary.isResolved();
    // }
    // if (isResolved) {
    // final ArrayReference arrayRef = (ArrayReference) object;
    // StringBuffer b = new StringBuffer("(");
    // if (arrayRef != null && arrayRef.length() > 0) {
    // final List<Value> vals = ((ArrayReference) object).getValues();
    // for (int i = 0; i < vals.size(); i++) {
    // final Value val = vals.get(i);
    // b.append(val == null ? "null" : val.toString());
    // if (i < vals.size() - 1) {
    // b.append(',');
    // }
    // }
    // }
    // b.append(')');
    // return modelFactory().createResolvedValue(b.toString(), "");
    // }
    // }
    // catch (ClassNotLoadedException e) {
    // }
    // }
    // non-resolved out-of-model value
    return valueFactory().createOutOfModelValue(object.toString());
  }

  private String escapeStringValue(final String value)
  {
    String result = "";
    for (final char ch : value.toCharArray())
    {
      if (Character.isWhitespace(ch))
      {
        result += " ";
      }
      else
      {
        result += ch;
      }
    }
    return result;
  }

  private IEventFactory eventFactory()
  {
    return manager().executionModel().eventFactory();
  }

  private IExecutionState executionState()
  {
    return manager().executionState();
  }

  private int fieldIndex(final Field field)
  {
    return field.declaringType().fields().indexOf(field);
  }

  private IJDIManager manager()
  {
    return JiveDebugPlugin.getDefault().jdiManager(owner);
  }

  @SuppressWarnings("unchecked")
  private IContourMember resolveCatchVariable(final ExceptionEvent event, final StackFrame frame,
      final IMethodContour catcher)
  {
    final Method method = frame.location().method();
    final ReferenceType type = method.declaringType();
    if (method.isNative())
    {
      final String message = "Unable to resolve catch variable: catcher " + type.name() + "."
          + method.name() + " is native";
      JiveDebugPlugin.info(message);
      return null;
    }
    final ObjectReference exception = event.exception();
    if (exception == null)
    {
      return null;
    }
    // try to resolve using JDI
    try
    {
      final List<LocalVariable> localVariables = frame.visibleVariables();
      for (final LocalVariable variable : localVariables)
      {
        if (exception.equals(frame.getValue(variable)))
        {
          return catcher.lookupMember(method.variables().indexOf(variable));
        }
      }
    }
    catch (final AbsentInformationException e)
    {
      JiveDebugPlugin.log(e);
    }
    // try to resolve using static information only
    for (final IContourMember var : catcher.members())
    {
      if (var.schema().lineFrom() == frame.location().lineNumber()
          && var.schema().modifiers().contains(NodeModifier.NM_CATCH_VARIABLE))
      {
        return var;
      }
    }
    final String message = "Unable to resolve catch variable: no matching variable found.";
    JiveDebugPlugin.warn(message);
    return null;
  }

  private IContextContour resolveContext(final Field field, final ObjectReference object)
  {
    final String declaringTypeName = field.declaringType().name();
    if (field.isStatic())
    {
      return contourFactory().lookupStaticContour(declaringTypeName);
    }
    else
    {
      return contourFactory().lookupInstanceContour(declaringTypeName, object.uniqueID());
    }
  }

  private ILineValue resolveLine(final Location location)
  {
    if (location != null)
    {
      try
      {
        final String fileName = location.sourcePath();
        final int lineNumber = location.lineNumber();
        return staticModelFactory().lookupLine(fileName, lineNumber);
      }
      catch (final AbsentInformationException e)
      {
      }
    }
    return valueFactory().createUnavailableLine();
  }

  private ILineValue resolveLine(final StackFrame frame)
  {
    if (frame != null)
    {
      return resolveLine(frame.location());
    }
    return valueFactory().createUnavailableLine();
  }

  private IValue resolvePrimitive(final com.sun.jdi.Value value)
  {
    if (value == null)
    {
      return valueFactory().createNullValue();
    }
    final com.sun.jdi.Type t = value.type();
    if (t instanceof PrimitiveType)
    {
      if (t instanceof CharType)
      {
        return valueFactory().createPrimitiveValue(escapeStringValue(value.toString()));
      }
      return valueFactory().createPrimitiveValue(value.toString());
    }
    return null;
  }

  private IValue resolveReference(final ThreadReference thread, final ObjectReference object,
      final String typeName)
  {
    if (object == null)
    {
      return valueFactory().createNullValue();
    }
    final ReferenceType type = (ReferenceType) object.type();
    // if (type instanceof ClassType && ((ClassType) type).isEnum()) {
    // // out-of-model value
    // return createValue(thread, type, object, typeName);
    // }
    // if the object reference has a matching contour, return it
    if (contourFactory().lookupInstanceContour(type.name(), object.uniqueID()) != null)
    {
      final IContextContour instance = contourFactory().retrieveInstanceContour(type.name(),
          object.uniqueID());
      // in-model value
      return valueFactory().createReference(instance);
    }
    // out-of-model value
    return createValue(thread, object, typeName);
  }

  private IValue resolveReturnValue(final MethodExitEvent event)
  {
    if (event.method().returnTypeName().equalsIgnoreCase(EventFactoryAdapter.VOID_TYPE_NAME))
    {
      return null;
    }
    // Java SE 6 or later supports method return values
    if (event.virtualMachine().canGetMethodReturnValues())
    {
      return resolveValue(event.thread(), event.returnValue(), event.method().returnTypeName());
    }
    // Support for J2SE 1.5
    else
    {
      return valueFactory().createUninitializedValue();
    }
  }

  private IThreadValue resolveThread(final LocatableEvent object)
  {
    return valueFactory().createThread(object.thread().uniqueID(), object.thread().name());
  }

  private IThreadValue resolveThread(final StackFrame object)
  {
    return valueFactory().createThread(object.thread().uniqueID(), object.thread().name());
  }

  private IThreadValue resolveThread(final ThreadReference object)
  {
    return valueFactory().createThread(object.uniqueID(), object.name());
  }

  private IValue resolveThrower(final ThreadReference thread, final boolean popFrame)
  {
    final IValue result = createThrower(executionState().framePeek(thread.uniqueID()));
    if (popFrame)
    {
      executionState().framePop(thread.uniqueID());
    }
    return result;
  }

  private IValue resolveValue(final ThreadReference thread, final com.sun.jdi.Value val,
      final String typeName)
  {
    // try to resolve as a primitive
    IValue newValue = resolvePrimitive(val);
    // try to resolve as a reference
    if (newValue == null)
    {
      assert (val instanceof ObjectReference);
      newValue = resolveReference(thread, (ObjectReference) val, typeName);
    }
    return newValue;
  }

  private IStaticModelFactory staticModelFactory()
  {
    return owner.model().staticModelFactory();
  }

  private IValueFactory valueFactory()
  {
    return manager().executionModel().valueFactory();
  }

  IJiveEvent createArrayCellWriteEvent(final Location location, final ThreadReference thread,
      final IContextContour array, final IContourMember cell, final Value cellValue,
      final String componentTypeName)
  {
    final IThreadValue threadId = resolveThread(thread);
    final IValue newValue = resolveValue(thread, cellValue, componentTypeName);
    // update the source location
    // executionState().nextLine(threadId, resolveLine(location));
    final ILineValue line = executionState().currentLine(threadId);
    return eventFactory().createFieldWriteEvent(threadId, line, array, newValue, cell);
  }

  IJiveEvent createCatchEvent(final StackFrame catchFrame, final ExceptionEvent event)
  {
    final IThreadValue threadId = resolveThread(catchFrame);
    final StackFrame frame = executionState().framePeek(catchFrame.thread().uniqueID());
    if (!executionState().containsInModelFrame(frame))
    {
      throw new IllegalStateException("A method contour was not found for the given stack frame.");
    }
    final IMethodContour catcher = executionState().lookupContour(frame);
    final IValue exception = resolveReference(event.thread(), event.exception(), null);
    final IContourMember variable = resolveCatchVariable(event, catchFrame, catcher);
    // update the source location-- no harm done if the location hasn't changed
    executionState().nextLine(threadId, resolveLine(event.catchLocation()));
    final ILineValue line = executionState().currentLine(threadId);
    return eventFactory().createExceptionCatchEvent(threadId, line, exception, variable);
  }

  IJiveEvent createFieldReadEvent(final AccessWatchpointEvent event)
  {
    final IThreadValue threadId = resolveThread(event);
    final IContextContour context = resolveContext(event.field(), event.object());
    // cannot use the field index directly on the context because the context has
    // either static or instance fields, not both as JDI
    final IDataNode memberSchema = context.schema().dataMembers().get(fieldIndex(event.field()));
    final IContourMember field = context.lookupMember(memberSchema);
    // field read lines may get out of synch during initialization, e.g., of enums
    executionState().nextLine(threadId, resolveLine(event.location()));
    final ILineValue line = executionState().currentLine(threadId);
    return eventFactory().createFieldReadEvent(threadId, line, context, field);
  }

  IJiveEvent createFieldWriteEvent(final ModificationWatchpointEvent event)
  {
    final IThreadValue threadId = resolveThread(event);
    final IContextContour context = resolveContext(event.field(), event.object());
    // cannot use the field index directly on the context because the context has
    // either static or instance fields, not both as JDI
    final IDataNode memberSchema = context.schema().dataMembers().get(fieldIndex(event.field()));
    final IContourMember field = context.lookupMember(memberSchema);
    final IValue newValue = resolveValue(event.thread(), event.valueToBe(), event.field()
        .typeName());
    final ILineValue line;
    final String methodKey = executionState().methodKey(event.location().method());
    if (methodKey.contains(".access$") && event.location().method().isSynthetic())
    {
      // System.err.println("SYNTHETIC_ACCESSOR_FIELD_WRITE[" + methodKey + "]");
      // retrieve the source location at the origin of the synthetic accessor call
      line = executionState().lookupSyntheticAccessorLine(methodKey);
    }
    else
    {
      // update the source location
      executionState().nextLine(threadId, resolveLine(event.location()));
      line = executionState().currentLine(threadId);
    }
    return eventFactory().createFieldWriteEvent(threadId, line, context, newValue, field);
  }

  IJiveEvent createLineStepEvent(final Location location, final StackFrame frame)
      throws AbsentInformationException
  {
    final IThreadValue threadId = resolveThread(frame);
    // record the new location
    executionState().nextLine(threadId, resolveLine(location));
    final ILineValue line = executionState().currentLine(threadId);
    return eventFactory().createLineStepEvent(threadId, line);
  }

  IJiveEvent createLockEvent(final ThreadReference thread, final LockOperation lockOperation,
      final IContour lock, final String lockDescription)
  {
    final IThreadValue threadId = resolveThread(thread);
    final StackFrame frame = executionState().framePeek(thread.uniqueID());
    final ILineValue line = executionState().nextLine(threadId, resolveLine(frame));
    return eventFactory().createLockEvent(threadId, line, lockOperation, lock, lockDescription);
  }

  IMethodCallEvent createMethodCallEvent(final MethodEntryEvent event,
      final StackFrame targetFrame, final boolean inModel, final boolean generateLocals)
  {
    final IThreadValue threadId = resolveThread(targetFrame);
    final IValue caller = createCaller(targetFrame.thread());
    final IValue target = createTarget(targetFrame, inModel, generateLocals);
    // defensively compute the call location
    ILineValue line = null;
    try
    {
      final ThreadReference thread = event != null ? event.thread() : targetFrame.thread();
      // if a frame is available with the call site, use it
      if (thread != null && thread.frameCount() > 1)
      {
        line = resolveLine(thread.frame(1));
      }
    }
    catch (final IncompatibleThreadStateException e)
    {
    }
    // record the called location
    executionState().nextLine(threadId,
        event == null ? resolveLine(targetFrame) : resolveLine(event.location()));
    if (line == null)
    {
      line = executionState().currentLine(threadId);
    }
    return (IMethodCallEvent) eventFactory().createMethodCallEvent(threadId, line, caller, target);
  }

  IJiveEvent createMethodEnterEvent(final IMethodCallEvent event, final ThreadReference thread)
  {
    ILineValue line = null;
    // try to resolve the method declaration statically
    if (event.execution().schema().lineFrom() != -1)
    {
      final StackFrame frame = executionState().framePeek(thread.uniqueID());
      String fileName;
      try
      {
        fileName = frame.location().sourcePath();
        final IMethodNode method = event.execution().schema();
        final int lineNumber = method.lineFrom();
        line = staticModelFactory().lookupLine(fileName, lineNumber);
      }
      catch (final AbsentInformationException e)
      {
        line = null;
      }
    }
    if (line == null)
    {
      line = executionState().currentLine(event.thread());
    }
    // defensively record the location-- no harm done if the location hasn't changed
    executionState().nextLine(event.thread(), line);
    return eventFactory().createMethodEnteredEvent(event.thread(), line);
  }

  IJiveEvent createMethodExitEvent(final MethodExitEvent event)
  {
    final IThreadValue threadId = resolveThread(event);
    ILineValue line = executionState().currentLine(threadId);
    final String methodKey = executionState().methodKey(event.location().method());
    if (methodKey.contains(".access$") && event.location().method().isSynthetic())
    {
      // System.err.println("SYNTHETIC_ACCESSOR_METHOD_EXIT[" + methodKey + "]");
      // retrieve the source location at the origin of the synthetic accessor call
      line = executionState().removeSyntheticAccessor(methodKey);
    }
    final IMethodTerminatorEvent terminator = (IMethodTerminatorEvent) eventFactory()
        .createMethodExitEvent(threadId, line);
    executionState().methodReturnedPending(event.thread().uniqueID(), terminator);
    executionState().framePop(event.thread().uniqueID());
    return terminator;
  }

  IJiveEvent createMethodExitEvent(final ThreadReference thread)
  {
    final IThreadValue threadId = resolveThread(thread);
    final boolean isTopInModel = executionState().containsInModelFrame(
        executionState().framePeek(thread.uniqueID()));
    final StackFrame popped = executionState().framePop(thread.uniqueID());
    // improved location resolution
    StackFrame activation;
    try
    {
      activation = thread.frames().get(0);
    }
    catch (final IncompatibleThreadStateException e)
    {
      activation = executionState().framePeek(threadId.id());
    }
    // only when this method is called from the thread death branch
    if (activation != null)
    {
      // defensively record the location-- no harm done if the location hasn't changed
      executionState().nextLine(threadId, resolveLine(activation.location()));
    }
    ILineValue line = executionState().currentLine(threadId);
    final String methodKey = executionState().methodKey(popped.location().method());
    if (methodKey.contains(".access$") && popped.location().method().isSynthetic())
    {
      // System.err.println("SYNTHETIC_ACCESSOR_METHOD_EXIT[" + methodKey + "]");
      // retrieve the source location at the origin of the synthetic accessor call
      line = executionState().removeSyntheticAccessor(methodKey);
    }
    final IMethodTerminatorEvent terminator = (IMethodTerminatorEvent) eventFactory()
        .createMethodExitEvent(threadId, line);
    // if the last frame was in-model, we must push it back in order to get the returned event
    if (executionState().framePeek(thread.uniqueID()) == null && isTopInModel)
    {
      executionState().framePush(thread.uniqueID(), popped, true);
    }
    executionState().methodReturnedPending(thread.uniqueID(), terminator);
    return terminator;
  }

  IJiveEvent createMethodResultEvent(final MethodExitEvent event)
  {
    final IThreadValue threadId = resolveThread(event);
    final StackFrame activation = executionState().framePeek(event.thread().uniqueID());
    final IMethodContour method = executionState().lookupContour(activation);
    final IValue value = resolveReturnValue(event);
    final IContourMember instance = method.lookupResultMember();
    // defensively record the location-- no harm done if the location hasn't changed
    executionState().nextLine(threadId, resolveLine(event.location()));
    final ILineValue line = executionState().currentLine(threadId);
    return eventFactory().createVarAssignEvent(threadId, line, value, instance);
  }

  IJiveEvent createMethodReturnedEvent(final ThreadReference thread)
  {
    // record the current location-- necessary for variable assignments to method return values
    final IThreadValue threadId = resolveThread(thread);
    final StackFrame frame = executionState().framePeek(thread.uniqueID());
    executionState().nextLine(threadId, resolveLine(frame));
    final IMethodTerminatorEvent terminator = executionState().methodReturnedProcessed(
        thread.uniqueID());
    return eventFactory().createMethodReturnedEvent(terminator);
  }

  IJiveEvent createNewObjectEvent(final ObjectReference object, final ThreadReference thread,
      final int length)
  {
    final IThreadValue threadId = resolveThread(thread);
    // retrieve the object's type
    final ReferenceType type = object.referenceType();
    // this objec't type has been loaded so its type node must exist
    final ITypeNode schema = staticModelFactory().lookupTypeNode(type.signature());
    if (schema == null)
    {
      System.err.println("TYPE_NOT_FOUND[" + type.signature() + "]");
    }
    // create an instance contour
    final IObjectContour contour = length == -1 ? schema.createInstanceContour(object.uniqueID())
        : schema.createArrayContour(object.uniqueID(), length);
    // map the contour to its object identifier
    executionState().observedContour(contour, object.uniqueID());
    // last recorded line
    final ILineValue line = executionState().currentLine(threadId);
    // create and return a new object event
    return eventFactory().createNewObjectEvent(threadId, line, contour);
  }

  IJiveEvent createSyntheticFieldWriteEvent(final ThreadReference thread,
      final ObjectReference object, final Field field, final Value valueToBe)
  {
    final IThreadValue threadId = resolveThread(thread);
    final IContextContour context = resolveContext(field, object);
    final IValue newValue = resolveValue(thread, valueToBe, field.typeName());
    final IDataNode memberSchema = context.schema().dataMembers().get(fieldIndex(field));
    final IContourMember member = context.lookupMember(memberSchema);
    return eventFactory().createFieldWriteEvent(threadId, valueFactory().createUnavailableLine(),
        context, newValue, member);
  }

  IMethodCallEvent createSyntheticMethodCallEvent(final StackFrame frame)
  {
    final IThreadValue thread = resolveThread(frame);
    final IValue caller = valueFactory().createSystemCaller();
    final IValue target = createSyntheticTarget(frame);
    return (IMethodCallEvent) eventFactory().createMethodCallEvent(thread,
        valueFactory().createUnavailableLine(), caller, target);
  }

  IMethodExitEvent createSyntheticMethodExitEvent(final StackFrame frame)
  {
    final IThreadValue threadId = resolveThread(frame.thread());
    return (IMethodExitEvent) eventFactory().createMethodExitEvent(threadId,
        valueFactory().createUnavailableLine());
  }

  IJiveEvent createSyntheticMethodReturned(final IMethodExitEvent terminator)
  {
    return eventFactory().createMethodReturnedEvent(terminator);
  }

  IJiveEvent createSyntheticObjectLoadEvent(final ThreadReference thread)
  {
    final IThreadValue threadId = resolveThread(thread);
    final IContextContour object = staticModelFactory().lookupObjectType().createStaticContour();
    return eventFactory().createTypeLoadEvent(threadId, valueFactory().createUnavailableLine(),
        object);
  }

  IJiveEvent createSyntheticTypeLoadEvent(final ThreadReference thread)
  {
    final IThreadValue threadId = resolveThread(thread);
    final IContextContour snapshot = staticModelFactory().lookupSnapshotType()
        .createStaticContour();
    return eventFactory().createTypeLoadEvent(threadId, valueFactory().createUnavailableLine(),
        snapshot);
  }

  IJiveEvent createSystemExitEvent()
  {
    return eventFactory().createSystemExitEvent();
  }

  IJiveEvent createThreadEndEvent(final ThreadReference thread)
  {
    return eventFactory().createThreadEndEvent(resolveThread(thread));
  }

  IJiveEvent createThrowEvent(final ExceptionEvent event, final boolean framePopped)
  {
    final IThreadValue threadId = resolveThread(event);
    final IValue thrower = resolveThrower(event.thread(), framePopped);
    final IValue exception = resolveReference(event.thread(), event.exception(), null);
    // defensively record the location-- no harm done if the location hasn't changed
    final ILineValue nextLine = resolveLine(event.location());
    if (nextLine != valueFactory().createUnavailableLine())
    {
      executionState().nextLine(threadId, nextLine);
    }
    final ILineValue line = executionState().currentLine(threadId);
    return eventFactory()
        .createExceptionThrowEvent(threadId, line, exception, thrower, framePopped);
  }

  IJiveEvent createThrowEvent(final ThreadReference thread, final boolean framePopped)
  {
    final IThreadValue threadId = resolveThread(thread);
    final IValue thrower = resolveThrower(thread, framePopped);
    final IValue exception = resolveReference(thread, null, null);
    final ILineValue line = executionState().currentLine(threadId);
    return eventFactory()
        .createExceptionThrowEvent(threadId, line, exception, thrower, framePopped);
  }

  IJiveEvent createThrowEvent(final ThreadReference thread, final ObjectReference ref)
  {
    final IThreadValue threadId = resolveThread(thread);
    final boolean wasFramePopped = false;
    final IValue thrower = resolveThrower(thread, false);
    final IValue exception = resolveReference(thread, ref, null);
    final StackFrame frame = executionState().framePeek(thread.uniqueID());
    // defensively record the location-- no harm done if the location hasn't changed
    final ILineValue nextLine = resolveLine(frame);
    if (nextLine != valueFactory().createUnavailableLine())
    {
      executionState().nextLine(threadId, nextLine);
    }
    final ILineValue line = executionState().currentLine(threadId);
    return eventFactory().createExceptionThrowEvent(threadId, line, exception, thrower,
        wasFramePopped);
  }

  IJiveEvent createTypeLoadEvent(final ReferenceType type, final ThreadReference thread)
  {
    final IThreadValue threadId = resolveThread(thread);
    // lookup the type node
    final ITypeNode schema = staticModelFactory().lookupTypeNode(type.signature());
    // create a static contour
    final IContextContour contour = schema.createStaticContour();
    final ILineValue line = executionState().currentLine(threadId);
    return eventFactory().createTypeLoadEvent(threadId, line, contour);
  }

  IJiveEvent createVarAssignEvent(final LocatableEvent event, final StackFrame frame,
      final com.sun.jdi.Value newValue, final IContourMember varInstance, final String typeName)
  {
    final IThreadValue threadId = resolveThread(frame);
    final IValue value = resolveValue(frame.thread(), newValue, typeName);
    // defensively record the location when the event is provided
    if (event != null)
    {
      executionState().nextLine(threadId, resolveLine(event.location()));
    }
    final ILineValue line = executionState().currentLine(threadId);
    return eventFactory().createVarAssignEvent(threadId, line, value, varInstance);
  }

  // private static class ComponentSummary {
  //
  // // characters and strings are resolved and escaped
  // final boolean isEscaped;
  //
  // // all number types descendant from Number are resolved
  // final boolean isNumber;
  //
  // // date types are resolved
  // final boolean isDate;
  //
  // // boolean types are resolved
  // final boolean isBoolean;
  //
  // // string types are resolved
  // final boolean isString;
  //
  // private ComponentSummary(final ClassType type) {
  //
  // final String superClassName = type.superclass() != null ? type.superclass().toString() : "";
  // final String typeName = type.name();
  //
  // // string types are resolved
  // isString = typeName.equals("java.lang.String");
  //
  // // characters and strings are resolved and escaped
  // isEscaped = typeName.equals("java.lang.Character") || isString;
  //
  // // all number types descendant from Number are resolved
  // isNumber = superClassName.equals("java.lang.Number") && !typeName.contains("Atomic");
  //
  // // date types are resolved
  // isDate = typeName.equals("java.util.Date");
  //
  // // boolean types are resolved
  // isBoolean = typeName.equals("java.lang.Boolean");
  // }
  //
  // boolean isResolved() {
  //
  // return isEscaped || isNumber || isDate || isBoolean;
  // }
  // }
  IJiveEvent createVarDeleteEvent(final LocatableEvent event, final StackFrame frame,
      final IContourMember varInstance)
  {
    final IThreadValue threadId = resolveThread(frame);
    // defensively record the location when the event is provided
    if (event != null)
    {
      executionState().nextLine(threadId, resolveLine(event.location()));
    }
    final ILineValue line = executionState().currentLine(threadId);
    return eventFactory().createVarDeleteEvent(threadId, line, varInstance);
  }

  private static class ClassSummary
  {
    // out-of-model enum types are resolved
    final boolean isEnum;
    // characters and strings are resolved and escaped
    final boolean isEscaped;
    // all number types descendant from Number are resolved
    final boolean isNumber;
    // date types are resolved
    final boolean isDate;
    // boolean types are resolved
    final boolean isBoolean;
    // string types are resolved
    final boolean isString;
    // declared and actual types match
    final boolean matchingTypes;
    // unqualified actual type name
    final String actualTypeName;

    private ClassSummary(final String declaredTypeName, final ObjectReference object)
    {
      final String superClassName = object != null && object.type() instanceof ClassType
          && ((ClassType) object.type()).superclass() != null ? ((ClassType) object.type())
          .superclass().toString() : "";
      final String typeName = object.referenceType().toString();
      // out-of-model enum types are resolved
      isEnum = object.referenceType() instanceof ClassType
          && ((ClassType) object.referenceType()).isEnum();
      // string types are resolved
      isString = typeName.equals("java.lang.String");
      // characters and strings are resolved and escaped
      isEscaped = typeName.equals("java.lang.Character") || isString;
      // all number types descendant from Number are resolved
      isNumber = superClassName.equals("java.lang.Number") && !typeName.contains("Atomic");
      // date types are resolved
      isDate = typeName.equals("java.util.Date");
      // boolean types are resolved
      isBoolean = typeName.equals("java.lang.Boolean");
      // declared and actual types match
      matchingTypes = typeName.equals(declaredTypeName);
      // actual type name
      actualTypeName = typeName.substring(typeName.lastIndexOf(".") + 1);
    }

    boolean isResolved()
    {
      return isEscaped || isNumber || isDate || isEnum || isBoolean;
    }
  }
}