package edu.buffalo.cse.jive.preferences.ui;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import edu.buffalo.cse.jive.preferences.PreferenceKeys;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;

public class SequenceDiagramPreferencePage extends FieldEditorPreferencePage implements
    IWorkbenchPreferencePage
{
  private IntegerFieldEditor activationWidthEditor;
  private IntegerFieldEditor eventHeightEditor;

  public SequenceDiagramPreferencePage()
  {
    super(FieldEditorPreferencePage.GRID);
    setPreferenceStore(PreferencesPlugin.getDefault().getPreferenceStore());
    setDescription("Sequence diagram user preferences:");
  }

  @Override
  public void init(final IWorkbench workbench)
  {
    // TODO Auto-generated method stub
  }

  private void createActivationWidthEditor()
  {
    activationWidthEditor = new IntegerFieldEditor(PreferenceKeys.PREF_SD_ACTIVATION_WIDTH,
        "Activation width (pixels):", getFieldEditorParent(), 2);
    activationWidthEditor.setValidRange(1, 21);
    addField(activationWidthEditor);
  }

  private void createEventHeightEditor()
  {
    eventHeightEditor = new IntegerFieldEditor(PreferenceKeys.PREF_SD_EVENT_HEIGHT,
        "Event height (pixels):", getFieldEditorParent(), 1);
    eventHeightEditor.setValidRange(1, 9);
    addField(eventHeightEditor);
  }

  private void createThreadColorEditor(final String name, final int threadNumber)
  {
    final ColorFieldEditor threadColorEditor;
    if (threadNumber == -1)
    {
      threadColorEditor = new ColorFieldEditor(name, "Snapshot Thread color:",
          getFieldEditorParent());
    }
    else if (threadNumber == 0)
    {
      threadColorEditor = new ColorFieldEditor(name, "Main Thread color:", getFieldEditorParent());
    }
    else
    {
      threadColorEditor = new ColorFieldEditor(name, "Thread #" + threadNumber + " color:",
          getFieldEditorParent());
    }
    addField(threadColorEditor);
  }

  @Override
  protected void adjustGridLayout()
  {
    super.adjustGridLayout();
    Text field = activationWidthEditor.getTextControl(getFieldEditorParent());
    GridData layoutData = (GridData) field.getLayoutData();
    layoutData.grabExcessHorizontalSpace = false;
    field = eventHeightEditor.getTextControl(getFieldEditorParent());
    layoutData = (GridData) field.getLayoutData();
    layoutData.grabExcessHorizontalSpace = false;
  }

  @Override
  protected void createFieldEditors()
  {
    createThreadColorEditor(PreferenceKeys.PREF_THREAD_COLOR_SNAPSHOT, -1);
    createThreadColorEditor(PreferenceKeys.PREF_THREAD_COLOR_MAIN, 0);
    createThreadColorEditor(PreferenceKeys.PREF_THREAD_COLOR_1, 1);
    createThreadColorEditor(PreferenceKeys.PREF_THREAD_COLOR_2, 2);
    createThreadColorEditor(PreferenceKeys.PREF_THREAD_COLOR_3, 3);
    createThreadColorEditor(PreferenceKeys.PREF_THREAD_COLOR_4, 4);
    createThreadColorEditor(PreferenceKeys.PREF_THREAD_COLOR_5, 5);
    createThreadColorEditor(PreferenceKeys.PREF_THREAD_COLOR_6, 6);
    createThreadColorEditor(PreferenceKeys.PREF_THREAD_COLOR_7, 7);
    createActivationWidthEditor();
    createEventHeightEditor();
  }
}