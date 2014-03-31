package edu.buffalo.cse.jive.ui;

import org.eclipse.jface.action.IAction;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;

/**
 * An interface used to specify an action for stepping through transactions on an
 * {@code InteractiveContourModel} associated with an {@code IJiveDebugTarget}. An
 * {@code IStepAction} is used as a parameter to either
 * {@link IStepManager#step(IJiveDebugTarget, IStepAction)} or
 * {@link IStepManager#run(IJiveDebugTarget, IStepAction)}.
 * 
 * @see IStepManager
 * @see IStepListener
 */
public interface IStepAction extends IAction
{
  /**
   * Returns whether or not stepping through the states of the underlying execution model associated
   * with the target can be performed.
   * 
   * @param target
   *          the target in which to step
   * @return <code>true</code> if stepping can proceed, <code>false</code> otherwise
   */
  public boolean canStep(IJiveDebugTarget target);

  /**
   * Determines if this step action has changed source lines.
   */
  public boolean lineChanged();

  /**
   * Performs a step through the states of the underlying execution model associated with the
   * target.
   * 
   * @param target
   *          the target in which to step
   */
  public void step(IJiveDebugTarget target);
}
