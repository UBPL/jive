package edu.buffalo.cse.jive.preferences.ui;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import edu.buffalo.cse.jive.preferences.PreferenceKeys;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;

/**
 * The JIVE root level preference page. All JIVE preferences should be located on pages nested
 * beneath the root page.
 */
public class RootPreferencePage extends FieldEditorPreferencePage implements
    IWorkbenchPreferencePage
{
  private IntegerFieldEditor updateInterval;

  public RootPreferencePage()
  {
    super(FieldEditorPreferencePage.GRID);
    setPreferenceStore(PreferencesPlugin.getDefault().getPreferenceStore());
    setDescription("JIVE user preferences:");
  }

  @Override
  public void init(final IWorkbench workbench)
  {
    // TODO Auto-generated method stub
  }

  @Override
  protected void createFieldEditors()
  {
    // PREFERENCE: update interval
    updateInterval = new IntegerFieldEditor(PreferenceKeys.PREF_UPDATE_INTERVAL,
        "Visualization update interval (ms):", getFieldEditorParent(), 5);
    updateInterval.setValidRange(250, 60000);
    addField(updateInterval);
  }
}