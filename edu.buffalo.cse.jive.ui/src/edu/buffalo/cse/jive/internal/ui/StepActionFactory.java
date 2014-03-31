package edu.buffalo.cse.jive.internal.ui;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_TM_PAUSE;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_TM_RUN_BACKWARD;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_TM_RUN_FORWARD;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_TM_STEP_BACKWARD;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_TM_STEP_FORWARD;

import org.eclipse.jface.action.Action;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.ui.IJiveView;
import edu.buffalo.cse.jive.ui.IStepAction;
import edu.buffalo.cse.jive.ui.IStepActionFactory;
import edu.buffalo.cse.jive.ui.IStepManager;
import edu.buffalo.cse.jive.ui.IUpdatableStepAction;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;

final class StepActionFactory implements IStepActionFactory
{
  private final IStepManager manager;

  StepActionFactory(final IJiveView view)
  {
    manager = JiveUIPlugin.getDefault().stepManager();
  }

  @Override
  public IUpdatableStepAction createPauseAction()
  {
    return new PauseAction();
  }

  @Override
  public IUpdatableStepAction createRunBackwardAction()
  {
    return new RunBackwardAction();
  }

  @Override
  public IUpdatableStepAction createRunForwardAction()
  {
    return new RunForwardAction();
  }

  @Override
  public IUpdatableStepAction createStepBackwardAction()
  {
    return new StepBackwardAction();
  }

  @Override
  public IUpdatableStepAction createStepForwardAction()
  {
    return new StepForwardAction();
  }

  private IJiveDebugTarget getActiveTarget()
  {
    return JiveLaunchPlugin.getDefault().getLaunchManager().activeTarget();
  }

  /**
   * An {@code Action} that steps through an {@code IJiveDebugTarget}'s transaction history until no
   * more steps should be taken or stepping is paused. The action can update itself based on the
   * state of the debug target and its contour model.
   * 
   * @see IStepManager
   * @see IStepAction
   */
  private abstract class AbstractRunAction extends AbstractStepAction
  {
    protected IJiveEvent sourceEvent = null;

    @Override
    public boolean lineChanged()
    {
      final IJiveEvent current = sourceEvent.model().temporalState().event();
      if (current == null || sourceEvent == null)
      {
        return current != sourceEvent;
      }
      return sourceEvent.line() != current.line();
    }

    @Override
    public void run()
    {
      sourceEvent = getActiveTarget().model().temporalState().event();
      manager.run(getActiveTarget(), this);
    }
  }

  /**
   * An {@code Action} that takes one step in an {@code IJiveDebugTarget}'s transaction history. The
   * action can update itself based on the state of the debug target and its contour model.
   * 
   * @see IStepManager
   * @see IStepAction
   */
  private abstract class AbstractStepAction extends Action implements IUpdatableStepAction
  {
    protected IJiveEvent sourceEvent = null;

    @Override
    public boolean lineChanged()
    {
      final IJiveEvent current = sourceEvent.model().temporalState().event();
      if (current == null || sourceEvent == null)
      {
        return current != sourceEvent;
      }
      return sourceEvent.line() != current.line();
    }

    @Override
    public void run()
    {
      sourceEvent = getActiveTarget().model().temporalState().event();
      manager.step(getActiveTarget(), this);
    }

    @Override
    public void steppingCompleted(final IJiveDebugTarget target, final IStepAction action)
    {
      if (target == getActiveTarget())
      {
        setEnabled(canStep(target));
      }
    }

    @Override
    public void steppingInitiated(final IJiveDebugTarget target)
    {
      if (target == getActiveTarget())
      {
        setEnabled(false);
      }
    }

    @Override
    public void update()
    {
      if (canReplay(getActiveTarget()))
      {
        setEnabled(canStep(getActiveTarget()));
      }
      else
      {
        setEnabled(false);
      }
    }

    /**
     * Determines if the supplied target (in it's current state) can replay states recorded by the
     * execution model.
     */
    private boolean canReplay(final IJiveDebugTarget target)
    {
      return target != null && !manager.isStepping(target) && target.canReplay();
    }
  }

  /**
   * An {@code Action} that pauses over an {@code IJiveDebugTarget}'s transaction history that may
   * be in progress. The action can update itself based on the state of the debug target and its
   * contour model.
   */
  private final class PauseAction extends Action implements IUpdatableStepAction
  {
    private PauseAction()
    {
      setText("Pause");
      setImageDescriptor(IM_TM_PAUSE.enabledDescriptor());
      setDisabledImageDescriptor(IM_TM_PAUSE.disabledDescriptor());
    }

    @Override
    public boolean canStep(final IJiveDebugTarget target)
    {
      return false;
    }

    @Override
    public boolean lineChanged()
    {
      return false;
    }

    @Override
    public void run()
    {
      manager.pause(getActiveTarget());
    }

    @Override
    public void step(final IJiveDebugTarget target)
    {
    }

    @Override
    public void steppingCompleted(final IJiveDebugTarget target, final IStepAction action)
    {
      if (target == getActiveTarget())
      {
        setEnabled(false);
      }
    }

    @Override
    public void steppingInitiated(final IJiveDebugTarget target)
    {
      if (target == getActiveTarget())
      {
        setEnabled(true);
      }
    }

    @Override
    public void update()
    {
      final IJiveDebugTarget target = getActiveTarget();
      if (target != null)
      {
        if (target.canReplay() && manager.isStepping(target))
        {
          setEnabled(true);
          return;
        }
        if (target.canSuspend())
        {
          setEnabled(true);
          return;
        }
      }
      setEnabled(false);
    }
  }

  /**
   * An {@code IAction} that runs backward in an {@code IJiveDebugTarget}'s transaction history
   * until the first transaction has been rolled back. The action can update itself based on the
   * state of the debug target and its contour model.
   */
  private final class RunBackwardAction extends AbstractRunAction
  {
    private RunBackwardAction()
    {
      setText("Run Backward");
      setImageDescriptor(IM_TM_RUN_BACKWARD.enabledDescriptor());
      setDisabledImageDescriptor(IM_TM_RUN_BACKWARD.disabledDescriptor());
    }

    @Override
    public boolean canStep(final IJiveDebugTarget target)
    {
      final IExecutionModel model = target.model();
      return (model != null) && model.temporalState().canRollback();
    }

    @Override
    public void step(final IJiveDebugTarget target)
    {
      final IExecutionModel model = target.model();
      if (model != null)
      {
        model.temporalState().setInitialState();
      }
    }
  }

  /**
   * An {@code IAction} that runs forward in an {@code IJiveDebugTarget}'s until the last
   * transaction has been committed. The action can update itself based on the state of the debug
   * target and its contour model.
   */
  private final class RunForwardAction extends AbstractRunAction
  {
    private RunForwardAction()
    {
      setText("Run Forward");
      setImageDescriptor(IM_TM_RUN_FORWARD.enabledDescriptor());
      setDisabledImageDescriptor(IM_TM_RUN_FORWARD.disabledDescriptor());
    }

    @Override
    public boolean canStep(final IJiveDebugTarget target)
    {
      final IExecutionModel model = target.model();
      return (model != null) && model.temporalState().canReplayCommit();
    }

    @Override
    public void step(final IJiveDebugTarget target)
    {
      final IExecutionModel model = target.model();
      if (model != null)
      {
        model.temporalState().setFinalState();
      }
    }
  }

  /**
   * An {@code IAction} that takes one step backward through an {@code IJiveDebugTarget}'s
   * transaction history. The action can update itself based on the state of the debug target and
   * its contour model.
   */
  private final class StepBackwardAction extends AbstractStepAction
  {
    private StepBackwardAction()
    {
      setText("Step Backward");
      setImageDescriptor(IM_TM_STEP_BACKWARD.enabledDescriptor());
      setDisabledImageDescriptor(IM_TM_STEP_BACKWARD.disabledDescriptor());
    }

    @Override
    public boolean canStep(final IJiveDebugTarget target)
    {
      final IExecutionModel model = target.model();
      return (model != null) && model.temporalState().canRollback();
    }

    @Override
    public void step(final IJiveDebugTarget target)
    {
      final IExecutionModel model = target.model();
      if (model == null)
      {
        return;
      }
      model.temporalState().rollback();
    }
  }

  /**
   * An {@code IAction} that takes one step forward through an {@code IJiveDebugTarget}'s
   * transaction history. The action can update itself based on the state of the debug target and
   * its contour model.
   */
  private final class StepForwardAction extends AbstractStepAction
  {
    private StepForwardAction()
    {
      setText("Step Forward");
      setImageDescriptor(IM_TM_STEP_FORWARD.enabledDescriptor());
      setDisabledImageDescriptor(IM_TM_STEP_FORWARD.disabledDescriptor());
    }

    @Override
    public boolean canStep(final IJiveDebugTarget target)
    {
      final IExecutionModel model = target.model();
      return (model != null) && model.temporalState().canReplayCommit();
    }

    @Override
    public void step(final IJiveDebugTarget target)
    {
      final IExecutionModel model = target.model();
      if (model == null)
      {
        return;
      }
      model.temporalState().commit();
    }
  }
}