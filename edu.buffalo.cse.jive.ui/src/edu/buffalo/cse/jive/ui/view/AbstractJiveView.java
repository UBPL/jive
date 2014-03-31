package edu.buffalo.cse.jive.ui.view;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_SLICE_CLEAR;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_TRACE_START;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_TRACE_STOP;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.debug.model.IJiveLaunchListener;
import edu.buffalo.cse.jive.debug.model.IJiveLaunchManager;
import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IExecutionModel.ITraceViewListener;
import edu.buffalo.cse.jive.ui.IJiveView;
import edu.buffalo.cse.jive.ui.IStepActionFactory;
import edu.buffalo.cse.jive.ui.IStepManager;
import edu.buffalo.cse.jive.ui.IUpdatableAction;
import edu.buffalo.cse.jive.ui.IUpdatableStepAction;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;

/**
 * An abstract view part used to present {@code IJiveDebugTarget}s. The view part supports viewing
 * multiple targets using a drop down action to switch between them. In order for a target to be
 * displayed, it must be present in the launch manager. As a convenience, the view also provides
 * actions to remove terminated launches in a fashion similar to that of the Console view.
 * <p>
 * Clients offering views of a JIVE model should derive from this class either directly or
 * indirectly.
 * 
 * @see AbstractStructuredJiveView
 * @see AbstractGraphicalJiveView
 */
public abstract class AbstractJiveView extends ViewPart implements IJiveView, IJiveLaunchListener,
    ITraceViewListener, IDebugEventSetListener
{
  /**
   * The group used to hold actions having to do with removing terminated launches.
   */
  // protected static final String REMOVE_TERMINATED_GROUP = "removeTerminatedGroup";
  /**
   * This group holds scalability related actions-- clear slice, start/stop event collection.
   */
  protected static final String GROUP_SCALABILITY = "scalabilityGroup";
  /**
   * The group used to hold actions having to do with stepping through a program in both directions.
   */
  protected static final String GROUP_STEP_CONTROLS = "stepControlsGroup";

  private static final IJiveLaunchManager launchManager()
  {
    return JiveLaunchPlugin.getDefault().getLaunchManager();
  }

  /**
   * The {@code IJiveDebugTarget} that is currently being presented by the view.
   */
  private IJiveDebugTarget fRenderedTarget;
  /**
   * An action that clears an active slice on the active target.
   */
  private IUpdatableAction actionSliceClear;
  /**
   * An action that starts Jive's event trace collection on the active target.
   */
  private IUpdatableAction actionTraceStart;
  /**
   * An action that stops Jive's event trace collection on the active target.
   */
  private IUpdatableAction actionTraceStop;
  /**
   * An action that halts any commit or rollback in progress.
   */
  private IUpdatableStepAction fPauseAction;
  /**
   * The action used to remove all terminated launches.
   */
  // private RemoveAllTerminatedLaunchesAction fRemoveAllTerminatedAction;
  /**
   * The action used to remove the launch of the active target if its process has terminated.
   */
  // private RemoveTerminatedLaunchAction fRemoveTerminatedAction;
  /**
   * An action that steps forward in the debug target's transaction history until the last
   * transaction has been committed.
   */
  private IUpdatableStepAction fRunBackwardAction;
  /**
   * An action that steps backward in the debug target's transaction history until the first
   * transaction has been rolled back.
   */
  private IUpdatableStepAction fRunForwardAction;
  /**
   * An action that takes one step backward in the debug target's transaction history.
   */
  private IUpdatableStepAction fStepBackwardAction;
  /**
   * An action that takes one step forward in the debug target's transaction history.
   */
  private IUpdatableStepAction fStepForwardAction;
  /**
   * The list of actions that should be updated when the active target changes.
   */
  private final List<IUpdatableAction> fUpdatableActionList;

  /**
   * Constructs the view.
   */
  public AbstractJiveView()
  {
    super();
    fRenderedTarget = null;
    fUpdatableActionList = new LinkedList<IUpdatableAction>();
  }

  @Override
  public void createPartControl(final Composite parent)
  {
    // allow views to be selectively enabled/disabled
    if (!this.isEnabled())
    {
      return;
    }
    // Initialize the viewer
    initializeViewer(parent);
    // Initialize the instance variables
    initializeTargetList();
    createActions();
    // Create the context menu
    createContextMenu();
    // Initialize the action bars
    final IActionBars actionBars = getViewSite().getActionBars();
    configureToolBar(actionBars.getToolBarManager());
    configurePullDownMenu(actionBars.getMenuManager());
    actionBars.updateActionBars();
    // Register as a listener
    DebugPlugin.getDefault().addDebugEventListener(this);
    JiveLaunchPlugin.getDefault().getLaunchManager().registerListener(this);
    // getSite().getPage().addPostSelectionListener(this);
    debugTargetDisplay(fRenderedTarget);
  }

  @Override
  public void display(final IJiveDebugTarget target)
  {
    if (target != fRenderedTarget)
    {
      debugTargetDisplay(target);
    }
  }

  @Override
  public void dispose()
  {
    // allow views to be selectively enabled/disabled
    if (!this.isEnabled())
    {
      return;
    }
    // Unregister as a listener
    DebugPlugin.getDefault().removeDebugEventListener(this);
    JiveLaunchPlugin.getDefault().getLaunchManager().unregisterListener(this);
    setRenderedTarget(null);
    final IStepManager stepManager = JiveUIPlugin.getDefault().stepManager();
    stepManager.removeStepListener(fStepBackwardAction);
    stepManager.removeStepListener(fStepForwardAction);
    stepManager.removeStepListener(fRunBackwardAction);
    stepManager.removeStepListener(fRunForwardAction);
    stepManager.removeStepListener(fPauseAction);
    super.dispose();
  }

  /**
   * Called when a new set of events is added to the model
   */
  @Override
  public void eventsInserted(final List<IJiveEvent> events)
  {
  }

  @Override
  public void handleDebugEvents(final DebugEvent[] events)
  {
    // allow views to be selectively enabled/disabled
    if (!this.isEnabled())
    {
      return;
    }
    // dispatch an async update for at most one event
    DebugEvent updateEvent = null;
    for (final DebugEvent event : events)
    {
      if (event.getKind() == DebugEvent.RESUME || event.getKind() == DebugEvent.SUSPEND
          || event.getKind() == DebugEvent.TERMINATE || event.getKind() == DebugEvent.STATE)
      {
        updateEvent = event;
        break;
      }
    }
    if (updateEvent != null)
    {
      updateActionBar();
    }
  }

  @Override
  public void targetSelected(final IJiveDebugTarget target)
  {
    // allow views to be selectively enabled/disabled
    if (!this.isEnabled())
    {
      return;
    }
    final Display display = JiveUIPlugin.getStandardDisplay();
    display.syncExec(new Runnable()
      {
        @Override
        public void run()
        {
          debugTargetDisplay(target);
        }
      });
  }

  /**
   * Called when a the trace is set from normal to virtualized or vice-versa.
   */
  @Override
  public void traceVirtualized(final boolean isVirtual)
  {
    updateActionBar();
  }

  public void updateActionBar()
  {
    final Display display = JiveUIPlugin.getStandardDisplay();
    display.asyncExec(new Runnable()
      {
        @Override
        public void run()
        {
          updateActions();
        }
      });
  }

  /**
   * Displays the supplied target in the view part. This target is put in front of the list of most
   * recently viewed targets.
   * <p>
   * This method should only be called on the UI thread.
   */
  private void debugTargetDisplay(final IJiveDebugTarget target)
  {
    setRenderedTarget(target);
    setViewerInput(target);
    updateContentDescription();
    updateActions();
  }

  private IJiveDebugTarget getActiveTarget()
  {
    return fRenderedTarget;
  }

  /**
   * Initializes the target list by consulting with the launch manager for {@code IJiveDebugTarget}
   * s, then registers with the manager as a listener. The first target, if any, is assigned as the
   * active target.
   */
  private void initializeTargetList()
  {
    setRenderedTarget(AbstractJiveView.launchManager().activeTarget());
    AbstractJiveView.launchManager().registerListener(this);
  }

  private void setRenderedTarget(final IJiveDebugTarget newTarget)
  {
    if (fRenderedTarget != null)
    {
      fRenderedTarget.model().traceView().unregister(this);
    }
    fRenderedTarget = newTarget;
    if (fRenderedTarget != null)
    {
      fRenderedTarget.model().traceView().register(this);
    }
  }

  /**
   * Updates the actions' states based on the active and inactive targets (depending on the action).
   * <p>
   * This method should only be called on the UI thread.
   */
  private void updateActions()
  {
    for (final IUpdatableAction action : fUpdatableActionList)
    {
      action.update();
    }
  }

  /**
   * Updates the content description based on the active target. If there isn't an active target,
   * then a default description is used.
   * <p>
   * This method should only be called on the UI thread.
   */
  private void updateContentDescription()
  {
    if (fRenderedTarget == null)
    {
      setContentDescription(getDefaultContentDescription());
    }
    else
    {
      setContentDescription(fRenderedTarget.getName());
    }
  }

  /**
   * Adds an {@code IUpdatableAction} to the list of actions that must be updated when the active
   * target changes.
   * 
   * @param action
   *          the action to add
   */
  protected void addUpdatableAction(final IUpdatableAction action)
  {
    fUpdatableActionList.add(action);
  }

  /**
   * Configures the view's pull-down menu with actions. Subclasses should override this method if a
   * pull-down menu is desired.
   * 
   * @param manager
   *          the menu manager in which to add actions
   */
  protected void configurePullDownMenu(final IMenuManager manager)
  {
    // do nothing
  }

  /**
   * Adds the actions to the tool bar using the supplied tool bar manager.
   * 
   * @param manager
   *          the manager used to add the actions to the tool bar
   */
  protected void configureToolBar(final IToolBarManager manager)
  {
    manager.add(new Separator(AbstractJiveView.GROUP_SCALABILITY));
    manager.appendToGroup(AbstractJiveView.GROUP_SCALABILITY, actionTraceStart);
    manager.appendToGroup(AbstractJiveView.GROUP_SCALABILITY, actionTraceStop);
    manager.appendToGroup(AbstractJiveView.GROUP_SCALABILITY, actionSliceClear);
    manager.add(new Separator(AbstractJiveView.GROUP_STEP_CONTROLS));
    manager.appendToGroup(AbstractJiveView.GROUP_STEP_CONTROLS, fRunBackwardAction);
    manager.appendToGroup(AbstractJiveView.GROUP_STEP_CONTROLS, fStepBackwardAction);
    manager.appendToGroup(AbstractJiveView.GROUP_STEP_CONTROLS, fPauseAction);
    manager.appendToGroup(AbstractJiveView.GROUP_STEP_CONTROLS, fStepForwardAction);
    manager.appendToGroup(AbstractJiveView.GROUP_STEP_CONTROLS, fRunForwardAction);
  }

  /**
   * Creates the actions that will be used by the view.
   */
  protected void createActions()
  {
    actionTraceStart = new TraceStartAction();
    actionTraceStop = new TraceStopAction();
    actionSliceClear = new SliceClearAction();
    addUpdatableAction(actionTraceStart);
    addUpdatableAction(actionTraceStop);
    addUpdatableAction(actionSliceClear);
    final IStepActionFactory factory = JiveUIPlugin.getDefault().createStepActionFactory(this);
    fStepBackwardAction = factory.createStepBackwardAction();
    fStepForwardAction = factory.createStepForwardAction();
    fRunBackwardAction = factory.createRunBackwardAction();
    fRunForwardAction = factory.createRunForwardAction();
    fPauseAction = factory.createPauseAction();
    final IStepManager stepManager = JiveUIPlugin.getDefault().stepManager();
    stepManager.addStepListener(fStepBackwardAction);
    stepManager.addStepListener(fStepForwardAction);
    stepManager.addStepListener(fRunBackwardAction);
    stepManager.addStepListener(fRunForwardAction);
    stepManager.addStepListener(fPauseAction);
    addUpdatableAction(fStepBackwardAction);
    addUpdatableAction(fStepForwardAction);
    addUpdatableAction(fRunBackwardAction);
    addUpdatableAction(fRunForwardAction);
    addUpdatableAction(fPauseAction);
  }

  /**
   * Creates the context menu for the view. Subclasses must implement this method.
   */
  protected abstract void createContextMenu();

  /**
   * Returns the content description to be used when there is no active target.
   * 
   * @return the default content description
   * @see #setContentDescription(String)
   */
  protected abstract String getDefaultContentDescription();

  /**
   * Returns the {@code ImageDescriptor} of the image to be used for the
   * {@code DisplayTargetDropDownAction} when it is disabled.
   * 
   * @return the disabled image descriptor for the display target drop down
   */
  protected abstract ImageDescriptor getDisplayDropDownDisabledImageDescriptor();

  /**
   * Returns the {@code ImageDescriptor} of the image to be used for the
   * {@code DisplayTargetDropDownAction} when it is enabled.
   * 
   * @return the enabled image descriptor for the display target drop down
   */
  protected abstract ImageDescriptor getDisplayDropDownEnabledImageDescriptor();

  /**
   * Returns the text to be used for the {@code DisplayTargetDropDownAction}.
   * 
   * @return the text for the display target drop down
   */
  protected abstract String getDisplayTargetDropDownText();

  /**
   * Called immediately in {@link #createPartControl(Composite)} to initialize the internal viewer
   * for the view. Subclasses must implement this method for the specific framework being used.
   * 
   * @param parent
   *          the parent widget of the viewer
   */
  protected abstract void initializeViewer(Composite parent);

  /**
   * If the view is not enabled, the createPartControl.
   */
  protected boolean isEnabled()
  {
    return fRenderedTarget == null || fRenderedTarget.viewsEnabled();
  }

  /**
   * Sets the internal viewer's input to that of the supplied target. Subclasses must implement this
   * method for the specific framework being used.
   * 
   * @param target
   *          the target to set as input.
   */
  protected abstract void setViewerInput(IJiveDebugTarget target);

  /**
   * This action clears an active slice on the model.
   */
  private class SliceClearAction extends Action implements IUpdatableAction
  {
    SliceClearAction()
    {
      super("Clear Slice");
      setImageDescriptor(IM_BASE_SLICE_CLEAR.enabledDescriptor());
      setDisabledImageDescriptor(IM_BASE_SLICE_CLEAR.disabledDescriptor());
    }

    @Override
    public void run()
    {
      if (fRenderedTarget != null)
      {
        fRenderedTarget.model().sliceView().clearSlice();
      }
    }

    @Override
    public void update()
    {
      final IJiveDebugTarget target = getActiveTarget();
      setEnabled(target != null && target.model().sliceView().activeSlice() != null);
    }
  }

  /**
   * This action starts Jive's tracing.
   */
  private class TraceStartAction extends Action implements IUpdatableAction
  {
    TraceStartAction()
    {
      super("Start Tracing");
      setImageDescriptor(IM_BASE_TRACE_START.enabledDescriptor());
      setDisabledImageDescriptor(IM_BASE_TRACE_START.disabledDescriptor());
    }

    @Override
    public void run()
    {
      // an unrendered target but an active target
      if (fRenderedTarget == null)
      {
        final IJiveDebugTarget target = JiveLaunchPlugin.getDefault().getLaunchManager()
            .activeTarget();
        if (target != null && !target.isTerminated() && !target.isActive())
        {
          target.start();
          updateActions();
        }
      }
      // an already rendered target
      else if (fRenderedTarget != null && !fRenderedTarget.isTerminated()
          && !fRenderedTarget.isActive())
      {
        fRenderedTarget.start();
        updateActions();
      }
    }

    @Override
    public void update()
    {
      // an unrendered target
      if (fRenderedTarget == null)
      {
        final IJiveDebugTarget target = JiveLaunchPlugin.getDefault().getLaunchManager()
            .activeTarget();
        setEnabled(target != null && !target.isTerminated() && !target.isActive());
      }
      // an already rendered target
      else
      {
        setEnabled(fRenderedTarget != null && !fRenderedTarget.isTerminated()
            && !fRenderedTarget.isActive());
      }
    }
  }

  /**
   * This action stops Jive's tracing.
   */
  private class TraceStopAction extends Action implements IUpdatableAction
  {
    TraceStopAction()
    {
      super("Stop Tracing");
      setImageDescriptor(IM_BASE_TRACE_STOP.enabledDescriptor());
      setDisabledImageDescriptor(IM_BASE_TRACE_STOP.disabledDescriptor());
    }

    @Override
    public void run()
    {
      if (fRenderedTarget != null && !fRenderedTarget.isTerminated() && fRenderedTarget.isActive())
      {
        fRenderedTarget.stop();
        updateActions();
      }
    }

    @Override
    public void update()
    {
      setEnabled(fRenderedTarget != null && !fRenderedTarget.isTerminated()
          && fRenderedTarget.isActive());
    }
  }
}