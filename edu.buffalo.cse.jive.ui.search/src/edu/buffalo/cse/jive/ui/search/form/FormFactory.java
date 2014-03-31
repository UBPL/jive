package edu.buffalo.cse.jive.ui.search.form;

import java.awt.Color;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import edu.buffalo.cse.jive.model.RelationalOperator;
import edu.buffalo.cse.jive.ui.search.JiveSearchPage;
import edu.buffalo.cse.jive.ui.search.form.SearchForm.ExceptionForm;
import edu.buffalo.cse.jive.ui.search.form.SearchForm.InvariantViolatedForm;
import edu.buffalo.cse.jive.ui.search.form.SearchForm.LineExecutedForm;
import edu.buffalo.cse.jive.ui.search.form.SearchForm.MethodForm;
import edu.buffalo.cse.jive.ui.search.form.SearchForm.MethodReturnedForm;
import edu.buffalo.cse.jive.ui.search.form.SearchForm.ObjectCreatedForm;
import edu.buffalo.cse.jive.ui.search.form.SearchForm.SlicingForm;
import edu.buffalo.cse.jive.ui.search.form.SearchForm.VariableChangedForm;
import edu.buffalo.cse.jive.ui.search.page.SlicingSearchPage;

public final class FormFactory
{
  public static ExceptionForm createExceptionForm(final JiveSearchPage owner, final Composite parent)
  {
    return new ExceptionFormImpl(owner, parent);
  }

  public static InvariantViolatedForm createInvariantViolatedForm(final JiveSearchPage owner,
      final Composite parent)
  {
    return new InvariantViolatedFormImpl(owner, parent);
  }

  public static LineExecutedForm createLineExecutedForm(final JiveSearchPage owner,
      final Composite parent)
  {
    return new LineExecutedFormImpl(owner, parent);
  }

  public static MethodForm createMethodCalledForm(final JiveSearchPage owner, final Composite parent)
  {
    return new MethodFormImpl(owner, parent);
  }

  public static MethodReturnedForm createMethodReturnedForm(final JiveSearchPage owner,
      final Composite parent)
  {
    return new MethodReturnedFormImpl(owner, parent);
  }

  public static ObjectCreatedForm createObjectCreatedForm(final JiveSearchPage owner,
      final Composite parent)
  {
    return new ObjectCreatedFormImpl(owner, parent);
  }

  public static SlicingForm createSlicingForm(final SlicingSearchPage owner, final Composite parent)
  {
    return new SlicingFormImpl(owner, parent);
  }

  public static VariableChangedForm createVariableChangedForm(final JiveSearchPage owner,
      final Composite parent)
  {
    return new VariableChangedFormImpl(owner, parent);
  }

  private static String formPrefix(final String className, final String instanceNumber,
      final String methodName, final String variableName)
  {
    String result = className;
    if (!"".equals(instanceNumber))
    {
      result += ":" + instanceNumber;
    }
    if (!"".equals(methodName))
    {
      result += "#" + methodName;
    }
    if (!"".equals(variableName))
    {
      result += "." + variableName;
    }
    return result;
  }

  private final static class ExceptionFormImpl implements ExceptionForm, ModifyListener
  {
    private final Composite control;
    private final Text widgetClass;
    private final Text widgetException;
    private final Text widgetInstance;
    private final Text widgetMethod;
    private String textClass = "";
    private String textException = "";
    private String textInstance = "";
    private String textMethod = "";

    private ExceptionFormImpl(final JiveSearchPage owner, final Composite parent)
    {
      control = new Composite(parent, SWT.NONE);
      control.setLayout(new GridLayout(2, false));
      Label label = new Label(control, SWT.NONE);
      label.setText("Class name:");
      widgetClass = new Text(control, SWT.BORDER);
      widgetClass.addModifyListener(this);
      widgetClass.addModifyListener(owner);
      final GridData gd1 = new GridData();
      gd1.horizontalAlignment = GridData.FILL;
      gd1.grabExcessHorizontalSpace = true;
      widgetClass.setLayoutData(gd1);
      label = new Label(control, SWT.NONE);
      label.setText("Instance number:");
      widgetInstance = new Text(control, SWT.BORDER);
      widgetInstance.addModifyListener(this);
      widgetInstance.addModifyListener(owner);
      label = new Label(control, SWT.NONE);
      label.setText("Method name:");
      widgetMethod = new Text(control, SWT.BORDER);
      widgetMethod.addModifyListener(this);
      widgetMethod.addModifyListener(owner);
      final GridData gd2 = new GridData();
      gd2.horizontalAlignment = GridData.FILL;
      gd2.grabExcessHorizontalSpace = true;
      widgetMethod.setLayoutData(gd2);
      label = new Label(control, SWT.NONE);
      label.setText("Exception name:");
      widgetException = new Text(control, SWT.BORDER);
      widgetException.addModifyListener(this);
      widgetException.addModifyListener(owner);
      final GridData gd3 = new GridData();
      gd3.horizontalAlignment = GridData.FILL;
      gd3.grabExcessHorizontalSpace = true;
      widgetException.setLayoutData(gd3);
    }

    @Override
    public String classText()
    {
      return textClass;
    }

    @Override
    public Control control()
    {
      return control;
    }

    @Override
    public String exceptionText()
    {
      return textException;
    }

    @Override
    public String instanceText()
    {
      return textInstance;
    }

    @Override
    public String methodText()
    {
      return textMethod;
    }

    @Override
    public void modifyText(final ModifyEvent e)
    {
      if (e.widget == widgetClass)
      {
        textClass = widgetClass.getText();
      }
      else if (e.widget == widgetException)
      {
        textException = widgetException.getText();
      }
      else if (e.widget == widgetInstance)
      {
        textInstance = widgetInstance.getText();
      }
      else if (e.widget == widgetMethod)
      {
        textMethod = widgetMethod.getText();
      }
    }

    @Override
    public void setClassText(final String value)
    {
      widgetClass.setText(value);
    }

    @Override
    public void setMethodText(final String value)
    {
      widgetMethod.setText(value);
    }

    @Override
    public String toString()
    {
      final String prefix = FormFactory.formPrefix(classText(), instanceText(), methodText(), "");
      if (exceptionText().length() > 0)
      {
        return exceptionText() + " in " + prefix;
      }
      return prefix;
    }
  }

  private final static class InvariantViolatedFormImpl implements InvariantViolatedForm,
      ModifyListener
  {
    private final Composite control;
    private final Text widgetClass;
    private final Text widgetInstance;
    private final Text widgetMethod;
    private final Text widgetLeftVariable;
    private final Combo widgetOperator;
    private final Text widgetRightVariable;
    private String textClass = "";
    private String textInstance = "";
    private String textLeftVariable = "";
    private String textMethod = "";
    private String textOperator = "";
    private String textRightVariable = "";

    private InvariantViolatedFormImpl(final JiveSearchPage owner, final Composite parent)
    {
      control = new Composite(parent, SWT.NONE);
      control.setLayout(new GridLayout(2, false));
      Label label = new Label(control, SWT.NONE);
      label.setText("Class name:");
      widgetClass = new Text(control, SWT.BORDER);
      widgetClass.addModifyListener(this);
      widgetClass.addModifyListener(owner);
      final GridData gd1 = new GridData();
      gd1.horizontalAlignment = GridData.FILL;
      gd1.grabExcessHorizontalSpace = true;
      widgetClass.setLayoutData(gd1);
      label = new Label(control, SWT.NONE);
      label.setText("Instance number:");
      widgetInstance = new Text(control, SWT.BORDER);
      widgetInstance.addModifyListener(this);
      widgetInstance.addModifyListener(owner);
      label = new Label(control, SWT.NONE);
      label.setText("Method name:");
      widgetMethod = new Text(control, SWT.BORDER);
      widgetMethod.addModifyListener(this);
      widgetMethod.addModifyListener(owner);
      label = new Label(control, SWT.NONE);
      label.setText("Left field name:");
      widgetLeftVariable = new Text(control, SWT.BORDER);
      widgetLeftVariable.addModifyListener(this);
      widgetLeftVariable.addModifyListener(owner);
      final GridData gd2 = new GridData();
      gd2.horizontalAlignment = GridData.FILL;
      gd2.grabExcessHorizontalSpace = true;
      widgetLeftVariable.setLayoutData(gd2);
      label = new Label(control, SWT.NONE);
      label.setText("");
      widgetOperator = new Combo(control, SWT.DROP_DOWN | SWT.READ_ONLY);
      widgetOperator.setItems(new String[]
      { RelationalOperator.EQUAL.toString(), RelationalOperator.NOT_EQUAL.toString(),
          RelationalOperator.LESS_THAN.toString(),
          RelationalOperator.LESS_THAN_OR_EQUAL.toString(),
          RelationalOperator.GREATER_THAN.toString(),
          RelationalOperator.GREATER_THAN_OR_EQUAL.toString() });
      widgetOperator.select(0);
      widgetOperator.addModifyListener(this);
      widgetOperator.addModifyListener(owner);
      label = new Label(control, SWT.NONE);
      label.setText("Right field name:");
      widgetRightVariable = new Text(control, SWT.BORDER);
      widgetRightVariable.addModifyListener(this);
      widgetRightVariable.addModifyListener(owner);
      final GridData gd3 = new GridData();
      gd3.horizontalAlignment = GridData.FILL;
      gd3.grabExcessHorizontalSpace = true;
      widgetRightVariable.setLayoutData(gd3);
    }

    @Override
    public String classText()
    {
      return textClass;
    }

    @Override
    public Control control()
    {
      return control;
    }

    @Override
    public String instanceText()
    {
      return textInstance;
    }

    @Override
    public String leftVariableText()
    {
      return textLeftVariable;
    }

    @Override
    public String methodText()
    {
      return textMethod;
    }

    @Override
    public void modifyText(final ModifyEvent e)
    {
      if (e.widget == widgetClass)
      {
        textClass = widgetClass.getText();
      }
      else if (e.widget == widgetInstance)
      {
        textInstance = widgetInstance.getText();
      }
      else if (e.widget == widgetMethod)
      {
        textMethod = widgetMethod.getText();
      }
      else if (e.widget == widgetLeftVariable)
      {
        textLeftVariable = widgetLeftVariable.getText();
      }
      else if (e.widget == widgetOperator)
      {
        textOperator = widgetOperator.getText();
      }
      else if (e.widget == widgetRightVariable)
      {
        textRightVariable = widgetRightVariable.getText();
      }
    }

    @Override
    public RelationalOperator operator()
    {
      return RelationalOperator.fromString(textOperator);
    }

    @Override
    public String operatorText()
    {
      return textOperator;
    }

    @Override
    public String rightVariableText()
    {
      return textRightVariable;
    }

    @Override
    public void setclassText(final String value)
    {
      widgetClass.setText(value);
    }

    @Override
    public void setLeftVariableText(final String value)
    {
      widgetLeftVariable.setText(value);
    }

    @Override
    public String toString()
    {
      final String prefix = FormFactory.formPrefix(classText(), instanceText(), methodText(), "");
      return "NOT (" + leftVariableText() + " " + operatorText() + " " + rightVariableText()
          + ") in " + prefix;
    }
  }

  private final static class LineExecutedFormImpl implements LineExecutedForm, ModifyListener
  {
    private final Composite control;
    private final Text widgetLineNumber;
    private final Text widgetSourcePath;
    private String textLineNumber = "";
    private String textSourcePath = "";

    private LineExecutedFormImpl(final JiveSearchPage owner, final Composite parent)
    {
      control = new Composite(parent, SWT.NONE);
      control.setLayout(new GridLayout(2, false));
      Label label = new Label(control, SWT.NONE);
      label.setText("Source path:");
      widgetSourcePath = new Text(control, SWT.BORDER);
      widgetSourcePath.addModifyListener(this);
      widgetSourcePath.addModifyListener(owner);
      final GridData gd1 = new GridData();
      gd1.horizontalAlignment = GridData.FILL;
      gd1.grabExcessHorizontalSpace = true;
      widgetSourcePath.setLayoutData(gd1);
      label = new Label(control, SWT.NONE);
      label.setText("Line number:");
      widgetLineNumber = new Text(control, SWT.BORDER);
      widgetLineNumber.addModifyListener(this);
      widgetLineNumber.addModifyListener(owner);
    }

    @Override
    public Control control()
    {
      return control;
    }

    @Override
    public String lineNumberText()
    {
      return textLineNumber;
    }

    @Override
    public void modifyText(final ModifyEvent e)
    {
      if (e.widget == widgetLineNumber)
      {
        textLineNumber = widgetLineNumber.getText();
      }
      else if (e.widget == widgetSourcePath)
      {
        textSourcePath = widgetSourcePath.getText();
      }
    }

    @Override
    public void setLineNumberText(final String value)
    {
      widgetLineNumber.setText(value);
    }

    @Override
    public void setSourcePathText(final String value)
    {
      widgetSourcePath.setText(value);
    }

    @Override
    public String sourcePathText()
    {
      return textSourcePath;
    }

    @Override
    public String toString()
    {
      return sourcePathText() + ", line " + lineNumberText();
    }
  }

  private static class MethodFormImpl implements MethodForm, ModifyListener
  {
    private final Composite control;
    private final Text widgetClass;
    private final Text widgetInstance;
    private final Text widgetMethod;
    private String textClass = "";
    private String textInstance = "";
    private String textMethod = "";

    private MethodFormImpl(final JiveSearchPage owner, final Composite parent)
    {
      control = new Composite(parent, SWT.NONE);
      control.setLayout(new GridLayout(2, false));
      Label label = new Label(control, SWT.NONE);
      label.setText("Class name:");
      widgetClass = new Text(control, SWT.BORDER);
      widgetClass.addModifyListener(this);
      widgetClass.addModifyListener(owner);
      final GridData gd1 = new GridData();
      gd1.horizontalAlignment = GridData.FILL;
      gd1.grabExcessHorizontalSpace = true;
      widgetClass.setLayoutData(gd1);
      label = new Label(control, SWT.NONE);
      label.setText("Instance number:");
      widgetInstance = new Text(control, SWT.BORDER);
      widgetInstance.addModifyListener(this);
      widgetInstance.addModifyListener(owner);
      label = new Label(control, SWT.NONE);
      label.setText("Method name:");
      widgetMethod = new Text(control, SWT.BORDER);
      widgetMethod.addModifyListener(this);
      widgetMethod.addModifyListener(owner);
      final GridData gd2 = new GridData();
      gd2.horizontalAlignment = GridData.FILL;
      gd2.grabExcessHorizontalSpace = true;
      widgetMethod.setLayoutData(gd2);
    }

    @Override
    public String classText()
    {
      return textClass;
    }

    @Override
    public Control control()
    {
      return control;
    }

    @Override
    public String instanceText()
    {
      return textInstance;
    }

    @Override
    public String methodText()
    {
      return textMethod;
    }

    @Override
    public void modifyText(final ModifyEvent e)
    {
      if (e.widget == widgetClass)
      {
        textClass = widgetClass.getText();
      }
      else if (e.widget == widgetInstance)
      {
        textInstance = widgetInstance.getText();
      }
      else if (e.widget == widgetMethod)
      {
        textMethod = widgetMethod.getText();
      }
    }

    @Override
    public void setClassText(final String value)
    {
      widgetClass.setText(value);
    }

    @Override
    public void setMethodText(final String value)
    {
      widgetMethod.setText(value);
    }

    @Override
    public String toString()
    {
      return FormFactory.formPrefix(classText(), instanceText(), methodText(), "");
    }
  }

  private final static class MethodReturnedFormImpl extends MethodFormImpl implements
      MethodReturnedForm
  {
    private final Combo widgetOperator;
    private final Text widgetReturnValue;
    private String textOperator = "";
    private String textReturnValue = "";

    private MethodReturnedFormImpl(final JiveSearchPage owner, final Composite parent)
    {
      super(owner, parent);
      Label label = new Label(super.control, SWT.NONE);
      label.setText("Return value is:");
      widgetOperator = new Combo(super.control, SWT.DROP_DOWN | SWT.READ_ONLY);
      widgetOperator.setItems(new String[]
      { RelationalOperator.NONE.toString(), RelationalOperator.EQUAL.toString(),
          RelationalOperator.NOT_EQUAL.toString(), RelationalOperator.LESS_THAN.toString(),
          RelationalOperator.LESS_THAN_OR_EQUAL.toString(),
          RelationalOperator.GREATER_THAN.toString(),
          RelationalOperator.GREATER_THAN_OR_EQUAL.toString() });
      widgetOperator.select(0);
      widgetOperator.addModifyListener(this);
      widgetOperator.addModifyListener(owner);
      label = new Label(super.control, SWT.NONE);
      label.setText("Value:");
      widgetReturnValue = new Text(super.control, SWT.BORDER);
      widgetReturnValue.addModifyListener(this);
      widgetReturnValue.addModifyListener(owner);
    }

    @Override
    public void modifyText(final ModifyEvent e)
    {
      super.modifyText(e);
      if (e.widget == widgetOperator)
      {
        textOperator = widgetOperator.getText();
      }
      else if (e.widget == widgetReturnValue)
      {
        textReturnValue = widgetReturnValue.getText();
      }
    }

    @Override
    public RelationalOperator operator()
    {
      return RelationalOperator.fromString(operatorText());
    }

    @Override
    public String operatorText()
    {
      return textOperator;
    }

    @Override
    public String returnValueText()
    {
      return textReturnValue;
    }

    @Override
    public void setReturnValueEnabled(final boolean value)
    {
      widgetReturnValue.setEnabled(value);
    }

    @Override
    public String toString()
    {
      String result = FormFactory.formPrefix(classText(), instanceText(), methodText(), "");
      result += operator() == RelationalOperator.NONE ? "" : " " + operatorText() + " "
          + returnValueText();
      return result;
    }
  }

  private final static class ObjectCreatedFormImpl implements ObjectCreatedForm, ModifyListener
  {
    private final Composite control;
    private final Text widgetClass;
    private final Text widgetInstance;
    private String textClass = "";
    private String textInstance = "";

    private ObjectCreatedFormImpl(final JiveSearchPage owner, final Composite parent)
    {
      control = new Composite(parent, SWT.NONE);
      control.setLayout(new GridLayout(2, false));
      Label label = new Label(control, SWT.NONE);
      label.setText("Class name:");
      widgetClass = new Text(control, SWT.BORDER);
      widgetClass.addModifyListener(this);
      widgetClass.addModifyListener(owner);
      final GridData gd1 = new GridData();
      gd1.horizontalAlignment = GridData.FILL;
      gd1.grabExcessHorizontalSpace = true;
      widgetClass.setLayoutData(gd1);
      label = new Label(control, SWT.NONE);
      label.setText("Instance number:");
      widgetInstance = new Text(control, SWT.BORDER);
      widgetInstance.addModifyListener(this);
      widgetInstance.addModifyListener(owner);
    }

    @Override
    public String classText()
    {
      return textClass;
    }

    @Override
    public Control control()
    {
      return control;
    }

    @Override
    public String instanceText()
    {
      return textInstance;
    }

    @Override
    public void modifyText(final ModifyEvent e)
    {
      if (e.widget == widgetClass)
      {
        textClass = widgetClass.getText();
      }
      else if (e.widget == widgetInstance)
      {
        textInstance = widgetInstance.getText();
      }
    }

    @Override
    public void setClassText(final String value)
    {
      widgetClass.setText(value);
    }

    @Override
    public String toString()
    {
      return FormFactory.formPrefix(classText(), instanceText(), "", "");
    }
  }

  private final static class SlicingFormImpl implements SlicingForm, ModifyListener
  {
    private final Composite control;
    private final Text widgetEvent;
    private String textEvent = "";

    private SlicingFormImpl(final JiveSearchPage owner, final Composite parent)
    {
      control = new Composite(parent, SWT.NONE);
      control.setLayout(new GridLayout(2, false));
      final Label label = new Label(control, SWT.NONE);
      label.setText("Event number:");
      widgetEvent = new Text(control, SWT.BORDER);
      widgetEvent.addModifyListener(this);
      widgetEvent.addModifyListener(owner);
    }

    @Override
    public Control control()
    {
      return control;
    }

    @Override
    public String eventText()
    {
      return textEvent;
    }

    @Override
    public void modifyText(final ModifyEvent e)
    {
      if (e.widget == widgetEvent)
      {
        textEvent = widgetEvent.getText();
      }
    }

    @Override
    public String toString()
    {
      return "event number = " + eventText();
    }
  }

  private final static class VariableChangedFormImpl implements VariableChangedForm, ModifyListener
  {
    private final Composite control;
    private final Text widgetClass;
    private final Text widgetInstance;
    private final Text widgetMethod;
    private final Combo widgetOperator;
    private final Text widgetValue;
    private final Text widgetVariable;
    private String textClass = "";
    private String textInstance = "";
    private String textMethod = "";
    private String textOperator = "";
    private String textValue = "";
    private String textVariable = "";

    private VariableChangedFormImpl(final JiveSearchPage owner, final Composite parent)
    {
      control = new Composite(parent, SWT.NONE);
      GridLayout gl = new GridLayout();
      gl.numColumns = 2;
      control.setLayout(gl);
      Label label = new Label(control, SWT.NONE);
      label.setText("Class name:");
      widgetClass = new Text(control, SWT.BORDER);
      widgetClass.addModifyListener(this);
      widgetClass.addModifyListener(owner);
      widgetClass.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
      label = new Label(control, SWT.NONE);
      label.setText("Instance number:");
      widgetInstance = new Text(control, SWT.BORDER);
      widgetInstance.addModifyListener(this);
      widgetInstance.addModifyListener(owner);
      label = new Label(control, SWT.NONE);
      label.setText("Method name:");
      widgetMethod = new Text(control, SWT.BORDER);
      widgetMethod.addModifyListener(this);
      widgetMethod.addModifyListener(owner);
      widgetMethod.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
      label = new Label(control, SWT.NONE);
      label.setText("Variable name:");
      widgetVariable = new Text(control, SWT.BORDER);
      widgetVariable.addModifyListener(this);
      widgetVariable.addModifyListener(owner);
      widgetClass.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
      label = new Label(control, SWT.NONE);
      label.setText("");
      widgetOperator = new Combo(control, SWT.DROP_DOWN | SWT.READ_ONLY);
      widgetOperator.setItems(new String[]
      { RelationalOperator.NONE.toString(), RelationalOperator.EQUAL.toString(),
          RelationalOperator.NOT_EQUAL.toString(), RelationalOperator.LESS_THAN.toString(),
          RelationalOperator.LESS_THAN_OR_EQUAL.toString(),
          RelationalOperator.GREATER_THAN.toString(),
          RelationalOperator.GREATER_THAN_OR_EQUAL.toString() });
      widgetOperator.select(0);
      widgetOperator.addModifyListener(this);
      widgetOperator.addModifyListener(owner);
      label = new Label(control, SWT.NONE);
      label.setText("Variable value:");
      widgetValue = new Text(control, SWT.BORDER);
      widgetValue.addModifyListener(this);
      widgetValue.addModifyListener(owner);
    }

    @Override
    public String classText()
    {
      return textClass;
    }

    @Override
    public Control control()
    {
      return control;
    }

    @Override
    public String instanceText()
    {
      return textInstance;
    }

    @Override
    public String methodText()
    {
      return textMethod;
    }

    @Override
    public void modifyText(final ModifyEvent e)
    {
      if (e.widget == widgetClass)
      {
        textClass = widgetClass.getText();
      }
      else if (e.widget == widgetInstance)
      {
        textInstance = widgetInstance.getText();
      }
      else if (e.widget == widgetMethod)
      {
        textMethod = widgetMethod.getText();
      }
      else if (e.widget == widgetOperator)
      {
        textOperator = widgetOperator.getText();
      }
      else if (e.widget == widgetValue)
      {
        textValue = widgetValue.getText();
      }
      else if (e.widget == widgetVariable)
      {
        textVariable = widgetVariable.getText();
      }
    }

    @Override
    public RelationalOperator operator()
    {
      return RelationalOperator.fromString(operatorText());
    }

    @Override
    public String operatorText()
    {
      return textOperator;
    }

    @Override
    public void setClassText(final String value)
    {
      widgetClass.setText(value);
    }

    @Override
    public void setMethodText(final String value)
    {
      widgetMethod.setText(value);
    }

    @Override
    public void setValueEnabled(final boolean value)
    {
      widgetValue.setEnabled(value);
    }

    @Override
    public void setVariableText(final String value)
    {
      widgetVariable.setText(value);
    }

    @Override
    public String toString()
    {
      String result = FormFactory.formPrefix(classText(), instanceText(), methodText(),
          variableText());
      result += operator() == RelationalOperator.NONE ? "" : " " + operatorText() + " "
          + valueText();
      return result;
    }

    @Override
    public String valueText()
    {
      return textValue;
    }

    @Override
    public String variableText()
    {
      return textVariable;
    }
  }
}