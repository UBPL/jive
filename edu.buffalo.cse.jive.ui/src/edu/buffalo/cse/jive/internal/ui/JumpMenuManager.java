package edu.buffalo.cse.jive.internal.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchActionConstants;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;
import edu.buffalo.cse.jive.ui.IJumpMenuManager;

final class JumpMenuManager implements IJumpMenuManager
{
  private final EditPartViewer viewer;

  JumpMenuManager(final EditPartViewer viewer)
  {
    this.viewer = viewer;
  }

  @Override
  public boolean createJumpAction(final IJiveEvent event, final IMenuManager manager)
  {
    if (isJumpAction(event))
    {
      manager.add(new RunToEventAction(getTarget(), event));
      return true;
    }
    return false;
  }

  @Override
  public void createJumpToMenu(final Object model, final IMenuManager manager,
      final boolean callsOnly)
  {
    if (!(model instanceof IContour) && !(model instanceof IInitiatorEvent)
        && !(model instanceof IThreadValue))
    {
      return;
    }
    // create the menu entry
    final IMenuManager jumpToMenu = createSubmenu("Jump to", manager);
    if (model instanceof IContour)
    {
      final IContour contour = (IContour) model;
      contour.model().readLock();
      try
      {
        final List<IJiveEvent> menuItems = jumpItems(contour, callsOnly);
        populateJumpToMenu(menuItems, jumpToMenu);
      }
      finally
      {
        contour.model().readUnlock();
      }
    }
    else
    {
      final IInitiatorEvent initiator = getInitiator(model);
      initiator.model().readLock();
      try
      {
        final List<IJiveEvent> menuItems = jumpItems(initiator, callsOnly);
        populateJumpToMenu(menuItems, jumpToMenu);
      }
      finally
      {
        initiator.model().readUnlock();
      }
    }
  }

  private IMenuManager createSubmenu(final String text, final IMenuManager manager)
  {
    manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    final IMenuManager jumpToMenu = new MenuManager(text);
    manager.add(jumpToMenu);
    return jumpToMenu;
  }

  private IInitiatorEvent getInitiator(final Object model)
  {
    return model instanceof IThreadValue ? ((IThreadValue) model).model().lookupThread(
        (IThreadValue) model) : model instanceof IInitiatorEvent ? (IInitiatorEvent) model : null;
  }

  private IJiveDebugTarget getTarget()
  {
    return (IJiveDebugTarget) viewer.getRootEditPart().getContents().getModel();
  }

  private boolean isJumpAction(final IJiveEvent event)
  {
    switch (event.kind())
    {
      case THREAD_LOCK:
      case SYSTEM_END:
      case SYSTEM_START:
      case THREAD_END:
      case THREAD_START:
      case TYPE_LOAD:
        return false;
        // case LINE_STEP:
        // case FIELD_READ:
        // case FIELD_WRITE:
        // case METHOD_CALL:
        // case METHOD_RETURN:
        // case EXCEPTION_CATCH:
        // case EXCEPTION_THROW:
        // case TYPE_LOAD:
        // case NEW_OBJECT:
        // case VAR_ASSIGN:
        // case VAR_DELETE:
        // return true;
      default:
        return true;
    }
  }

  private List<IJiveEvent> jumpItems(final Object model, final boolean callsOnly)
  {
    // use the nested initiators of this static or instance contour
    if (model instanceof IContextContour)
    {
      return new ArrayList<IJiveEvent>(((IContextContour) model).nestedInitiators());
    }
    // use the nested initiators of this method contour
    else if (model instanceof IMethodContour)
    {
      final IMethodContour method = (IMethodContour) model;
      final IContextContour parent = method.parent();
      // find the call event corresponding to this method contour
      for (final IMethodCallEvent call : parent.nestedInitiators())
      {
        // add the calls initiated in this method's execution
        if (call.execution().equals(method))
        {
          return new ArrayList<IJiveEvent>(call.nestedInitiators());
        }
      }
    }
    else if (model instanceof IInitiatorEvent)
    {
      final IInitiatorEvent initiator = (IInitiatorEvent) model;
      return new ArrayList<IJiveEvent>(callsOnly ? initiator.nestedInitiators()
          : initiator.events());
    }
    return new ArrayList<IJiveEvent>();
  }

  /**
   * Improved menu usability-- a value of K fixed the size of each menu "page" and a item "more..."
   * contains the overflow items in the menu.
   */
  private void populateJumpToMenu(final List<IJiveEvent> items, final IMenuManager manager)
  {
    // for now, a hard-coded page size
    final int pageSize = 15;
    int itemCount = 0;
    IMenuManager page = manager;
    for (final IJiveEvent event : items)
    {
      if (event == null)
      {
        continue;
      }
      // if the current page has pageSize items, create a new page
      if (itemCount > 0 && itemCount % pageSize == 0 && isJumpAction(event))
      {
        page = createSubmenu("more...", page);
      }
      if (createJumpAction(event, page))
      {
        itemCount++;
      }
    }
  }
}