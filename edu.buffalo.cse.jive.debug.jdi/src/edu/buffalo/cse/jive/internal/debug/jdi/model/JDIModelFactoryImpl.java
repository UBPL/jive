package edu.buffalo.cse.jive.internal.debug.jdi.model;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.sun.jdi.VirtualMachine;

import edu.buffalo.cse.jive.debug.jdi.model.IJDIManager;
import edu.buffalo.cse.jive.debug.jdi.model.IJDIModelFactory;
import edu.buffalo.cse.jive.debug.jdi.model.IModelFilter;
import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory.IStaticModelDelegate;

public enum JDIModelFactoryImpl implements IJDIModelFactory
{
  INSTANCE;
  private final ConcurrentMap<Object, JDIManager> adapterMap = new ConcurrentHashMap<Object, JDIManager>(
      16, 0.75F, 4);

  @Override
  public IStaticModelDelegate createStaticModelDelegate(final IExecutionModel model,
      final VirtualMachine vm, final IModelFilter filter)
  {
    return new StaticModelDelegateForJDI(model, vm, filter);
  }

  /**
   * The factory instance depends on the debug target. A different event factory and trace store is
   * associated with each target since event streams are numbered based on the target.
   */
  @Override
  public IJDIManager jdiManager(final IJiveDebugTarget owner)
  {
    if (adapterMap.get(owner) == null)
    {
      adapterMap.putIfAbsent(owner, new JDIManager(owner));
    }
    return adapterMap.get(owner);
  }
}