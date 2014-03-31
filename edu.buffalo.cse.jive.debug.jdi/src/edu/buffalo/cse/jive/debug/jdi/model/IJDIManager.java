package edu.buffalo.cse.jive.debug.jdi.model;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IExecutionModel;

public interface IJDIManager
{
  public void done();

  public IExecutionModel executionModel();

  public IExecutionState executionState();

  public boolean generateArrayEvents();

  public boolean generateLocalEvents();

  public boolean isManualStart();

  public IJDIEventHandler jdiHandler();

  public IJiveEventDispatcher jiveDispatcher();

  public IModelFilter modelFilter();

  public IJiveDebugTarget owner();

  public void reset();
}