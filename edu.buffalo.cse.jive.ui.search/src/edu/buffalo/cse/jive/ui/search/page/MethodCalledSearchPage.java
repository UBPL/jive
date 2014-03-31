package edu.buffalo.cse.jive.ui.search.page;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_EVENT_METHOD_CALL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IQueryModel.EventQuery;
import edu.buffalo.cse.jive.model.factory.IQueryFactory;
import edu.buffalo.cse.jive.ui.search.IJiveSearchQuery;
import edu.buffalo.cse.jive.ui.search.JiveSearchPage;
import edu.buffalo.cse.jive.ui.search.JiveSearchQuery;
import edu.buffalo.cse.jive.ui.search.form.FormFactory;
import edu.buffalo.cse.jive.ui.search.form.SearchForm.MethodForm;

/**
 * View portion encapsulating a search form.
 * 
 * When an instance number is given, calls only on that particular instance of the class are
 * included in the result. When it is left out, calls on any instance of the class are included in
 * the result.
 */
public class MethodCalledSearchPage extends JiveSearchPage
{
  private MethodForm searchForm;

  @Override
  public void createControl(final Composite parent)
  {
    searchForm = FormFactory.createMethodCalledForm(this, parent);
  }

  @Override
  public IJiveSearchQuery createSearchQuery()
  {
    return new MethodCalledSearchQuery();
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
    // @change 04/22/10 by Demian - must provide either a class name or a method name
    if (searchForm.classText().equals("") && searchForm.methodText().equals(""))
    {
      return false;
    }
    final String instanceNumber = searchForm.instanceText();
    return instanceNumber.equals("") || instanceNumber.matches("[1-9]\\d*");
  }

  /**
   * View portion encapsulating an event predicate query.
   */
  private class MethodCalledSearchQuery extends JiveSearchQuery
  {
    @Override
    public ImageDescriptor getImageDescriptor()
    {
      return IM_EVENT_METHOD_CALL.enabledDescriptor();
    }

    @Override
    public String getResultLabel(final int matchCount)
    {
      return "'" + searchForm.toString() + "' - " + matchCount
          + (matchCount == 1 ? " call" : " calls");
    }

    @Override
    public Class<? extends Object> getResultType()
    {
      return IMethodCallEvent.class;
    }

    @Override
    protected EventQuery createQuery(final IQueryFactory predicateFactory)
    {
      return predicateFactory.createMethodCalledQuery(searchForm);
    }
  }
}