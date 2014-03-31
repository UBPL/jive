package edu.buffalo.cse.jive.model.factory;

import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IObjectContour;
import edu.buffalo.cse.jive.model.IEventModel;

/**
 * Entry point for creating objects of the event model.
 */
public interface IEventFactory extends IEventModel
{
  public IJiveEvent createDestroyEvent(IThreadValue thread, ILineValue line, IObjectContour contour);

  public IJiveEvent createExceptionCatchEvent(IThreadValue thread, ILineValue line,
      IValue exception, IContourMember member);

  public IJiveEvent createExceptionThrowEvent(IThreadValue thread, ILineValue line,
      IValue exception, IValue thrower, boolean wasFramePopped);

  public IJiveEvent createFieldReadEvent(IThreadValue thread, ILineValue line,
      IContextContour contour, IContourMember member);

  public IJiveEvent createFieldWriteEvent(IThreadValue thread, ILineValue line,
      IContextContour contour, IValue newValue, IContourMember member);

  public IJiveEvent createLineStepEvent(IThreadValue thread, ILineValue line);

  public IJiveEvent createLockEvent(final IThreadValue thread, ILineValue line,
      final LockOperation lockOperation, final IContour lock, final String lockDescription);

  public IJiveEvent createMethodCallEvent(IThreadValue thread, ILineValue line, IValue caller,
      IValue target);

  public IJiveEvent createMethodEnteredEvent(IThreadValue thread, ILineValue line);

  public IJiveEvent createMethodExitEvent(IThreadValue thread, ILineValue line);

  public IJiveEvent createMethodReturnedEvent(IMethodTerminatorEvent terminator);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createMonitorLockBeginEvent(long timestamp, IThreadValue thread,
      ILineValue line, String monitor);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createMonitorLockEndEvent(long timestamp, IThreadValue thread, ILineValue line,
      String monitor);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createMonitorLockFastEvent(long timestamp, IThreadValue thread,
      ILineValue line, String monitor);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createMonitorRelockEvent(long timestamp, IThreadValue thread, ILineValue line,
      String monitor);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createMonitorUnlockBeginEvent(long timestamp, IThreadValue thread,
      ILineValue line, String monitor);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createMonitorUnlockCompleteEvent(long timestamp, IThreadValue thread,
      ILineValue line, String monitor);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createMonitorUnlockEndEvent(long timestamp, IThreadValue thread,
      ILineValue line, String monitor);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createMonitorUnlockFastEvent(long timestamp, IThreadValue thread,
      ILineValue line, String monitor);

  public IJiveEvent createNewObjectEvent(IThreadValue thread, ILineValue line,
      IObjectContour contour);

  public IJiveEvent createRTDestroyEvent(long timestamp, IThreadValue thread, ILineValue line,
      IObjectContour contour);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createRTFieldWriteEvent(long timestamp, IThreadValue thread, ILineValue line,
      IContextContour contour, IValue newValue, IContourMember member);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createRTMethodCallEvent(long timestamp, IThreadValue thread, ILineValue line,
      IValue caller, IValue target);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createRTMethodEnteredEvent(long timestamp, IThreadValue thread, ILineValue line);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createRTMethodExitEvent(long timestamp, IThreadValue thread, ILineValue line);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createRTNewObjectEvent(long timestamp, IThreadValue thread, ILineValue line,
      IObjectContour contour, String scope);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createRTSystemExitEvent(long timestamp);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createRTSystemStartEvent(long timestamp);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createRTThreadEndEvent(long timestamp, IThreadValue thread);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createRTThreadNewEvent(long timestamp, IThreadValue thread, ILineValue line,
      IObjectContour contour, long newThreadId);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createRTThreadPriorityEvent(long timestamp, IThreadValue thread,
      String scheduler, int priority);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createRTThreadSleepEvent(long timestamp, IThreadValue thread, long waketime);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createRTThreadStartEvent(long timestamp, IThreadValue thread, String scheduler,
      int priority);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createRTThreadWakeEvent(long timestamp, IThreadValue thread, long waketime);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createRTThreadYieldEvent(long timestamp, IThreadValue thread, long waketime);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createRTTypeLoadEvent(long timestamp, IThreadValue thread, ILineValue line,
      IContextContour contour);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createScopeAllocEvent(long timestamp, IThreadValue thread, ILineValue line,
      String scope, int size, boolean b);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createScopeAssignEvent(long timestamp, IThreadValue thread, ILineValue line,
      String scopeLHS, int indexLHS, long lhs, String scopeRHS, int indexRHS, long rhs);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createScopeBackingAllocEvent(long timestamp, IThreadValue thread,
      ILineValue line, int size);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createScopeBackingFreeEvent(long timestamp, IThreadValue thread, ILineValue line);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createScopeEnterEvent(long timestamp, IThreadValue thread, ILineValue line,
      String scope);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createScopeExitEvent(long timestamp, IThreadValue thread, ILineValue line,
      String scope);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createScopeFreeEvent(long timestamp, IThreadValue thread, ILineValue line,
      String scope);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createScopePopEvent(long timestamp, IThreadValue thread, ILineValue line,
      String scope);

  /**
   * Fiji specific event.
   */
  public IJiveEvent createScopePushEvent(long timestamp, IThreadValue thread, ILineValue line,
      String scope);

  public IJiveEvent createSystemExitEvent();

  public IJiveEvent createThreadEndEvent(IThreadValue thread);

  public IJiveEvent createTypeLoadEvent(IThreadValue thread, ILineValue line,
      IContextContour contour);

  public IJiveEvent createVarAssignEvent(IThreadValue thread, ILineValue line, IValue value,
      IContourMember member);

  public IJiveEvent createVarDeleteEvent(IThreadValue thread, ILineValue line, IContourMember member);
}