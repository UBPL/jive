package edu.buffalo.cse.jive.debug.jdi.model;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Field;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
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

import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IEventModel.IEventProducer;

/**
 * This dispatchers takes JDI model elements, especially JDI events, and dispatches the respective
 * Jive event. Other JDI model elements are used to generate Jive events such as local variable
 * changes inferred from inspection of stack frames.
 */
public interface IJiveEventDispatcher extends IEventProducer
{
  public void dispatchArrayCellWriteEvent(Location location, ThreadReference thread,
      IContextContour array, IContourMember cell, Value cellValue, String componentTypeName);

  /**
   * Creates and dispatches a {@code CatchEvent} for the method activation represented by the given
   * {@code StackFrame}, which must be a valid object. A {@code StackFrame} object is valid if it is
   * obtained from a suspended VM. It is invalidated once the VM is resumed.
   * 
   * @param exception
   */
  public void dispatchCatchEvent(StackFrame catchFrame, ExceptionEvent exception);

  public void dispatchDestroyEvent(ThreadReference thread, long oid);

  /**
   * Creates and dispatches a field read event.
   */
  public void dispatchFieldReadEvent(AccessWatchpointEvent event);

  /**
   * Creates and dispatches a field write event.
   */
  public void dispatchFieldWriteEvent(ModificationWatchpointEvent event);

  /**
   * Creates and dispatches an in-model method call event from the given {@code StackFrame}. The
   * stack frame represents the method activation resulting from the call.
   */
  public void dispatchInModelCallEvent(MethodEntryEvent event, StackFrame frame);

  /**
   * Creates and dispatches a type load event.
   */
  public void dispatchLoadEvent(ReferenceType type, ThreadReference thread);

  /**
   * Creates and dispatches an in-model {@code ReturnEvent} from the given {@code MethodExitEvent}.
   */
  public void dispatchMethodExitEvent(MethodExitEvent event);

  /**
   * Creates and dispatches an out-of-model {@code ReturnEvent} for the topmost stack frame of the
   * given thread.
   */
  public void dispatchMethodExitEvent(ThreadReference thread);

  /**
   * Creates and dispatches an assignment to a method's special result variable.
   */
  public void dispatchMethodResultEvent(MethodExitEvent event);

  public void dispatchMethodReturnedEvent(ThreadReference thread);

  /**
   * Creates and dispatches a {@code NewEvent} for the the object represented by the given
   * {@code ObjectReference}, which must be a valid object.
   * 
   * @throws AbsentInformationException
   */
  public void dispatchNewEvent(ObjectReference newObject, ThreadReference thread, int length);

  /**
   * Create and dispatches an out-of-model {@code CallEvent} for the method activation represented
   * by the given {@code StackFrame}.
   * 
   * @throws AbsentInformationException
   */
  public void dispatchOutOfModelCallEvent(StackFrame targetFrame);

  /**
   * Creates and dispatches a {@code StepEvent} for the given {@code StackFrame}. The frame's
   * location must represent a valid source path and line number.
   */
  public void dispatchStepEvent(Location location, StackFrame frame)
      throws AbsentInformationException;

  public void dispatchSyntheticFieldWrite(ThreadReference thread, ObjectReference object,
      Field field, Value valueToBe);

  public void dispatchSyntheticMethodCall(StackFrame frame);

  public void dispatchSyntheticMethodExit(StackFrame frame);

  public void dispatchSyntheticTypeLoad(ThreadReference thread);

  /**
   * Creates and dispatches a system exit event.
   */
  public void dispatchSystemExitEvent();

  /**
   * Creates and dispatches a thread death event.
   */
  public void dispatchThreadDeath(ThreadReference thread);

  /**
   * Creates and dispatches a {@code ThrowEvent} for the topmost stack frame of the given thread.
   * Throw events are generated for the stack frame where the exception is thrown (regardless if it
   * is caught there) and for any subsequent stack frame that does not handle the exception.
   */
  public void dispatchThrowEvent(ExceptionEvent exception, boolean framePopped);

  public void dispatchThrowEvent(ThreadReference thread, boolean framePopped);

  public void dispatchVarAssignEvent(LocatableEvent event, StackFrame frame, Value val,
      IContourMember varInstance, String typeName);

  /**
   * Creates and dispatches a local variable {@code JiveVarDeleteEvent}.
   */
  public void dispatchVarDeleteEvent(LocatableEvent event, StackFrame frame,
      IContourMember varInstance);
}