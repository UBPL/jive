package edu.buffalo.cse.jive.internal.debug.jdi.model;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import edu.buffalo.cse.jive.debug.JiveDebugPlugin;
import edu.buffalo.cse.jive.debug.jdi.model.IExecutionState;
import edu.buffalo.cse.jive.debug.jdi.model.IJDIEventHandler;
import edu.buffalo.cse.jive.debug.jdi.model.IJDIManager;
import edu.buffalo.cse.jive.debug.jdi.model.IJiveEventDispatcher;
import edu.buffalo.cse.jive.debug.jdi.model.IModelFilter;
import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.store.Factory;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;

final class JDIManager implements IJDIManager
{
  private final JiveEventDispatcher jiveDispatcher;
  private final IModelFilter modelFilter;
  private final IExecutionModel executionModel;
  private final boolean generateLocalEvents;
  private final boolean generateArrayEvents;
  private final IJiveDebugTarget owner;
  private final IExecutionState executionState;
  private final IJDIEventHandler jdiHandler;
  private final boolean isManualStart;

  @SuppressWarnings("unchecked")
  JDIManager(final IJiveDebugTarget owner)
  {
    ModelFilter filter = null;
    boolean localEvents = true;
    boolean arrayEvents = false;
    boolean manualStart = false;
    try
    {
      final ILaunchConfigurationWorkingCopy config = owner.getLaunch().getLaunchConfiguration()
          .getWorkingCopy();
      filter = new ModelFilter()
        {
          {
            final List<String> filters = config.getAttribute(PreferencesPlugin.getDefault()
                .getExclusionFiltersKey(), (List<String>) null);
            final Iterator<String> iter = filters == null ? Collections.EMPTY_LIST.iterator()
                : filters.iterator();
            while (iter.hasNext())
            {
              addExclusionFilter(iter.next());
            }
          }
        };
      localEvents = config.getAttribute(PreferencesPlugin.getDefault().getGenerateLocalEventsKey(),
          true);
      arrayEvents = config.getAttribute(PreferencesPlugin.getDefault().getGenerateArrayEventsKey(),
          false);
      manualStart = config.getAttribute(PreferencesPlugin.getDefault().getManualStartEventsKey(),
          false);
    }
    catch (final CoreException e)
    {
      JiveDebugPlugin.log(e);
    }
    // owner of this manager
    this.owner = owner;
    // adapts JDI events to Jive events
    this.jiveDispatcher = new JiveEventDispatcher(this.owner);
    this.jdiHandler = new JDIEventHandler(this.owner);
    // model filter
    this.modelFilter = filter;
    // execution model and its adapter
    this.executionModel = Factory.memoryExecutionModel(jiveDispatcher);
    this.executionModel.traceView().register(owner);
    // optional event generation flags
    this.generateLocalEvents = localEvents;
    this.generateArrayEvents = arrayEvents;
    this.isManualStart = manualStart;
    // State management based on JDI event processing
    this.executionState = new ExecutionState(executionModel);
  }

  @Override
  public void done()
  {
    jiveDispatcher.unsubscribe(this.executionModel);
  }

  @Override
  public IExecutionModel executionModel()
  {
    return this.executionModel;
  }

  @Override
  public IExecutionState executionState()
  {
    return this.executionState;
  }

  @Override
  public boolean generateLocalEvents()
  {
    return this.generateLocalEvents;
  }

  @Override
  public boolean generateArrayEvents()
  {
    return this.generateArrayEvents;
  }

  public IJiveDebugTarget getDebugTarget()
  {
    return this.owner;
  }

  @Override
  public boolean isManualStart()
  {
    return this.isManualStart;
  }

  @Override
  public IJDIEventHandler jdiHandler()
  {
    return this.jdiHandler;
  }

  /**
   * Downstream.
   */
  @Override
  public IJiveEventDispatcher jiveDispatcher()
  {
    return this.jiveDispatcher;
  }

  @Override
  public IModelFilter modelFilter()
  {
    return this.modelFilter;
  }

  @Override
  public IJiveDebugTarget owner()
  {
    return this.owner;
  }

  @Override
  public void reset()
  {
    executionModel.readLock();
    try
    {
      // clears and resets the model store
      executionModel.reset();
      // clears and resets the local state
      executionState.reset();
      // resets the handler
      jdiHandler.reset();
      // clears event queues
      jiveDispatcher.reset();
    }
    finally
    {
      executionModel.readUnlock();
    }
  }
}