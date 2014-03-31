package edu.buffalo.cse.jive.ui.search;

import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.Match;

import edu.buffalo.cse.jive.model.IExecutionModel;

/**
 * An {@code ISearchResult} used to represent results of an {@code IJiveSearchQuery}. This will be
 * expanded in the future to support results of different types.
 */
public interface IJiveSearchResult extends ISearchResult
{
  public Object[] getElements();

  public Match[] getMatches(Object element);

  /**
   * A query is performed on a single model.
   */
  public IExecutionModel getModel();

  @Override
  public IJiveSearchQuery getQuery();

  public void setModel(final IExecutionModel model);
}