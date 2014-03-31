package edu.buffalo.cse.jive.ui.search;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IQueryModel.EventQuery;
import edu.buffalo.cse.jive.model.factory.IQueryFactory;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;

/**
 * An abstract implementation of an {@code IJiveSearchQuery}. This class handles much of the common
 * implementation details required by the {@code ISearchQuery} interface. The
 * {@link ISearchQuery#run(IProgressMonitor)} method is implemented to call
 * {@link IJiveSearchQuery#performSearch(IProgressMonitor, IQueryFactory)} for each available
 * target. It also handles updating the progress monitor and canceling searches.
 * 
 * @see ISearchQuery
 * @see IJiveSearchQuery
 * @see IProgressMonitor
 * @see #addMatch(Match)
 */
public abstract class JiveSearchQuery implements IJiveSearchQuery
{
  private IJiveSearchResult result;

  protected JiveSearchQuery()
  {
    super();
  }

  @Override
  public boolean canRerun()
  {
    return true;
  }

  @Override
  public boolean canRunInBackground()
  {
    return true;
  }

  @Override
  public String getLabel()
  {
    return "JIVE Search";
  }

  @Override
  public Class<? extends Object> getResultType()
  {
    return IJiveEvent.class;
  }

  @Override
  public IJiveSearchResult getSearchResult()
  {
    if (result == null)
    {
      result = createSearchResult();
    }
    return result;
  }

  @Override
  public IStatus performSearch(final IProgressMonitor monitor, final IJiveDebugTarget target)
  {
    final IExecutionModel model = target.model();
    final IQueryFactory factory = model.queryFactory();
    getSearchResult().setModel(model);
    try
    {
      search(monitor, factory);
      return new Status(IStatus.OK, JiveUIPlugin.PLUGIN_ID, "Search completed on " + target + ".");
    }
    catch (final OperationCanceledException e)
    {
      return new Status(IStatus.CANCEL, JiveUIPlugin.PLUGIN_ID, "Search canceled by user.");
    }
    catch (final Exception e)
    {
      return new Status(IStatus.ERROR, JiveUIPlugin.PLUGIN_ID,
          "An error occurred while searching.", e);
    }
  }

  @Override
  public IStatus run(final IProgressMonitor monitor) throws OperationCanceledException
  {
    // TODO Make the result an IQueryListener instead
    ((AbstractTextSearchResult) result).removeAll();
    final ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    IJiveDebugTarget activeTarget = null;
    // TODO: run the query only on the active debug target!
    for (final IDebugTarget target : manager.getDebugTargets())
    {
      if (target instanceof IJiveDebugTarget && isActive(target))
      {
        activeTarget = (IJiveDebugTarget) target;
        break;
      }
    }
    monitor.beginTask("Searching for events...", 1);
    final IStatus status = performSearch(monitor, activeTarget);
    switch (status.getSeverity())
    {
      case IStatus.OK:
      case IStatus.INFO:
        monitor.worked(1);
        break;
      case IStatus.WARNING:
      case IStatus.ERROR:
        JiveUIPlugin.log(status);
        monitor.worked(1);
        break;
      case IStatus.CANCEL:
        monitor.done();
        return status;
    }
    if (monitor.isCanceled())
    {
      monitor.done();
      return new Status(IStatus.CANCEL, JiveUIPlugin.PLUGIN_ID, "Search canceled by user.");
    }
    monitor.done();
    final JiveSearchResult result = (JiveSearchResult) getSearchResult();
    return new Status(IStatus.OK, JiveUIPlugin.PLUGIN_ID, "Found " + result.getMatchCount()
        + " matches.");
  }

  private boolean isActive(final Object o)
  {
    final IProcess process = DebugUITools.getCurrentProcess();
    return (process.getLaunch().getDebugTarget().equals(o));
  }

  protected abstract EventQuery createQuery(final IQueryFactory queryFactory);

  /**
   * Creates the search result to use for the query. This method is called by
   * {@link #getSearchResult()} when initializing the search result.
   * <p>
   * This method may be overridden by subclasses to provide different types of search results.
   */
  protected IJiveSearchResult createSearchResult()
  {
    return new JiveSearchResult(this);
  }

  protected void search(final IProgressMonitor monitor, final IQueryFactory queryFactory)
  {
    final EventQuery query = createQuery(queryFactory);
    if (query == null)
    {
      return;
    }
    final JiveSearchResult result = (JiveSearchResult) getSearchResult();
    query.open();
    try
    {
      while (!monitor.isCanceled() && query.processEvent())
      {
        // add a result only if the last processed event was a match
        if (query.match() != null)
        {
          result.addMatch(new Match(query.match(), 0, 1));
        }
      }
    }
    finally
    {
      query.close();
    }
  }
}