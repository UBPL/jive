package edu.buffalo.cse.jive.model;

import java.util.List;

import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IContourModel.IObjectContour;
import edu.buffalo.cse.jive.model.IExecutionModel.IStateChange;

public interface IEventModel extends IModel
{
  /**
   * Every data event is of a particular kind.
   */
  public enum EventKind
  {
    //
    // Exception control events
    //
    EXCEPTION_CATCH("Exception Catch"),
    EXCEPTION_THROW("Exception Throw"),
    //
    // Field data events
    //
    FIELD_READ("Field Read"),
    FIELD_WRITE("Field Write"),
    //
    // Line control event
    //
    LINE_STEP("Line Step"),
    //
    // Method control events
    //
    METHOD_CALL("Method Call"),
    METHOD_ENTERED("Method Entered"),
    METHOD_EXIT("Method Exit"),
    METHOD_RETURNED("Method Returned"),
    //
    // Monitor related events
    //
    MONITOR_LOCK_BEGIN("Monitor Lock Begin"),
    MONITOR_LOCK_END("Monitor Lock End"),
    MONITOR_LOCK_FAST("Monitor Lock Fast"),
    MONITOR_RELOCK("Monitor Relock"),
    MONITOR_UNLOCK_BEGIN("Monitor Unlock Begin"),
    MONITOR_UNLOCK_COMPLETE("Monitor Unlock Complete"),
    MONITOR_UNLOCK_END("Monitor Unlock End"),
    MONITOR_UNLOCK_FAST("Monitor Unlock Fast"),
    //
    // Object Allocation
    //
    OBJECT_DESTROY("Destroy Object"),
    OBJECT_NEW("New Object"),
    //
    // Scope control events
    //
    SCOPE_ALLOC("Scope Alloc"),
    SCOPE_ASSIGN("Scope Assign"),
    SCOPE_BACKING_ALLOC("Scope Backing Alloc"),
    SCOPE_BACKING_FREE("Scope Backing Free"),
    SCOPE_ENTER("Scope Enter"),
    SCOPE_EXIT("Scope Exit"),
    SCOPE_FREE("Scope Free"),
    SCOPE_POP("Scope Pop"),
    SCOPE_PUSH("Scope Push"),
    //
    // System start/end markers
    //
    SYSTEM_END("System End"),
    SYSTEM_START("System Start"),
    //
    // Thread control events
    //
    THREAD_CREATE("Thread Create"),
    THREAD_END("Thread End"),
    THREAD_LOCK("Lock State"),
    THREAD_PRIORITY("Thread Priority"),
    THREAD_SLEEP("Thread Sleep"),
    THREAD_START("Thread Start"),
    THREAD_WAKE("Thread Wake"),
    THREAD_YIELD("Thread Yield"),
    //
    // Type allocation
    //
    TYPE_LOAD("Type Load"),
    //
    // Local variable data events
    //
    VAR_ASSIGN("Variable Write"),
    VAR_DELETE("Variable Delete");
    private final String eventName;

    private EventKind(final String eventName)
    {
      this.eventName = eventName;
    }

    public String eventName()
    {
      return this.eventName;
    }

    @Override
    public String toString()
    {
      return this.eventName;
    }
  }

  /**
   * Generic assignment event.
   */
  public interface IAssignEvent extends IDataEvent
  {
    /**
     * Lookup method (TODO: push to store).
     * 
     */
    public IValue getLastValue();

    /**
     * Data method.
     * 
     * New value assigned to the variable.
     * 
     * @return the variable's new value
     */
    public IValue newValue();

    /**
     * Business action method (TODO: push to store).
     * 
     * Used during slicing to keep track of the in-slice old value of this event's contour member.
     */
    public void setLastAssignment(IDataEvent event);
  }

  /**
   * Generic event containing one data (contour member) reference.
   */
  public interface IDataEvent extends IMethodBodyEvent
  {
    /**
     * Data method.
     * 
     * Get the contour that contains the variable that changed. If this is a variable assignment,
     * then the contour is a method contour, otherwise it is a field's instance or static contour.
     * 
     * @return method contour containing the changed variable
     */
    public IContour contour();

    /**
     * Data method.
     * 
     * Member that has been assigned a value. This is a member of the contour with the given
     * {@code contourId()} value.
     * 
     * @return member that changed
     */
    public IContourMember member();
  }

  /**
   * An event corresponding to the destruction of an object. When an object is destructed, contours
   * are removed for it and all of its superclass objects.
   */
  public interface IDestroyObjectEvent extends IJiveEvent
  {
    /**
     * Data method.
     * 
     * Innermost contour destroyed in response to this event. Traversing the parent axis provides
     * all instance contours destroyed for this object.
     */
    public IObjectContour destroyedContour();
  }

  /**
   * Classes that wish to be notified of the occurrence of data events.
   */
  public interface IEventListener
  {
    /**
     * Called when an event is created by this event source.
     * 
     * @param source
     *          the event source
     * @param event
     *          the event that occurred
     */
    public void eventOccurred(IEventProducer source, List<IJiveEvent> events);
  }

  /**
   * Producer data events.
   */
  public interface IEventProducer
  {
    /**
     * Add a listener to this event source.
     * 
     * @param listener
     */
    public void subscribe(IEventListener listener);

    /**
     * Remove a listener from this event source.
     * 
     * @param listener
     */
    public void unsubscribe(IEventListener listener);
  }

  /**
   * TODO: This method doesn't need the catcher nor the member. These are naturally captured by the
   * VarAssignEvent when the exception is assigned to the variable. Likewise, the exception caught
   * form query can be executed as a variable changed query, using the appropriate variable type
   * and/or name.
   * 
   * An event corresponding to an exception being caught.
   */
  public interface IExceptionCatchEvent extends IDataEvent
  {
    /**
     * Data method.
     * 
     * Returns the contour of the method activation that caught the exception.
     * 
     * @return the catcher's contour
     */
    @Override
    public IMethodContour contour();

    /**
     * Data method.
     * 
     * Returns the exception that was caught.
     * 
     * @return the caught exception
     */
    public IValue exception();
  }

  /**
   * Event representing an exception thrown by the application. This event is generated at the
   * initial throw (regardless of whether the exception is caught there) and also for any methods
   * that do not handle the exception.
   */
  public interface IExceptionThrowEvent extends IMethodTerminatorEvent
  {
    /**
     * Data method.
     * 
     * Exception that was thrown.
     */
    public IValue exception();

    /**
     * Data method.
     * 
     * Method activation that (originally) threw the exception.
     */
    public IValue thrower();
  }

  /**
   * Field assignment.
   */
  public interface IFieldAssignEvent extends IAssignEvent
  {
    /**
     * Data method.
     * 
     * Context contour that contains the field. The context contour is either a static or instance
     * contour and must be in the model, since we only track changes to monitored contours.
     */
    @Override
    public IContextContour contour();
  }

  /**
   * Field read event.
   */
  public interface IFieldReadEvent extends IDataEvent
  {
    /**
     * Data method.
     * 
     * Context contour that contains the field. The context contour is either a static or instance
     * contour and must be in the model, since we only track changes to monitored contours.
     */
    @Override
    public IContextContour contour();
  }

  /**
   * Events that mark the beginning of an execution unit. Since initiator events uniquely identify
   * its associated execution unit, the event also provides services to explore and analyze the
   * underlying execution.
   */
  public interface IInitiatorEvent extends IJiveEvent
  {
    /**
     * Lookup method (TODO: push to store).
     * 
     * Duration of the execution in terms of the number of events occurring while the execution
     * occurrence is active. This includes events
     * <ul>
     * <li>in this execution,</li>
     * <li>in child executions, and
     * <li>in other threads before this execution completes.</li>
     * </ul>
     * 
     * @return number of events elapsing while the execution is active
     */
    public long duration();

    /**
     * Lookup method (TODO: push to store).
     * 
     * Events occurring within this method's execution.
     */
    public List<IJiveEvent> events();

    /**
     * Data method.
     * 
     * Identifier of the method environment (e.g., method contour) that represents the execution
     * initiated by this event.
     */
    public IMethodContour execution();

    /**
     * Data method.
     * 
     * Identifier of the context (e.g., static or instance contour) that represents the environment
     * within which the execution initiated by this event takes place.
     */
    public IContextContour executionContext();

    /**
     * Lookup method (TODO: push to store).
     * 
     * Indicates whether some event has occurred in the context of this initiator.
     */
    public boolean hasChildren();

    /**
     * Lookup method (TODO: push to store).
     * 
     * Indicates whether the event initiates an in-model execution.
     */
    public boolean inModel();

    /**
     * Lookup method (TODO: push to store).
     * 
     * Last event to occur within the execution initiated by this event. Will coincide with the
     * event's terminator only when the initiated execution terminates.
     */
    public IJiveEvent lastChildEvent();

    /**
     * Lookup method (TODO: push to store).
     * 
     * Events that initiated nested executions in the context of the initiated execution.
     */
    public List<? extends IInitiatorEvent> nestedInitiators();

    /**
     * Data method.
     * 
     * Event that marks the end of the execution of this unit.
     * 
     * @return the terminator event, if this execution unit is complete, otherwise {@code null}
     */
    public ITerminatorEvent terminator();
  }

  /**
   * Base type for all trace events. Some of members actually contain new model information while
   * others simply compute their data from the event log, as necessary.
   */
  public interface IJiveEvent extends IModel, Comparable<IJiveEvent>
  {
    /**
     * Lookup method (TODO: push to store).
     * 
     * Details of the event.
     */
    public String details();

    /**
     * Data method.
     * 
     * Identifier of this event. Each event has a unique identifier with respect to an execution.
     */
    public long eventId();

    /**
     * Lookup method (TODO: push to store).
     * 
     * Determines if this event is visible in the current view of the model. False only when a
     * filter is active and this method does not satisfy the filter condition.
     */
    public boolean isVisible();

    /**
     * Data method.
     * 
     * Kind of the event that determines the data encoded by the event.
     */
    public EventKind kind();

    /**
     * Data method.
     * 
     * Source line causing the event.
     */
    public ILineValue line();

    /**
     * Lookup method (TODO: push to store).
     * 
     * Convenience method to get the next event in the trace.
     */
    public IJiveEvent next();

    /**
     * Data method.
     * 
     * Event that identifies the execution context within which this event occurred.
     */
    public IInitiatorEvent parent();

    /**
     * Lookup method (TODO: push to store).
     * 
     * Convenience method to get the prior event in the trace.
     */
    public IJiveEvent prior();

    /**
     * Data method.
     * 
     * Thread on which the event takes place.
     */
    public IThreadValue thread();

    /**
     * Lookup method (TODO: push to store).
     * 
     * Transaction encapsulating the state changes caused by this event.
     */
    public ITransaction transaction();
  }

  /**
   * An begin-of-statement event. Such events are produced when a statement is about to be executed
   * by the underlying program.
   * 
   * @see {@code com.sun.jdi.event.StepEvent}
   */
  public interface ILineStepEvent extends IMethodBodyEvent
  {
  }

  public interface ILockEvent extends IJiveEvent
  {
    /**
     * Data method.
     */
    public IContour lock();

    /**
     * Data method.
     */
    public String lockDescription();

    /**
     * Data method.
     */
    public LockOperation lockOperation();
  }

  /**
   * All events signaled from a method execution.
   */
  public interface IMethodBodyEvent extends IJiveEvent
  {
    /**
     * Data method.
     * 
     * Event that identifies the execution context within which this event occurred.
     */
    @Override
    public IMethodCallEvent parent();
  }

  /**
   * An event corresponding to a method call. The terminator can be either a return or throw event.
   * The former is the standard and happens when a method execution terminates normally, while the
   * latter indicates that the execution terminated in response to an exception.
   */
  public interface IMethodCallEvent extends IInitiatorEvent
  {
    /**
     * Data method.
     * 
     * Entity that represents the method where the call originates. For in-model callers,
     * {@code Value} is a reference value.
     */
    public IValue caller();

    /**
     * Lookup method (TODO: push to store).
     * 
     * Method call events that initiated nested executions in the context of the initiated
     * execution.
     */
    @Override
    public List<IMethodCallEvent> nestedInitiators();

    /**
     * Data method.
     * 
     * Entity that represents the called method. For in-model targets, {@code Value} is a an
     * in-model target value ({@code Value.InModelTargetValue}).
     */
    public IValue target();

    /**
     * Data method.
     * 
     * Can only be terminated by a method exit or exception.
     */
    @Override
    public IMethodTerminatorEvent terminator();
  }

  /**
   * Signals that the body of a method (parent initiator) is about to execute.
   */
  public interface IMethodEnteredEvent extends IMethodBodyEvent
  {
  }

  /**
   * Event that represents a method's return. The returning method may be in the model or outside of
   * the model.
   */
  public interface IMethodExitEvent extends IMethodTerminatorEvent
  {
    /**
     * Data method.
     * 
     * Entity that represents the returning method. For in-model targets, {@code Value} is a a
     * reference value.
     */
    public IValue returnContext();

    /**
     * Data method.
     * 
     * The value returned by the method, if it is known, or an {@code UninitializedValue} otherwise.
     * If the method's return type is <tt>void</tt>, the result of this call is meaningless. For the
     * time being, this is only useful for querying method return.
     */
    public IValue returnValue();
  }

  public interface IMethodReturnedEvent extends IJiveEvent
  {
    /**
     * Data method.
     */
    public IMethodTerminatorEvent terminator();
  }

  /**
   * Signals the end of the execution of a method.
   */
  public interface IMethodTerminatorEvent extends ITerminatorEvent
  {
    /**
     * Data method.
     * 
     * Indicates whether the terminator causes a frame to be popped. Method returns always return
     * true, whereas an exception throw only returns true if the exception was not handled locally
     * within its execution context.
     */
    public boolean framePopped();

    /**
     * Data method.
     * 
     * Event that identifies the method call execution context within which this event occurred.
     */
    @Override
    public IMethodCallEvent parent();
  }

  /**
   * Events occurring as part of the JVM thread processing. As of this time (June/12) this method is
   * only relevant to Fiji.
   * 
   */
  public interface IMonitorEvent extends IRealTimeEvent
  {
    /**
     * Data method.
     * 
     * Monitor hexadecimal identifier on which the event occurred.
     */
    public String monitor();
  }

  /**
   * An event corresponding to the creation of an object. When an object is created, contours are
   * introduced for it and all of its superclass objects.
   */
  public interface INewObjectEvent extends IJiveEvent
  {
    /**
     * Data method.
     * 
     * Innermost contour created in response to this event. Traversing the parent axis provides all
     * instance contours created for this object.
     */
    public IObjectContour newContour();
  }

  public interface INewThreadEvent extends INewObjectEvent, IRealTimeEvent
  {
    /**
     * Data method.
     */
    public long newThreadId();
  }

  public interface IRealTimeEvent
  {
    /**
     * Data method.
     */
    public long timestamp();
  }

  public interface IRealTimeThreadEvent extends IRealTimeEvent
  {
    /**
     * Data method.
     */
    public int priority();

    /**
     * Data method.
     */
    public String scheduler();
  }

  public interface IRTDestroyObjectEvent extends IDestroyObjectEvent, IRealTimeEvent
  {
  }

  public interface IScopeAllocEvent extends IScopeEvent
  {
    /**
     * Data method.
     */
    public boolean isImmortal();

    /**
     * Data method.
     */
    public int size();
  }

  public interface IScopeAssignEvent extends IScopeEvent
  {
    /**
     * Data method.
     */
    public int indexLHS();

    /**
     * Data method.
     */
    public int indexRHS();

    /**
     * Data method.
     */
    public long lhs();

    /**
     * Data method.
     */
    public long rhs();

    /**
     * Data method.
     */
    public String scopeRHS();
  }

  public interface IScopeBackingAllocEvent extends IRealTimeEvent
  {
    /**
     * Data method.
     */
    public int size();
  }

  public interface IScopeBackingFreeEvent extends IRealTimeEvent
  {
  }

  public interface IScopeEvent extends IRealTimeEvent
  {
    /**
     * Data method.
     */
    public String scope();
  }

  /**
   * Termination of a virtual machine instance.
   */
  public interface ISystemExitEvent extends ITerminatorEvent
  {
  }

  /**
   * Start of a virtual machine instance on which the subject program executes.
   */
  public interface ISystemStartEvent extends IInitiatorEvent
  {
    /**
     * Lookup method (TODO: push to store).
     * 
     * Thread start events that executed in the context of the system execution.
     */
    @Override
    public List<IThreadStartEvent> nestedInitiators();

    /**
     * Data method.
     * 
     * System exit event that marks the end of the execution.
     */
    @Override
    public ISystemExitEvent terminator();
  }

  /**
   * Signals the termination of an execution unit.
   */
  public interface ITerminatorEvent extends IJiveEvent
  {
  }

  /**
   * Signals the termination of a thread execution.
   */
  public interface IThreadEndEvent extends ITerminatorEvent
  {
  }

  /**
   * Start of a thread execution.
   */
  public interface IThreadStartEvent extends IInitiatorEvent
  {
    /**
     * Lookup method (TODO: push to store).
     * 
     * List of children method executions occurring in the context of this execution.
     */
    @Override
    public List<IMethodCallEvent> nestedInitiators();

    /**
     * Data method.
     * 
     * Thread end event that marks the end of this thread's execution.
     */
    @Override
    public IThreadEndEvent terminator();
  }

  public interface IThreadTimedEvent extends IRealTimeEvent
  {
    /**
     * Data method.
     */
    public long wakeTime();
  }

  /**
   * A container that encapsulates a list of atomic changes associated with an event. A transaction
   * can be in one of two states, committed or uncommitted, which can be used by clients to replay
   * the state of executions.
   */
  public interface ITransaction
  {
    public List<IStateChange> changes();

    public boolean isCommitted();
  }

  /**
   * An event corresponding to a class' loading. A load event may be fired for one class' loading,
   * or it can represent the loading of a set of classes. For example, the first class loaded
   * usually has the <tt>main</tt> method in it; this means that all ancestors of the class are also
   * loaded and hence get static contours.
   */
  public interface ITypeLoadEvent extends IJiveEvent
  {
    /**
     * Data method.
     * 
     * Innermost contour created in response to this event. Traversing the parent axis provides all
     * static contours containing this contour.
     */
    public IContextContour newContour();
  }

  /**
   * Local variable assignment.
   */
  public interface IVarAssignEvent extends IAssignEvent
  {
    /**
     * Data method.
     */
    @Override
    public IMethodContour contour();
  }

  /**
   * Signals that a local variable is deleted from scope.
   */
  public interface IVarDeleteEvent extends IDataEvent
  {
  }

  public enum LockOperation
  {
    LOCK_ACQUIRE("ACQUIRE"),
    LOCK_RELEASE("RELEASE"),
    LOCK_WAIT("WAITING");
    private final String lock;

    private LockOperation(final String lock)
    {
      this.lock = lock;
    }

    @Override
    public String toString()
    {
      return lock;
    }
  }
}
