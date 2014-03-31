package edu.buffalo.cse.jive.ui.view.diagram.contour;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchActionConstants;

import edu.buffalo.cse.jive.internal.ui.view.contour.editparts.ContourEditPart;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.ui.IJumpMenuManager;
import edu.buffalo.cse.jive.ui.ISliceMenuManager;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;

public class ContourDiagramContextMenuProvider extends ContextMenuProvider
{
  private final IJumpMenuManager jumpMenuFactory;
  private final ISliceMenuManager sliceMenuFactory;

  public ContourDiagramContextMenuProvider(final EditPartViewer viewer)
  {
    super(viewer);
    this.jumpMenuFactory = JiveUIPlugin.getDefault().createJumpMenuManager(viewer);
    this.sliceMenuFactory = JiveUIPlugin.getDefault().createSliceMenuManager();
  }

  @Override
  public void buildContextMenu(final IMenuManager manager)
  {
    final EditPartViewer viewer = getViewer();
    final IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
    if (!selection.isEmpty())
    {
      final EditPart part = (EditPart) selection.getFirstElement();
      if (part instanceof ContourEditPart)
      {
        if (part.getModel() instanceof IContour)
        {
          buildContextMenuFor((IContour) part.getModel(), manager);
        }
      }
    }
    manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
  }

  private void buildContextMenuFor(final IContour contour, final IMenuManager manager)
  {
    jumpMenuFactory.createJumpToMenu(contour, manager, true);
    sliceMenuFactory.createSliceMenu(contour, manager);
  }
}