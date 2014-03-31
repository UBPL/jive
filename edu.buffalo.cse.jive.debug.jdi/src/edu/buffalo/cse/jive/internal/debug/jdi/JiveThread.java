package edu.buffalo.cse.jive.internal.debug.jdi;

import java.util.List;

import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.internal.debug.core.IJDIEventListener;
import org.eclipse.jdt.internal.debug.core.model.JDIDebugTarget;
import org.eclipse.jdt.internal.debug.core.model.JDIThreadAdapter;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;

import edu.buffalo.cse.jive.debug.jdi.model.IJDIEventHandler;
import edu.buffalo.cse.jive.debug.jdi.model.IModelFilter;
import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.debug.model.IJiveStep;
import edu.buffalo.cse.jive.model.IExecutionModel;

class JiveThread extends JDIThreadAdapter implements IJavaThread, IJiveStep
{
  private JiveStepHandler stepHandler;

  JiveThread(final JDIDebugTarget target, final ThreadReference thread)
      throws ObjectCollectedException
  {
    super(target, thread);
  }

  @Override
  public boolean canResume()
  {
    /**
     * If Jive is active, can only step if no slice is active and the the temporal state is the last
     * event in the trace.
     */
    final IExecutionModel model = getJiveDebugTarget().model();
    return super.canResume()
        && (!isActive() || model == null || (model.sliceView().activeSlice() == null && !model
            .temporalState().canReplayCommit()));
  }

  @Override
  public IJavaDebugTarget getDebugTarget()
  {
    return (IJavaDebugTarget) super.getDebugTarget();
  }

  @Override
  public String getModelIdentifier()
  {
    return getDebugTarget().getModelIdentifier();
  }

  @Override
  public void initialize()
  {
    if (!getJiveDebugTarget().isManualStart() || getJiveDebugTarget().isActive())
    {
      createRequests();
    }
    super.initialize();
  }

  @Override
  public void stepInto() throws DebugException
  {
    synchronized (this)
    {
      if (!canStepInto())
      {
        return;
      }
    }
    final JDTStepHandler handler = new StepIntoHandler();
    handler.step();
  }

  @Override
  public void stepOver() throws DebugException
  {
    synchronized (this)
    {
      if (!canStepOver())
      {
        return;
      }
    }
    final JDTStepHandler handler = new StepOverHandler();
    handler.step();
  }

  @Override
  public void stepReturn() throws DebugException
  {
    synchronized (this)
    {
      if (!canStepReturn())
      {
        return;
      }
    }
    final JDTStepHandler handler = new StepReturnHandler();
    handler.step();
  }

  private IJiveDebugTarget getJiveDebugTarget()
  {
    return (IJiveDebugTarget) super.getDebugTarget();
  }

  private boolean isActive()
  {
    return getJiveDebugTarget().isActive();
  }

  @Override
  protected boolean canStep()
  {
    /**
     * If Jive is active, can only step if no slice is active and the the temporal state is the last
     * event in the trace.
     */
    final IExecutionModel model = getJiveDebugTarget().model();
    return super.canStep()
        && (model == null || (model.sliceView().activeSlice() == null && !model.temporalState()
            .canReplayCommit()));
  }

  @Override
  protected void dropToFrame(final IStackFrame frame) throws DebugException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the step handler currently handling a step request, or <code>null</code> if none.
   * 
   * @return step handler, or <code>null</code> if none
   */
  @Override
  protected JDTStepHandler getPendingStepHandler()
  {
    return (JDTStepHandler) (super.getPendingStepHandler());
  }

  /**
   * Sets the step handler currently handling a step request.
   * 
   * @param handler
   *          the current step handler, or <code>null</code> if none
   */
  protected void setPendingStepHandler(final JDTStepHandler handler)
  {
    super.setPendingStepHandler(handler);
  }

  @Override
  protected void stepToFrame(final IStackFrame frame) throws DebugException
  {
    synchronized (this)
    {
      if (!canStepReturn())
      {
        return;
      }
    }
    final JDTStepHandler handler = new StepToFrameHandler(frame);
    handler.step();
  }

  void createRequests()
  {
    if (stepHandler == null)
    {
      this.stepHandler = new JiveStepHandler();
    }
    stepHandler.createRequest();
    // signal change
    fireChangeEvent(DebugEvent.STATE);
  }

  void removeRequests()
  {
    stepHandler.removeRequest();
    // signal change
    fireChangeEvent(DebugEvent.STATE);
  }

  /**
   * Helper class to perform stepping on a thread.
   */
  protected abstract class JDTStepHandler extends JDIThreadAdapter.StepHandlerAdapter
  {
    @Override
    protected void abort()
    {
      setPendingStepHandler(null);
    }

    @Override
    protected void attachFiltersToStepRequest(final StepRequest request)
    {
      // TODO Filters are applied in the JiveStepHandler
    }

    @Override
    protected void createSecondaryStepRequest()
    {
      // Do nothing
    }

    @Override
    protected StepRequest createStepRequest()
    {
      // TODO Add message to exception and log error
      throw new UnsupportedOperationException();
    }

    @Override
    protected void deleteStepRequest()
    {
      // Do nothing
    }

    @Override
    protected abstract int getStepDetail();

    @Override
    protected abstract int getStepKind();

    @Override
    protected StepRequest getStepRequest()
    {
      throw new IllegalAccessError(
          "JDTStepHandler.getStepRequest() should never be called from Jive.");
    }

    @Override
    protected void setStepRequest(final StepRequest request)
    {
      throw new IllegalAccessError(
          "JDTStepHandler.setStepRequest(StepRequest) should never be called from Jive.");
    }

    @Override
    protected void step() throws DebugException
    {
      final ISchedulingRule rule = getThreadRule();
      try
      {
        Job.getJobManager().beginRule(rule, null);
        final ThreadReference thread = getUnderlyingThread();
        final int stackDepth = thread.frameCount();
        if (stackDepth == 0)
        {
          System.err.println("no stacks to step");
          return;
        }
        else
        {
          setOriginalStepKind(getStepKind());
          final Location location = thread.frame(0).location();
          setOriginalStepLocation(location);
          setOriginalStepStackDepth(stackDepth);
          setPendingStepHandler(this);
          setRunning(true);
          preserveStackFrames();
          fireResumeEvent(getStepDetail());
          invokeThread();
        }
      }
      catch (final IncompatibleThreadStateException e)
      {
        setPendingStepHandler(null);
        return;
      }
      finally
      {
        Job.getJobManager().endRule(rule);
      }
    }
  }

  /**
   * Helper class to perform stepping on a thread.
   */
  protected class JiveStepHandler implements IJDIEventListener
  {
    private final IJDIEventHandler jdiHandler;
    private StepRequest stepRequest;

    protected JiveStepHandler()
    {
      jdiHandler = ((JiveDebugTarget) getDebugTarget()).jdiHandler();
    }

    @Override
    public void eventSetComplete(final Event event, final JDIDebugTarget target,
        final boolean suspend, final EventSet eventSet)
    {
      // TODO: add support for Eclipse 3.5 event handling
    }

    @Override
    public boolean handleEvent(final Event event, final JDIDebugTarget target,
        final boolean suspendVote, final EventSet eventSet)
    {
      final StepEvent stepEvent = (StepEvent) event;
      if (isActive())
      {
        jdiHandler.jdiStep(stepEvent);
      }
      boolean result = true;
      final JDTStepHandler stepHandler = getPendingStepHandler();
      if (stepHandler != null)
      {
        switch (stepHandler.getStepKind())
        {
          case StepRequest.STEP_INTO:
            if (shouldHandleStepInto(stepEvent.location()))
            {
              result = stepHandler.handleEvent(event, target, suspendVote, eventSet);
            }
            break;
          case StepRequest.STEP_OVER:
            if (shouldHandleStepOver(stepEvent.location()))
            {
              result = stepHandler.handleEvent(event, target, suspendVote, eventSet);
            }
            break;
          case StepRequest.STEP_OUT:
            if (shouldHandleStepReturn(stepEvent.location()))
            {
              result = stepHandler.handleEvent(event, target, suspendVote, eventSet);
            }
            break;
          default:
            // TODO log error
            break;
        }
      }
      return result;
    }

    protected void createRequest()
    {
      final EventRequestManager manager = getEventRequestManager();
      if (manager != null)
      {
        try
        {
          if (stepRequest != null)
          {
            removeRequest();
          }
          stepRequest = manager.createStepRequest(getUnderlyingThread(), StepRequest.STEP_LINE,
              StepRequest.STEP_INTO);
          final JiveDebugTarget target = (JiveDebugTarget) getDebugTarget();
          final IModelFilter requestFilter = target.jdiManager().modelFilter();
          requestFilter.filter(stepRequest);
          stepRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
          stepRequest.enable();
          addJDIEventListener(this, stepRequest);
        }
        catch (final RuntimeException e)
        {
          logError(e);
        }
      }
    }

    protected void removeRequest()
    {
      final EventRequestManager manager = getEventRequestManager();
      if (manager != null && stepRequest != null)
      {
        try
        {
          removeJDIEventListener(this, stepRequest);
          manager.deleteEventRequest(stepRequest);
          stepRequest = null;
        }
        catch (final RuntimeException e)
        {
          logError(e);
        }
      }
    }

    protected boolean shouldHandleStepInto(final Location location)
    {
      try
      {
        if (getOriginalStepStackDepth() != getUnderlyingFrameCount())
        {
          return true;
        }
        if (getOriginalStepLocation().lineNumber() != location.lineNumber())
        {
          return true;
        }
        return false;
      }
      catch (final DebugException e)
      {
        // TODO log error
        return false;
      }
    }

    protected boolean shouldHandleStepOver(final Location location)
    {
      try
      {
        if (getOriginalStepStackDepth() == getUnderlyingFrameCount()
            && getOriginalStepLocation().lineNumber() != location.lineNumber())
        {
          return true;
        }
        if (getOriginalStepStackDepth() > getUnderlyingFrameCount())
        {
          return true;
        }
        return false;
      }
      catch (final DebugException e)
      {
        // TODO log error
        return false;
      }
    }

    protected boolean shouldHandleStepReturn(final Location location)
    {
      try
      {
        if (getOriginalStepStackDepth() > getUnderlyingFrameCount())
        {
          return true;
        }
        return false;
      }
      catch (final DebugException e)
      {
        // TODO log error
        return false;
      }
    }
  }

  /**
   * Handler for step into requests.
   */
  class StepIntoHandler extends JDTStepHandler
  {
    @Override
    protected int getStepDetail()
    {
      return DebugEvent.STEP_INTO;
    }

    @Override
    protected int getStepKind()
    {
      return StepRequest.STEP_INTO;
    }
  }

  /**
   * Handler for step over requests.
   */
  class StepOverHandler extends JDTStepHandler
  {
    @Override
    protected int getStepDetail()
    {
      return DebugEvent.STEP_OVER;
    }

    @Override
    protected int getStepKind()
    {
      return StepRequest.STEP_OVER;
    }
  }

  /**
   * Handler for step return requests.
   */
  class StepReturnHandler extends JDTStepHandler
  {
    @Override
    protected int getStepDetail()
    {
      return DebugEvent.STEP_RETURN;
    }

    @Override
    protected int getStepKind()
    {
      return StepRequest.STEP_OUT;
    }

    @Override
    protected boolean locationShouldBeFiltered(final Location location) throws DebugException
    {
      // if still at the same depth, do another step return (see bug 38744)
      if (getOriginalStepStackDepth() == getUnderlyingFrameCount())
      {
        return true;
      }
      return super.locationShouldBeFiltered(location);
    }
  }

  /**
   * Handler for step to frame requests.
   */
  class StepToFrameHandler extends StepReturnHandler
  {
    private int fRemainingFrames;

    protected StepToFrameHandler(final IStackFrame frame) throws DebugException
    {
      final List<?> frames = computeStackFrames();
      setRemainingFrames(frames.size() - frames.indexOf(frame));
    }

    @Override
    public boolean handleEvent(final Event event, final JDIDebugTarget target,
        final boolean suspendVote, final EventSet eventSet)
    {
      try
      {
        final int numFrames = getUnderlyingFrameCount();
        if (numFrames <= getRemainingFrames())
        {
          stepEnd(eventSet);
          return false;
        }
        // reset running state and keep going
        setRunning(true);
        deleteStepRequest();
        createSecondaryStepRequest();
        return true;
      }
      catch (final DebugException e)
      {
        logError(e);
        stepEnd(eventSet);
        return false;
      }
    }

    protected int getRemainingFrames()
    {
      return fRemainingFrames;
    }

    protected void setRemainingFrames(final int num)
    {
      fRemainingFrames = num;
    }
  }
}