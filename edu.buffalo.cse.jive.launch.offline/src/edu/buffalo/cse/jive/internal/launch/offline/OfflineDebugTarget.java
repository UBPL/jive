package edu.buffalo.cse.jive.internal.launch.offline;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaHotCodeReplaceListener;
import org.eclipse.jdt.debug.core.IJavaThreadGroup;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;

import edu.buffalo.cse.jive.core.ast.ASTFactory;
import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.launch.offline.IOfflineImporter;
import edu.buffalo.cse.jive.launch.offline.OfflineImporterException;
import edu.buffalo.cse.jive.model.IEventModel.IEventListener;
import edu.buffalo.cse.jive.model.IEventModel.IEventProducer;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IJiveProject;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNodeRef;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.model.IStaticModel.NodeOrigin;
import edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory.IStaticModelDelegate;
import edu.buffalo.cse.jive.model.store.Factory;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;

class OfflineDebugTarget implements IJavaDebugTarget, IJiveDebugTarget
{
  private final AtomicInteger viewsEnabled;
  private final ILaunch launch;
  private final IJiveProject project;
  private final IExecutionModel model;
  private final String url;
  private boolean isImported;

  OfflineDebugTarget(final ILaunch launch, final IJiveProject project)
  {
    this.isImported = false;
    this.launch = launch;
    this.project = project;
    this.viewsEnabled = new AtomicInteger(0);
    this.model = Factory.memoryExecutionModel(new IEventProducer()
      {
        @Override
        public void subscribe(final IEventListener listener)
        {
        }

        @Override
        public void unsubscribe(final IEventListener listener)
        {
        }
      });
    this.model.traceView().register(this);
    String url;
    try
    {
      url = launch.getLaunchConfiguration().getAttribute(
          PreferencesPlugin.getDefault().getOfflineURLKey(), "");
    }
    catch (final CoreException e)
    {
      url = "";
      // cowardly ignore for now
    }
    this.url = url;
    DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
  }

  @Override
  public void addHotCodeReplaceListener(final IJavaHotCodeReplaceListener arg0)
  {
    // synthetic targets do not support hot code events
  }

  @Override
  public void breakpointAdded(final IBreakpoint breakpoint)
  {
    // synthetic targets do not support breakpoint events
  }

  @Override
  public void breakpointChanged(final IBreakpoint breakpoint, final IMarkerDelta delta)
  {
    // synthetic targets do not support breakpoint events
  }

  @Override
  public void breakpointRemoved(final IBreakpoint breakpoint, final IMarkerDelta delta)
  {
    // synthetic targets do not support breakpoint events
  }

  @Override
  public boolean canDisconnect()
  {
    // synthetic targets cannot disconnect
    return false;
  }

  @Override
  public boolean canReplay()
  {
    // synthetic targets can always replay
    return true;
  }

  @Override
  public boolean canResume()
  {
    // synthetic targets cannot resume
    return false;
  }

  @Override
  public boolean canSuspend()
  {
    // synthetic targets cannot suspend
    return false;
  }

  @Override
  public boolean canTerminate()
  {
    // synthetic targets cannot terminate
    return false;
  }

  @Override
  public int disableViews()
  {
    return viewsEnabled.decrementAndGet();
  }

  @Override
  public void disconnect() throws DebugException
  {
    // synthetic targets do not disconnect
  }

  @Override
  public int enableViews()
  {
    return viewsEnabled.incrementAndGet();
  }

  @Override
  public void eventsInserted(final List<IJiveEvent> events)
  {
    // synthetic targets do not process event inserted events
  }

  @Override
  public IJavaVariable findVariable(final String arg0) throws DebugException
  {
    // synthetic targets cannot find variables
    return null;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public Object getAdapter(final Class adapter)
  {
    // synthetic targets do not have adapters
    return null;
  }

  @Override
  public IJavaThreadGroup[] getAllThreadGroups() throws DebugException
  {
    // synthetic targets do not support thread groups
    return new IJavaThreadGroup[] {};
  }

  @Override
  public IDebugTarget getDebugTarget()
  {
    // this debug target (do not return null)
    return this;
  }

  @Override
  public String getDefaultStratum()
  {
    // synthetic targets have no stratum
    return null;
  }

  @Override
  public IJavaType[] getJavaTypes(final String arg0) throws DebugException
  {
    // synthetic targets do not support Java types
    return new IJavaType[] {};
  }

  @Override
  public Object getJVM()
  {
    return null;
  }

  @Override
  public ILaunch getLaunch()
  {
    return launch;
  }

  @Override
  public IMemoryBlock getMemoryBlock(final long startAddress, final long length)
      throws DebugException
  {
    // synthetic targets do not support memory block retrieval
    return null;
  }

  @Override
  public String getModelIdentifier()
  {
    return "edu.buffalo.cse.jive.launch.offline";
  }

  @Override
  public String getName()
  {
    return launch.toString();
  }

  @Override
  public IProcess getProcess()
  {
    return null;
  }

  @Override
  public IJiveProject getProject()
  {
    return this.project;
  }

  @Override
  public int getRequestTimeout()
  {
    return 0;
  }

  @Override
  public IJavaThreadGroup[] getRootThreadGroups() throws DebugException
  {
    return new IJavaThreadGroup[] {};
  }

  @Override
  public String[] getStepFilters()
  {
    return new String[] {};
  }

  @Override
  public IThread[] getThreads() throws DebugException
  {
    return new IThread[] {};
  }

  @Override
  public String getVersion() throws DebugException
  {
    return "1.0";
  }

  @Override
  public String getVMName() throws DebugException
  {
    return "Jive Offline Target";
  }

  @Override
  public boolean hasThreads() throws DebugException
  {
    return false;
  }

  @Override
  public boolean isActive()
  {
    return true;
  }

  @Override
  public boolean isDisconnected()
  {
    return true;
  }

  @Override
  public boolean isFilterConstructors()
  {
    return false;
  }

  @Override
  public boolean isFilterGetters()
  {
    return false;
  }

  @Override
  public boolean isFilterSetters()
  {
    return false;
  }

  @Override
  public boolean isFilterStaticInitializers()
  {
    return false;
  }

  @Override
  public boolean isFilterSynthetics()
  {
    return false;
  }

  @Override
  public boolean isManualStart()
  {
    return false;
  }

  @Override
  public boolean isOffline()
  {
    return true;
  }

  @Override
  public boolean isOutOfSynch() throws DebugException
  {
    return false;
  }

  @Override
  public boolean isPerformingHotCodeReplace()
  {
    return false;
  }

  @Override
  public boolean isStarted()
  {
    // synthetic targets are always started
    return true;
  }

  @Override
  public boolean isStepFiltersEnabled()
  {
    // synthetic targets cannot step through filters
    return false;
  }

  @Override
  public boolean isStepThruFilters()
  {
    // synthetic targets cannot step through filters
    return false;
  }

  @Override
  public boolean isStopped()
  {
    // synthetic targets are never stopped
    return false;
  }

  @Override
  public boolean isSuspended()
  {
    // synthetic targets cannot are never suspended
    return false;
  }

  @Override
  public boolean isTerminated()
  {
    // synthetic targets are always terminated
    return true;
  }

  @Override
  public void launchAdded(final ILaunch launch)
  {
    // synthetic targets do not process launch notifications
    fireEvent(DebugEvent.CHANGE);
  }

  @Override
  public void launchChanged(final ILaunch launch)
  {
    // synthetic targets do not process launch notifications
    fireEvent(DebugEvent.CHANGE);
  }

  @Override
  public void launchRemoved(final ILaunch launch)
  {
    // synthetic targets do not process launch notifications
    if (this.launch == launch)
    {
      DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
      fireEvent(DebugEvent.CHANGE);
    }
  }

  @Override
  public boolean mayBeOutOfSynch() throws DebugException
  {
    // synthetic targets are never out of synch
    return false;
  }

  @Override
  public IExecutionModel model()
  {
    return this.model;
  }

  @Override
  public IJavaValue newValue(final boolean arg0)
  {
    // synthetic targets cannot create/retrieve Java values
    return null;
  }

  @Override
  public IJavaValue newValue(final byte arg0)
  {
    // synthetic targets cannot create/retrieve Java values
    return null;
  }

  @Override
  public IJavaValue newValue(final char arg0)
  {
    // synthetic targets cannot create/retrieve Java values
    return null;
  }

  @Override
  public IJavaValue newValue(final double arg0)
  {
    // synthetic targets cannot create/retrieve Java values
    return null;
  }

  @Override
  public IJavaValue newValue(final float arg0)
  {
    // synthetic targets cannot create/retrieve Java values
    return null;
  }

  @Override
  public IJavaValue newValue(final int arg0)
  {
    // synthetic targets cannot create/retrieve Java values
    return null;
  }

  @Override
  public IJavaValue newValue(final long arg0)
  {
    // synthetic targets cannot create/retrieve Java values
    return null;
  }

  @Override
  public IJavaValue newValue(final short arg0)
  {
    // synthetic targets cannot create/retrieve Java values
    return null;
  }

  @Override
  public IJavaValue newValue(final String arg0)
  {
    // synthetic targets cannot create/retrieve Java values
    return null;
  }

  @Override
  public IJavaValue nullValue()
  {
    // synthetic targets cannot create/retrieve Java values
    return null;
  }

  @Override
  public void refreshState() throws DebugException
  {
    // synthetic targets do not support state refreshing
  }

  @Override
  public void removeHotCodeReplaceListener(final IJavaHotCodeReplaceListener arg0)
  {
    // synthetic targets do not support hot code events
  }

  @Override
  public void resume() throws DebugException
  {
    // synthetic targets cannot resume
  }

  @Override
  public byte[] sendCommand(final byte arg0, final byte arg1, final byte[] arg2)
      throws DebugException
  {
    // synthetic targets cannot send commands
    return null;
  }

  @Override
  public void setDefaultStratum(final String arg0)
  {
    // synthetic targets have no stratum-- ignore
  }

  @Override
  public void setFilterConstructors(final boolean arg0)
  {
    // synthetic targets cannot be filtered-- ignore
  }

  @Override
  public void setFilterGetters(final boolean arg0)
  {
    // synthetic targets cannot be filtered-- ignore
  }

  @Override
  public void setFilterSetters(final boolean arg0)
  {
    // synthetic targets cannot be filtered-- ignore
  }

  @Override
  public void setFilterStaticInitializers(final boolean arg0)
  {
    // synthetic targets cannot be filtered-- ignore
  }

  @Override
  public void setFilterSynthetics(final boolean arg0)
  {
    // synthetic targets cannot be filtered-- ignore
  }

  @Override
  public void setRequestTimeout(final int arg0)
  {
    // synthetic targets cannot be timed out-- ignore
  }

  @Override
  public void setStepFilters(final String[] arg0)
  {
    // synthetic targets cannot be stepped-- ignore
  }

  @Override
  public void setStepFiltersEnabled(final boolean enabled)
  {
    // synthetic targets cannot be stepped-- ignore
  }

  @Override
  public void setStepThruFilters(final boolean arg0)
  {
    // synthetic targets cannot be stepped-- ignore
  }

  // TODO: own job/thread?
  @Override
  public synchronized void start()
  {
    // synthetic targets cannot be started
    if (!isImported)
    {
      fireEvent(DebugEvent.CREATE);
      importFrom(url);
      this.isImported = true;
      fireEvent(DebugEvent.CHANGE);
      fireEvent(DebugEvent.TERMINATE);
    }
  }

  @Override
  public void stop()
  {
    // synthetic targets cannot be stopped
  }

  @Override
  public boolean supportsAccessWatchpoints()
  {
    // synthetic targets do not support traditional debug functions
    return false;
  }

  @Override
  public boolean supportsBreakpoint(final IBreakpoint breakpoint)
  {
    // synthetic targets do not support traditional debug functions
    return false;
  }

  @Override
  public boolean supportsForceReturn()
  {
    // synthetic targets do not support traditional debug functions
    return false;
  }

  @Override
  public boolean supportsHotCodeReplace()
  {
    // synthetic targets do not support traditional debug functions
    return false;
  }

  @Override
  public boolean supportsInstanceBreakpoints()
  {
    // synthetic targets do not support traditional debug functions
    return false;
  }

  @Override
  public boolean supportsInstanceRetrieval()
  {
    // synthetic targets do not support traditional debug functions
    return false;
  }

  @Override
  public boolean supportsModificationWatchpoints()
  {
    // synthetic targets do not support traditional debug functions
    return false;
  }

  @Override
  public boolean supportsMonitorInformation()
  {
    // synthetic targets do not support traditional debug functions
    return false;
  }

  @Override
  public boolean supportsRequestTimeout()
  {
    // synthetic targets do not support traditional debug functions
    return false;
  }

  @Override
  public boolean supportsSelectiveGarbageCollection()
  {
    // synthetic targets do not support traditional debug functions
    return false;
  }

  @Override
  public boolean supportsStepFilters()
  {
    // synthetic targets do not support traditional debug functions
    return false;
  }

  @Override
  public boolean supportsStorageRetrieval()
  {
    // synthetic targets do not support traditional debug functions
    return false;
  }

  @Override
  public void suspend()
  {
    // synthetic targets cannot be suspended
  }

  @Override
  public synchronized int targetId()
  {
    return project.targetId();
  }

  @Override
  public void terminate() throws DebugException
  {
    // synthetic targets cannot be terminated
  }

  @Override
  public String toString()
  {
    return launch.toString() + " " + url;
  }

  @Override
  public void traceVirtualized(final boolean isVirtual)
  {
    // synthetic targets do not handle trace virtualization
  }

  @Override
  public boolean viewsEnabled()
  {
    return viewsEnabled == null || viewsEnabled.get() == 0;
  }

  @Override
  public IJavaValue voidValue()
  {
    // synthetic targets cannot create/retrieve Java values
    return null;
  }

  private IOfflineImporter createImporter(final String url2)
  {
    final IExtensionRegistry registry = RegistryFactory.getRegistry();
    final IExtensionPoint[] extpts = registry
        .getExtensionPoints("edu.buffalo.cse.jive.launch.offline");
    for (final IExtensionPoint ep : extpts)
    {
      if (ep.getSimpleIdentifier().equals("offlineImporters"))
      {
        for (final IExtension e : ep.getExtensions())
        {
          for (final IConfigurationElement ce : e.getConfigurationElements())
          {
            final String protocol = ce.getAttribute("protocol");
            if (protocol != null && url.startsWith(protocol))
            {
              try
              {
                return (IOfflineImporter) Class.forName(ce.getAttribute("class")).newInstance();
              }
              catch (final InvalidRegistryObjectException ex)
              {
                throw new RuntimeException(new OfflineImporterException(ex));
              }
              catch (final InstantiationException ex)
              {
                throw new RuntimeException(new OfflineImporterException(ex));
              }
              catch (final IllegalAccessException ex)
              {
                throw new RuntimeException(new OfflineImporterException(ex));
              }
              catch (final ClassNotFoundException ex)
              {
                throw new RuntimeException(new OfflineImporterException(ex));
              }
            }
          }
        }
      }
    }
    return null;
  }

  private void fireEvent(final int event)
  {
    DebugPlugin.getDefault().fireDebugEventSet(
        new DebugEvent[]
        { new DebugEvent(this, event, event == DebugEvent.CHANGE ? DebugEvent.STATE
            : DebugEvent.UNSPECIFIED) });
  }

  private boolean importFrom(final String url)
  {
    final IOfflineImporter importer = createImporter(url);
    if (importer == null)
    {
      return false;
    }
    try
    {
      final IStaticModelDelegate downstream = new IStaticModelDelegate()
        {
          @Override
          public IMethodNode resolveMethod(final ITypeNode type, final Object methodObject,
              final String methodKey)
          {
            // no need for method resolution
            return null;
          }

          @Override
          public ITypeNodeRef resolveType(final String typeKey, final String typeName)
          {
            final IStaticModelFactory factory = model.staticModelFactory();
            return factory.createTypeNode(typeKey, typeName, factory.lookupRoot(), -1, -1,
                typeKey.startsWith("[") ? NodeKind.NK_ARRAY : NodeKind.NK_CLASS,
                NodeOrigin.NO_JIVE, Collections.<NodeModifier> emptySet(),
                NodeVisibility.NV_PUBLIC, factory.lookupObjectType(),
                Collections.<ITypeNodeRef> emptySet(), model.valueFactory().createNullValue());
          }

          @Override
          public void setUpstream(final IStaticModelDelegate upstream)
          {
            // no need for upstream
          }
        };
      final IStaticModelDelegate upstream = ASTFactory.createStaticModelDelegate(model, project,
          null, downstream);
      importer.process(url, model, upstream);
      return true;
    }
    catch (final Exception e)
    {
      e.printStackTrace();
    }
    return false;
  }
}
