package edu.buffalo.cse.jive.ui.search.form;

import org.eclipse.swt.widgets.Control;

import edu.buffalo.cse.jive.model.IQueryModel.ExceptionQueryParams;
import edu.buffalo.cse.jive.model.IQueryModel.InvariantViolatedQueryParams;
import edu.buffalo.cse.jive.model.IQueryModel.LineExecutedQueryParams;
import edu.buffalo.cse.jive.model.IQueryModel.MethodQueryParams;
import edu.buffalo.cse.jive.model.IQueryModel.MethodReturnedQueryParams;
import edu.buffalo.cse.jive.model.IQueryModel.ObjectCreatedQueryParams;
import edu.buffalo.cse.jive.model.IQueryModel.SlicingQueryParams;
import edu.buffalo.cse.jive.model.IQueryModel.VariableChangedQueryParams;

public interface SearchForm
{
  /**
   * The control corresponding to this form.
   */
  public Control control();

  public interface ExceptionForm extends SearchForm, ExceptionQueryParams
  {
    /**
     * Changes the text of the class widget.
     */
    public void setClassText(String value);

    /**
     * Changes the text of the method widget.
     */
    public void setMethodText(String value);
  }

  public interface InvariantViolatedForm extends SearchForm, InvariantViolatedQueryParams
  {
    /**
     * Changes the text of the class widget.
     */
    public void setclassText(String value);

    /**
     * Changes the text of the left variable widget.
     */
    public void setLeftVariableText(String value);
  }

  public interface LineExecutedForm extends SearchForm, LineExecutedQueryParams
  {
    /**
     * Changes the text of the line number widget.
     */
    public void setLineNumberText(String value);

    /**
     * Changes the text of the source path widget.
     */
    public void setSourcePathText(String value);
  }

  public interface MethodForm extends SearchForm, MethodQueryParams
  {
    /**
     * Changes the text of the class widget.
     */
    public void setClassText(String value);

    /**
     * Changes the text of the method widget.
     */
    public void setMethodText(String value);
  }

  public interface MethodReturnedForm extends MethodForm, MethodReturnedQueryParams
  {
    /**
     * Toggles the enabled state of the return value widget.
     */
    public void setReturnValueEnabled(boolean value);
  }

  public interface ObjectCreatedForm extends SearchForm, ObjectCreatedQueryParams
  {
    /**
     * Changes the text of the class widget.
     */
    public void setClassText(String value);
  }

  public interface SlicingForm extends SearchForm, SlicingQueryParams
  {
  }

  public interface VariableChangedForm extends SearchForm, VariableChangedQueryParams
  {
    /**
     * Changes the text of the class widget.
     */
    public void setClassText(String value);

    /**
     * Changes the text of the method widget.
     */
    public void setMethodText(String value);

    /**
     * Changes the enabled state of the value widget.
     */
    public void setValueEnabled(boolean value);

    /**
     * Changes the text of the variable widget.
     */
    public void setVariableText(String value);
  }
}
