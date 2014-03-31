package edu.buffalo.cse.jive.ui;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;

/**
 * An interface used to manage stepping on an {@code IJiveDebugTarget}. Stepping typically consists
 * of traversing the transaction history of an {@code InteractiveContourModel} associated with the
 * target. Both single stepping and running are supported. In the case of running, pausing is also
 * available.
 * 
 * @see IStepAction
 * @see IStepListener
 */
public interface IStepManager
{
  /**
   * Adds an {@code IStepListener} to be notified when a step is initiated or completed.
   * 
   * @param listener
   *          the listener to be added
   */
  public void addStepListener(IStepListener listener);

  /**
   * Returns whether the manager is in the progress of stepping over the transaction history of the
   * supplied debug target's contour model.
   * 
   * @param target
   *          the target that may be stepping
   * @return <code>true</code> if stepping is in progress, <code>false</code> otherwise
   */
  public boolean isStepping(IJiveDebugTarget target);

  // public IJiveEvent lastCommittedEvent(IJiveDebugTarget target);
  /**
   * Pauses stepping on the supplied target by canceling any requested steps and notifying listeners
   * that stepping has completed.
   * 
   * @param target
   */
  public void pause(IJiveDebugTarget target);

  /**
   * Removes an {@code IStepListener} from being notified when a step is initiated or completed.
   * 
   * @param listener
   *          the listener to remove
   */
  public void removeStepListener(IStepListener listener);

  /**
   * Initiates running on the supplied target by delegating to the given action. This method behaves
   * similar to {@link #step(IJiveDebugTarget, IStepAction)} except for the following differences:
   * <ul>
   * <li>{@link IStepAction#step(IJiveDebugTarget)} is called repeatedly until either
   * {@link IStepAction#canStep(IJiveDebugTarget)} or {@link #isStepping(IJiveDebugTarget)} returns
   * false,</li>
   * <li>each call to {@link IStepAction#step(IJiveDebugTarget)} occurrs on a separate
   * {@code Runnable}, and</li>
   * <li>listeners are notified only when the first step is initiated and the last step is
   * completed.</li>
   * </ul>
   * 
   * @param target
   *          the target on which to run
   * @param action
   *          the action used to run
   */
  public void run(IJiveDebugTarget target, IStepAction action);

  /**
   * Performs a single step on the supplied target by delegating to the given action. First
   * {@link IStepAction#canStep(IJiveDebugTarget)} is called. If this returns <code>true</code>,
   * then the following occurs:
   * <ol>
   * <li>listeners are notified that a step is being initiated,</li>
   * <li>{@link IStepAction#step(IJiveDebugTarget)} is called, and</li>
   * <li>listeners are notified that a step has completed.</li>
   * </ol>
   * 
   * @param target
   *          the target on which to step
   * @param action
   *          the action used to step
   */
  public void step(IJiveDebugTarget target, IStepAction action);
}
