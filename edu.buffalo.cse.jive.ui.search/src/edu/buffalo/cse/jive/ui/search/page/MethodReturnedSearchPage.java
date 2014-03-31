package edu.buffalo.cse.jive.ui.search.page;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_EVENT_METHOD_RETURN;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import edu.buffalo.cse.jive.model.IEventModel.IMethodReturnedEvent;
import edu.buffalo.cse.jive.model.IQueryModel.EventQuery;
import edu.buffalo.cse.jive.model.RelationalOperator;
import edu.buffalo.cse.jive.model.factory.IQueryFactory;
import edu.buffalo.cse.jive.ui.search.IJiveSearchQuery;
import edu.buffalo.cse.jive.ui.search.JiveSearchPage;
import edu.buffalo.cse.jive.ui.search.JiveSearchQuery;
import edu.buffalo.cse.jive.ui.search.form.FormFactory;
import edu.buffalo.cse.jive.ui.search.form.SearchForm.MethodReturnedForm;

/**
 * View portion encapsulating a search form.
 * 
 * When an instance number is given, returns only on that particular instance of the class are
 * included in the result. When it is left out, returns on any instance of the class are included in
 * the result. Additionally, if a condition on the return value is given, then only returns meeting
 * this condition will be included in the result.
 */
public class MethodReturnedSearchPage extends JiveSearchPage
{
  private MethodReturnedForm searchForm;

  @Override
  public void createControl(final Composite parent)
  {
    searchForm = FormFactory.createMethodReturnedForm(this, parent);
  }

  @Override
  public IJiveSearchQuery createSearchQuery()
  {
    return new MethodReturnedSearchQuery();
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
    final RelationalOperator operator = RelationalOperator.fromString(searchForm.operatorText());
    searchForm.setReturnValueEnabled(operator != RelationalOperator.NONE);
    // @change 04/22/10 by Demian - must provide either a class name or a method name
    if (searchForm.classText().equals("") && searchForm.methodText().equals(""))
    {
      return false;
    }
    final String instanceNumber = searchForm.instanceText();
    if (!instanceNumber.equals("") && !instanceNumber.matches("[1-9]\\d*"))
    {
      return false;
    }
    if (operator == RelationalOperator.NONE)
    {
      return true;
    }
    return !searchForm.returnValueText().equals("");
  }

  /**
   * View portion encapsulating an event predicate query.
   */
  private class MethodReturnedSearchQuery extends JiveSearchQuery
  {
    @Override
    public ImageDescriptor getImageDescriptor()
    {
      return IM_EVENT_METHOD_RETURN.enabledDescriptor();
    }

    @Override
    public String getResultLabel(final int matchCount)
    {
      return "'" + searchForm.toString() + "' - " + matchCount
          + (matchCount == 1 ? " return" : " returns");
    }

    @Override
    public Class<? extends Object> getResultType()
    {
      return IMethodReturnedEvent.class;
    }

    @Override
    protected EventQuery createQuery(final IQueryFactory predicateFactory)
    {
      return predicateFactory.createMethodReturnedQuery(searchForm);
    }
  }
}