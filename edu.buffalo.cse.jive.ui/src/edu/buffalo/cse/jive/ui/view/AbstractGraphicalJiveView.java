package edu.buffalo.cse.jive.ui.view;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.handlers.IHandlerService;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.ui.IDiagramOutputActionFactory;
import edu.buffalo.cse.jive.ui.IUpdatableAction;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;

/**
 * A view part used to present {@code IJiveDebugTarget}s using a GEF {@code GraphicalViewer}. GEF
 * viewers use {@code EditPart}s as MVC controllers. These edit parts take elements of a model and
 * construct their visual representations using {@code IFigure}s.
 * 
 * This implementation uses a {@code ScalableRootEditPart} as its {@code RootEditPart}. It also
 * provides zooming controls for its tool bar.
 * 
 * @see GraphicalViewer
 * @see EditPart
 * @see IFigure
 * @see ScalableRootEditPart
 */
public abstract class AbstractGraphicalJiveView extends AbstractJiveView
{
  /**
   * The group used to hold actions having to do with diagram zooming.
   */
  protected static final String ZOOM_CONTROLS_GROUP = "zoomControlsGroup";
  /**
   * The viewer used to display the model.
   */
  private GraphicalViewer viewer;

  @Override
  @SuppressWarnings("rawtypes")
  public Object getAdapter(final Class type)
  {
    if (type == ZoomManager.class)
    {
      return viewer.getProperty(ZoomManager.class.toString());
    }
    return super.getAdapter(type);
  }

  public GraphicalViewer getViewer()
  {
    return viewer;
  }

  @Override
  public void setFocus()
  {
    if (isEnabled())
    {
      viewer.getControl().setFocus();
    }
  }

  @Override
  protected void configureToolBar(final IToolBarManager manager)
  {
    super.configureToolBar(manager);
    final IDiagramOutputActionFactory factory = JiveUIPlugin.getDefault()
        .getDiagramOutputActionFactory();
    final ScalableRootEditPart root = (ScalableRootEditPart) viewer.getRootEditPart();
    final IAction zoomIn = new ZoomInAction(root.getZoomManager());
    final IAction zoomOut = new ZoomOutAction(root.getZoomManager());
    final IUpdatableAction saveAs = factory.createDiagramExportAction(viewer);
    final IUpdatableAction print = factory.createPrintAction(viewer);
    addUpdatableAction(saveAs);
    addUpdatableAction(print);
    // reference: http://dev.eclipse.org/newslists/news.eclipse.platform/msg60866.html
    // register action handlers for zoom in and zoom out:
    final IHandlerService handlerService = (IHandlerService) getSite().getService(
        IHandlerService.class);
    handlerService.activateHandler(zoomIn.getActionDefinitionId(), new ActionHandler(zoomIn));
    handlerService.activateHandler(zoomOut.getActionDefinitionId(), new ActionHandler(zoomOut));
    manager.insertBefore(AbstractJiveView.GROUP_STEP_CONTROLS, new Separator(
        AbstractGraphicalJiveView.ZOOM_CONTROLS_GROUP));
    manager.appendToGroup(AbstractGraphicalJiveView.ZOOM_CONTROLS_GROUP, saveAs);
    manager.appendToGroup(AbstractGraphicalJiveView.ZOOM_CONTROLS_GROUP, print);
    manager.appendToGroup(AbstractGraphicalJiveView.ZOOM_CONTROLS_GROUP, zoomIn);
    manager.appendToGroup(AbstractGraphicalJiveView.ZOOM_CONTROLS_GROUP, zoomOut);
  }

  @Override
  protected void createContextMenu()
  {
    final ContextMenuProvider manager = createContextMenuProvider();
    viewer.setContextMenu(manager);
    // Register the context menu such that other plug-ins may contribute to it
    getSite().registerContextMenu(manager, viewer);
  }

  /**
   * Creates a {@code ContextMenuProvider} to be used as the {@code MenuManager} for the viewer's
   * context menu. This method is called by {@link #createContextMenu()}, and the resulting manager
   * is used during the lifetime of the view part.
   * 
   * @return the context menu provider that was created
   */
  protected abstract ContextMenuProvider createContextMenuProvider();

  /**
   * Creates the {@code EditPartFactory} to be used by the viewer in order to create
   * {@code EditPart}s for elements of the model. This method is called by
   * {@link #initializeViewer(Composite)}, and the resulting factory is used during the lifetime of
   * the view part.
   * 
   * @return the edit part factory the was created.
   */
  protected abstract EditPartFactory createEditPartFactory();

  /**
   * Creates a {@code GraphicalViewer} to be used as the view part's viewer. This method is called
   * by {@link #initializeViewer(Composite)}, and the resulting viewer is used during the lifetime
   * of the view part.
   * 
   * @return the viewer that was created
   */
  protected abstract GraphicalViewer createGraphicalViewer();

  @Override
  protected void initializeViewer(final Composite parent)
  {
    parent.setLayout(new FillLayout());
    // Create the viewer using the supplied parent
    viewer = createGraphicalViewer();
    final Control c = viewer.createControl(parent);
    c.setBackground(ColorConstants.white);
    // Create and initialize the root edit part
    final ScalableRootEditPart root = new ScalableRootEditPart();
    viewer.setRootEditPart(root);
    viewer.setEditDomain(new DefaultEditDomain(null));
    // Create and initialize the edit part factory
    final EditPartFactory factory = createEditPartFactory();
    viewer.setEditPartFactory(factory);
    // Register the viewer as a selection provider
    getSite().setSelectionProvider(viewer);
  }

  @Override
  protected void setViewerInput(final IJiveDebugTarget target)
  {
    viewer.setContents(target);
    viewer.getRootEditPart().refresh();
  }
}