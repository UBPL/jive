package edu.buffalo.cse.jive.ui.view.diagram.sequence;

import java.util.List;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchActionConstants;

import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.actions.SequenceDiagramActionFactory;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.EventOccurrenceEditPart;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.ExecutionOccurrenceEditPart;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.LifelineEditPart;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.Message;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.SequenceDiagramEditPart;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.IThreadStartEvent;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;
import edu.buffalo.cse.jive.ui.IJiveGraphicalView;
import edu.buffalo.cse.jive.ui.IJumpMenuManager;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;

public class SequenceDiagramContextMenuProvider extends ContextMenuProvider
{
  private final SequenceDiagramActionFactory actionFactory;
  private final IJumpMenuManager jumpMenuFactory;

  public SequenceDiagramContextMenuProvider(final IJiveGraphicalView view)
  {
    super(view.getViewer());
    this.actionFactory = new SequenceDiagramActionFactory(view);
    this.jumpMenuFactory = JiveUIPlugin.getDefault().createJumpMenuManager(view.getViewer());
  }

  @Override
  public void buildContextMenu(final IMenuManager manager)
  {
    final IStructuredSelection selection = (IStructuredSelection) getViewer().getSelection();
    if (!selection.isEmpty())
    {
      // TODO Change to use either the primary selection (last in list) or all elements
      final EditPart part = (EditPart) selection.getFirstElement();
      if (part.getParent() instanceof SequenceDiagramEditPart)
      {
        createHideThreadsMenu(manager, (SequenceDiagramEditPart) part.getParent());
      }
      if (part instanceof LifelineEditPart)
      {
        if (part.getModel() instanceof IContextContour || part.getModel() instanceof IThreadValue)
        {
          createLifelineMenu(part.getModel(), manager);
        }
      }
      else if (part instanceof ExecutionOccurrenceEditPart)
      {
        final IInitiatorEvent initiator = (IInitiatorEvent) part.getModel();
        createInitiatorMenu(initiator, manager);
      }
      else if (part instanceof EventOccurrenceEditPart)
      {
        final IJiveEvent event = ((EventOccurrenceEditPart) part).getEvent();
        jumpMenuFactory.createJumpAction(event, manager);
      }
      else if (part instanceof AbstractConnectionEditPart)
      {
        jumpMenuFactory.createJumpAction(((Message) part.getModel()).target(), manager);
      }
    }
    manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
  }

  private void createHideThreadsMenu(IMenuManager manager, SequenceDiagramEditPart part)
  {
    final IMenuManager hideMenu = new MenuManager("Hidden Threads");
    List<IThreadStartEvent> threads = part.executionModel().lookupThreads();
    for (final IThreadStartEvent thread : threads)
    {
      hideMenu.add(actionFactory.createThreadVisibilityToggleAction(thread.thread()));
    }
    manager.add(hideMenu);
    //manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
  }

  private void createLifelineMenu(final Object model, final IMenuManager manager)
  {
    final IMenuManager foldMenu = new MenuManager("Collapse...");
    foldMenu.add(actionFactory.createCollapseLifelineChildren(model));
    foldMenu.add(actionFactory.createCollapseLifeline(model));
    final IMenuManager unfoldMenu = new MenuManager("Expand...");
    unfoldMenu.add(actionFactory.createExpandLifelineChildren(model));
    unfoldMenu.add(actionFactory.createExpandLifeline(model));
    manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    manager.add(foldMenu);
    manager.add(unfoldMenu);
    jumpMenuFactory.createJumpToMenu(model, manager, true);
  }

  protected void createInitiatorMenu(final IInitiatorEvent initiator, final IMenuManager manager)
  {
    final IMenuManager collapseMenu = new MenuManager("Collapse...");
    collapseMenu.add(actionFactory.createCollapseAfterAction(initiator));
    collapseMenu.add(actionFactory.createCollapseBeforeAction(initiator));
    collapseMenu.add(actionFactory.createCollapseChildrenAction(initiator));
    collapseMenu.add(actionFactory.createCollapseNodeAction(initiator));
    final IMenuManager expandMenu = new MenuManager("Expand...");
    expandMenu.add(actionFactory.createExpandAfterAction(initiator));
    expandMenu.add(actionFactory.createExpandBeforeAction(initiator));
    expandMenu.add(actionFactory.createExpandChildrenAction(initiator));
    expandMenu.add(actionFactory.createExpandNodeAction(initiator));
    manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    manager.add(collapseMenu);
    manager.add(expandMenu);
    manager.add(actionFactory.createFocusAction(initiator));
    jumpMenuFactory.createJumpToMenu(initiator, manager, false);
  }
}