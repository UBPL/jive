package edu.buffalo.cse.jive.ui.view.diagram.contour;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_TREE;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.internal.ui.view.contour.actions.ContourStateActionFactory;
import edu.buffalo.cse.jive.internal.ui.view.contour.editparts.ContourConnectionEditPart;
import edu.buffalo.cse.jive.internal.ui.view.contour.editparts.ContourDiagramEditPart;
import edu.buffalo.cse.jive.internal.ui.view.contour.editparts.ContourEditPart;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.ui.view.AbstractGraphicalJiveView;

/**
 * A view part to present {@code InteractiveContourModel}s associated with {@code IJiveDebugTarget}
 * s. The content provider used by this view is specific to Java contour models. The view uses a GEF
 * {@code ScrollingGraphicalViewer} to display the contours. Controls are also available to step and
 * run through model transactions in both the forward and reverse directions.
 * 
 * @see IJiveDebugTarget
 * @see ScrollingGraphicalViewer
 * @see ContourModelEditPartFactory
 */
public class ContourDiagramView extends AbstractGraphicalJiveView
{
  /**
   * The group used to actions specific to the contour diagram.
   */
  private static final String CONTOUR_DIAGRAM_GROUP = "contourDiagramGroup";
  private IAction fExpandedObjectStateAction;
  private IAction fExpandedStackedStateAction;
  private IAction fMinimizedStateAction;
  // private static final String CONTOUR_MENU_GROUP = "contourMenuGroup";
  private IAction fObjectCallPathStateAction;
  private IAction fObjectStateAction;
  private IAction fScrollLockAction;
  private IAction fStackedStateAction;

  /**
   * Constructs the view.
   */
  public ContourDiagramView()
  {
    super();
  }

  @Override
  public void dispose()
  {
    super.dispose();
  }

  @Override
  protected void configurePullDownMenu(final IMenuManager manager)
  {
    // org.eclipse.gef.ui.actions.GEFActionConstants
    manager.add(fObjectStateAction); // #1. "Objects" (default)
    manager.add(fStackedStateAction); // #2. "Stacked"
    manager.add(fMinimizedStateAction); // #3. "Minimized"
    // manager.add(new Separator()); // separators create a new group
    manager.add(fExpandedObjectStateAction); // #4. "Objects with Tables"
    manager.add(fExpandedStackedStateAction); // #5. "Stacked with Tables"
    manager.add(new Separator());
    manager.add(fObjectCallPathStateAction); // "Focus on Call Path"
  }

  @Override
  protected void configureToolBar(final IToolBarManager manager)
  {
    super.configureToolBar(manager);
    manager.insertBefore(AbstractGraphicalJiveView.ZOOM_CONTROLS_GROUP, new Separator(
        ContourDiagramView.CONTOUR_DIAGRAM_GROUP));
    manager.appendToGroup(ContourDiagramView.CONTOUR_DIAGRAM_GROUP, fScrollLockAction);
  }

  @Override
  protected void createActions()
  {
    super.createActions();
    fScrollLockAction = ContourStateActionFactory.createScrollLockAction();
    // Create the pull down menu actions
    fMinimizedStateAction = ContourStateActionFactory.createMinimizedStateAction();
    fObjectStateAction = ContourStateActionFactory.createObjectStateAction();
    fStackedStateAction = ContourStateActionFactory.createStackedStateAction();
    fExpandedObjectStateAction = ContourStateActionFactory.createObjectExpandedStateAction();
    fExpandedStackedStateAction = ContourStateActionFactory.createStackedExpandedStateAction();
    fObjectCallPathStateAction = ContourStateActionFactory.createCallPathStateAction();
  }

  @Override
  protected ContextMenuProvider createContextMenuProvider()
  {
    return new ContourDiagramContextMenuProvider(getViewer());
  }

  @Override
  protected EditPartFactory createEditPartFactory()
  {
    return new ContourModelEditPartFactory();
  }

  @Override
  protected GraphicalViewer createGraphicalViewer()
  {
    return new ScrollingGraphicalViewer();
  }

  @Override
  protected String getDefaultContentDescription()
  {
    return "No contour diagrams to display at this time.";
  }

  @Override
  protected ImageDescriptor getDisplayDropDownDisabledImageDescriptor()
  {
    return IM_BASE_TREE.disabledDescriptor();
  }

  @Override
  protected ImageDescriptor getDisplayDropDownEnabledImageDescriptor()
  {
    return IM_BASE_TREE.enabledDescriptor();
  }

  @Override
  protected String getDisplayTargetDropDownText()
  {
    return "Display Contour Diagram";
  }

  /**
   * An {@code EditPartFactory} used to create {@code EditPart}s for elements in the
   * {@code ContourModel}.
   * 
   * @see ContourDiagramEditPart
   * @see ContourEditPart
   * @see ContourConnectionEditPart
   */
  private static class ContourModelEditPartFactory implements EditPartFactory
  {
    /**
     * An empty edit part to be used when there are no targets being debugged.
     */
    private static final EditPart EMPTY_EDIT_PART = new AbstractGraphicalEditPart()
      {
        @Override
        protected void createEditPolicies()
        {
          // TODO Determine if this should be implemented
        }

        @Override
        protected IFigure createFigure()
        {
          return new Figure();
        }
      };

    @Override
    public EditPart createEditPart(final EditPart context, final Object model)
    {
      if (model == null)
      {
        return ContourModelEditPartFactory.EMPTY_EDIT_PART;
      }
      if (model instanceof IJiveDebugTarget)
      {
        return new ContourDiagramEditPart(model);
      }
      if (model instanceof IContour)
      {
        return new ContourEditPart((IContour) model);
      }
      if (model instanceof IContourMember)
      {
        return new ContourConnectionEditPart(model);
      }
      throw new IllegalArgumentException("Unknown element type:  " + model.getClass());
    }
  }
}