package edu.buffalo.cse.jive.launch;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

import edu.buffalo.cse.jive.debug.model.IJiveLaunchManager;
import edu.buffalo.cse.jive.internal.launch.JiveLaunchManager;

/**
 * Activator that controls the plug-in life cycle and provides utility methods.
 */
public class JiveLaunchPlugin extends Plugin
{
  /**
   * The mode used to determine if JIVE is enabled if JIVE is enabled for the corresponding launch.
   */
  public static final String JIVE_MODE = "jive"; //$NON-NLS-1$
  /**
   * The unique identifier of the plug-in.
   */
  public static final String PLUGIN_ID = "edu.buffalo.cse.jive.launch"; //$NON-NLS-1$
  public static String VERSION_WARNING = "It is strongly recommended that you use Jive with a Java Virtual Machine version 6 or newer. You are currently running with version %s. Although we try our best to keep the system running smoothly with previous JVM versions, please note that some features may not work properly or may be missing altogether.\n";
  /**
   * 
   * The shared instance of the plug-in.
   */
  private static JiveLaunchPlugin plugin;

  /**
   * Returns the shared instance of the JIVE launching plug-in.
   * 
   * @return the shared instance
   */
  public static JiveLaunchPlugin getDefault()
  {
    return JiveLaunchPlugin.plugin;
  }

  /**
   * Logs a status object to the Eclipse error log.
   * 
   * @param status
   *          the status object to record
   */
  public static void log(final IStatus status)
  {
    JiveLaunchPlugin.getDefault().getLog().log(status);
  }

  /**
   * Logs a string to the Eclipse error log as an <code>IStatus.ERROR</code> object.
   * 
   * @param message
   *          the message to be recorded
   */
  public static void log(final String message)
  {
    JiveLaunchPlugin.log(new Status(IStatus.ERROR, JiveLaunchPlugin.PLUGIN_ID, IStatus.ERROR,
        message, null));
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
    JiveLaunchPlugin.log(new Status(IStatus.ERROR, JiveLaunchPlugin.PLUGIN_ID, IStatus.ERROR, e
        .getMessage(), e));
  }

  /**
   * Constructs the JIVE launch plug-in. This constructor is called by the Eclipse platform and
   * should not be called by clients.
   * 
   * @throws IllegalStateException
   *           if the plug-in has already been instantiated
   */
  public JiveLaunchPlugin()
  {
    if (JiveLaunchPlugin.plugin != null)
    {
      // TODO Add log message and internationalize the string literal
      throw new IllegalStateException("The JIVE launch plug-in class already exists.");
    }
  }

  public IJiveLaunchManager getLaunchManager()
  {
    return JiveLaunchManager.INSTANCE;
  }

  @Override
  public void start(final BundleContext context) throws Exception
  {
    super.start(context);
    JiveLaunchPlugin.plugin = this;
  }

  @Override
  public void stop(final BundleContext context) throws Exception
  {
    JiveLaunchPlugin.plugin = null;
    super.stop(context);
  }
}