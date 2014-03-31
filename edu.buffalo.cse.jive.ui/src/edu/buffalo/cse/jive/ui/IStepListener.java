package edu.buffalo.cse.jive.ui;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;

/**
 * An interface used for those who need to be notified when stepping occurs on a
 * {@code IJiveDebugTarget}. Stepping typically consists of traversing the transaction history of an
 * {@code InteractiveContourModel} associated with the target. Classes conforming to this interface
 * are notified when stepping is initiated and when it is completed. Stepping can refer to either
 * single steps or multiple steps (i.e., running).
 * 
 * @see IStepAction
 * @see IStepManager
 */
public interface IStepListener
{
  /**
   * Called when stepping has completed on the supplied target.
   * 
   * @param target
   *          the target upon which stepping has completed
   * @param action
   */
  public void steppingCompleted(IJiveDebugTarget target, IStepAction action);

  /**
   * Called when stepping has been initiated on the supplied target.
   * 
   * @param target
   *          the target upon which stepping was initiated
   */
  public void steppingInitiated(IJiveDebugTarget target);
}
