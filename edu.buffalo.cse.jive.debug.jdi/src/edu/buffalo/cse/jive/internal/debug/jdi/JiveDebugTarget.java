package edu.buffalo.cse.jive.internal.debug.jdi;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.internal.debug.core.model.JDIDebugTarget;
import org.eclipse.jdt.internal.debug.core.model.JDIDebugTargetAdapter;
import org.eclipse.jdt.internal.debug.core.model.JDIThread;

import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

import edu.buffalo.cse.jive.debug.JiveDebugPlugin;
import edu.buffalo.cse.jive.debug.jdi.model.IEventHandlerFactory;
import edu.buffalo.cse.jive.debug.jdi.model.IJDIEventHandler;
import edu.buffalo.cse.jive.debug.jdi.model.IJDIManager;
import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IJiveProject;

class JiveDebugTarget extends JDIDebugTargetAdapter implements IJavaDebugTarget, IJiveDebugTarget
{
  private final IJiveProject project;
  private final AtomicInteger viewsEnabled;
  private boolean isStarted;
  private boolean isStopped;
  private final IEventHandlerFactory eventHandlerFactory;

  JiveDebugTarget(final ILaunch launch, final VirtualMachine jvm, final String name,
      final boolean supportTerminate, final boolean supportDisconnect, final IProcess process,
      final boolean resume, final IJiveProject project)
  {
    super(launch, jvm, name, supportTerminate, supportDisconnect, process, resume);
    this.project = project;
    this.isStarted = !isManualStart();
    this.isStopped = false;
    new ThreadDeathHandler();
    this.eventHandlerFactory = new EventHandlerFactory(this);
    this.viewsEnabled = new AtomicInteger(0);
  }

  @Override
  public boolean canPopFrames()
  {
    return false;
  }

  @Override
  public boolean canReplay()
  {
    return isStarted && (!isAvailable() || isSuspended());
  }

  @Override
  public boolean canResume()
  {
    /**
     * If Jive is active, can only resume if no slice is active and the the temporal state is the
     * last event in the trace.
     */
    return super.canResume()
        && (!isStarted || (model() != null && model().sliceView().activeSlice() == null && !model()
            .temporalState().canReplayCommit()));
  }

  @Override
  public boolean canSuspend()
  {
    return super.canSuspend();
    /**
     * Allow suspending if any thread is running. Note that this is different from the default
     * behavior of Java Debug Targets, which allow suspending only if no threads are suspended.
     */
    // if (!isStarted) {
    // return false;
    // }
    // if (!isSuspended() && isAvailable()) {
    // for (final IThread thread : getThreads()) {
    // if (!thread.isSuspended()) {
    // return true;
    // }
    // }
    // }
    // return false;
  }

  @Override
  public int disableViews()
  {
    return viewsEnabled.decrementAndGet();
  }

  @Override
  public int enableViews()
  {
    return viewsEnabled.incrementAndGet();
  }

  @Override
  public void eventsInserted(final List<IJiveEvent> events)
  {
    // TODO Auto-generated method stub
  }

  @Override
  public Object getJVM()
  {
    return super.getVM();
  }

  @Override
  public String getModelIdentifier()
  {
    return super.getModelIdentifier();
  }

  @Override
  public String getName()
  {
    String superName = null;
    try
    {
      superName = super.getName();
    }
    catch (final DebugException e)
    {
    }
    return superName == null || superName.length() == 0 ? project.name() : superName;
  }

  @Override
  public synchronized IJiveProject getProject()
  {
    return this.project;
  }

  @Override
  public void handleVMDeath(final VMDeathEvent event)
  {
    if (isActive())
    {
      // this waits for all pending thread deaths to be processed
      jdiHandler().jdiVMDeath(event);
    }
    super.handleVMDeath(event);
  }

  @Override
  public void handleVMDisconnect(final VMDisconnectEvent event)
  {
    if (isActive())
    {
      // this waits for all pending thread deaths to be processed
      jdiHandler().jdiVMDisconnect(event);
    }
    super.handleVMDisconnect(event);
  }

  @Override
  public void handleVMStart(final VMStartEvent event)
  {
    if (isActive())
    {
      jdiHandler().jdiVMStart(event);
    }
    super.handleVMStart(event);
  }

  @Override
  public boolean isActive()
  {
    return isStarted && !isStopped;
  }

  @Override
  public boolean isManualStart()
  {
    return jdiManager().isManualStart();
  }

  @Override
  public boolean isOffline()
  {
    return false;
  }

  @Override
  public boolean isStarted()
  {
    return isStarted;
  }

  @Override
  public boolean isStopped()
  {
    return isStopped;
  }

  @Override
  public void launchRemoved(final ILaunch launch)
  {
    if (jdiManager() != null)
    {
      jdiManager().executionModel().done();
      jdiManager().done();
    }
    super.launchRemoved(launch);
  }

  @Override
  public IExecutionModel model()
  {
    return jdiManager() != null ? jdiManager().executionModel() : null;
  }

  @Override
  public void resume() throws DebugException
  {
    // Run to the end of the recording before resuming
    // NOTE: the target's resume button is not yet disabled when in a past state (unless the target
    // is unselected then reselected)
    final IExecutionModel model = model();
    if (isActive() && model != null)
    {
      model.temporalState().setFinalState();
    }
    super.resume();
  }

  @Override
  public void start()
  {
    // disable view updates
    disableViews();
    try
    {
      // this target has been manually stopped so we must reset all relevant data structures
      if (isStopped)
      {
        jdiManager().reset();
        isStarted = false;
      }
      if (!isStarted && (isStopped || isManualStart()))
      {
        try
        {
          suspend();
        }
        catch (final Exception de)
        {
          de.printStackTrace();
        }
        // create a snapshot from the virtual machine
        jdiManager().jdiHandler().createSnapshot(getVM());
        // mark started
        isStarted = true;
        // mark not stopped
        isStopped = false;
        // create requests
        createRequests();
        // signal change
        fireChangeEvent(DebugEvent.STATE);
      }
    }
    finally
    {
      // re-enable view updates
      enableViews();
    }
  }

  @Override
  public void stop()
  {
    if (!isStopped)
    {
      // remove requests
      removeRequests();
      // mark stopped
      isStopped = true;
      // signal change
      fireChangeEvent(DebugEvent.STATE);
    }
  }

  @Override
  public synchronized int targetId()
  {
    return project.targetId();
  }

  @Override
  public void terminate()
  {
    try
    {
      super.terminate();
      // terminate the trace as consistently as possible
      if (!isTerminated() && model() != null)
      {
        model().terminate();
      }
    }
    catch (final Exception de)
    {
      de.printStackTrace();
    }
  }

  @Override
  public void traceVirtualized(final boolean isVirtual)
  {
    // signal change
    fireChangeEvent(DebugEvent.STATE);
  }

  @Override
  public boolean viewsEnabled()
  {
    return viewsEnabled == null || viewsEnabled.get() == 0;
  }

  protected void createRequests()
  {
    for (final IThread thread : this.getThreads())
    {
      if (thread instanceof JiveThread)
      {
        ((JiveThread) thread).createRequests();
      }
    }
    eventHandlerFactory.createRequests();
  }

  @Override
  protected void initializeRequests()
  {
    setThreadStartHandler(new ThreadStartHandler());
  }

  protected IJDIEventHandler jdiHandler()
  {
    return jdiManager().jdiHandler();
  }

  @Override
  protected JDIThread newThread(final ThreadReference reference)
  {
    try
    {
      // no need to track system threads
      if (!jdiManager().modelFilter().acceptsThread(reference))
      {
        return new JDIThread(this, reference);
      }
      return new JiveThread(this, reference);
    }
    catch (final ObjectCollectedException exception)
    {
      // ObjectCollectionException can be thrown if the thread has already
      // completed (exited) in the VM.
    }
    return null;
  }

  protected void removeRequests()
  {
    for (final IThread thread : this.getThreads())
    {
      if (thread instanceof JiveThread)
      {
        ((JiveThread) thread).removeRequests();
      }
    }
    eventHandlerFactory.removeRequests();
  }

  boolean generateArrayEvents()
  {
    return jdiManager().generateArrayEvents();
  }

  IJDIManager jdiManager()
  {
    return JiveDebugPlugin.getDefault() != null ? JiveDebugPlugin.getDefault().jdiManager(this)
        : null;
  }

  private final class ThreadDeathHandler extends JDIDebugTargetAdapter.ThreadDeathHandlerAdapter
  {
    protected ThreadDeathHandler()
    {
      super();
    }

    @Override
    public boolean handleEvent(final Event event, final JDIDebugTarget target,
        final boolean suspendVote, final EventSet eventSet)
    {
      if (isActive()
          && jdiManager().modelFilter().acceptsThread(((ThreadDeathEvent) event).thread()))
      {
        jdiHandler().jdiThreadDeath((ThreadDeathEvent) event);
      }
      return super.handleEvent(event, target, suspendVote, eventSet);
    }

    @Override
    protected void createRequest()
    {
      final EventRequestManager manager = getEventRequestManager();
      if (manager != null)
      {
        try
        {
          final EventRequest request = manager.createThreadDeathRequest();
          request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
          request.enable();
          addJDIEventListener(this, request);
        }
        catch (final RuntimeException e)
        {
          logError(e);
        }
      }
    }
  }

  private final class ThreadStartHandler extends JDIDebugTargetAdapter.ThreadStartHandlerAdapter
  {
    protected ThreadStartHandler()
    {
      super();
    }

    @Override
    public boolean handleEvent(final Event event, final JDIDebugTarget target,
        final boolean suspendVote, final EventSet eventSet)
    {
      if (isActive()
          && jdiManager().modelFilter().acceptsThread(((ThreadStartEvent) event).thread()))
      {
        jdiHandler().jdiThreadStart((ThreadStartEvent) event);
      }
      return super.handleEvent(event, target, suspendVote, eventSet);
    }
  }
}