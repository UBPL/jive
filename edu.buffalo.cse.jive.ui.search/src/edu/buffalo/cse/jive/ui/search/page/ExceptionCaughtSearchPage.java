package edu.buffalo.cse.jive.ui.search.page;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_EVENT_CATCH;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import edu.buffalo.cse.jive.model.IEventModel.IExceptionCatchEvent;
import edu.buffalo.cse.jive.model.IQueryModel.EventQuery;
import edu.buffalo.cse.jive.model.factory.IQueryFactory;
import edu.buffalo.cse.jive.ui.search.IJiveSearchQuery;
import edu.buffalo.cse.jive.ui.search.JiveSearchPage;
import edu.buffalo.cse.jive.ui.search.JiveSearchQuery;
import edu.buffalo.cse.jive.ui.search.form.FormFactory;
import edu.buffalo.cse.jive.ui.search.form.SearchForm.ExceptionForm;

/**
 * View portion encapsulating a search form.
 * 
 * When a class name is given, only exceptions caught by the class are matched. Providing an
 * instance number or method name limits the matches to exceptions caught by the particular instance
 * or in the given method, respectively. When an exception name is supplied, matches are limited to
 * exceptions starting with that name.
 */
public class ExceptionCaughtSearchPage extends JiveSearchPage
{
  private ExceptionForm searchForm;

  @Override
  public void createControl(final Composite parent)
  {
    searchForm = FormFactory.createExceptionForm(this, parent);
  }

  @Override
  public IJiveSearchQuery createSearchQuery()
  {
    return new ExceptionCaughtSearchQuery();
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
    searchForm.setMethodText(parser.getMethodName());
  }

  @Override
  public boolean isInputValid()
  {
    final String className = searchForm.classText();
    final String instanceNumber = searchForm.instanceText();
    if (!instanceNumber.equals(""))
    {
      if (!instanceNumber.matches("[1-9]\\d*"))
      {
        return false;
      }
      else
      {
        if (className.equals(""))
        {
          return false;
        }
      }
    }
    if (!searchForm.methodText().equals("") && className.equals(""))
    {
      return false;
    }
    return true;
  }

  /**
   * View portion encapsulating an event predicate query.
   */
  private class ExceptionCaughtSearchQuery extends JiveSearchQuery
  {
    @Override
    public ImageDescriptor getImageDescriptor()
    {
      return IM_EVENT_CATCH.enabledDescriptor();
    }

    @Override
    public String getResultLabel(final int matchCount)
    {
      return "'" + searchForm.toString() + "'" + " - " + matchCount
          + (matchCount == 1 ? " exception caught" : " exceptions caught");
    }

    @Override
    public Class<? extends Object> getResultType()
    {
      return IExceptionCatchEvent.class;
    }

    @Override
    protected EventQuery createQuery(final IQueryFactory predicateFactory)
    {
      return predicateFactory.createExceptionCaughtQuery(searchForm);
    }
  }
}