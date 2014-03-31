package edu.buffalo.cse.jive.debug.jdi.model;

import com.sun.jdi.VirtualMachine;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory.IStaticModelDelegate;

public interface IJDIModelFactory
{
  public IStaticModelDelegate createStaticModelDelegate(final IExecutionModel model,
      VirtualMachine vm, final IModelFilter filter);

  public IJDIManager jdiManager(final IJiveDebugTarget owner);
}