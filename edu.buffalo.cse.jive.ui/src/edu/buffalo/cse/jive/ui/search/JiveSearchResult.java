package edu.buffalo.cse.jive.ui.search;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;

import edu.buffalo.cse.jive.model.IExecutionModel;

/**
 * A search result for an {@code IJiveSearchQuery}. Currently, this uses much of the functionality
 * provided by {@link AbstractTextSearchResult}.
 */
public class JiveSearchResult extends AbstractTextSearchResult implements IJiveSearchResult
{
  private IExecutionModel model;
  private final IJiveSearchQuery query;

  public JiveSearchResult(final IJiveSearchQuery query)
  {
    this.query = query;
  }

  @Override
  public IEditorMatchAdapter getEditorMatchAdapter()
  {
    return null;
  }

  @Override
  public IFileMatchAdapter getFileMatchAdapter()
  {
    return null;
  }

  @Override
  public ImageDescriptor getImageDescriptor()
  {
    return query.getImageDescriptor();
  }

  @Override
  public String getLabel()
  {
    return query.getResultLabel(getMatchCount());
  }

  @Override
  public IExecutionModel getModel()
  {
    return model;
  }

  @Override
  public IJiveSearchQuery getQuery()
  {
    return query;
  }

  @Override
  public String getTooltip()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setModel(final IExecutionModel model)
  {
    this.model = model;
  }
}