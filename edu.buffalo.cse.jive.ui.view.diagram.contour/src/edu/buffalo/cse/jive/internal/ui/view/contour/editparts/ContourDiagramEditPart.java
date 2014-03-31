package edu.buffalo.cse.jive.internal.ui.view.contour.editparts;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.internal.ui.view.contour.graph.ContourGraph;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IExecutionModel.ITraceViewListener;
import edu.buffalo.cse.jive.preferences.PreferenceKeys;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;
import edu.buffalo.cse.jive.ui.IJiveDiagramEditPart;
import edu.buffalo.cse.jive.ui.IStepAction;
import edu.buffalo.cse.jive.ui.IStepListener;
import edu.buffalo.cse.jive.ui.IStepManager;
import edu.buffalo.cse.jive.ui.IThreadColorListener;
import edu.buffalo.cse.jive.ui.IThreadColorManager;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;
import edu.buffalo.cse.jive.ui.view.diagram.contour.ContourDiagramFactory;

/**
 * An {@code EditPart} serving as a controller for the state part of the execution model, which is
 * visualized by a {@code ContourDiagramFigure}. The edit part also serves as a contour model
 * listener. It handles contour model events by delegating to the appropriate edit part.
 * 
 * @see ContourEditPart
 * @see ContourConnectionEditPart
 * @see TabularContourDiagramFigure
 */
public class ContourDiagramEditPart extends AbstractGraphicalEditPart implements
    IJiveDiagramEditPart, ITraceViewListener, IDebugEventSetListener, IPropertyChangeListener,
    IThreadColorListener, IStepListener
{
  private boolean callPathFocus;
  private State contourState;
  private ContourGraph graph;
  private volatile boolean modelChanged = false;
  /**
   * scrollLock was being used only to reveal the last method contour; since this was not documented
   * and I found no obvious case in which the call to reveal last method contour made any
   * difference, I removed it from the code; I never understood the relationship between reveal last
   * method contour and scroll lock...
   */
  @SuppressWarnings("unused")
  private boolean scrollLock;
  private long updateInterval;
  private final Job updateJob = new Job("OD Update Job")
    {
      @Override
      protected IStatus run(final IProgressMonitor monitor)
      {
        try
        {
          final IJiveDebugTarget target = getModel();
          if (modelChanged && target.viewsEnabled())
          {
            update();
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

  public ContourDiagramEditPart(final Object model)
  {
    final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
    updateContourState(store.getString(PreferenceKeys.PREF_OD_STATE));
    updateCallPathFocus(store.getBoolean(PreferenceKeys.PREF_OD_CALLPATH_FOCUS));
    updateScrollLock(store.getBoolean(PreferenceKeys.PREF_SCROLL_LOCK));
    updateUpdateInterval(store.getLong(PreferenceKeys.PREF_UPDATE_INTERVAL));
    setModel(model);
  }

  @Override
  public void activate()
  {
    super.activate();
    final IExecutionModel model = executionModel();
    model.traceView().register(this);
    DebugPlugin.getDefault().addDebugEventListener(this);
    final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
    store.addPropertyChangeListener(this);
    final IThreadColorManager colorManager = JiveUIPlugin.getDefault().getThreadColorManager();
    colorManager.addThreadColorListener(this);
    final IStepManager stepManager = JiveUIPlugin.getDefault().stepManager();
    stepManager.addStepListener(this);
    updateJob.setSystem(true);
    updateJob.schedule();
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
    final IThreadColorManager manager = JiveUIPlugin.getDefault().getThreadColorManager();
    manager.removeThreadColorListener(this);
    final IStepManager stepManager = JiveUIPlugin.getDefault().stepManager();
    stepManager.removeStepListener(this);
    updateJob.cancel();
    setSelected(EditPart.SELECTED_NONE); // TODO Determine if this is needed
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

  public State getContourState()
  {
    return contourState;
  }

  @Override
  public IJiveDebugTarget getModel()
  {
    return (IJiveDebugTarget) super.getModel();
  }

  @Override
  public void handleDebugEvents(final DebugEvent[] events)
  {
    modelChanged = true;
  }

  public boolean isCallPathFocused()
  {
    return callPathFocus;
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event)
  {
    final String property = event.getProperty();
    if (property.equals(PreferenceKeys.PREF_OD_STATE))
    {
      updateContourState((String) event.getNewValue());
      forceUpdate();
    }
    else if (property.equals(PreferenceKeys.PREF_OD_CALLPATH_FOCUS))
    {
      updateCallPathFocus((Boolean) event.getNewValue());
      forceUpdate();
    }
    else if (property.equals(PreferenceKeys.PREF_SCROLL_LOCK))
    {
      updateScrollLock((Boolean) event.getNewValue());
      forceUpdate();
    }
    else if (property.equals(PreferenceKeys.PREF_UPDATE_INTERVAL))
    {
      final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
      updateUpdateInterval(store.getLong(PreferenceKeys.PREF_UPDATE_INTERVAL));
    }
  }

  @Override
  public void refresh()
  {
    final IJiveDebugTarget target = getModel();
    if (!target.viewsEnabled())
    {
      return;
    }
    executionModel().readLock();
    try
    {
      graph = new ContourGraph(executionModel());
      // refreshChildren gets called from {@code super.refresh()} with an up-to-date graph
      super.refresh();
    }
    finally
    {
      executionModel().readUnlock();
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
  }

  private void forceUpdate()
  {
    update();
    modelChanged = false;
  }

  private void update()
  {
    final Display display = JiveUIPlugin.getStandardDisplay();
    display.asyncExec(new Runnable()
      {
        @Override
        public void run()
        {
          if (isActive())
          {
            for (final Object o : getChildren().toArray())
            {
              removeChild((EditPart) o);
            }
            refresh();
          }
        }
      });
  }

  private void updateCallPathFocus(final boolean callPathFocus)
  {
    this.callPathFocus = callPathFocus;
  }

  private void updateContourState(final String state)
  {
    if (state.equals(PreferenceKeys.PREF_OD_OBJECTS))
    {
      contourState = State.OBJECTS;
    }
    else if (state.equals(PreferenceKeys.PREF_OD_OBJECTS_MEMBERS))
    {
      contourState = State.OBJECTS_MEMBERS;
    }
    else if (state.equals(PreferenceKeys.PREF_OD_STACKED))
    {
      contourState = State.STACKED;
    }
    else if (state.equals(PreferenceKeys.PREF_OD_STACKED_MEMBERS))
    {
      contourState = State.STACKED_MEMBERS;
    }
    else if (state.equals(PreferenceKeys.PREF_OD_MINIMIZED))
    {
      contourState = State.MINIMIZED;
    }
    else
    {
      contourState = State.STACKED;
    }
  }

  private void updateScrollLock(final boolean scrollLock)
  {
    this.scrollLock = scrollLock;
  }

  private void updateUpdateInterval(final long interval)
  {
    updateInterval = interval;
  }

  @Override
  protected void addChildVisual(final EditPart childEditPart, final int index)
  {
    final IFigure childFigure = ((GraphicalEditPart) childEditPart).getFigure();
    assert graph != null : "The contour graph was not constructed.";
    final IContour c = (IContour) childEditPart.getModel();
    getFigure().add(childFigure, graph.getPosition(c), index);
  }

  @Override
  protected void createEditPolicies()
  {
    // installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new NonResizableEditPolicy());
  }

  @Override
  protected IFigure createFigure()
  {
    return ContourDiagramFactory.createContourDigramFigure();
  }

  @Override
  protected List<IContour> getModelChildren()
  {
    return executionModel().contourView().lookupRoots();
  }

  public enum State
  {
    MINIMIZED,
    OBJECTS,
    OBJECTS_MEMBERS,
    STACKED,
    STACKED_MEMBERS
  }
}
