package edu.buffalo.cse.jive.ui.search;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;

/**
 * An {@code ISearchQuery} used to represent a search query over an {@code IJiveDebugTarget}.
 * Implementors should call {@link #performSearch(IProgressMonitor, IJiveDebugTarget)} in
 * {@link ISearchQuery#run(IProgressMonitor)} for the active target.
 */
public interface IJiveSearchQuery extends ISearchQuery
{
  /**
   * Returns an {@code ImageDescriptor} used to represent the search query's result in the Search
   * view's 'Show Previous Searches' tool bar action.
   * 
   * @return an image descriptor representing the result
   */
  ImageDescriptor getImageDescriptor();

  /**
   * Returns a label describing the search result used in the Search view. Typically, this describes
   * what is being searched for and the number of matches, as supplied by <tt>matchCount</tt>.
   * 
   * @param matchCount
   *          the number of matches thus far
   * @return a description of what is being searched
   */
  String getResultLabel(int matchCount);

  /**
   * Returns the type of search result matches collected by the search query. This is used by the
   * Search view to determine how to display the search result tabularly.
   * <p>
   * <em>NOTE:  This will be changed in the future.  For now, return
   * <tt>Event.class</tt> for a result containing matches of mixed event types
   * or return the specific class literal for a result containing matches of
   * a single event type (e.g., <tt>AssignEvent.class</tt>).</em>
   * 
   * @return the type of search result matches
   */
  Class<? extends Object> getResultType();

  @Override
  IJiveSearchResult getSearchResult();

  /**
   * Performs the query over the given {@code IJiveDebugTarget} using the supplied
   * {@code IProgressMonitor}. Implementors can call this method for each target in existence.
   * 
   * @param monitor
   *          the progress monitor of the search
   * @param target
   *          the target to search
   * @return the status after completing the search
   */
  IStatus performSearch(IProgressMonitor monitor, IJiveDebugTarget target);
}
