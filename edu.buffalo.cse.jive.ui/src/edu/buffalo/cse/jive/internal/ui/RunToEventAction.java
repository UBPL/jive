package edu.buffalo.cse.jive.internal.ui;

import org.eclipse.jface.action.Action;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.ui.IStepAction;
import edu.buffalo.cse.jive.ui.IStepManager;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;
import edu.buffalo.cse.jive.ui.view.JiveEventLabelProvider;

final class RunToEventAction extends Action implements IStepAction
{
  private final JiveEventLabelProvider labelProvider = new JiveEventLabelProvider();
  private final IStepManager manager;
  private final IJiveDebugTarget target;
  private final IJiveEvent sourceEvent;
  private final IJiveEvent targetEvent;
  private final boolean isBackward;

  RunToEventAction(final IJiveDebugTarget target, final IJiveEvent event)
  {
    setText(labelProvider.getText(event));
    setImageDescriptor(labelProvider.getImageDescriptor(event));
    this.target = target;
    this.targetEvent = event instanceof IInitiatorEvent ? event.next() : event;
    this.manager = JiveUIPlugin.getDefault().stepManager();
    this.sourceEvent = targetEvent.model().temporalState().event();
    this.isBackward = sourceEvent.eventId() > targetEvent.eventId();
  }

  @Override
  public boolean canStep(final IJiveDebugTarget target)
  {
    final IExecutionModel model = target.model();
    if (model == null)
    {
      return false;
    }
    if (isBackward && model.temporalState().canRollback(targetEvent))
    {
      return true;
    }
    if (!isBackward && model.temporalState().canCommit(targetEvent.prior()))
    {
      return true;
    }
    return false;
  }

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
    manager.run(target, this);
  }

  @Override
  public void step(final IJiveDebugTarget target)
  {
    if (!target.canReplay())
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
    final IExecutionModel model = target.model();
    if (model == null)
    {
      return;
    }
    model.temporalState().consolidateTo(targetEvent);
  }
}