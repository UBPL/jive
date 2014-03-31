package edu.buffalo.cse.jive.internal.ui;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_SAVE;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.exporter.IJiveExporter;
import edu.buffalo.cse.jive.exporter.JiveExporterPlugin;
import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;
import edu.buffalo.cse.jive.ui.IJiveStructuredView;
import edu.buffalo.cse.jive.ui.IUpdatableAction;

final class TraceExportAction extends Action implements IUpdatableAction
{
  public static final String ACTION_ID = "ExportTraceActionId";

  private static final IJiveDebugTarget activeTarget()
  {
    return JiveLaunchPlugin.getDefault().getLaunchManager().activeTarget();
  }

  private final IJiveStructuredView view;

  TraceExportAction(final IJiveStructuredView view)
  {
    super("Export As...");
    this.view = view;
    setId(TraceExportAction.ACTION_ID);
    setImageDescriptor(IM_BASE_SAVE.enabledDescriptor());
    setDisabledImageDescriptor(IM_BASE_SAVE.disabledDescriptor());
  }

  @Override
  public void run()
  {
    final Shell shell = view.getSite().getShell();
    final FileDialog saveDialog = new FileDialog(shell, SWT.SAVE);
    saveDialog.setFilterExtensions(new String[]
    { "*.csv", "*.xml" });
    saveDialog.setFilterNames(new String[]
    { "CSV format (*.csv)", "XML format (*.xml)" });
    final String filePath = saveDialog.open();
    final IJiveDebugTarget target = TraceExportAction.activeTarget();
    IJiveExporter je = JiveExporterPlugin.getDefault().getJiveExporters().get("TRACE");
    if (je.export(target.model(), filePath, null))
    {
      MessageDialog.openInformation(shell, "Export Complete", "Diagram has been exported to "
          + filePath);
    }
  }

  @Override
  public void update()
  {
    setEnabled(checkEnabled());
  }

  private boolean checkEnabled()
  {
    return TraceExportAction.activeTarget() != null && view.getViewer() != null;
  }
}