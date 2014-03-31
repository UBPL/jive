package edu.buffalo.cse.jive.ui;

import org.eclipse.jface.action.IAction;

/**
 * An {@code IAction} that can update its own state, for instance, to enable or disable itself.
 */
public interface IUpdatableAction extends IAction
{
  /**
   * Updates the state of the action.
   */
  public void update();
}