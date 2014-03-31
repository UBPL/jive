package edu.buffalo.cse.jive.internal.ui;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_PRINT;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.print.PrintGraphicalViewerOperation;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Shell;

import edu.buffalo.cse.jive.ui.IJiveDiagramEditPart;
import edu.buffalo.cse.jive.ui.IUpdatableAction;

final class DiagramPrintAction extends Action implements IUpdatableAction
{
  public static final String ACTION_ID = "PrintImageActionId";
  private final EditPartViewer viewer;

  DiagramPrintAction(final EditPartViewer viewer)
  {
    super("Print...");
    this.viewer = viewer;
    setId(DiagramPrintAction.ACTION_ID);
    setImageDescriptor(IM_BASE_PRINT.enabledDescriptor());
    setDisabledImageDescriptor(IM_BASE_PRINT.disabledDescriptor());
  }

  @Override
  public void run()
  {
    final Shell shell = viewer.getControl().getShell();
    final PrintDialog dialog = new PrintDialog(shell, SWT.NULL);
    final PrinterData data = dialog.open();
    if (data != null)
    {
      final PrintGraphicalViewerOperation operation = new PrintGraphicalViewerOperation(
          new Printer(data), (GraphicalViewer) viewer);
      // here you can set the Print Mode
      // operation.setPrintMode(PrintFigureOperation.FIT_PAGE);
      operation.run("Printing...");
    }
    // Set the Text and register that to your toolbar….
    // printAction.setText("Print");
    // getEditorSite().getActionBars().getToolBarManager().add(printAction);
  }

  @Override
  public void update()
  {
    setEnabled(checkEnabled());
  }

  private boolean checkEnabled()
  {
    return viewer != null && (viewer.getContents() instanceof IJiveDiagramEditPart);
  }
}