package edu.buffalo.cse.jive.ui.view;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.ui.IDiagramOutputActionFactory;
import edu.buffalo.cse.jive.ui.IJiveStructuredView;
import edu.buffalo.cse.jive.ui.IUpdatableAction;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;

/**
 * A view part used to present {@code IJiveDebugTarget}s using a JFace {@code StructuredViewer}.
 * JFace viewers use content providers to obtain model elements and label providers to obtain the
 * visual representation of those elements.
 * 
 * This implementation uses a {@code StructuredViewer} for presentation.
 * 
 * @see StructuredViewer
 * @see #createViewer(Composite)
 * @see #createContentProvider()
 * @see #createLabelProvider()
 */
public abstract class AbstractStructuredJiveView extends AbstractJiveView implements
    IJiveStructuredView
{
  /**
   * The group used to hold actions having to do with output.
   */
  protected static final String OUTPUT_CONTROLS_GROUP = "outputControlsGroup";
  private StructuredViewer viewer;

  @Override
  public StructuredViewer getViewer()
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
    final IUpdatableAction saveAs = factory.createTraceExportAction(this);
    addUpdatableAction(saveAs);
    manager.insertAfter(AbstractJiveView.GROUP_STEP_CONTROLS, new Separator(
        AbstractStructuredJiveView.OUTPUT_CONTROLS_GROUP));
    manager.appendToGroup(AbstractStructuredJiveView.OUTPUT_CONTROLS_GROUP, saveAs);
  }

  /**
   * Creates an {@code IStructuredContentProvider} to be used as the content provider for the viewer
   * created by {@link #createViewer(Composite)}. This mehtod is called by
   * {@link #initializeViewer(Composite)}.
   * 
   * @return the content provider that was created
   */
  protected abstract IStructuredContentProvider createContentProvider();

  @Override
  protected void createContextMenu()
  {
    final MenuManager manager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener()
      {
        @Override
        public void menuAboutToShow(final IMenuManager mgr)
        {
          fillContextMenu(mgr);
          mgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        }
      });
    final Control menuControl = viewer.getControl();
    final Menu menu = manager.createContextMenu(menuControl);
    menuControl.setMenu(menu);
    // Register the context menu such that other plug-ins may contribute to it
    getSite().registerContextMenu(manager, viewer);
  }

  /**
   * Creates an {@code IBaseLabelProvider} to be used as the label provider for the viewer created
   * by {@link #createViewer(Composite)}. This method is called by
   * {@link #initializeViewer(Composite)}.
   */
  // TODO: label provider creation should be handled internally by viewers--
  // (viewers, such as tables, may have multiple label providers)
  protected IBaseLabelProvider createLabelProvider()
  {
    // if null, assumes the viewer created its label provider(s)
    return null;
  }

  /**
   * Creates a {@code StructuredViewer} to be used as the view part's viewer. This method is called
   * by {@link #initializeViewer(Composite)}, and the resulting viewer is used during the lifetime
   * of the view part.
   * 
   * @param parent
   *          the parent widget of the viewer
   * @return the viewer that was created
   * @see #getViewer()
   */
  protected abstract StructuredViewer createViewer(Composite parent);

  /**
   * Fills the context menu with actions. Subclasses should override this method if a context menu
   * is desired.
   * 
   * @param manager
   *          the context menu to fill
   */
  protected void fillContextMenu(final IMenuManager manager)
  {
    // do nothing
  }

  @Override
  protected void initializeViewer(final Composite parent)
  {
    viewer = createViewer(parent);
    if (viewer == null)
    {
      return;
    }
    // Register the viewer as a selection provider
    getSite().setSelectionProvider(viewer);
    // Create the content provider
    final IContentProvider cp = createContentProvider();
    assert cp != null : "Structured view initialization error: no content provider!";
    viewer.setContentProvider(cp);
    // if this viewer creates a label provider separately
    final IBaseLabelProvider blp = createLabelProvider();
    if (blp != null)
    {
      viewer.setLabelProvider(blp);
    }
  }

  @Override
  protected void setViewerInput(final IJiveDebugTarget target)
  {
    viewer.setInput(target);
  }
}