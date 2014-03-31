package edu.buffalo.cse.jive.preferences.ui;

import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;

import edu.buffalo.cse.jive.preferences.PreferenceInitializer;

/**
 * An editor for adding and removing filters from the list of default event filters.
 */
class EventFiltersEditor extends ListEditor
{
  /**
   * Creates an event filter editor with the supplied name and label as a child of the given parent.
   * 
   * @param name
   *          the attribute name of the editor's preference
   * @param labelText
   *          the label describing the editor's list
   * @param parent
   *          the containing component
   */
  public EventFiltersEditor(final String name, final String labelText, final Composite parent)
  {
    super(name, labelText, parent);
  }

  @Override
  protected String createList(final String[] items)
  {
    return PreferenceInitializer.convertFilters(items);
  }

  @Override
  protected String getNewInputObject()
  {
    final InputDialog inputDialog = new InputDialog(getShell(), "Exclude classes",
        "Enter class exclusion filter", null, new IInputValidator()
          {
            @Override
            public String isValid(final String newText)
            {
              // TODO: Perform better validation
              final String delimiter = PreferenceInitializer.EVENT_FILTER_DELIMITER;
              if (newText.contains(delimiter))
              {
                return "The filter cannot contain the delimiter: " + delimiter;
              }
              else if (newText.isEmpty())
              {
                return "The filter cannot be empty";
              }
              else
              {
                return null;
              }
            }
          });
    inputDialog.open();
    if (inputDialog.getReturnCode() == Window.OK)
    {
      return inputDialog.getValue();
    }
    else
    {
      return null;
    }
  }

  @Override
  protected String[] parseString(final String stringList)
  {
    final StringTokenizer tokenizer = new StringTokenizer(stringList,
        PreferenceInitializer.EVENT_FILTER_DELIMITER);
    final int tokenCount = tokenizer.countTokens();
    final String[] result = new String[tokenCount];
    for (int i = 0; i < tokenCount; i++)
    {
      result[i] = tokenizer.nextToken();
    }
    return result;
  }
}
