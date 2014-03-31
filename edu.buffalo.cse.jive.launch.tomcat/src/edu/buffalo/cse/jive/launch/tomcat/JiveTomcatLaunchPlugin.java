package edu.buffalo.cse.jive.launch.tomcat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IPublishListener;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerLifecycleListener;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerEvent;
import org.osgi.framework.BundleContext;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.debug.model.IJiveLaunchManager;
import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;

/**
 * Activator that controls the plug-in life cycle and provides utility methods.
 */
public class JiveTomcatLaunchPlugin extends Plugin implements IPublishListener, IServerListener
{
  /**
   * The unique identifier of the plug-in.
   */
  public static final String PLUGIN_ID = "edu.buffalo.cse.jive.launch.tomcat"; //$NON-NLS-1$
  public static String VERSION_WARNING = "It is strongly recommended that you use Jive with a Java Virtual Machine version 6 or newer. You are currently running with version %s. Although we try our best to keep the system running smoothly with previous JVM versions, please note that some features may not work properly or may be missing altogether.\n";
  /**
   * 
   * The shared instance of the plug-in.
   */
  private static JiveTomcatLaunchPlugin plugin;

  /**
   * Returns the shared instance of the JIVE launching plug-in.
   * 
   * @return the shared instance
   */
  public static JiveTomcatLaunchPlugin getDefault()
  {
    return JiveTomcatLaunchPlugin.plugin;
  }

  /**
   * Logs a status object to the Eclipse error log.
   * 
   * @param status
   *          the status object to record
   */
  public static void log(final IStatus status)
  {
    JiveTomcatLaunchPlugin.getDefault().getLog().log(status);
  }

  /**
   * Logs a string to the Eclipse error log as an <code>IStatus.ERROR</code> object.
   * 
   * @param message
   *          the message to be recorded
   */
  public static void log(final String message)
  {
    JiveTomcatLaunchPlugin.log(new Status(IStatus.ERROR, JiveTomcatLaunchPlugin.PLUGIN_ID,
        IStatus.ERROR, message, null));
  }

  /**
   * Logs the message assoicated with a throwable object to the Eclipse error log as an
   * <code>IStatus.ERROR</code> object.
   * 
   * @param e
   *          the throwable object whose message is recorded
   */
  public static void log(final Throwable e)
  {
    JiveTomcatLaunchPlugin.log(new Status(IStatus.ERROR, JiveTomcatLaunchPlugin.PLUGIN_ID,
        IStatus.ERROR, e.getMessage(), e));
  }

  /**
   * Constructs the JIVE launch plug-in. This constructor is called by the Eclipse platform and
   * should not be called by clients.
   * 
   * @throws IllegalStateException
   *           if the plug-in has already been instantiated
   */
  public JiveTomcatLaunchPlugin()
  {
    if (JiveTomcatLaunchPlugin.plugin != null)
    {
      // TODO Add log message and internationalize the string literal
      throw new IllegalStateException("The JIVE Tomcat launch plug-in class already exists.");
    }
  }

  public IJiveLaunchManager getLaunchManager()
  {
    return JiveLaunchPlugin.getDefault().getLaunchManager();
  }

  @Override
  public void publishFinished(final IServer server, final IStatus status)
  {
    // debug messages for now
    System.out.println("publish finished: " + server.toString());
    // update modules
    updateModules(server);
  }

  @Override
  public void publishStarted(final IServer server)
  {
    // debug messages for now
    System.out.println("publish started: " + server.toString());
    // no-op: we only process at publish finish
  }

  @Override
  public void serverChanged(final ServerEvent event)
  {
    // update modules
    updateModules(event.getServer());
  }

  @Override
  public void start(final BundleContext context) throws Exception
  {
    super.start(context);
    JiveTomcatLaunchPlugin.plugin = this;
    // debug message for now
    System.err
        .println("starting Jive Tomcat Launch plugin and registering as a server lifecycle listener.");
    // this plug-in will listen to server lifecycle events
    ServerCore.addServerLifecycleListener(new IServerLifecycleListener()
      {
        @Override
        public void serverAdded(final IServer server)
        {
          // server.addPublishListener(plugin);
          server.addServerListener(JiveTomcatLaunchPlugin.plugin);
        }

        @Override
        public void serverChanged(final IServer server)
        {
          // no-op: we do not care about server changes
        }

        @Override
        public void serverRemoved(final IServer server)
        {
          // server.removePublishListener(plugin);
          server.removeServerListener(JiveTomcatLaunchPlugin.plugin);
        }
      });
    // for all servers that are already available
    for (final IServer server : ServerCore.getServers())
    {
      // server.addPublishListener(plugin);
      server.addServerListener(JiveTomcatLaunchPlugin.plugin);
    }
  }

  @Override
  public void stop(final BundleContext context) throws Exception
  {
    JiveTomcatLaunchPlugin.plugin = null;
    super.stop(context);
  }

  private void updateModules(final IServer server)
  {
    /**
     * Relevant modules for the server-- new ones may be added at any time; they should be
     * associated with the server's debug target.
     * 
     * The server should be associated with a debug target which, by its turn, is associated with a
     * java project. In the case of a server application this will be empty. However, the modules in
     * here should be still referenced by the project.
     */
    final IJiveDebugTarget target = JiveLaunchPlugin.getDefault().getLaunchManager()
        .lookupTarget(server.getLaunch());
    if (target == null)
    {
      return;
    }
    for (final IModule module : server.getModules())
    {
      target.getProject().addProject(module.getProject());
    }
  }
}