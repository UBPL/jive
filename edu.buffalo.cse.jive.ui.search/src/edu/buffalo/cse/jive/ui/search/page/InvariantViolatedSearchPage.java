package edu.buffalo.cse.jive.ui.search.page;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_SEARCH_INVARIANT_VIOLATED;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import edu.buffalo.cse.jive.model.IEventModel.IAssignEvent;
import edu.buffalo.cse.jive.model.IQueryModel.EventQuery;
import edu.buffalo.cse.jive.model.factory.IQueryFactory;
import edu.buffalo.cse.jive.ui.search.IJiveSearchQuery;
import edu.buffalo.cse.jive.ui.search.JiveSearchPage;
import edu.buffalo.cse.jive.ui.search.JiveSearchQuery;
import edu.buffalo.cse.jive.ui.search.form.FormFactory;
import edu.buffalo.cse.jive.ui.search.form.SearchForm.InvariantViolatedForm;

/**
 * View portion encapsulating a search form.
 * 
 * When an instance number is given, the invariant is checked only for that particular instance of
 * the class. When it is left out, the invariant is checked for every instance of the class.
 */
public class InvariantViolatedSearchPage extends JiveSearchPage
{
  protected InvariantViolatedForm searchForm;

  @Override
  public void createControl(final Composite parent)
  {
    searchForm = FormFactory.createInvariantViolatedForm(this, parent);
  }

  @Override
  public IJiveSearchQuery createSearchQuery()
  {
    return new InvariantViolatedSearchQuery();
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
    searchForm.setclassText(parser.getClassName());
    // Only set the variable name if it is not a local variable
    if (!parser.getMethodName().equals(""))
    {
      searchForm.setLeftVariableText(parser.getVariableName());
    }
  }

  @Override
  public boolean isInputValid()
  {
    if (searchForm.classText().equals(""))
    {
      return false;
    }
    final String leftInstanceNumber = searchForm.instanceText();
    if (!leftInstanceNumber.equals("") && !leftInstanceNumber.matches("[1-9]\\d*"))
    {
      return false;
    }
    if (searchForm.leftVariableText().equals(""))
    {
      return false;
    }
    if (searchForm.rightVariableText().equals(""))
    {
      return false;
    }
    return true;
  }

  /**
   * View portion encapsulating an event predicate query.
   */
  private class InvariantViolatedSearchQuery extends JiveSearchQuery
  {
    @Override
    public ImageDescriptor getImageDescriptor()
    {
      return IM_SEARCH_INVARIANT_VIOLATED.enabledDescriptor();
    }

    @Override
    public String getResultLabel(final int matchCount)
    {
      return "'" + searchForm.toString() + "' - " + matchCount
          + (matchCount == 1 ? " violation" : " violations");
    }

    @Override
    public Class<? extends Object> getResultType()
    {
      return IAssignEvent.class;
    }

    @Override
    protected EventQuery createQuery(final IQueryFactory predicateFactory)
    {
      return predicateFactory.createInvariantViolatedQuery(searchForm);
    }
  }
}