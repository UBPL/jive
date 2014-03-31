package edu.buffalo.cse.jive.ui.search.page;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_EVENT_NEW;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import edu.buffalo.cse.jive.model.IEventModel.INewObjectEvent;
import edu.buffalo.cse.jive.model.IQueryModel.EventQuery;
import edu.buffalo.cse.jive.model.factory.IQueryFactory;
import edu.buffalo.cse.jive.ui.search.IJiveSearchQuery;
import edu.buffalo.cse.jive.ui.search.JiveSearchPage;
import edu.buffalo.cse.jive.ui.search.JiveSearchQuery;
import edu.buffalo.cse.jive.ui.search.form.FormFactory;
import edu.buffalo.cse.jive.ui.search.form.SearchForm.ObjectCreatedForm;

/**
 * View portion encapsulating a search form.
 * 
 * When an instance number is given, creation of only that particular instance of the class are
 * included in the result. When it is left out, creation of all instances of the class are included
 * in the result.
 */
public class ObjectCreatedSearchPage extends JiveSearchPage
{
  private ObjectCreatedForm searchForm;

  @Override
  public void createControl(final Composite parent)
  {
    searchForm = FormFactory.createObjectCreatedForm(this, parent);
  }

  @Override
  public IJiveSearchQuery createSearchQuery()
  {
    return new ObjectCreatedSearchQuery();
  }

  @Override
  public Control getControl()
  {
    return searchForm.control();
  }

  @Override
  public void initializeInput(final ISelection selection)
  {
    final SelectionTokenizer parser = new SelectionTokenizer(selection);
    searchForm.setClassText(parser.getClassName());
  }

  @Override
  public boolean isInputValid()
  {
    if (searchForm.classText().equals(""))
    {
      return false;
    }
    final String instanceNumber = searchForm.instanceText();
    return instanceNumber.equals("") || instanceNumber.matches("[1-9]\\d*");
  }

  /**
   * View portion encapsulating an event predicate query.
   */
  private class ObjectCreatedSearchQuery extends JiveSearchQuery
  {
    @Override
    public ImageDescriptor getImageDescriptor()
    {
      return IM_EVENT_NEW.enabledDescriptor();
    }

    @Override
    public String getResultLabel(final int matchCount)
    {
      return "'" + searchForm.toString() + "' - " + matchCount
          + (matchCount == 1 ? " creation" : " creations");
    }

    @Override
    public Class<? extends Object> getResultType()
    {
      return INewObjectEvent.class;
    }

    @Override
    protected EventQuery createQuery(final IQueryFactory predicateFactory)
    {
      return predicateFactory.createObjectCreatedQuery(searchForm);
    }
  }
}