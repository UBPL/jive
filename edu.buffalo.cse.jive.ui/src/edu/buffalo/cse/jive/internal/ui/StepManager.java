package edu.buffalo.cse.jive.internal.ui;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.widgets.Display;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.ui.IStepAction;
import edu.buffalo.cse.jive.ui.IStepListener;
import edu.buffalo.cse.jive.ui.IStepManager;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;

/**
 * {@code IStepManager} implementation used to perform stepping on {@code IJiveDebugTarget}s.
 * 
 * @see IStepManager
 * @see IStepAction
 * @see IStepListener
 */
final class StepManager implements IStepManager
{
  /**
   * {@code IJiveDebugTarget} list in which stepping is in progress.
   */
  private final List<IJiveDebugTarget> steppingList;
  /**
   * {@code IStepListener} list to notify on step occurrences.
   */
  private final List<IStepListener> updateList;

  StepManager()
  {
    updateList = new LinkedList<IStepListener>();
    steppingList = new ArrayList<IJiveDebugTarget>();
  }

  @Override
  public void addStepListener(final IStepListener listener)
  {
    if (!updateList.contains(listener))
    {
      updateList.add(listener);
    }
  }

  @Override
  public synchronized boolean isStepping(final IJiveDebugTarget target)
  {
    return steppingList.contains(target);
  }

  @Override
  public synchronized void pause(final IJiveDebugTarget target)
  {
    if (isStepping(target))
    {
      completeStep(target, null);
    }
    else if (target.canSuspend())
    {
      try
      {
        target.suspend();
      }
      catch (final Exception e)
      {
        JiveUIPlugin.log(e.getMessage());
      }
    }
  }

  @Override
  public void removeStepListener(final IStepListener listener)
  {
    if (updateList.contains(listener))
    {
      updateList.remove(listener);
    }
  }

  @Override
  public synchronized void run(final IJiveDebugTarget target, final IStepAction action)
  {
    if (action.canStep(target))
    {
      initiateStep(target);
      requestStep(target, action);
    }
  }

  @Override
  public synchronized void step(final IJiveDebugTarget target, final IStepAction action)
  {
    if (action.canStep(target))
    {
      initiateStep(target);
      action.step(target);
      completeStep(target, action);
    }
  }

  /**
   * Completes stepping on the supplied target by recording that a stepping is no longer in progress
   * and by notifying any listeners.
   * 
   * @param target
   *          the target in which stepping has completed
   */
  private synchronized void completeStep(final IJiveDebugTarget target, final IStepAction action)
  {
    assert isStepping(target);
    steppingList.remove(target);
    fireStepCompletedEvent(target, action);
  }

  /**
   * Notifies listeners that stepping has completed on the given target.
   * 
   * @param target
   *          the target in which stepping has completed
   */
  private void fireStepCompletedEvent(final IJiveDebugTarget target, final IStepAction action)
  {
    for (final IStepListener listener : updateList)
    {
      listener.steppingCompleted(target, action);
    }
  }

  /**
   * Notifies listeners that stepping has initiated on the given target.
   * 
   * @param target
   *          the target in which stepping has initiated
   */
  private void fireStepInitiatedEvent(final IJiveDebugTarget target)
  {
    for (final IStepListener listener : updateList)
    {
      listener.steppingInitiated(target);
    }
  }

  /**
   * Initiates stepping on the supplied target by recording that stepping is in progress and by
   * notifying any listeners.
   * 
   * @param target
   *          the target in which stepping is being initiated
   */
  private synchronized void initiateStep(final IJiveDebugTarget target)
  {
    assert !isStepping(target);
    steppingList.add(target);
    fireStepInitiatedEvent(target);
  }

  /**
   * Requests another step to be taken on the supplied target by delegating to the given action. The
   * step is performed on a separate {@code Runnable} in order to allow pausing.
   * 
   * @param target
   *          the target on which to step
   * @param action
   *          the step action to use
   */
  private synchronized void requestStep(final IJiveDebugTarget target, final IStepAction action)
  {
    final Display display = JiveUIPlugin.getStandardDisplay();
    display.asyncExec(new Runnable()
      {
        @Override
        public void run()
        {
          // Check if stepping was not paused
          if (isStepping(target))
          {
            if (action.canStep(target))
            {
              action.step(target); // take the step
              requestStep(target, action); // recursively request another step
            }
            else
            {
              completeStep(target, action);
            }
          }
        }
      });
  }

  /**
   * Cleans up the manager when it is no longer needed.
   */
  void dispose()
  {
    updateList.clear();
    steppingList.clear();
  }
}