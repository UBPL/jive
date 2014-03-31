package edu.buffalo.cse.jive.ui;

import org.eclipse.jface.action.IMenuManager;

import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;

public interface IJumpMenuManager
{
  public boolean createJumpAction(IJiveEvent event, IMenuManager manager);

  public void createJumpToMenu(Object model, IMenuManager manager, boolean callsOnly);
  // public boolean isJumpAction(IJiveEvent event);
}