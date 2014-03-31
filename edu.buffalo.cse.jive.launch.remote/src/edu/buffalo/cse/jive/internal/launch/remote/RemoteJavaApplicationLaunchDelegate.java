package edu.buffalo.cse.jive.internal.launch.remote;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jdt.internal.launching.JavaRemoteApplicationLaunchConfigurationDelegate;
import org.eclipse.jdt.internal.launching.LaunchingMessages;
import org.eclipse.jdt.internal.launching.SocketAttachConnector;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.jdt.launching.JavaRuntime;

public class RemoteJavaApplicationLaunchDelegate extends
    JavaRemoteApplicationLaunchConfigurationDelegate
{
  @Override
  @SuppressWarnings(
  { "unchecked", "rawtypes", "deprecation" })
  public void launch(final ILaunchConfiguration configuration, final String mode,
      final ILaunch launch, IProgressMonitor monitor) throws CoreException
  {
    if (monitor == null)
    {
      monitor = new NullProgressMonitor();
    }
    monitor.beginTask(MessageFormat.format(
        LaunchingMessages.JavaRemoteApplicationLaunchConfigurationDelegate_Attaching_to__0_____1,
        (Object[]) new String[]
        { configuration.getName() }), 3);
    // check for cancellation
    if (monitor.isCanceled())
    {
      return;
    }
    try
    {
      monitor
          .subTask(LaunchingMessages.JavaRemoteApplicationLaunchConfigurationDelegate_Verifying_launch_attributes____1);
      final String connectorId = getVMConnectorId(configuration);
      IVMConnector connector = null;
      if (connectorId == null)
      {
        connector = JavaRuntime.getDefaultVMConnector();
      }
      else
      {
        connector = JavaRuntime.getVMConnector(connectorId);
      }
      if (connector == null)
      {
        abort(
            LaunchingMessages.JavaRemoteApplicationLaunchConfigurationDelegate_Connector_not_specified_2,
            null, IJavaLaunchConfigurationConstants.ERR_CONNECTOR_NOT_AVAILABLE);
      }
      final Map argMap = configuration.getAttribute(
          IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, (Map) null);
      final int connectTimeout = JavaRuntime.getPreferences().getInt(
          JavaRuntime.PREF_CONNECT_TIMEOUT);
      argMap.put("timeout", Integer.toString(connectTimeout)); //$NON-NLS-1$
      // check for cancellation
      if (monitor.isCanceled())
      {
        return;
      }
      monitor.worked(1);
      monitor
          .subTask(LaunchingMessages.JavaRemoteApplicationLaunchConfigurationDelegate_Creating_source_locator____2);
      // set the default source locator if required
      setDefaultSourceLocator(launch, configuration);
      monitor.worked(1);
      // replace the socket connector with our own to hijack the debug target creation
      if (connector instanceof SocketAttachConnector)
      {
        connector = new JiveSocketAttachConnector();
      }
      // connect to remote VM
      connector.connect(argMap, monitor, launch);
      // check for cancellation
      if (monitor.isCanceled())
      {
        final IDebugTarget[] debugTargets = launch.getDebugTargets();
        for (final IDebugTarget target : debugTargets)
        {
          if (target.canDisconnect())
          {
            target.disconnect();
          }
        }
        return;
      }
    }
    finally
    {
      monitor.done();
    }
  }
}