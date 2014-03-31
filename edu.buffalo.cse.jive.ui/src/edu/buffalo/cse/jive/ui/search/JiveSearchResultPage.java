package edu.buffalo.cse.jive.ui.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.ui.IContextMenuConstants;
import org.eclipse.search.ui.IQueryListener;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.IPageSite;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;
import edu.buffalo.cse.jive.model.IEventModel.IAssignEvent;
import edu.buffalo.cse.jive.model.IEventModel.IExceptionCatchEvent;
import edu.buffalo.cse.jive.model.IEventModel.IExceptionThrowEvent;
import edu.buffalo.cse.jive.model.IEventModel.IFieldAssignEvent;
import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.ILineStepEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodReturnedEvent;
import edu.buffalo.cse.jive.model.IEventModel.INewObjectEvent;
import edu.buffalo.cse.jive.model.IEventModel.ISystemStartEvent;
import edu.buffalo.cse.jive.model.IEventModel.IThreadStartEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.ui.IJiveGraphicalView;
import edu.buffalo.cse.jive.ui.IStepAction;
import edu.buffalo.cse.jive.ui.IStepManager;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;
import edu.buffalo.cse.jive.ui.view.JiveEventLabelProvider;
import edu.buffalo.cse.jive.ui.view.JiveTableViewer;

public class JiveSearchResultPage extends AbstractTextSearchViewPage
{
  private static final String LAYOUT_GROUP = "layoutGroup";

  private static final IJiveDebugTarget activeTarget()
  {
    return JiveLaunchPlugin.getDefault().getLaunchManager().activeTarget();
  }

  private Class<? extends Object> resultType;
  private StructuredViewer viewer;

  @Override
  public void init(final IPageSite pageSite)
  {
    super.init(pageSite);
    final IMenuManager menuManager = pageSite.getActionBars().getMenuManager();
    menuManager.insertBefore(IContextMenuConstants.GROUP_PROPERTIES, new Separator(
        JiveSearchResultPage.LAYOUT_GROUP));
    menuManager.appendToGroup(IContextMenuConstants.GROUP_PROPERTIES, new Action("Preferences...")
      {
        @Override
        public void run()
        {
          final String pageId = "org.eclipse.search.preferences.SearchPreferencePage"; //$NON-NLS-1$
          final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
          PreferencesUtil.createPreferenceDialogOn(shell, pageId, null, null).open();
        }
      });
  }

  @Override
  public void setInput(final ISearchResult newSearch, final Object viewState)
  {
    super.setInput(newSearch, viewState);
    if (newSearch != null)
    {
      if (getLayout() == AbstractTextSearchViewPage.FLAG_LAYOUT_FLAT)
      {
        final IJiveSearchResult result = (IJiveSearchResult) newSearch;
        final IJiveSearchQuery query = result.getQuery();
        resultType = query.getResultType();
        setLayout(AbstractTextSearchViewPage.FLAG_LAYOUT_TREE);
        setLayout(AbstractTextSearchViewPage.FLAG_LAYOUT_FLAT);
      }
      // Display the sequence diagram and notify it of changing results
      final IJiveGraphicalView diagram = (IJiveGraphicalView) showView(JiveUIPlugin.ID_SEQUENCE_DIAGRAM_VIEW);
      final IJiveDebugTarget activeTarget = JiveSearchResultPage.activeTarget();
      if (activeTarget != null)
      {
        final GraphicalViewer viewer = diagram.getViewer();
        final IQueryListener contents = (IQueryListener) viewer.getContents();
        contents.queryFinished(newSearch.getQuery());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void showInSequenceDiagram(final IJiveEvent event)
  {
    final IJiveGraphicalView diagram = (IJiveGraphicalView) showView(JiveUIPlugin.ID_SEQUENCE_DIAGRAM_VIEW);
    // the best we can do is show in the currently active diagram since the search
    // may have been performed across several targets (NOTE: is this desirable?)
    final IJiveDebugTarget activeTarget = JiveSearchResultPage.activeTarget();
    if (activeTarget != null)
    {
      final IStepManager stepManager = JiveUIPlugin.getDefault().stepManager();
      stepManager.pause(activeTarget);
      final IStepAction action = JiveUIPlugin.getDefault().coreFactory()
          .createRunToEventAction(activeTarget, event);
      stepManager.run(activeTarget, action);
      final GraphicalViewer viewer = diagram.getViewer();
      final Map<Object, EditPart> editPartRegistry = viewer.getEditPartRegistry();
      if (editPartRegistry.containsKey(event))
      {
        final EditPart editPart = editPartRegistry.get(event);
        viewer.reveal(editPart);
      }
    }
  }

  private IViewPart showView(final String viewId)
  {
    try
    {
      final IWorkbench workbench = PlatformUI.getWorkbench();
      final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
      final IWorkbenchPage page = window.getActivePage();
      return page.showView(viewId);
    }
    catch (final PartInitException e)
    {
      throw new RuntimeException(e);
    }
  }

  /**
   * This method is called whenever all elements have been removed from the shown
   * <code>AbstractSearchResult</code>. This method is guaranteed to be called in the UI thread.
   * Note that this notification is asynchronous. i.e. further changes may have occurred by the time
   * this method is called. They will be described in a future call.
   */
  @Override
  protected void clear()
  {
    viewer.refresh();
  }

  /**
   * Configures the given viewer. Implementers have to set at least a content provider and a label
   * provider. This method may be called if the page was constructed with the flag
   * <code>FLAG_LAYOUT_FLAT</code>.
   * 
   * @param viewer
   *          the viewer to be configured
   */
  @Override
  protected void configureTableViewer(final TableViewer viewer)
  {
    final Table table = viewer.getTable();
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    if (resultType == null)
    {
      JiveTableViewer.configureTraceTable(viewer);
    }
    else if (resultType.equals(IFieldAssignEvent.class))
    {
      JiveTableViewer.configureAssignEventTable(viewer);
    }
    else if (resultType.equals(IMethodCallEvent.class))
    {
      JiveTableViewer.configureCallEventTable(viewer);
    }
    else if (resultType.equals(IExceptionCatchEvent.class))
    {
      JiveTableViewer.configureCatchEventTable(viewer);
    }
    else if (resultType.equals(ILineStepEvent.class))
    {
      JiveTableViewer.configureStepEventTable(viewer);
    }
    else if (resultType.equals(INewObjectEvent.class))
    {
      JiveTableViewer.configureNewEventTable(viewer);
    }
    else if (resultType.equals(IMethodReturnedEvent.class))
    {
      JiveTableViewer.configureReturnEventTable(viewer);
    }
    else if (resultType.equals(IExceptionThrowEvent.class))
    {
      JiveTableViewer.configureThrowEventTable(viewer);
    }
    else if (resultType.equals(IAssignEvent.class))
    {
      JiveTableViewer.configureInvariantViolatedTable(viewer);
    }
    else
    {
      JiveTableViewer.configureDefaultTable(viewer);
    }
    this.viewer = viewer;
    viewer.setContentProvider(new JiveSearchTableContentProvider());
  }

  /**
   * Configures the given viewer. Implementers have to set at least a content provider and a label
   * provider. This method may be called if the page was constructed with the flag
   * <code>FLAG_LAYOUT_TREE</code>.
   * 
   * @param viewer
   *          the viewer to be configured
   */
  @Override
  protected void configureTreeViewer(final TreeViewer viewer)
  {
    this.viewer = viewer;
    viewer.setContentProvider(new JiveSearchTreeContentProvider(viewer));
    viewer.setLabelProvider(new JiveEventLabelProvider());
  }

  /**
   * This method is called whenever the set of matches for the given elements changes. This method
   * is guaranteed to be called in the UI thread. Note that this notification is asynchronous. i.e.
   * further changes may have occurred by the time this method is called. They will be described in
   * a future call.
   * 
   * @param objects
   *          array of objects that has to be refreshed
   */
  @Override
  protected void elementsChanged(final Object[] objects)
  {
    viewer.refresh();
  }

  @Override
  protected void showMatch(final Match match, final int currentOffset, final int currentLength,
      final boolean activate) throws PartInitException
  {
    if (match != null)
    {
      final IJiveEvent event = (IJiveEvent) match.getElement();
      showInSequenceDiagram(event);
    }
  }

  /**
   * Tabular model for search results.
   */
  private static final class JiveSearchTableContentProvider implements IStructuredContentProvider
  {
    @Override
    public void dispose()
    {
      // no-op
    }

    @Override
    public Object[] getElements(final Object inputElement)
    {
      if (inputElement instanceof JiveSearchResult)
      {
        final JiveSearchResult results = (JiveSearchResult) inputElement;
        final Object[] elements = results.getElements();
        final List<IJiveEvent> events = new ArrayList<IJiveEvent>(elements.length);
        for (final Object element : elements)
        {
          events.add((IJiveEvent) element);
        }
        Collections.sort(events);
        return events.toArray();
      }
      else
      {
        return new Object[] {};
      }
    }

    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput)
    {
      viewer.refresh();
    }
  }

  /**
   * Hierarchical model for search results.
   */
  private static final class JiveSearchTreeContentProvider implements ITreeContentProvider
  {
    /**
     * events contained in the actual search results, augmented with the necessary events to provide
     * the hierarchical model, namely, all call events in the call chain of every query answer
     */
    private final HashSet<IJiveEvent> eventsOfInterest;
    /**
     * events contained in the actual search results
     */
    private final HashSet<IJiveEvent> resultEvents;
    private JiveSearchResult results;

    JiveSearchTreeContentProvider(final Viewer viewer)
    {
      resultEvents = new HashSet<IJiveEvent>();
      eventsOfInterest = new HashSet<IJiveEvent>();
    }

    @Override
    public void dispose()
    {
      // no-op
    }

    @Override
    public Object[] getChildren(final Object parentElement)
    {
      if (results == null || !(parentElement instanceof IInitiatorEvent))
      {
        return new Object[0];
      }
      final IExecutionModel model = results.getModel();
      assert model != null : "Search result expected to provide a non-null model.";
      final List<IJiveEvent> children = ((IInitiatorEvent) parentElement).events();
      // only children of interest, not all
      children.retainAll(eventsOfInterest);
      return children.toArray();
    }

    @Override
    public Object[] getElements(final Object inputElement)
    {
      results = null;
      resultEvents.clear();
      eventsOfInterest.clear();
      if (inputElement instanceof JiveSearchResult)
      {
        results = (JiveSearchResult) inputElement;
        final IExecutionModel model = results.getModel();
        if (model == null)
        {
          return new Object[] {};
        }
        final Object[] elements = results.getElements();
        // load the results into the local sets
        for (final Object element : elements)
        {
          resultEvents.add((IJiveEvent) element);
          // scaffolding for the tree view-- events representing the relevant call chains
          eventsOfInterest.add((IJiveEvent) element);
        }
        // augment eventsOfInterest with the relevant call chains
        final Set<IInitiatorEvent> rootCallers = new HashSet<IInitiatorEvent>();
        // for each event in the result, its path is relevant
        for (final IJiveEvent event : resultEvents)
        {
          IInitiatorEvent parent = event.parent();
          while (parent != null && !(parent instanceof ISystemStartEvent))
          {
            eventsOfInterest.add(parent);
            if (parent instanceof IThreadStartEvent)
            {
              rootCallers.add(parent);
            }
            parent = parent.parent();
          }
        }
        // return the roots for the tree view
        return rootCallers.toArray();
      }
      return new Object[] {};
    }

    @Override
    public Object getParent(final Object element)
    {
      final IExecutionModel model = results.getModel();
      if (model == null)
      {
        return null;
      }
      return ((IJiveEvent) element).parent();
    }

    @Override
    public boolean hasChildren(final Object element)
    {
      return getChildren(element).length > 0;
    }

    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput)
    {
      viewer.refresh();
    }
  }
}