package edu.buffalo.cse.jive.preferences.ui;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import edu.buffalo.cse.jive.preferences.PreferenceKeys;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;

/**
 * A preference page for specifying the default event filters for various launch configuration
 * types.
 */
public class DefaultEventFiltersPreferencePage extends FieldEditorPreferencePage implements
    IWorkbenchPreferencePage
{
  /**
   * The editor for common package/class filters.
   */
  private EventFiltersEditor commonFiltersEditor;

  /**
   * Creates the default event filters preference page using a grid layout.
   */
  public DefaultEventFiltersPreferencePage()
  {
    super(FieldEditorPreferencePage.GRID);
    setPreferenceStore(PreferencesPlugin.getDefault().getPreferenceStore());
    final String description = "JIVE collects events from a program as it "
        + "executes.  Class exclusion filters may be specified in order "
        + "to reduce the resulting overhead and to produce smaller " + "visualizations.";
    setDescription(description);
  }

  @Override
  public void init(final IWorkbench workbench)
  {
    // TODO Auto-generated method stub
  }

  /**
   * Creates an {@code EventFilterEditor} with the supplied name and label.
   * 
   * @param name
   *          the attribute name of the preference
   * @param labelText
   *          the label describing the editor's list
   * @return an event filter editor contained on this page
   */
  private EventFiltersEditor createEventFilterEditor(final String name, final String labelText)
  {
    return new EventFiltersEditor(name, labelText, getFieldEditorParent());
  }

  @Override
  protected void adjustGridLayout()
  {
    super.adjustGridLayout();
    final List field = commonFiltersEditor.getListControl(getFieldEditorParent());
    final GridData layoutData = (GridData) field.getLayoutData();
    layoutData.grabExcessHorizontalSpace = true;
    layoutData.grabExcessVerticalSpace = true;
    field.setLayoutData(layoutData);
  }

  @Override
  protected void createFieldEditors()
  {
    commonFiltersEditor = createEventFilterEditor(PreferenceKeys.PREF_FILTERS_COMMON,
        "Default package and class filters");
    addField(commonFiltersEditor);
  }
}