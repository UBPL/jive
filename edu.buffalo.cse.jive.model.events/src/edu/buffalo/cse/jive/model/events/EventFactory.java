package edu.buffalo.cse.jive.model.events;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import edu.buffalo.cse.jive.lib.StringTools;
import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IContourModel.IObjectContour;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IExecutionModel.AtomicUpdate;
import edu.buffalo.cse.jive.model.IExecutionModel.IStateChange;
import edu.buffalo.cse.jive.model.factory.IEventFactory;

public final class EventFactory implements IEventFactory
{
  private static final long EVENT_NULL = 0;
  private final IExecutionModel model;

  public EventFactory(final IExecutionModel executionModel)
  {
    this.model = executionModel;
  }

  @Override
  public IJiveEvent createDestroyEvent(final IThreadValue thread, final ILineValue line,
      final IObjectContour contour)
  {
    return new DestroyEvent(thread, line, contour);
  }

  @Override
  public IJiveEvent createExceptionCatchEvent(final IThreadValue thread, final ILineValue line,
      final IValue exception, final IContourMember variable)
  {
    return new ExceptionCatchEvent(thread, line, exception, variable);
  }

  @Override
  public IJiveEvent createExceptionThrowEvent(final IThreadValue thread, final ILineValue line,
      final IValue exception, final IValue thrower, final boolean wasFramePopped)
  {
    return new ExceptionThrowEvent(thread, line, exception, thrower, wasFramePopped);
  }

  @Override
  public IJiveEvent createFieldReadEvent(final IThreadValue thread, final ILineValue line,
      final IContextContour contour, final IContourMember member)
  {
    return new FieldReadEvent(thread, line, contour, member);
  }

  @Override
  public IJiveEvent createFieldWriteEvent(final IThreadValue thread, final ILineValue line,
      final IContextContour contour, final IValue newValue, final IContourMember member)
  {
    return new FieldWriteEvent(thread, line, contour, newValue, member);
  }

  @Override
  public IJiveEvent createLineStepEvent(final IThreadValue thread, final ILineValue line)
  {
    return new LineStepEvent(thread, line);
  }

  @Override
  public IJiveEvent createLockEvent(final IThreadValue thread, final ILineValue line,
      final LockOperation lockOperation, final IContour lock, final String lockDescription)
  {
    return new LockEvent(thread, line, lockOperation, lock, lockDescription);
  }

  @Override
  public IJiveEvent createMethodCallEvent(final IThreadValue thread, final ILineValue line,
      final IValue caller, final IValue target)
  {
    return new MethodCallEvent(thread, line, caller, target);
  }

  @Override
  public IJiveEvent createMethodEnteredEvent(final IThreadValue thread, final ILineValue line)
  {
    return new MethodEnteredEvent(thread, line);
  }

  @Override
  public IJiveEvent createMethodExitEvent(final IThreadValue thread, final ILineValue line)
  {
    return new MethodExitEvent(thread, line);
  }

  @Override
  public IJiveEvent createMethodReturnedEvent(final IMethodTerminatorEvent terminator)
  {
    return new MethodReturnedEvent(terminator);
  }

  @Override
  public IJiveEvent createMonitorLockBeginEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final String monitor)
  {
    return new MonitorEvent(timestamp, thread, line, monitor, EventKind.MONITOR_LOCK_BEGIN);
  }

  @Override
  public IJiveEvent createMonitorLockEndEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final String monitor)
  {
    return new MonitorEvent(timestamp, thread, line, monitor, EventKind.MONITOR_LOCK_END);
  }

  @Override
  public IJiveEvent createMonitorLockFastEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final String monitor)
  {
    return new MonitorEvent(timestamp, thread, line, monitor, EventKind.MONITOR_LOCK_FAST);
  }

  @Override
  public IJiveEvent createMonitorRelockEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final String monitor)
  {
    return new MonitorEvent(timestamp, thread, line, monitor, EventKind.MONITOR_RELOCK);
  }

  @Override
  public IJiveEvent createMonitorUnlockBeginEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final String monitor)
  {
    return new MonitorEvent(timestamp, thread, line, monitor, EventKind.MONITOR_UNLOCK_BEGIN);
  }

  @Override
  public IJiveEvent createMonitorUnlockCompleteEvent(final long timestamp,
      final IThreadValue thread, final ILineValue line, final String monitor)
  {
    return new MonitorEvent(timestamp, thread, line, monitor, EventKind.MONITOR_UNLOCK_COMPLETE);
  }

  @Override
  public IJiveEvent createMonitorUnlockEndEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final String monitor)
  {
    return new MonitorEvent(timestamp, thread, line, monitor, EventKind.MONITOR_UNLOCK_END);
  }

  @Override
  public IJiveEvent createMonitorUnlockFastEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final String monitor)
  {
    return new MonitorEvent(timestamp, thread, line, monitor, EventKind.MONITOR_UNLOCK_FAST);
  }

  @Override
  public IJiveEvent createNewObjectEvent(final IThreadValue thread, final ILineValue line,
      final IObjectContour contour)
  {
    return new NewEvent(thread, line, contour);
  }

  @Override
  public IJiveEvent createRTDestroyEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final IObjectContour contour)
  {
    return new RTDestroyEvent(timestamp, thread, line, contour);
  }

  @Override
  public IJiveEvent createRTFieldWriteEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final IContextContour contour, final IValue newValue,
      final IContourMember member)
  {
    return new RTFieldWriteEvent(timestamp, thread, line, contour, newValue, member);
  }

  @Override
  public IJiveEvent createRTMethodCallEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final IValue caller, final IValue target)
  {
    return new RTMethodCallEvent(timestamp, thread, line, caller, target);
  }

  @Override
  public IJiveEvent createRTMethodEnteredEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line)
  {
    return new RTMethodEnteredEvent(timestamp, thread, line);
  }

  @Override
  public IJiveEvent createRTMethodExitEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line)
  {
    return new RTMethodExitEvent(timestamp, thread, line);
  }

  @Override
  public IJiveEvent createRTNewObjectEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final IObjectContour contour, final String scope)
  {
    return new RTNewEvent(timestamp, thread, line, contour, scope);
  }

  @Override
  public IJiveEvent createRTSystemExitEvent(final long timestamp)
  {
    return new RTSystemExitEvent(timestamp, model.valueFactory().createSystemThread());
  }

  @Override
  public IJiveEvent createRTSystemStartEvent(final long timestamp)
  {
    return new RTSystemStartEvent(timestamp, model.valueFactory().createSystemThread());
  }

  @Override
  public IJiveEvent createRTThreadEndEvent(final long timestamp, final IThreadValue thread)
  {
    return new RTThreadEndEvent(timestamp, thread);
  }

  @Override
  public IJiveEvent createRTThreadNewEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final IObjectContour contour, final long newThreadId)
  {
    return new RTThreadNewEvent(timestamp, thread, line, contour, newThreadId);
  }

  @Override
  public IJiveEvent createRTThreadPriorityEvent(final long timestamp, final IThreadValue thread,
      final String scheduler, final int priority)
  {
    return new RTThreadPriorityEvent(timestamp, thread, scheduler, priority);
  }

  @Override
  public IJiveEvent createRTThreadSleepEvent(final long timestamp, final IThreadValue thread,
      final long waketime)
  {
    return new RTThreadSleepEvent(timestamp, thread, waketime);
  }

  @Override
  public IJiveEvent createRTThreadStartEvent(final long timestamp, final IThreadValue thread,
      final String scheduler, final int priority)
  {
    return new RTThreadStartEvent(timestamp, thread, scheduler, priority);
  }

  @Override
  public IJiveEvent createRTThreadWakeEvent(final long timestamp, final IThreadValue thread,
      final long waketime)
  {
    return new RTThreadWakeEvent(timestamp, thread, waketime);
  }

  @Override
  public IJiveEvent createRTThreadYieldEvent(final long timestamp, final IThreadValue thread,
      final long waketime)
  {
    return new RTThreadYieldEvent(timestamp, thread, waketime);
  }

  @Override
  public IJiveEvent createRTTypeLoadEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final IContextContour contour)
  {
    return new RTTypeLoadEvent(timestamp, thread, line, contour);
  }

  @Override
  public IJiveEvent createScopeAllocEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final String scope, final int size, final boolean isImmortal)
  {
    return new ScopeAllocEvent(timestamp, thread, line, scope, size, isImmortal);
  }

  @Override
  public IJiveEvent createScopeAssignEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final String scopeLHS, final int indexLHS, final long lhs,
      final String scopeRHS, final int indexRHS, final long rhs)
  {
    return new ScopeAssignEvent(timestamp, thread, line, scopeLHS, indexLHS, lhs, scopeRHS,
        indexRHS, rhs);
  }

  @Override
  public IJiveEvent createScopeBackingAllocEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final int size)
  {
    return new ScopeBackingAllocEvent(timestamp, thread, line, size);
  }

  @Override
  public IJiveEvent createScopeBackingFreeEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line)
  {
    return new ScopeBackingFreeEvent(timestamp, thread, line);
  }

  @Override
  public IJiveEvent createScopeEnterEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final String scope)
  {
    return new ScopeEvent(timestamp, thread, line, scope, EventKind.SCOPE_ENTER);
  }

  @Override
  public IJiveEvent createScopeExitEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final String scope)
  {
    return new ScopeEvent(timestamp, thread, line, scope, EventKind.SCOPE_EXIT);
  }

  @Override
  public IJiveEvent createScopeFreeEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final String scope)
  {
    return new ScopeEvent(timestamp, thread, line, scope, EventKind.SCOPE_FREE);
  }

  @Override
  public IJiveEvent createScopePopEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final String scope)
  {
    return new ScopeEvent(timestamp, thread, line, scope, EventKind.SCOPE_POP);
  }

  @Override
  public IJiveEvent createScopePushEvent(final long timestamp, final IThreadValue thread,
      final ILineValue line, final String scope)
  {
    return new ScopeEvent(timestamp, thread, line, scope, EventKind.SCOPE_PUSH);
  }

  @Override
  public IJiveEvent createSystemExitEvent()
  {
    return new SystemExitEvent(model.valueFactory().createSystemThread());
  }

  public IJiveEvent createSystemStartEvent()
  {
    return new SystemStartEvent(model.valueFactory().createSystemThread());
  }

  @Override
  public IJiveEvent createThreadEndEvent(final IThreadValue thread)
  {
    return new ThreadEndEvent(thread);
  }

  public IJiveEvent createThreadStartEvent(final IThreadValue thread)
  {
    return new ThreadStartEvent(thread);
  }

  @Override
  public IJiveEvent createTypeLoadEvent(final IThreadValue thread, final ILineValue line,
      final IContextContour contour)
  {
    return new TypeLoadEvent(thread, line, contour);
  }

  @Override
  public IJiveEvent createVarAssignEvent(final IThreadValue thread, final ILineValue line,
      final IValue newValue, final IContourMember variable)
  {
    return new VarAssignEvent(thread, line, newValue, variable);
  }

  @Override
  public IJiveEvent createVarDeleteEvent(final IThreadValue thread, final ILineValue line,
      final IContourMember variable)
  {
    return new VarDeleteEvent(thread, line, variable);
  }

  @Override
  public IExecutionModel model()
  {
    return model;
  }

  /**
   * Data members introduced by this event: the new value.
   */
  public abstract class AssignEvent extends DataEvent implements IAssignEvent
  {
    private final IValue newValue;
    // relevant only for sliced models
    private IValue lastValue;

    private AssignEvent(final IThreadValue thread, final ILineValue line, final IContour contour,
        final IValue newValue, final IContourMember member)
    {
      super(thread, line, contour, member);
      if (member == null)
      {
        throw new IllegalArgumentException("Cannot create the event with a null member.");
      }
      if (newValue == null)
      {
        throw new IllegalArgumentException("Cannot create an assign event with a null new value.");
      }
      this.newValue = newValue;
      this.lastValue = null;
    }

    // relevant only for sliced models
    @Override
    public IValue getLastValue()
    {
      return model.sliceView() == null ? null : this.lastValue;
    }

    @Override
    public IValue newValue()
    {
      return this.newValue;
    }

    // relevant only for sliced models
    @Override
    public void setLastAssignment(final IDataEvent event)
    {
      if (event == null)
      {
        this.lastValue = null;
      }
      else if (event instanceof IAssignEvent || event instanceof IVarDeleteEvent)
      {
        final List<IStateChange> changes = event.transaction().changes();
        if (changes.size() == 1 && changes.get(0) instanceof AtomicUpdate)
        {
          this.lastValue = ((AtomicUpdate) changes.get(0)).oldValue();
        }
      }
    }
  }

  /**
   * Data members introduced by this event: a list of initiators occurring under this execution, the
   * (transient) last event in this initiator, the terminator associated with this execution, and a
   * flag indicating whether nested initiators were ever started under this execution.
   */
  public abstract class InitiatorEvent extends JiveEvent implements IInitiatorEvent
  {
    private boolean hasChildren;
    private final List<? extends IInitiatorEvent> initiators;
    private IJiveEvent lastEvent;
    private ITerminatorEvent terminator;
    /**
     * Virtual fragment of the state.
     */
    private boolean virtualHasChildren;
    private List<? extends IInitiatorEvent> virtualInitiators;
    private IJiveEvent virtualLastEvent;

    private InitiatorEvent(final IThreadValue thread, final ILineValue line)
    {
      super(thread, line);
      hasChildren = false;
      initiators = TypeTools.newArrayList();
      virtualHasChildren = false;
    }

    /**
     * NOTE: This method does not need to take any special action to handle virtual/normal modes in
     * the initiator since it relies on methods (rather than fields) that are supposed to resolve
     * this consistently.
     * 
     * TODO: reduce the cyclomatic complexity.
     */
    public void addInitiator(final IInitiatorEvent initiator)
    {
      /**
       * if this is an in-model parent and it is "initiating" an execution for which there are other
       * out-of-model calls in-between, we use NULL values to separate groups of such virtually
       * initiated executions, that is, for which the current event is the top in-model initiator.
       * It is simply a matter of comparing the last event actually seen in this execution with the
       * initiator's identifier.
       */
      @SuppressWarnings("unchecked")
      final List<IInitiatorEvent> initiators = (List<IInitiatorEvent>) nestedInitiators();
      // in-model child
      if (initiator.parent() == this)
      {
        // previous initiator was an out-of-model descendant, mark an in/out+/in return
        if (!initiators.isEmpty() && initiators.get(initiators.size() - 1) != null
            && initiators.get(initiators.size() - 1).parent() != this)
        {
          initiators.add(null);
        }
      }
      // out-of-model descendant
      else
      {
        // first out-of-model descendant, mark an in/out+/in call
        if (initiators.isEmpty())
        {
          initiators.add(null);
        }
        else
        {
          // previous initiator was an in-model child, mark an in/out+/in call
          if (initiators.get(initiators.size() - 1) != null
              && initiators.get(initiators.size() - 1).parent() == this)
          {
            initiators.add(null);
          }
          // previous initiator was an out-of-model descendant
          else
          {
            final IJiveEvent event = lookupLastEvent();
            // an in-model event happened within the containing initiator between nested
            // executions
            if (initiators.get(initiators.size() - 1) != null && event != null
                && initiators.get(initiators.size() - 1).eventId() < event.eventId())
            {
              initiators.add(null);
            }
          }
        }
      }
      // mark whether this initiator has *some* in-model descendant
      // hasInitiators = initiator.inModel() || hasInitiators;
      initiators.add(initiator);
    }

    /**
     * NOTE: This method does not need to take any special action to handle virtual/normal modes in
     * the initiator since it relies on methods (rather than fields) that are supposed to resolve
     * this consistently.
     */
    @Override
    public long duration()
    {
      if (/* !model.store().isVirtual() && */this instanceof IMethodCallEvent)
      {
        // for ongoing method calls, use the thread's last event
        if (terminator() == null)
        {
          return model.lookupThread(thread()).lastChildEvent().eventId() - eventId();
        }
        // otherwise, use the terminator as the last event
        else
        {
          return terminator().eventId() - eventId();
        }
      }
      // otherwise, use the locally maintained last event
      return lastEvent() == null ? 1 : lastEvent().eventId() - eventId();
    }

    /**
     * Returns a list containing all visible events in the context of this initiator.
     * 
     * NOTE: This method does not need to take any special action to handle virtual/normal modes in
     * the initiator since it relies on methods (rather than fields) that are supposed to resolve
     * this consistently.
     */
    @Override
    public List<IJiveEvent> events()
    {
      final List<IJiveEvent> result = TypeTools.newArrayList();
      model.readLock();
      try
      {
        // assumes a system start contains only thread start/end events
        if (this instanceof SystemStartEvent)
        {
          for (final IThreadStartEvent event : model.lookupThreads())
          {
            if (((JiveEvent) event).isVisible())
            {
              result.add(event);
            }
          }
          final ISystemStartEvent root = model.lookupRoot();
          if (root.terminator() != null && ((JiveEvent) root.terminator()).isVisible())
          {
            result.add(root.terminator());
          }
        }
        else
        {
          final Iterator<IJiveEvent> iterator = new EventIterator(this);
          while (iterator.hasNext())
          {
            final JiveEvent event = (JiveEvent) iterator.next();
            if (event.isVisible())
            {
              result.add(event);
            }
          }
        }
      }
      finally
      {
        model.readUnlock();
      }
      return result;
    }

    /**
     * Resolves the has children flag based on whether this event is virtual/normal.
     */
    @Override
    public boolean hasChildren()
    {
      return model.store().isVirtual() ? virtualHasChildren : hasChildren;
    }

    /**
     * NOTE: This method does not need to take any special action to handle virtual/normal modes in
     * the initiator since it relies on methods (rather than fields) that are supposed to resolve
     * this consistently.
     */
    @Override
    public IJiveEvent lastChildEvent()
    {
      if (this instanceof IMethodCallEvent)
      {
        // for ongoing method calls, use the thread's last event
        if (terminator() == null)
        {
          return model.lookupThread(thread()).lastChildEvent();
        }
        // otherwise, use the terminator as the last event
        else
        {
          return terminator();
        }
      }
      // otherwise, use the locally maintained last event
      return this.lastEvent();
    }

    /**
     * Resolves the nested initiators based on whether this event is virtual/normal.
     */
    @Override
    public List<? extends IInitiatorEvent> nestedInitiators()
    {
      return model.store().isVirtual() ? virtualInitiators : initiators;
    }

    @Override
    public void resetVirtualId()
    {
      super.resetVirtualId();
      virtualHasChildren = false;
      virtualInitiators.clear();
      virtualInitiators = null;
      virtualLastEvent = null;
    }

    public void setLastEvent(final IJiveEvent event)
    {
      if (this instanceof IThreadStartEvent || this instanceof ISystemStartEvent)
      {
        this.lastEvent = event;
      }
    }

    public void setTerminator(final ITerminatorEvent terminator)
    {
      this.lastEvent = terminator;
      this.terminator = terminator;
      // returning from out-of-model to in-model
      if (terminator.parent() != null && terminator.parent().parent() != null
          && !terminator.parent().inModel() && terminator.parent().parent().inModel())
      {
        final IInitiatorEvent inModel = terminator.parent().parent();
        @SuppressWarnings("unchecked")
        final List<IInitiatorEvent> initiators = (List<IInitiatorEvent>) inModel.nestedInitiators();
        // previous initiator was an out-of-model descendant, mark an in/out+/in return
        if (!initiators.isEmpty() && initiators.get(initiators.size() - 1) != null)
        {
          initiators.add(null);
        }
      }
    }

    /**
     * Important-- when a call event is virtualized, all its children must have been virtualized
     * already (otherwise, all nested children would be empty!).
     */
    @Override
    public void setVirtualId(final long virtualId)
    {
      super.setVirtualId(virtualId);
      final List<IInitiatorEvent> virtualInitiators = TypeTools.newArrayList();
      boolean isLastNull = false;
      IInitiatorEvent lastChild = null;
      // compute the virtual nested initiators
      for (final IInitiatorEvent child : initiators)
      {
        // out-of-model call separator
        if (child == null)
        {
          isLastNull = true;
          continue;
        }
        // only if visible
        if (((JiveEvent) child).isVisible())
        {
          // process an outstanding out-of-model call separator
          if (isLastNull)
          {
            virtualInitiators.add(null);
          }
          virtualInitiators.add(child);
          lastChild = child;
          isLastNull = false;
        }
      }
      // process an outstanding out-of-model call separator
      if (isLastNull && lastChild != null)
      {
        virtualInitiators.add(null);
      }
      // make it formal
      this.virtualHasChildren = lastChild != null;
      // at this point, we need brute force
      if (hasChildren && !virtualHasChildren)
      {
        // assumes a system start contains only thread start/end events
        if (this instanceof SystemStartEvent)
        {
          for (final IThreadStartEvent event : model.lookupThreads())
          {
            if (((JiveEvent) event).isVisible())
            {
              virtualHasChildren = true;
              break;
            }
          }
        }
        else
        {
          final Iterator<IJiveEvent> iterator = new EventIterator(this);
          while (iterator.hasNext())
          {
            final JiveEvent event = (JiveEvent) iterator.next();
            if (event.isVisible())
            {
              virtualHasChildren = true;
              break;
            }
          }
        }
      }
      // make it formal
      this.virtualInitiators = virtualInitiators;
      // now set the virtual last event
      if (lastEvent != null)
      {
        setLastVirtualEvent(lastChild);
      }
    }

    /**
     * Resolves the terminator based on whether this event is virtual/normal.
     */
    @Override
    public ITerminatorEvent terminator()
    {
      return terminator != null && terminator.eventId() > EventFactory.EVENT_NULL ? terminator
          : null;
    }

    /**
     * Returns the last event occurring in the context of this initiator.
     * 
     * NOTE: This method does not need to take any special action to handle virtual/normal modes in
     * the initiator since it relies on methods (rather than fields) that are supposed to resolve
     * this consistently.
     */
    @SuppressWarnings("unchecked")
    private IJiveEvent lookupLastEvent()
    {
      final IJiveEvent priorEvent = lastChildEvent();
      if (priorEvent == null || priorEvent.eventId() <= eventId())
      {
        return null;
      }
      // restricted to initiators (TODO: check whether this can be generalized)
      if (this instanceof ThreadStartEvent)
      {
        final List<IInitiatorEvent> initiators = (List<IInitiatorEvent>) nestedInitiators();
        if (!initiators.isEmpty())
        {
          return initiators.get(initiators.size() - 1);
        }
        return null;
      }
      // traverse back through the visible events within this initiator's execution context
      JiveEvent currentEvent = (JiveEvent) priorEvent.prior();
      while (currentEvent != null
          && currentEvent.eventId() > eventId()
          && !executionContext().equals(
              currentEvent.parent() == null ? null : currentEvent.parent().executionContext()))
      {
        currentEvent = (JiveEvent) currentEvent.prior();
      }
      // either null or a visible event in this event's execution context
      return currentEvent;
    }

    /**
     * Called for thread and system initiators only.
     * 
     * TODO: reduce the cyclomatic complexity.
     */
    private void setLastVirtualEvent(final IJiveEvent lastChild)
    {
      // the virtual last event is the actual last event
      if (lastEvent.eventId() > EventFactory.EVENT_NULL)
      {
        virtualLastEvent = lastEvent;
      }
      else
      {
        // traverse back from the *actual* last event and find the last virtual event
        IJiveEvent currentEvent = model.store().lookupRawEvent(((JiveEvent) lastEvent).eventId);
        // search anywhere
        if (this instanceof ISystemStartEvent)
        {
          while (currentEvent != null && (((JiveEvent) currentEvent).eventId >= super.eventId)
              && currentEvent.eventId() == EventFactory.EVENT_NULL)
          {
            // && (currentEvent.eventId() == EVENT_NULL || currentEvent.parent() != this)) {
            currentEvent = model.store().lookupRawEvent(((JiveEvent) currentEvent).eventId - 1);
          }
        }
        // search anywhere in this thread
        else if (this instanceof IThreadStartEvent)
        {
          while (currentEvent != null
              && (((JiveEvent) currentEvent).eventId >= super.eventId)
              && (currentEvent.eventId() == EventFactory.EVENT_NULL || currentEvent.thread() != thread()))
          {
            // && (currentEvent.eventId() == EVENT_NULL || currentEvent.parent() != this)) {
            currentEvent = model.store().lookupRawEvent(((JiveEvent) currentEvent).eventId - 1);
          }
        }
        // search anywhere in this method
        else
        {
          while (currentEvent != null
              && (((JiveEvent) currentEvent).eventId >= super.eventId)
              && (currentEvent.eventId() == EventFactory.EVENT_NULL || currentEvent.parent() != this))
          {
            currentEvent = model.store().lookupRawEvent(((JiveEvent) currentEvent).eventId - 1);
          }
        }
        IJiveEvent lastFromChild = null;
        if (lastChild != null)
        {
          // this must have been computed in an earlier iteration
          lastFromChild = ((InitiatorEvent) lastChild).lastEvent();
        }
        if (lastFromChild != null && currentEvent != null)
        {
          // current event occurred before the call to the child
          if (lastFromChild.eventId() > currentEvent.eventId())
          {
            virtualLastEvent = lastFromChild;
          }
          // current event occurred after the call to the child
          else
          {
            virtualLastEvent = currentEvent;
          }
        }
        // this initiator's own event
        else if (currentEvent != null)
        {
          virtualLastEvent = currentEvent;
        }
        // the last child's event is inherited
        else
        {
          virtualLastEvent = lastFromChild;
        }
      }
    }

    protected void setHasChildren()
    {
      this.hasChildren = true;
    }

    /**
     * Resolves the last event based on whether this event is virtual/normal.
     */
    IJiveEvent lastEvent()
    {
      return model.store().isVirtual() ? virtualLastEvent : lastEvent;
    }
  }

  /**
   * Data members introduced by this event: event identifier, parent execution, thread identifier,
   * and line reference.
   */
  public abstract class JiveEvent implements IJiveEvent
  {
    private long eventId;
    private ILineValue line;
    private IInitiatorEvent parentExecution;
    private final IThreadValue thread;
    private ITransaction transaction;
    private long virtualId;

    private JiveEvent(final IThreadValue thread, final ILineValue line)
    {
      if (thread == null && !(this instanceof ISystemExitEvent)
          && !(this instanceof ISystemStartEvent))
      {
        throw new IllegalArgumentException(
            "Cannot create an non-system event with a null thread identifier.");
      }
      this.eventId = -1;
      this.thread = thread;
      if (line == null)
      {
        this.line = model.valueFactory().createUnavailableLine();
      }
      else
      {
        this.line = line;
      }
      this.virtualId = EventFactory.EVENT_NULL;
    }

    /**
     * Commits this event's changes to the model atomically.
     */
    public void commit()
    {
      model.transactionLog().atomicEmpty(this);
    }

    /**
     * The event identifier should be used even if the event is in virtual mode.
     */
    @Override
    public int compareTo(final IJiveEvent other)
    {
      if (other instanceof JiveEvent)
      {
        return eventId > ((JiveEvent) other).eventId ? 1
            : ((JiveEvent) other).eventId == eventId ? 0 : -1;
      }
      // for all other objects, this one is larger
      return 1;
    }

    @Override
    public String details()
    {
      return StringTools.eventDetails(this);
    }

    /**
     * Resolves the identifier based on whether this event is virtual/normal.
     */
    @Override
    public long eventId()
    {
      // Check the store and use either the virtual or the actual identifier.
      return model.store().isVirtual() ? virtualId : eventId;
    }

    @Override
    public boolean isVisible()
    {
      return eventId() > EventFactory.EVENT_NULL;
    }

    @Override
    public ILineValue line()
    {
      return this.line;
    }

    @Override
    public IExecutionModel model()
    {
      return model;
    }

    /**
     * Resolves the next event based on whether this event is virtual/normal.
     */
    @Override
    public IJiveEvent next()
    {
      return model.store().lookupNextEvent(this);
    }

    @Override
    public IInitiatorEvent parent()
    {
      return this.parentExecution;
    }

    /**
     * Resolves the prior event based on whether this event is virtual/normal.
     */
    @Override
    public IJiveEvent prior()
    {
      return model.store().lookupPriorEvent(this);
    }

    public void resetVirtualId()
    {
      virtualId = EventFactory.EVENT_NULL;
    }

    public void setEventId(final long eventId)
    {
      this.eventId = eventId;
    }

    public void setParentInitiator(final IInitiatorEvent event)
    {
      ((InitiatorEvent) event).setHasChildren();
      this.parentExecution = event;
    }

    public void setTransaction(final ITransaction transaction)
    {
      this.transaction = transaction;
    }

    public void setVirtualId(final long virtualId)
    {
      assert virtualId > 0 : "A virtual event identifier must be a positive value.";
      this.virtualId = virtualId;
    }

    @Override
    public IThreadValue thread()
    {
      return this.thread;
    }

    @Override
    public String toString()
    {
      return StringTools.eventToString(this);
    }

    @Override
    public ITransaction transaction()
    {
      return transaction;
    }
  }

  /**
   * Data members introduced by this event: the context contour and the respective member.
   */
  private abstract class DataEvent extends JiveEvent
  {
    private final IContour contour;
    private final IContourMember member;

    private DataEvent(final IThreadValue thread, final ILineValue line, final IContour contour,
        final IContourMember member)
    {
      super(thread, line);
      if (contour == null)
      {
        throw new IllegalArgumentException("Cannot create the event with a null contour.");
      }
      this.contour = contour;
      this.member = member;
    }

    public IContour contour()
    {
      return this.contour;
    }

    public IContourMember member()
    {
      return this.member;
    }

    @Override
    public IMethodCallEvent parent()
    {
      return (IMethodCallEvent) super.parent();
    }
  }

  /**
   * Data members introduced by this event: the destroyed contour.
   */
  private class DestroyEvent extends JiveEvent implements IDestroyObjectEvent
  {
    private final IObjectContour contour;

    DestroyEvent(final IThreadValue thread, final ILineValue line, final IObjectContour contour)
    {
      super(thread, line);
      this.contour = contour;
    }

    @Override
    public void commit()
    {
      model.transactionLog().atomicObjectDestroy(this, destroyedContour());
    }

    @Override
    public IObjectContour destroyedContour()
    {
      return contour;
    }

    @Override
    public EventKind kind()
    {
      return EventKind.OBJECT_DESTROY;
    }
  }

  /**
   * Iterator that traverses all child events of an execution.
   */
  private final class EventIterator implements Iterator<IJiveEvent>
  {
    private IJiveEvent nextEvent;
    private final IInitiatorEvent rootEvent;

    EventIterator(final IInitiatorEvent root)
    {
      this.rootEvent = root;
      this.nextEvent = root;
      calculateNextEvent(true);
    }

    @Override
    public boolean hasNext()
    {
      return nextEvent != null;
    }

    @Override
    public IJiveEvent next()
    {
      if (!hasNext())
      {
        throw new NoSuchElementException();
      }
      final IJiveEvent result = nextEvent;
      calculateNextEvent(false);
      return result;
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }

    private void calculateNextEvent(final boolean firstEvent)
    {
      // short-circuit processing
      if (nextEvent == null)
      {
        return;
      }
      // short-circuit processing for same thread events
      if (!firstEvent)
      {
        // the root event terminator determines the end of iteration
        if (rootEvent.terminator() != null
            && nextEvent.eventId() >= rootEvent.terminator().eventId())
        {
          nextEvent = null;
          return;
        }
        // skip through events in this nested execution, as well as the return/exception
        if (nextEvent instanceof IInitiatorEvent)
        {
          // for an initiator that has already terminated, skip to its terminator
          if (((IInitiatorEvent) nextEvent).terminator() != null)
          {
            nextEvent = ((IInitiatorEvent) nextEvent).terminator();
          }
          // otherwise, we have no further events to process for this root
          else
          {
            nextEvent = null;
            return;
          }
        }
      }
      // skip through events in other threads
      IJiveEvent currentEvent = nextEvent.next();
      while (currentEvent != null && !currentEvent.thread().equals(rootEvent.thread()))
      {
        currentEvent = currentEvent.next();
      }
      // either null or an event in the root event's thread
      nextEvent = currentEvent;
    }
  }

  /**
   * Data members introduced by this event: the exception value.
   */
  private final class ExceptionCatchEvent extends LocalDataEvent implements IExceptionCatchEvent
  {
    private final IValue exception;

    ExceptionCatchEvent(final IThreadValue thread, final ILineValue line, final IValue exception,
        final IContourMember variable)
    {
      super(thread, line, variable);
      this.exception = exception;
    }

    @Override
    public IValue exception()
    {
      return this.exception;
    }

    @Override
    public EventKind kind()
    {
      return EventKind.EXCEPTION_CATCH;
    }
  }

  /**
   * Data members introduced by this event: the exception thrown, the thrower, and a flag indicating
   * whether frames were popped.
   */
  private final class ExceptionThrowEvent extends JiveEvent implements IExceptionThrowEvent
  {
    private final IValue exception;
    private final boolean framePopped;
    private final IValue thrower;

    ExceptionThrowEvent(final IThreadValue thread, final ILineValue line, final IValue exception,
        final IValue thrower, final boolean wasFramePopped)
    {
      super(thread, line);
      if (thrower == null)
      {
        throw new IllegalArgumentException(
            "Cannot create an exception throw event with a null thrower.");
      }
      this.exception = exception;
      this.thrower = thrower;
      this.framePopped = wasFramePopped;
    }

    @Override
    public void commit()
    {
      // frame popped and the thrower is in-model
      if (framePopped() && thrower() instanceof IContourReference)
      {
        final IMethodContour method = parent().execution();
        model.transactionLog().atomicRemoveContour(this, method);
      }
      else
      {
        super.commit();
      }
    }

    @Override
    public IValue exception()
    {
      return this.exception;
    }

    @Override
    public boolean framePopped()
    {
      return this.framePopped;
    }

    @Override
    public EventKind kind()
    {
      return EventKind.EXCEPTION_THROW;
    }

    @Override
    public IMethodCallEvent parent()
    {
      return (IMethodCallEvent) super.parent();
    }

    @Override
    public IValue thrower()
    {
      return this.thrower;
    }
  }

  /**
   * Data members introduced by this event: none.
   */
  private final class FieldReadEvent extends DataEvent implements IFieldReadEvent
  {
    private FieldReadEvent(final IThreadValue thread, final ILineValue line,
        final IContextContour contour, final IContourMember member)
    {
      super(thread, line, contour, member);
      if (member == null)
      {
        throw new IllegalArgumentException("Cannot create the event with a null member.");
      }
    }

    @Override
    public IContextContour contour()
    {
      return (IContextContour) super.contour();
    }

    @Override
    public EventKind kind()
    {
      return EventKind.FIELD_READ;
    }
  }

  /**
   * Data members introduced by this event: none.
   */
  private class FieldWriteEvent extends AssignEvent implements IFieldAssignEvent
  {
    private FieldWriteEvent(final IThreadValue thread, final ILineValue line,
        final IContour contour, final IValue newValue, final IContourMember member)
    {
      super(thread, line, contour, newValue, member);
    }

    @Override
    public void commit()
    {
      model.transactionLog().atomicValueSet(this, contour(), member(), newValue());
    }

    @Override
    public IContextContour contour()
    {
      return (IContextContour) super.contour();
    }

    @Override
    public EventKind kind()
    {
      return EventKind.FIELD_WRITE;
    }
  }

  /**
   * Data members introduced by this event: none.
   */
  private final class LineStepEvent extends JiveEvent implements ILineStepEvent
  {
    LineStepEvent(final IThreadValue thread, final ILineValue line)
    {
      super(thread, line);
    }

    @Override
    public EventKind kind()
    {
      return EventKind.LINE_STEP;
    }

    @Override
    public IMethodCallEvent parent()
    {
      return (IMethodCallEvent) super.parent();
    }
  }

  /**
   * Data members introduced by this event: the contour member.
   */
  private abstract class LocalDataEvent extends JiveEvent
  {
    private final IContourMember member;

    private LocalDataEvent(final IThreadValue thread, final ILineValue line,
        final IContourMember member)
    {
      super(thread, line);
      this.member = member;
    }

    public IMethodContour contour()
    {
      return parent().execution();
    }

    public IContourMember member()
    {
      return this.member;
    }

    @Override
    public IMethodCallEvent parent()
    {
      return (IMethodCallEvent) super.parent();
    }
  }

  /**
   * Data members introduced by this event: description, locked contour, operation.
   */
  private final class LockEvent extends JiveEvent implements ILockEvent
  {
    private final String description;
    private final IContour lock;
    private final LockOperation lockOperation;

    LockEvent(final IThreadValue thread, final ILineValue line, final LockOperation lockOperation,
        final IContour lock, final String lockDescription)
    {
      super(thread, line);
      this.lockOperation = lockOperation;
      this.lock = lock;
      this.description = lockDescription;
    }

    @Override
    public EventKind kind()
    {
      return EventKind.THREAD_LOCK;
    }

    @Override
    public IContour lock()
    {
      return this.lock;
    }

    @Override
    public String lockDescription()
    {
      return this.lock != null ? lock.signature() : this.description;
    }

    @Override
    public LockOperation lockOperation()
    {
      return this.lockOperation;
    }
  }

  /**
   * Data members introduced by this event: caller and target.
   */
  private class MethodCallEvent extends InitiatorEvent implements IMethodCallEvent
  {
    private final IValue caller;
    private final IValue target;

    public MethodCallEvent(final IThreadValue thread, final ILineValue line, final IValue caller,
        final IValue target)
    {
      super(thread, line);
      if (caller == null)
      {
        throw new IllegalArgumentException("Cannot create a method call event with a null caller.");
      }
      this.caller = caller;
      this.target = target;
    }

    @Override
    public void addInitiator(final IInitiatorEvent initiator)
    {
      if (!(initiator instanceof IMethodCallEvent))
      {
        throw new IllegalArgumentException(
            "The initator of a nested method execution must be a MethodCallEvent instance, however, found: "
                + initiator);
      }
      super.addInitiator(initiator);
    }

    @Override
    public IValue caller()
    {
      return this.caller;
    }

    @Override
    public void commit()
    {
      // call target is in-model
      if (inModel())
      {
        final IMethodContourReference imtv = (IMethodContourReference) target();
        model.transactionLog().atomicMethodEnter(this, caller(), imtv.contour());
      }
      else
      {
        super.commit();
      }
    }

    @Override
    public IMethodContour execution()
    {
      if (target instanceof IMethodContourReference)
      {
        return ((IMethodContourReference) target).contour();
      }
      return null;
    }

    @Override
    public IContextContour executionContext()
    {
      if (target instanceof IMethodContourReference)
      {
        return execution().parent();
      }
      return null;
    }

    @Override
    public boolean inModel()
    {
      return target instanceof IMethodContourReference;
    }

    @Override
    public EventKind kind()
    {
      return EventKind.METHOD_CALL;
    }

    /**
     * The overriden {@code addInitiator} method in this class guarantees that only MethodCallEvent
     * initiators are added to this event's initiator list.
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<IMethodCallEvent> nestedInitiators()
    {
      return (List<IMethodCallEvent>) super.nestedInitiators();
    }

    @Override
    public IValue target()
    {
      return this.target;
    }

    @Override
    public IMethodTerminatorEvent terminator()
    {
      return (IMethodTerminatorEvent) super.terminator();
    }
  }

  /**
   * Data members introduced by this event: none.
   */
  private class MethodEnteredEvent extends JiveEvent implements IMethodEnteredEvent
  {
    MethodEnteredEvent(final IThreadValue thread, final ILineValue line)
    {
      super(thread, line);
    }

    @Override
    public EventKind kind()
    {
      return EventKind.METHOD_ENTERED;
    }

    @Override
    public IMethodCallEvent parent()
    {
      return (IMethodCallEvent) super.parent();
    }
  }

  /**
   * Data members introduced by this event: none.
   */
  private class MethodExitEvent extends JiveEvent implements IMethodExitEvent
  {
    MethodExitEvent(final IThreadValue thread, final ILineValue line)
    {
      super(thread, line);
    }

    @Override
    public void commit()
    {
      // return context is in-model
      if (parent().inModel())
      {
        model.transactionLog().atomicMethodExit(this, parent().execution());
      }
      else
      {
        super.commit();
      }
    }

    @Override
    public boolean framePopped()
    {
      return true;
    }

    @Override
    public EventKind kind()
    {
      return EventKind.METHOD_EXIT;
    }

    @Override
    public IMethodCallEvent parent()
    {
      return (IMethodCallEvent) super.parent();
    }

    @Override
    public IValue returnContext()
    {
      return parent().target();
    }

    @Override
    public IValue returnValue()
    {
      if (parent() != null && parent().execution() != null)
      {
        final IContourMember result = parent().execution().lookupResultMember();
        if (result != null)
        {
          return result.value();
        }
      }
      return model.valueFactory().createUninitializedValue();
    }
  }

  /**
   * Data members introduced by this event: none.
   */
  private final class MethodReturnedEvent extends JiveEvent implements IMethodReturnedEvent
  {
    private final IMethodTerminatorEvent terminator;

    MethodReturnedEvent(final IMethodTerminatorEvent terminator)
    {
      super(terminator.thread(), null);
      this.terminator = terminator;
    }

    @Override
    public EventKind kind()
    {
      return EventKind.METHOD_RETURNED;
    }

    @Override
    public ILineValue line()
    {
      return /* terminator.parent() == null ? super.line() : */terminator.parent().line();
    }

    @Override
    public IMethodTerminatorEvent terminator()
    {
      return this.terminator;
    }
  }

  /**
   * Data members introduced by this event: monitor, timestamp.
   * 
   * TODO: update the string representation in StringTools
   */
  private final class MonitorEvent extends JiveEvent implements IMonitorEvent
  {
    private final String monitor;
    private final EventKind kind;
    private final long timestamp;

    public MonitorEvent(final long timestamp, final IThreadValue thread, final ILineValue line,
        final String monitor, final EventKind kind)
    {
      super(thread, line);
      this.kind = kind;
      this.monitor = monitor;
      this.timestamp = timestamp;
    }

    @Override
    public EventKind kind()
    {
      return kind;
    }

    @Override
    public String monitor()
    {
      return monitor;
    }

    @Override
    public long timestamp()
    {
      return timestamp;
    }
  }

  /**
   * Data members introduced by this event: the newly created contour.
   */
  private class NewEvent extends JiveEvent implements INewObjectEvent
  {
    private final IObjectContour contour;

    NewEvent(final IThreadValue thread, final ILineValue line, final IObjectContour contour)
    {
      super(thread, line);
      this.contour = contour;
    }

    @Override
    public void commit()
    {
      model.transactionLog().atomicObjectNew(this, newContour());
    }

    @Override
    public EventKind kind()
    {
      return EventKind.OBJECT_NEW;
    }

    // @Override
    // public IMethodCallEvent parent() {
    //
    // return (IMethodCallEvent) super.parent();
    // }
    @Override
    public IObjectContour newContour()
    {
      return contour;
    }
  }

  /**
   * Data members introduced by this event: the destroyed contour.
   */
  private final class RTDestroyEvent extends DestroyEvent implements IRTDestroyObjectEvent
  {
    private final long timestamp;

    RTDestroyEvent(final long timestamp, final IThreadValue thread, final ILineValue line,
        final IObjectContour contour)
    {
      super(thread, line, contour);
      this.timestamp = timestamp;
    }

    @Override
    public long timestamp()
    {
      return timestamp;
    }
  }

  /**
   * Data members introduced by this event: timestamp.
   * 
   * TODO: update the string representation in StringTools
   */
  private final class RTFieldWriteEvent extends FieldWriteEvent implements IRealTimeEvent
  {
    private final long timestamp;

    private RTFieldWriteEvent(final long timestamp, final IThreadValue thread,
        final ILineValue line, final IContour contour, final IValue newValue,
        final IContourMember member)
    {
      super(thread, line, contour, newValue, member);
      this.timestamp = timestamp;
    }

    @Override
    public long timestamp()
    {
      return this.timestamp;
    }
  }

  /**
   * Data members introduced by this event: timestamp.
   * 
   * TODO: update the string representation in StringTools
   */
  private final class RTMethodCallEvent extends MethodCallEvent implements IRealTimeEvent
  {
    private final long timestamp;

    public RTMethodCallEvent(final long timestamp, final IThreadValue thread,
        final ILineValue line, final IValue caller, final IValue target)
    {
      super(thread, line, caller, target);
      this.timestamp = timestamp;
    }

    @Override
    public long timestamp()
    {
      return this.timestamp;
    }
  }

  /**
   * Data members introduced by this event: timestamp.
   * 
   * TODO: update the string representation in StringTools
   */
  private final class RTMethodEnteredEvent extends MethodEnteredEvent implements IRealTimeEvent
  {
    private final long timestamp;

    RTMethodEnteredEvent(final long timestamp, final IThreadValue thread, final ILineValue line)
    {
      super(thread, line);
      this.timestamp = timestamp;
    }

    @Override
    public long timestamp()
    {
      return this.timestamp;
    }
  }

  /**
   * Data members introduced by this event: timestamp.
   * 
   * TODO: update the string representation in StringTools
   */
  private final class RTMethodExitEvent extends MethodExitEvent implements IRealTimeEvent
  {
    private final long timestamp;

    RTMethodExitEvent(final long timestamp, final IThreadValue thread, final ILineValue line)
    {
      super(thread, line);
      this.timestamp = timestamp;
    }

    @Override
    public long timestamp()
    {
      return this.timestamp;
    }
  }

  /**
   * // * Data members introduced by this event: scope, timestamp.
   * 
   * TODO: update the string representation in StringTools
   */
  private final class RTNewEvent extends NewEvent implements IScopeEvent
  {
    private final String scope;
    private final long timestamp;

    RTNewEvent(final long timestamp, final IThreadValue thread, final ILineValue line,
        final IObjectContour contour, final String scope)
    {
      super(thread, line, contour);
      this.timestamp = timestamp;
      this.scope = scope;
    }

    @Override
    public EventKind kind()
    {
      return EventKind.OBJECT_NEW;
    }

    @Override
    public String scope()
    {
      return this.scope;
    }

    @Override
    public long timestamp()
    {
      return this.timestamp;
    }
  }

  /**
   * Data members introduced by this event: timestamp.
   * 
   * TODO: update the string representation in StringTools
   */
  private final class RTSystemExitEvent extends SystemExitEvent implements IRealTimeEvent
  {
    private final long timestamp;

    RTSystemExitEvent(final long timestamp, final IThreadValue thread)
    {
      super(thread);
      this.timestamp = timestamp;
    }

    @Override
    public long timestamp()
    {
      return this.timestamp;
    }
  }

  /**
   * Data members introduced by this event: timestamp.
   * 
   * TODO: update the string representation in StringTools
   */
  private final class RTSystemStartEvent extends SystemStartEvent implements IRealTimeEvent
  {
    private final long timestamp;

    RTSystemStartEvent(final long timestamp, final IThreadValue thread)
    {
      super(thread);
      this.timestamp = timestamp;
    }

    @Override
    public long timestamp()
    {
      return this.timestamp;
    }
  }

  /**
   * Data members introduced by this event: timestamp.
   * 
   * TODO: update the string representation in StringTools
   */
  private final class RTThreadEndEvent extends ThreadEndEvent implements IRealTimeEvent
  {
    private final long timestamp;

    private RTThreadEndEvent(final long timestamp, final IThreadValue thread)
    {
      super(thread);
      this.timestamp = timestamp;
    }

    @Override
    public long timestamp()
    {
      return this.timestamp;
    }
  }

  /**
   * Data members introduced by this event: timestamp.
   * 
   * TODO: update the string representation in StringTools
   */
  private final class RTThreadNewEvent extends NewEvent implements INewThreadEvent
  {
    private final long newThreadId;
    private final long timestamp;

    RTThreadNewEvent(final long timestamp, final IThreadValue thread, final ILineValue line,
        final IObjectContour contour, final long newThreadId)
    {
      super(thread, line, contour);
      this.newThreadId = newThreadId;
      this.timestamp = timestamp;
    }

    @Override
    public EventKind kind()
    {
      return EventKind.THREAD_CREATE;
    }

    @Override
    public long newThreadId()
    {
      return this.newThreadId;
    }

    @Override
    public long timestamp()
    {
      return this.timestamp;
    }
  }

  /**
   * Data members introduced by this event: scheduler, priority, timestamp.
   * 
   * TODO: update the string representation in StringTools
   */
  private final class RTThreadPriorityEvent extends JiveEvent implements IRealTimeThreadEvent
  {
    private final String scheduler;
    private final int priority;
    private final long timestamp;

    public RTThreadPriorityEvent(final long timestamp, final IThreadValue thread,
        final String scheduler, final int priority)
    {
      super(thread, null);
      this.priority = priority;
      this.scheduler = scheduler;
      this.timestamp = timestamp;
    }

    @Override
    public EventKind kind()
    {
      return EventKind.THREAD_PRIORITY;
    }

    @Override
    public int priority()
    {
      return this.priority;
    }

    @Override
    public String scheduler()
    {
      return this.scheduler;
    }

    @Override
    public long timestamp()
    {
      return this.timestamp;
    }
  }

  private final class RTThreadSleepEvent extends RTThreadTimedEvent
  {
    public RTThreadSleepEvent(final long timestamp, final IThreadValue thread, final long wakeTime)
    {
      super(timestamp, thread, wakeTime);
    }

    @Override
    public EventKind kind()
    {
      return EventKind.THREAD_SLEEP;
    }
  }

  /**
   * Data members introduced by this event: scheduler, priority, timestamp.
   * 
   * TODO: update the string representation in StringTools
   */
  private final class RTThreadStartEvent extends ThreadStartEvent implements IRealTimeThreadEvent
  {
    private final int priority;
    private final String scheduler;
    private final long timestamp;

    private RTThreadStartEvent(final long timestamp, final IThreadValue thread,
        final String scheduler, final int priority)
    {
      super(thread);
      this.priority = priority;
      this.scheduler = scheduler;
      this.timestamp = timestamp;
    }

    @Override
    public int priority()
    {
      return this.priority;
    }

    @Override
    public String scheduler()
    {
      return this.scheduler;
    }

    @Override
    public long timestamp()
    {
      return this.timestamp;
    }
  }

  /**
   * Data members introduced by this event: timestamp, waketime.
   * 
   * TODO: update the string representation in StringTools
   */
  private abstract class RTThreadTimedEvent extends JiveEvent implements IThreadTimedEvent
  {
    private final long wakeTime;
    private final long timestamp;

    public RTThreadTimedEvent(final long timestamp, final IThreadValue thread, final long wakeTime)
    {
      super(thread, null);
      this.timestamp = timestamp;
      this.wakeTime = wakeTime;
    }

    @Override
    public long timestamp()
    {
      return this.timestamp;
    }

    @Override
    public long wakeTime()
    {
      return this.wakeTime;
    }
  }

  private final class RTThreadWakeEvent extends RTThreadTimedEvent
  {
    public RTThreadWakeEvent(final long timestamp, final IThreadValue thread, final long wakeTime)
    {
      super(timestamp, thread, wakeTime);
    }

    @Override
    public EventKind kind()
    {
      return EventKind.THREAD_WAKE;
    }
  }

  private final class RTThreadYieldEvent extends RTThreadTimedEvent
  {
    public RTThreadYieldEvent(final long timestamp, final IThreadValue thread, final long wakeTime)
    {
      super(timestamp, thread, wakeTime);
    }

    @Override
    public EventKind kind()
    {
      return EventKind.THREAD_YIELD;
    }
  }

  /**
   * Data members introduced by this event: timestamp.
   * 
   * TODO: update the string representation in StringTools
   */
  private final class RTTypeLoadEvent extends TypeLoadEvent implements IRealTimeEvent
  {
    private final long timestamp;

    RTTypeLoadEvent(final long timestamp, final IThreadValue thread, final ILineValue line,
        final IContextContour contour)
    {
      super(thread, line, contour);
      this.timestamp = timestamp;
    }

    @Override
    public long timestamp()
    {
      return this.timestamp;
    }
  }

  /**
   * Data members introduced by this event: isImmortal, size.
   * 
   * TODO: update the string representation in StringTools
   */
  private final class ScopeAllocEvent extends ScopeEvent implements IScopeAllocEvent
  {
    private final boolean isImmortal;
    private final int size;

    public ScopeAllocEvent(final long timestamp, final IThreadValue thread, final ILineValue line,
        final String scope, final int size, final boolean isImmortal)
    {
      super(timestamp, thread, line, scope, EventKind.SCOPE_ALLOC);
      this.isImmortal = isImmortal;
      this.size = size;
    }

    @Override
    public boolean isImmortal()
    {
      return isImmortal;
    }

    @Override
    public int size()
    {
      return size;
    }
  }

  /**
   * Data members introduced by this event: rhsScope.
   * 
   * TODO: update the string representation in StringTools
   */
  private final class ScopeAssignEvent extends ScopeEvent implements IScopeAssignEvent
  {
    private final int indexLHS;
    private final int indexRHS;
    private final long lhs;
    private final long rhs;
    private final String scopeRHS;

    public ScopeAssignEvent(final long timestamp, final IThreadValue thread, final ILineValue line,
        final String scopeLHS, final int indexLHS, final long lhs, final String scopeRHS,
        final int indexRHS, final long rhs)
    {
      super(timestamp, thread, line, scopeLHS, EventKind.SCOPE_ASSIGN);
      this.indexLHS = indexLHS;
      this.lhs = lhs;
      this.scopeRHS = scopeRHS;
      this.indexRHS = indexRHS;
      this.rhs = rhs;
    }

    @Override
    public int indexLHS()
    {
      return indexLHS;
    }

    @Override
    public int indexRHS()
    {
      return indexRHS;
    }

    @Override
    public long lhs()
    {
      return lhs;
    }

    @Override
    public long rhs()
    {
      return rhs;
    }

    @Override
    public String scopeRHS()
    {
      return scopeRHS;
    }
  }

  /**
   * Data members introduced by this event: size, timestamp.
   * 
   * TODO: update the string representation in StringTools
   */
  private final class ScopeBackingAllocEvent extends JiveEvent implements IScopeBackingAllocEvent
  {
    private final int size;
    private final long timestamp;

    public ScopeBackingAllocEvent(final long timestamp, final IThreadValue thread,
        final ILineValue line, final int size)
    {
      super(thread, line);
      this.size = size;
      this.timestamp = timestamp;
    }

    @Override
    public EventKind kind()
    {
      return EventKind.SCOPE_BACKING_ALLOC;
    }

    @Override
    public int size()
    {
      return size;
    }

    @Override
    public long timestamp()
    {
      return timestamp;
    }
  }

  /**
   * Data members introduced by this event: timestamp.
   * 
   * TODO: update the string representation in StringTools
   */
  private final class ScopeBackingFreeEvent extends JiveEvent implements IScopeBackingFreeEvent
  {
    private final long timestamp;

    public ScopeBackingFreeEvent(final long timestamp, final IThreadValue thread,
        final ILineValue line)
    {
      super(thread, line);
      this.timestamp = timestamp;
    }

    @Override
    public EventKind kind()
    {
      return EventKind.SCOPE_BACKING_FREE;
    }

    @Override
    public long timestamp()
    {
      return timestamp;
    }
  }

  /**
   * Data members introduced by this event: scope, timestamp.
   * 
   * TODO: update the string representation in StringTools
   */
  private class ScopeEvent extends JiveEvent implements IScopeEvent
  {
    private final String scope;
    private final EventKind kind;
    private final long timestamp;

    public ScopeEvent(final long timestamp, final IThreadValue thread, final ILineValue line,
        final String scope, final EventKind kind)
    {
      super(thread, line);
      this.kind = kind;
      this.scope = scope;
      this.timestamp = timestamp;
    }

    @Override
    public EventKind kind()
    {
      return kind;
    }

    @Override
    public String scope()
    {
      return scope;
    }

    @Override
    public long timestamp()
    {
      return timestamp;
    }
  }

  /**
   * Data members introduced by this event: none.
   */
  private class SystemExitEvent extends JiveEvent implements ISystemExitEvent
  {
    SystemExitEvent(final IThreadValue thread)
    {
      super(thread, null);
    }

    @Override
    public EventKind kind()
    {
      return EventKind.SYSTEM_END;
    }
  }

  /**
   * Data members introduced by this event: none.
   */
  private class SystemStartEvent extends InitiatorEvent implements ISystemStartEvent
  {
    SystemStartEvent(final IThreadValue thread)
    {
      super(thread, null);
    }

    @Override
    public void addInitiator(final IInitiatorEvent initiator)
    {
      if (!(initiator instanceof IThreadStartEvent))
      {
        throw new IllegalArgumentException(
            "The initator of a thread must be a ThreadStartEvent instance, however, found: "
                + initiator);
      }
      super.addInitiator(initiator);
    }

    @Override
    public IMethodContour execution()
    {
      return null;
    }

    @Override
    public IContextContour executionContext()
    {
      return null;
    }

    @Override
    public boolean inModel()
    {
      return true;
    }

    @Override
    public EventKind kind()
    {
      return EventKind.SYSTEM_START;
    }

    /**
     * The overriden {@code addInitiator} method in this class guarantees that only ThreadStartEvent
     * initiators are added to this event's initiator list.
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<IThreadStartEvent> nestedInitiators()
    {
      return (List<IThreadStartEvent>) super.nestedInitiators();
    }

    @Override
    public void setTerminator(final ITerminatorEvent terminator)
    {
      if (!(terminator instanceof ISystemExitEvent))
      {
        throw new IllegalArgumentException(
            "The terminator of the system execution must be a SystemExitEvent instance, however, found: "
                + terminator);
      }
      super.setTerminator(terminator);
    }

    /**
     * The overriden {@code setTerminator} method in this class guarantees that only a
     * SystemExitEvent terminator is allowed as a terminator of this event's associated execution.
     */
    @Override
    public ISystemExitEvent terminator()
    {
      return (ISystemExitEvent) super.terminator();
    }
  }

  /**
   * Data members introduced by this event: none.
   */
  private class ThreadEndEvent extends JiveEvent implements IThreadEndEvent
  {
    private ThreadEndEvent(final IThreadValue thread)
    {
      super(thread, null);
    }

    @Override
    public EventKind kind()
    {
      return EventKind.THREAD_END;
    }
  }

  /**
   * Data members introduced by this event: none.
   */
  private class ThreadStartEvent extends InitiatorEvent implements IThreadStartEvent
  {
    private ThreadStartEvent(final IThreadValue thread)
    {
      super(thread, null);
    }

    @Override
    public void addInitiator(final IInitiatorEvent initiator)
    {
      if (!(initiator instanceof IMethodCallEvent))
      {
        throw new IllegalArgumentException(
            "The initator of a nested method execution must be a MethodCallEvent instance, however, found: "
                + initiator);
      }
      super.addInitiator(initiator);
    }

    @Override
    public IMethodContour execution()
    {
      return null;
    }

    @Override
    public IContextContour executionContext()
    {
      return null;
    }

    @Override
    public boolean inModel()
    {
      return true;
    }

    @Override
    public EventKind kind()
    {
      return EventKind.THREAD_START;
    }

    /**
     * The overriden {@code addInitiator} method in this class guarantees that only MethodCallEvent
     * initiators are added to this event's initiator list.
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<IMethodCallEvent> nestedInitiators()
    {
      return (List<IMethodCallEvent>) super.nestedInitiators();
    }

    @Override
    public void setTerminator(final ITerminatorEvent terminator)
    {
      if (!(terminator instanceof IThreadEndEvent))
      {
        throw new IllegalArgumentException(
            "The terminator of a thread execution must be a ThreadEndEvent instance, however, found: "
                + terminator);
      }
      super.setTerminator(terminator);
    }

    /**
     * The overriden {@code setTerminator} method in this class guarantees that only a
     * ThreadEndEvent terminator is allowed as a terminator of this event's associated execution.
     */
    @Override
    public IThreadEndEvent terminator()
    {
      return (IThreadEndEvent) super.terminator();
    }
  }

  /**
   * Data members introduced by this event: the new contour.
   */
  private class TypeLoadEvent extends JiveEvent implements ITypeLoadEvent
  {
    private final IContextContour contour;

    TypeLoadEvent(final IThreadValue thread, final ILineValue line, final IContextContour contour)
    {
      super(thread, line);
      this.contour = contour;
    }

    @Override
    public void commit()
    {
      model.transactionLog().atomicTypeLoad(this, newContour());
    }

    @Override
    public EventKind kind()
    {
      return EventKind.TYPE_LOAD;
    }

    @Override
    public ILineValue line()
    {
      return model().sliceView() == null ? super.line() : model().valueFactory()
          .createUnavailableLine();
    }

    @Override
    public IContextContour newContour()
    {
      return this.contour;
    }
  }

  /**
   * Data members introduced by this event: the newly assigned value.
   */
  private final class VarAssignEvent extends LocalDataEvent implements IVarAssignEvent
  {
    // relevant only for sliced models
    private IValue lastValue;
    private final IValue newValue;

    private VarAssignEvent(final IThreadValue thread, final ILineValue line, final IValue newValue,
        final IContourMember member)
    {
      super(thread, line, member);
      if (member == null)
      {
        throw new IllegalArgumentException("Cannot create the event with a null member.");
      }
      if (newValue == null)
      {
        throw new IllegalArgumentException("Cannot create an assign event with a null new value.");
      }
      this.newValue = newValue;
      this.lastValue = null;
    }

    @Override
    public void commit()
    {
      model.transactionLog().atomicValueSet(this, contour(), member(), newValue());
    }

    // relevant only for sliced models
    @Override
    public IValue getLastValue()
    {
      return model.sliceView() == null ? null : this.lastValue;
    }

    @Override
    public EventKind kind()
    {
      return EventKind.VAR_ASSIGN;
    }

    @Override
    public IValue newValue()
    {
      return this.newValue;
    }

    // relevant only for sliced models
    @Override
    public void setLastAssignment(final IDataEvent event)
    {
      final List<IStateChange> changes = event.transaction().changes();
      if (changes.size() == 1 && changes.get(0) instanceof AtomicUpdate)
      {
        this.lastValue = ((AtomicUpdate) changes.get(0)).oldValue();
      }
    }
  }

  /**
   * Data members introduced by this event: none.
   */
  private final class VarDeleteEvent extends LocalDataEvent implements IVarDeleteEvent
  {
    private VarDeleteEvent(final IThreadValue thread, final ILineValue line,
        final IContourMember variable)
    {
      super(thread, line, variable);
      if (variable == null)
      {
        throw new IllegalArgumentException("Cannot create the event with a null member.");
      }
    }

    @Override
    public void commit()
    {
      model.transactionLog().atomicValueSet(this, contour(), member(),
          model.valueFactory().createUninitializedValue());
    }

    @Override
    public EventKind kind()
    {
      return EventKind.VAR_DELETE;
    }
  }
}