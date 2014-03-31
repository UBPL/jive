package edu.buffalo.cse.jive.ui.search.page;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_EVENT_ASSIGN;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import edu.buffalo.cse.jive.model.IEventModel.IFieldAssignEvent;
import edu.buffalo.cse.jive.model.IQueryModel.EventQuery;
import edu.buffalo.cse.jive.model.RelationalOperator;
import edu.buffalo.cse.jive.model.factory.IQueryFactory;
import edu.buffalo.cse.jive.ui.search.IJiveSearchQuery;
import edu.buffalo.cse.jive.ui.search.JiveSearchPage;
import edu.buffalo.cse.jive.ui.search.JiveSearchQuery;
import edu.buffalo.cse.jive.ui.search.form.FormFactory;
import edu.buffalo.cse.jive.ui.search.form.SearchForm.VariableChangedForm;

/**
 * View portion encapsulating a search form.
 * 
 * When an instance number is given, the condition is checked only for that particular instance of
 * the class. When it is left out, the condition is checked for every instance of the class. If a
 * method name is supplied, then the variable is assumed to be a parameter or local variable of the
 * method. Otherwise, the variable is considered a field.
 */
public class VariableChangedSearchPage extends JiveSearchPage
{
  private VariableChangedForm searchForm;

  @Override
  public void createControl(final Composite parent)
  {
    searchForm = FormFactory.createVariableChangedForm(this, parent);
  }

  @Override
  public IJiveSearchQuery createSearchQuery()
  {
    return new VariableChangedSearchQuery();
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
    searchForm.setVariableText(parser.getVariableName());
  }

  @Override
  public boolean isInputValid()
  {
    final RelationalOperator operator = RelationalOperator.fromString(searchForm.operatorText());
    searchForm.setValueEnabled(operator != RelationalOperator.NONE);
    if (searchForm.classText().equals("") && searchForm.variableText().equals(""))
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
    return !searchForm.valueText().equals("");
  }

  /**
   * View portion encapsulating an event predicate query.
   */
  private class VariableChangedSearchQuery extends JiveSearchQuery
  {
    @Override
    public ImageDescriptor getImageDescriptor()
    {
      return IM_EVENT_ASSIGN.enabledDescriptor();
    }

    @Override
    public String getResultLabel(final int matchCount)
    {
      return "'" + searchForm.toString() + "' - " + matchCount
          + (matchCount == 1 ? " assignment" : " assignments");
    }

    @Override
    public Class<? extends Object> getResultType()
    {
      return IFieldAssignEvent.class;
    }

    @Override
    protected EventQuery createQuery(final IQueryFactory predicateFactory)
    {
      return predicateFactory.createVariableChangedQuery(searchForm);
    }
  }
}