package edu.buffalo.cse.jive.internal.debug.jdi.model;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

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

import edu.buffalo.cse.jive.debug.JiveDebugPlugin;
import edu.buffalo.cse.jive.debug.jdi.model.IJDIManager;
import edu.buffalo.cse.jive.debug.jdi.model.IJiveEventDispatcher;
import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IEventModel.IEventListener;
import edu.buffalo.cse.jive.model.IEventModel.IEventProducer;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodExitEvent;

/**
 * Adapter for converting JDI events to JIVE events. Conversion ensures that the appropriate
 * contours exist and that Jive's model of the program's call stacks matches that of the JVM.
 */
final class JiveEventDispatcher implements IJiveEventDispatcher
{
  private final LocalDispatcher dispatcher;
  private final IJiveDebugTarget owner;
  private final EventFactoryAdapter adapter;

  JiveEventDispatcher(final IJiveDebugTarget owner)
  {
    super();
    this.owner = owner;
    // event factory adapter
    this.adapter = new EventFactoryAdapter(this.owner);
    this.dispatcher = new LocalDispatcher();
  }

  @Override
  public void dispatchArrayCellWriteEvent(final Location location, final ThreadReference thread,
      final IContextContour array, final IContourMember cell, final Value cellValue,
      final String componentTypeName)
  {
    dispatchEvent(adapter().createArrayCellWriteEvent(location, thread, array, cell, cellValue,
        componentTypeName));
  }

  /**
   * Creates and dispatches a {@code CatchEvent} for the method activation represented by the given
   * {@code StackFrame}, which must be a valid object. A {@code StackFrame} object is valid if it is
   * obtained from a suspended VM. It is invalidated once the VM is resumed.
   */
  @Override
  public void dispatchCatchEvent(final StackFrame catchFrame, final ExceptionEvent event)
  {
    dispatchEvent(adapter().createCatchEvent(catchFrame, event));
  }

  @Override
  public void dispatchDestroyEvent(final ThreadReference thread, final long oid)
  {
    dispatchEvent(adapter().createDestroyEvent(thread, oid));
  }

  @Override
  public void dispatchFieldReadEvent(final AccessWatchpointEvent event)
  {
    dispatchEvent(adapter().createFieldReadEvent(event));
  }

  @Override
  public void dispatchFieldWriteEvent(final ModificationWatchpointEvent event)
  {
    dispatchEvent(adapter().createFieldWriteEvent(event));
  }

  /**
   * Creates and dispatches an in-model {@code CallEvent} from the given {@code MethodEntryEvent}
   * and {@code StackFrame}. The stack frame represents the method activation resulting from the
   * call.
   */
  @Override
  public void dispatchInModelCallEvent(final MethodEntryEvent event, final StackFrame frame)
  {
    final IMethodCallEvent initiator = adapter().createMethodCallEvent(event, frame, true,
        manager().generateLocalEvents());
    dispatchEvent(initiator);
    dispatchEvent(adapter().createMethodEnterEvent(initiator, frame.thread()));
  }

  @Override
  public void dispatchLoadEvent(final ReferenceType type, final ThreadReference thread)
  {
    dispatchEvent(adapter().createTypeLoadEvent(type, thread));
  }

  /**
   * Creates and dispatches an in-model {@code ReturnEvent} from the given {@code MethodExitEvent}.
   */
  @Override
  public void dispatchMethodExitEvent(final MethodExitEvent event)
  {
    dispatchEvent(adapter().createMethodExitEvent(event));
  }

  /**
   * Creates and dispatches an out-of-model {@code ReturnEvent} for the topmost stack frame of the
   * given thread.
   */
  @Override
  public void dispatchMethodExitEvent(final ThreadReference thread)
  {
    dispatchEvent(adapter().createMethodExitEvent(thread));
  }

  @Override
  public void dispatchMethodResultEvent(final MethodExitEvent event)
  {
    dispatchEvent(adapter().createMethodResultEvent(event));
  }

  @Override
  public void dispatchMethodReturnedEvent(final ThreadReference thread)
  {
    dispatchEvent(adapter().createMethodReturnedEvent(thread));
  }

  /**
   * Creates and dispatches a {@code NewEvent} for the the object represented by the given
   * {@code ObjectReference}, which must be a valid object.
   * 
   * @throws AbsentInformationException
   */
  @Override
  public void dispatchNewEvent(final ObjectReference newObject, final ThreadReference thread,
      final int length)
  {
    dispatchEvent(adapter().createNewObjectEvent(newObject, thread, length));
  }

  /**
   * Create and dispatches an out-of-model {@code CallEvent} for the method activation represented
   * by the given {@code StackFrame}.
   * 
   * @throws AbsentInformationException
   */
  @Override
  public void dispatchOutOfModelCallEvent(final StackFrame frame)
  {
    dispatchEvent(adapter().createMethodCallEvent(null, frame, false,
    // dispatchEvent(adapter().createMethodEnterEvent(frame.thread()));
        manager().generateLocalEvents()));
  }

  /**
   * Creates and dispatches a {@code StepEvent} for the given {@code StackFrame}. The frame's
   * location must represent a valid source path and line number.
   */
  @Override
  public void dispatchStepEvent(final Location location, final StackFrame frame)
      throws AbsentInformationException
  {
    dispatchEvent(adapter().createLineStepEvent(location, frame));
  }

  @Override
  public void dispatchSyntheticFieldWrite(final ThreadReference thread,
      final ObjectReference object, final Field field, final Value valueToBe)
  {
    dispatchEvent(adapter().createSyntheticFieldWriteEvent(thread, object, field, valueToBe));
  }

  @Override
  public void dispatchSyntheticMethodCall(final StackFrame frame)
  {
    final IMethodCallEvent event = adapter().createSyntheticMethodCallEvent(frame);
    dispatchEvent(event);
    dispatchEvent(adapter().createMethodEnterEvent(event, frame.thread()));
  }

  // @Override
  // public void dispatchSystemStart() {
  //
  // dispatchEvent(adapter().createSystemStartEvent());
  // }
  @Override
  public void dispatchSyntheticMethodExit(final StackFrame frame)
  {
    final IMethodExitEvent event = adapter().createSyntheticMethodExitEvent(frame);
    dispatchEvent(event);
    dispatchEvent(adapter().createSyntheticMethodReturned(event));
  }

  // @Override
  // public void dispatchThreadStart(final ThreadReference thread) {
  //
  // if (manager().modelFilter().acceptsThread(thread)) {
  // dispatchEvent(adapter().createThreadStartEvent(thread));
  // }
  // }
  @Override
  public void dispatchSyntheticTypeLoad(final ThreadReference thread)
  {
    dispatchEvent(adapter().createSyntheticObjectLoadEvent(thread));
    dispatchEvent(adapter().createSyntheticTypeLoadEvent(thread));
  }

  @Override
  public void dispatchSystemExitEvent()
  {
    dispatchEvent(adapter().createSystemExitEvent());
  }

  @Override
  public void dispatchThreadDeath(final ThreadReference thread)
  {
    if (manager().modelFilter().acceptsThread(thread))
    {
      dispatchEvent(adapter().createThreadEndEvent(thread));
    }
  }

  /**
   * Creates and dispatches a {@code ThrowEvent} for the topmost stack frame of the given thread.
   * Throw events are generated for the stack frame where the exception is thrown (regardless if it
   * is caught there) and for any subsequent stack frame that does not handle the exception.
   */
  @Override
  public void dispatchThrowEvent(final ExceptionEvent event, final boolean framePopped)
  {
    dispatchEvent(adapter().createThrowEvent(event, framePopped));
  }

  @Override
  public void dispatchThrowEvent(final ThreadReference thread, final boolean framePopped)
  {
    dispatchEvent(adapter().createThrowEvent(thread, framePopped));
  }

  @Override
  public void dispatchVarAssignEvent(final LocatableEvent event, final StackFrame frame,
      final Value val, final IContourMember varInstance, final String typeName)
  {
    dispatchEvent(adapter().createVarAssignEvent(event, frame, val, varInstance, typeName));
  }

  /**
   * Creates and dispatches a local variable {@code JiveVarDeleteEvent}.
   */
  @Override
  public void dispatchVarDeleteEvent(final LocatableEvent event, final StackFrame frame,
      final IContourMember varInstance)
  {
    dispatchEvent(adapter().createVarDeleteEvent(event, frame, varInstance));
  }

  @Override
  public void subscribe(final IEventListener listener)
  {
    dispatcher.subscribe(listener);
  }

  @Override
  public void unsubscribe(final IEventListener listener)
  {
    dispatcher.unsubscribe(listener);
  }

  private EventFactoryAdapter adapter()
  {
    return this.adapter;
  }

  private void dispatchEvent(final IJiveEvent event)
  {
    dispatcher.dispatchEvent(event);
  }

  private IJDIManager manager()
  {
    return JiveDebugPlugin.getDefault().jdiManager(owner);
  }

  void reset()
  {
    dispatcher.queue.clear();
    dispatcher.dispatcherDelegate.queue.clear();
  }

  private final static class DispatcherDelegate extends Job
  {
    /**
     * A list of listeners to be notified when {@code JiveEvent}s occur.
     */
    private final ListenerList listenerList = new ListenerList();
    private final BlockingQueue<List<IJiveEvent>> queue = new ArrayBlockingQueue<List<IJiveEvent>>(
        4096);
    private final IEventProducer owner;

    DispatcherDelegate(final IEventProducer owner)
    {
      super("JIVE Event Dispatcher");
      setPriority(Job.LONG);
      setSystem(true);
      this.owner = owner;
    }

    /**
     * Notifies all the listeners of the given event.
     * 
     * @param event
     *          event to dispatch
     */
    private void fireEvents(final List<IJiveEvent> events)
    {
      for (final Object listener : listenerList.getListeners())
      {
        try
        {
          ((IEventListener) listener).eventOccurred(owner, events);
        }
        catch (final Exception e)
        {
          JiveDebugPlugin.log(e);
        }
      }
    }

    @Override
    protected IStatus run(final IProgressMonitor monitor)
    {
      while (queue.peek() != null)
      {
        fireEvents(queue.poll());
      }
      return Status.OK_STATUS;
    }

    void dispatchEvents(final List<IJiveEvent> events)
    {
      try
      {
        queue.put(events);
        schedule();
      }
      catch (final Exception e)
      {
        JiveDebugPlugin.log(e);
      }
    }

    void subscribe(final IEventListener listener)
    {
      listenerList.add(listener);
    }

    void unsubscribe(final IEventListener listener)
    {
      listenerList.remove(listener);
    }
  }

  /**
   * A job for notifying listeners of JIVE events. The dispatcher operates periodically on a
   * separate thread so as to avoid suspending the virtual machine while JIVE events are being
   * dispatched to the listeners.
   */
  private final static class LocalDispatcher extends Job implements IEventProducer
  {
    private final DispatcherDelegate dispatcherDelegate;
    /**
     * A blocking concurrent queue used to store newly created events. The
     * {@code JiveEventDispatcher} periodically removes events from the queue and notifies
     * listeners.
     */
    private final BlockingQueue<IJiveEvent> queue = new ArrayBlockingQueue<IJiveEvent>(128);

    LocalDispatcher()
    {
      super("JIVE Event Queue");
      setPriority(Job.SHORT);
      setSystem(true);
      dispatcherDelegate = new DispatcherDelegate(this);
    }

    @Override
    public void subscribe(final IEventListener listener)
    {
      dispatcherDelegate.subscribe(listener);
    }

    @Override
    public void unsubscribe(final IEventListener listener)
    {
      dispatcherDelegate.unsubscribe(listener);
    }

    @Override
    protected IStatus run(final IProgressMonitor monitor)
    {
      if (queue.peek() != null)
      {
        final List<IJiveEvent> events = new LinkedList<IJiveEvent>();
        queue.drainTo(events);
        // bulk dispatch
        dispatcherDelegate.dispatchEvents(events);
      }
      return Status.OK_STATUS;
    }

    /**
     * Dispatches the event to all event listeners registered with the dispatcher. Listeners are
     * notified of events on a separate thread.
     * 
     * @param event
     *          event to dispatch
     */
    void dispatchEvent(final IJiveEvent event)
    {
      try
      {
        queue.put(event);
        schedule();
      }
      catch (final Exception e)
      {
        JiveDebugPlugin.log(e);
      }
    }
  }
}