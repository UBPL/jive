package edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.search.ui.IQueryListener;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultListener;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.SearchResultEvent;
import org.eclipse.search.ui.text.Match;
import org.eclipse.search.ui.text.MatchEvent;
import org.eclipse.search.ui.text.RemoveAllEvent;
import org.eclipse.swt.widgets.Display;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.figures.SequenceDiagramFigure;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IEventModel.ISystemStartEvent;
import edu.buffalo.cse.jive.model.IEventModel.IThreadStartEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IExecutionModel.ITraceViewListener;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;
import edu.buffalo.cse.jive.preferences.PreferenceKeys;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;
import edu.buffalo.cse.jive.ui.IJiveDiagramEditPart;
import edu.buffalo.cse.jive.ui.IStepAction;
import edu.buffalo.cse.jive.ui.IStepListener;
import edu.buffalo.cse.jive.ui.IStepManager;
import edu.buffalo.cse.jive.ui.IThreadColorListener;
import edu.buffalo.cse.jive.ui.IThreadColorManager;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;
import edu.buffalo.cse.jive.ui.search.IJiveSearchQuery;
import edu.buffalo.cse.jive.ui.search.IJiveSearchResult;

public class SequenceDiagramEditPart extends AbstractGraphicalEditPart implements
    IJiveDiagramEditPart, ITraceViewListener, IDebugEventSetListener, IPropertyChangeListener,
    IQueryListener, ISearchResultListener, IStepListener, IThreadColorListener
{
  private volatile boolean modelChanged = false;
  private final UIAdapter uiAdapter;
  private final ModelAdapter modelAdapter;
  private final Map<IInitiatorEvent, List<IJiveEvent>> searchResultMap = new LinkedHashMap<IInitiatorEvent, List<IJiveEvent>>();
  private boolean showExpandedLifeLines;
  private long updateInterval;
  private final Set<IThreadValue> hiddenThreads = new HashSet<IThreadValue>();
  private final Job updateJob = new Job("SD Update Job")
    {
      @Override
      protected IStatus run(final IProgressMonitor monitor)
      {
        try
        {
          final IJiveDebugTarget target = getModel();
          if (modelChanged && target.viewsEnabled())
          {
            update(null);
            modelChanged = false;
          }
          return Status.OK_STATUS;
        }
        catch (final Exception e)
        {
          JiveUIPlugin.log(e);
          return Status.OK_STATUS;
        }
        finally
        {
          schedule(updateInterval);
        }
      }
    };

  public SequenceDiagramEditPart()
  {
    uiAdapter = new UIAdapter();
    modelAdapter = new ModelAdapter(uiAdapter);
    final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
    updateThreadActivationsState(store.getBoolean(PreferenceKeys.PREF_SD_SHOW_THREAD_ACTIVATIONS));
    showExpandedLifeLines = store.getBoolean(PreferenceKeys.PREF_SD_EXPAND_LIFELINES);
    updateLifelinesState(showExpandedLifeLines);
    updateUpdateInterval(store.getLong(PreferenceKeys.PREF_UPDATE_INTERVAL));
  }

  @Override
  public void activate()
  {
    super.activate();
    final IExecutionModel model = executionModel();
    uiAdapter.setModel(model);
    model.traceView().register(this);
    DebugPlugin.getDefault().addDebugEventListener(this);
    final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
    store.addPropertyChangeListener(this);
    final IStepManager stepManager = JiveUIPlugin.getDefault().stepManager();
    stepManager.addStepListener(this);
    final IThreadColorManager threadColorManager = JiveUIPlugin.getDefault()
        .getThreadColorManager();
    threadColorManager.addThreadColorListener(this);
    NewSearchUI.addQueryListener(this);
    updateJob.setSystem(true);
    updateJob.schedule();
  }

  public void collapseByTime(final IInitiatorEvent execution, final boolean isBefore)
  {
    uiAdapter.collapseByTime(execution, isBefore);
    forceUpdate(execution);
  }

  public void collapseChildren(final IInitiatorEvent execution)
  {
    uiAdapter.collapseChildren(execution);
    forceUpdate(execution);
  }

  public void collapseExecution(final IInitiatorEvent execution)
  {
    uiAdapter.collapseExecution(execution);
    forceUpdate(execution);
  }

  public void collapseLifeline(final IContextContour contour)
  {
    for (final IInitiatorEvent event : contour.nestedInitiators())
    {
      uiAdapter.collapseExecution(event);
    }
    forceUpdate();
  }

  public void collapseLifelineChildren(final IContextContour contour)
  {
    for (final IInitiatorEvent execution : contour.nestedInitiators())
    {
      uiAdapter.collapseChildren(execution);
    }
    forceUpdate();
  }

  @Override
  public void deactivate()
  {
    final IExecutionModel model = executionModel();
    if (model != null)
    {
      model.traceView().unregister(this);
    }
    DebugPlugin.getDefault().removeDebugEventListener(this);
    final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
    store.removePropertyChangeListener(this);
    final IStepManager stepManager = JiveUIPlugin.getDefault().stepManager();
    stepManager.removeStepListener(this);
    final IThreadColorManager threadColorManager = JiveUIPlugin.getDefault()
        .getThreadColorManager();
    threadColorManager.removeThreadColorListener(this);
    NewSearchUI.removeQueryListener(this);
    updateJob.cancel();
    setSelected(EditPart.SELECTED_NONE);
    super.deactivate();
  }

  @Override
  public void eventsInserted(final List<IJiveEvent> events)
  {
    modelChanged = true;
  }

  public IExecutionModel executionModel()
  {
    final IJiveDebugTarget target = getModel();
    return target != null ? target.model() : null;
  }

  public void expandChildren(final IInitiatorEvent execution)
  {
    uiAdapter.expandChildren(execution);
    forceUpdate(execution);
  }

  public void expandExecution(final IInitiatorEvent execution)
  {
    uiAdapter.expandExecution(execution);
    forceUpdate(execution);
  }

  public void expandExecutionByTime(final IInitiatorEvent execution, final boolean isBefore)
  {
    uiAdapter.expandByTime(execution, isBefore);
    forceUpdate(execution);
  }

  public void expandLifeline(final IContextContour contour)
  {
    for (final IInitiatorEvent event : contour.nestedInitiators())
    {
      uiAdapter.expandExecution(event);
    }
    forceUpdate();
  }

  public void expandLifelineChildren(final IContextContour contour)
  {
    for (final IInitiatorEvent execution : contour.nestedInitiators())
    {
      uiAdapter.expandChildren(execution);
    }
    forceUpdate();
  }

  public void focus(final IInitiatorEvent execution)
  {
    uiAdapter.collapseByTime(execution, true);
    uiAdapter.collapseByTime(execution, false);
    forceUpdate(execution);
  }

  @Override
  public IJiveDebugTarget getModel()
  {
    return (IJiveDebugTarget) super.getModel();
  }

  public ModelAdapter getModelAdapter()
  {
    return modelAdapter;
  }

  public UIAdapter getUIAdapter()
  {
    return uiAdapter;
  }

  @Override
  public void handleDebugEvents(final DebugEvent[] events)
  {
    modelChanged = true;
  }

  public boolean isCollapsed(final IInitiatorEvent execution)
  {
    return uiAdapter.isCollapsed(execution);
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event)
  {
    final String property = event.getProperty();
    if (property.equals(PreferenceKeys.PREF_SD_SHOW_THREAD_ACTIVATIONS))
    {
      updateThreadActivationsState((Boolean) event.getNewValue());
      forceUpdate();
    }
    else if (property.equals(PreferenceKeys.PREF_SD_EXPAND_LIFELINES))
    {
      lifeLineStateChange((Boolean) event.getNewValue());
    }
    else if (property.equals(PreferenceKeys.PREF_SD_ACTIVATION_WIDTH))
    {
      forceUpdate();
    }
    else if (property.equals(PreferenceKeys.PREF_SD_EVENT_HEIGHT))
    {
      forceUpdate();
    }
    else if (property.equals(PreferenceKeys.PREF_UPDATE_INTERVAL))
    {
      final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
      updateUpdateInterval(store.getLong(PreferenceKeys.PREF_UPDATE_INTERVAL));
    }
  }

  @Override
  public void queryAdded(final ISearchQuery query)
  {
    // no-op
  }

  @Override
  public void queryFinished(final ISearchQuery query)
  {
    unfocusResults();
    if (query instanceof IJiveSearchQuery)
    {
      // update search results
      final IJiveSearchResult jiveResult = (IJiveSearchResult) query.getSearchResult();
      for (final Object element : jiveResult.getElements())
      {
        addSearchResults(jiveResult.getMatches(element));
      }
      // focus the sequence diagram on the current results
      focusResults();
      query.getSearchResult().addListener(this);
      // TODO Keep track of result in order to remove listener in case the view is closed
    }
    forceUpdate();
  }

  @Override
  public void queryRemoved(final ISearchQuery query)
  {
    if (query instanceof IJiveSearchQuery)
    {
      // clear focus on results
      unfocusResults();
      forceUpdate();
      query.getSearchResult().removeListener(this);
    }
  }

  @Override
  public void queryStarting(final ISearchQuery query)
  {
    // no-op
  }

  @Override
  public void searchResultChanged(final SearchResultEvent e)
  {
    final ISearchResult result = e.getSearchResult();
    if (result instanceof IJiveSearchResult)
    {
      if (e instanceof MatchEvent)
      {
        final MatchEvent matchEvent = (MatchEvent) e;
        switch (matchEvent.getKind())
        {
          case MatchEvent.ADDED:
            addSearchResults(matchEvent.getMatches());
            break;
          case MatchEvent.REMOVED:
            if (searchResultMap.size() > 1)
            {
              removeSearchResults(matchEvent.getMatches());
              focusResults();
            }
            else
            {
              unfocusResults();
            }
            break;
        }
      }
      else if (e instanceof RemoveAllEvent)
      {
        unfocusResults();
      }
      forceUpdate();
    }
  }

  @Override
  public void steppingCompleted(final IJiveDebugTarget target, final IStepAction action)
  {
    if (getModel() == target)
    {
      forceUpdate();
    }
  }

  @Override
  public void steppingInitiated(final IJiveDebugTarget target)
  {
    // no-op
  }

  @Override
  public void threadColorsChanged(final IJiveDebugTarget target)
  {
    if (getModel() == target)
    {
      forceUpdate();
    }
  }

  @Override
  public void traceVirtualized(final boolean isVirtual)
  {
    forceUpdate();
    final IJiveEvent event = getModel().model().temporalState().event();
    // synchronize the source from the UI thread
    final Display display = JiveUIPlugin.getStandardDisplay();
    display.asyncExec(new Runnable()
      {
        @Override
        public void run()
        {
          JiveUIPlugin.getDefault().coreFactory().sourceLookupFacility()
              .selectLine(getModel(), event);
        }
      });
  }

  private void addSearchResults(final Match[] matches)
  {
    for (final Match match : matches)
    {
      final IJiveEvent event = (IJiveEvent) match.getElement();
      // slice queries may return system start events
      if (event instanceof ISystemStartEvent || event instanceof IThreadStartEvent)
      {
        continue;
      }
      // events from threads that are currently hidden are ignored
      if (hiddenThreads.contains(event.thread()))
      {
        continue;
      }
      IInitiatorEvent execution = event.parent();
      if (execution instanceof IMethodCallEvent
          && ((IMethodCallEvent) execution).target().isOutOfModel())
      {
        execution = execution.parent();
      }
      if (!searchResultMap.containsKey(execution))
      {
        searchResultMap.put(execution, new LinkedList<IJiveEvent>());
      }
      searchResultMap.get(execution).add(event);
    }
  }

  private void focusResults()
  {
    if (searchResultMap.size() == 0)
    {
      return;
    }
    // break the results by thread
    final Map<IThreadValue, List<IInitiatorEvent>> map = partitionResults();
    // the sequence model only supports visitation on a per-thread basis
    for (final IThreadValue thread : map.keySet())
    {
      uiAdapter.collapseBetween(map.get(thread));
    }
    final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
    final boolean showThreads = store.getBoolean(PreferenceKeys.PREF_SD_SHOW_THREAD_ACTIVATIONS);
    for (final IThreadStartEvent event : getModel().model().lookupThreads())
    {
      if (map.containsKey(event.thread()))
      {
        continue;
      }
      if (showThreads)
      {
        uiAdapter.collapseExecution(event);
      }
      else
      {
        uiAdapter.collapseChildren(event);
      }
    }
    final IJiveDebugTarget target = getModel();
    final IJiveEvent first = getFirstResult();
    final IStepManager stepManager = JiveUIPlugin.getDefault().stepManager();
    stepManager.pause(target);
    final IStepAction action = JiveUIPlugin.getDefault().coreFactory()
        .createRunToEventAction(target, first);
    stepManager.run(target, action);
    final Display display = JiveUIPlugin.getStandardDisplay();
    display.asyncExec(new Runnable()
      {
        @Override
        public void run()
        {
          // jump to the first answer
          showInSequenceDiagram(first instanceof IInitiatorEvent ? (IInitiatorEvent) first : first
              .parent());
        }
      });
  }

  /**
   * Called in response to a user interaction.
   */
  private void forceUpdate()
  {
    final IJiveEvent event = getModel().model().temporalState().event();
    if (event == null)
    {
      forceUpdate(null);
    }
    forceUpdate(event instanceof IInitiatorEvent ? (IInitiatorEvent) event : event.parent());
  }

  /**
   * Called in response to a user interaction.
   */
  private void forceUpdate(final IInitiatorEvent execution)
  {
    update(execution instanceof ISystemStartEvent ? null : execution);
    modelChanged = false;
  }

  private IJiveEvent getFirstResult()
  {
    IJiveEvent first = null;
    // compute a per-thread list of execution occurrences for the results
    for (final IInitiatorEvent key : searchResultMap.keySet())
    {
      for (final IJiveEvent e : searchResultMap.get(key))
      {
        if (first == null)
        {
          first = e;
        }
        else if (e.eventId() < first.eventId())
        {
          first = e;
        }
      }
    }
    return first;
  }

  private void lifeLineStateChange(final boolean newValue)
  {
    final Display display = JiveUIPlugin.getStandardDisplay();
    display.syncExec(new Runnable()
      {
        @Override
        public void run()
        {
          if (isActive())
          {
            executionModel().readLock();
            try
            {
              for (final Object o : getChildren().toArray())
              {
                final EditPart part = (EditPart) o;
                removeChild(part);
              }
              showExpandedLifeLines = newValue;
              updateLifelinesState(showExpandedLifeLines);
              refresh();
              final IJiveEvent event = getModel().model().temporalState().event();
              final IInitiatorEvent execution = event instanceof IInitiatorEvent ? (IInitiatorEvent) event
                  : event.parent();
              if (execution != null)
              {
                showInSequenceDiagram(execution);
              }
            }
            finally
            {
              executionModel().readUnlock();
            }
          }
        }
      });
  }

  /**
   * Partitions the search results based upon their originating threads and returns the earliest
   * event in the input array.
   */
  private Map<IThreadValue, List<IInitiatorEvent>> partitionResults()
  {
    // compute a per-thread list of execution occurrences for the results
    final Map<IThreadValue, List<IInitiatorEvent>> map = new HashMap<IThreadValue, List<IInitiatorEvent>>();
    for (final IInitiatorEvent e : searchResultMap.keySet())
    {
      final IThreadValue thread = e.thread();
      if (!map.containsKey(thread))
      {
        map.put(thread, new ArrayList<IInitiatorEvent>());
      }
      map.get(thread).add(e);
    }
    return map;
  }

  private void removeSearchResults(final Match[] matches)
  {
    final IExecutionModel model = executionModel();
    model.readLock();
    try
    {
      for (final Match match : matches)
      {
        final IJiveEvent event = (IJiveEvent) match.getElement();
        final IInitiatorEvent execution = event.parent();
        if (searchResultMap.containsKey(execution))
        {
          final List<IJiveEvent> results = searchResultMap.get(execution);
          results.remove(event);
          if (results.isEmpty())
          {
            searchResultMap.remove(execution);
          }
        }
      }
    }
    finally
    {
      model.readUnlock();
    }
  }

  @SuppressWarnings("unchecked")
  private void showInSequenceDiagram(final IInitiatorEvent event)
  {
    final IJiveDebugTarget target = getModel();
    if (target != null && event != null)
    {
      final Map<Object, EditPart> registry = getViewer().getEditPartRegistry();
      if (registry.containsKey(event))
      {
        final EditPart editPart = registry.get(event);
        getViewer().deselectAll();
        getViewer().flush();
        getViewer().select(editPart);
        getViewer().reveal(editPart);
      }
    }
  }

  private void unfocusResults()
  {
    if (searchResultMap.size() > 0)
    {
      final Map<IThreadValue, List<IInitiatorEvent>> map = partitionResults();
      // the sequence model only supports visitation on a per-thread basis
      for (final IThreadValue thread : map.keySet())
      {
        for (final IInitiatorEvent event : map.get(thread))
        {
          uiAdapter.expandPath(event);
        }
      }
      searchResultMap.clear();
    }
  }

  private void update(final IInitiatorEvent execution)
  {
    final Display display = JiveUIPlugin.getStandardDisplay();
    display.asyncExec(new Runnable()
      {
        @Override
        public void run()
        {
          if (isActive())
          {
            executionModel().readLock();
            try
            {
              for (final Object o : getChildren().toArray())
              {
                final EditPart part = (EditPart) o;
                removeChild(part);
              }
              refresh();
              if (execution != null)
              {
                showInSequenceDiagram(execution);
              }
            }
            finally
            {
              executionModel().readUnlock();
            }
          }
        }
      });
  }

  private void updateLifelinesState(final boolean expandedLifelines)
  {
    modelAdapter.setExpandLifelines(expandedLifelines);
  }

  private void updateTemporalContext()
  {
    final IJiveDebugTarget target = getModel();
    final IExecutionModel model = executionModel();
    final SequenceDiagramFigure figure = (SequenceDiagramFigure) getFigure();
    if (target.isTerminated() || target.isDisconnected() || target.isSuspended())
    {
      if ((model.temporalState().readyToRecord() || model.temporalState().canRollback())
          && model.temporalState().event() != null)
      {
        figure.setCurrentEventNumber((int) model.temporalState().event().eventId());
      }
      else
      {
        figure.setCurrentEventNumber(0);
      }
    }
    else
    {
      final IInitiatorEvent root = executionModel().lookupRoot();
      figure.setCurrentEventNumber(root == null || root.lastChildEvent() == null ? 0 : (int) root
          .lastChildEvent().eventId());
    }
  }

  private void updateThreadActivationsState(final boolean showThreadActivations)
  {
    modelAdapter.setShowThreadActivations(showThreadActivations);
  }

  private void updateUpdateInterval(final long interval)
  {
    updateInterval = interval;
  }

  @Override
  protected void createEditPolicies()
  {
    // no-op
  }

  @Override
  protected IFigure createFigure()
  {
    return new SequenceDiagramFigure();
  }

  @Override
  protected List<Object> getModelChildren()
  {
    final IExecutionModel model = executionModel();
    model.readLock();
    try
    {
      final List<Object> result = new ArrayList<Object>();
      result.add(new Gutter(model.lookupRoot()));
      modelAdapter.update(model, hiddenThreads);
      result.addAll(modelAdapter.lifelines());
      return result;
    }
    finally
    {
      model.readUnlock();
    }
  }

  @Override
  protected void refreshVisuals()
  {
    super.refreshVisuals();
    executionModel().readLock();
    try
    {
      updateTemporalContext();
    }
    finally
    {
      executionModel().readUnlock();
    }
  }

  List<Message> getFoundMessages(final IContour context)
  {
    return modelAdapter.getSourceMessages(context);
  }

  List<Message> getLostMessages(final IContour context)
  {
    return modelAdapter.getTargetMessages(context);
  }

  List<IJiveEvent> getSearchResults(final IInitiatorEvent execution)
  {
    if (searchResultMap.containsKey(execution))
    {
      return searchResultMap.get(execution);
    }
    else
    {
      return Collections.emptyList();
    }
  }

  List<Message> getSourceMessages(final IInitiatorEvent execution)
  {
    return modelAdapter.getSourceMessages(execution);
  }

  List<Message> getTargetMessages(final IInitiatorEvent execution)
  {
    return modelAdapter.getTargetMessages(execution);
  }

  boolean showExpandedLifeLines()
  {
    return showExpandedLifeLines;
  }

  public boolean isHiddenThread(IThreadValue thread)
  {
    return hiddenThreads.contains(thread);
  }

  public void toggleHiddenThread(IThreadValue thread, boolean hidden)
  {
    if (hidden)
    {
      hiddenThreads.add(thread);
    }
    else
    {
      hiddenThreads.remove(thread);
    }
    forceUpdate();
  }
}