package edu.buffalo.cse.jive.debug;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.osgi.framework.BundleContext;

import com.sun.jdi.VirtualMachine;

import edu.buffalo.cse.jive.core.ast.ASTFactory;
import edu.buffalo.cse.jive.core.ast.ASTPluginProxy;
import edu.buffalo.cse.jive.debug.jdi.model.IJDIManager;
import edu.buffalo.cse.jive.debug.jdi.model.IJDIModelFactory;
import edu.buffalo.cse.jive.debug.jdi.model.IModelFilter;
import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.internal.debug.jdi.JDIDebugFactoryImpl;
import edu.buffalo.cse.jive.internal.debug.jdi.model.JDIModelFactoryImpl;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IJiveProject;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory.IStaticModelDelegate;

/**
 * Activator that controls the plug-in life cycle and provides utility methods.
 */
public class JiveDebugPlugin extends Plugin
{
  /**
   * The unique identifier of the plug-in.
   */
  public static final String PLUGIN_ID = "edu.buffalo.cse.jive.debug"; //$NON-NLS-1$
  /**
   * The shared instance of the plug-in.
   */
  private static JiveDebugPlugin plugin;

  public static IDebugTarget createDebugTarget(final ILaunch launch, final VirtualMachine vm,
      final String name, final IProcess process, final boolean allowTerminate,
      final boolean allowDisconnect, final boolean resume, final IJiveProject project)
  {
    return JDIDebugFactoryImpl.createDebugTarget(launch, vm, name, process, allowTerminate,
        allowDisconnect, resume, project);
  }

  public static IStaticModelDelegate createStaticModelDelegate(final IExecutionModel model,
      final IJiveProject project, final VirtualMachine vm, final IModelFilter filter)
  {
    final IStaticModelDelegate downstream = JiveDebugPlugin.getDefault().jdiModelFactory()
        .createStaticModelDelegate(model, vm, filter);
    final IStaticModelDelegate upstream = ASTFactory.createStaticModelDelegate(model, project,
        filter.modelCache(), downstream);
    return upstream;
  }

  /**
   * Returns the shared instance of the JIVE core plug-in.
   * 
   * @return the shared instance
   */
  public static JiveDebugPlugin getDefault()
  {
    return JiveDebugPlugin.plugin;
  }

  public static void info(final String message)
  {
    JiveDebugPlugin.log(IStatus.INFO, message, null);
  }

  public static void info(final String message, final Throwable e)
  {
    JiveDebugPlugin.log(IStatus.INFO, message, e);
  }

  /**
   * Logs a status object to the Eclipse error log.
   * 
   * @param status
   *          the status object to record
   */
  public static void log(final IStatus status)
  {
    JiveDebugPlugin.getDefault().getLog().log(status);
  }

  /**
   * Logs a string to the Eclipse error log as an <code>IStatus.ERROR</code> object.
   * 
   * @param message
   *          the message to be recorded
   */
  public static void log(final String message)
  {
    JiveDebugPlugin.log(new Status(IStatus.ERROR, JiveDebugPlugin.PLUGIN_ID, IStatus.ERROR,
        message, null));
  }

  /**
   * Logs the message associated with a throwable object to the Eclipse error log as an
   * <code>IStatus.ERROR</code> object.
   * 
   * @param e
   *          the throwable object whose message is recorded
   */
  public static void log(final Throwable e)
  {
    e.printStackTrace();
    JiveDebugPlugin.log(new Status(IStatus.ERROR, JiveDebugPlugin.PLUGIN_ID, IStatus.ERROR, e
        .getMessage(), e));
  }

  public static void warn(final String message)
  {
    JiveDebugPlugin.log(IStatus.WARNING, message);
  }

  public static void warn(final String message, final Throwable e)
  {
    JiveDebugPlugin.log(IStatus.WARNING, message, e);
  }

  private static void log(final int severity, final String message)
  {
    JiveDebugPlugin.log(severity, message, null);
  }

  private static void log(final int severity, final String message, final Throwable e)
  {
    JiveDebugPlugin.log(new Status(severity, "edu.buffalo.cse.jive.debug.core", severity, message,
        e));
  }

  /**
   * Constructs the JIVE core plug-in. This constructor is called by the Eclipse platform and should
   * not be called by clients.
   * 
   * @throws IllegalStateException
   *           if the plug-in has already been instantiated
   */
  public JiveDebugPlugin()
  {
    if (JiveDebugPlugin.plugin != null)
    {
      // TODO Add log message and internationalize the string literal
      throw new IllegalStateException("The JIVE core plug-in class already exists.");
    }
    ASTPluginProxy.setOwner(this);
  }

  public IJDIManager jdiManager(final IJiveDebugTarget owner)
  {
    return jdiModelFactory().jdiManager(owner);
  }

  @Override
  public void start(final BundleContext context) throws Exception
  {
    super.start(context);
    JiveDebugPlugin.plugin = this;
  }

  @Override
  public void stop(final BundleContext context) throws Exception
  {
    JiveDebugPlugin.plugin = null;
    super.stop(context);
  }

  private IJDIModelFactory jdiModelFactory()
  {
    return JDIModelFactoryImpl.INSTANCE;
  }
}