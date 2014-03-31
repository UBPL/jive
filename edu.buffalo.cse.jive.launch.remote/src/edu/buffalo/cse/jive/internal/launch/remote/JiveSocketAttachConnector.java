package edu.buffalo.cse.jive.internal.launch.remote;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jdi.TimeoutException;
import org.eclipse.jdt.internal.launching.LaunchingMessages;
import org.eclipse.jdt.internal.launching.SocketAttachConnector;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import com.ibm.icu.text.MessageFormat;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

import edu.buffalo.cse.jive.debug.JiveDebugPlugin;
import edu.buffalo.cse.jive.launch.IJiveLaunchFactory;
import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;
import edu.buffalo.cse.jive.launch.LaunchFactory;
import edu.buffalo.cse.jive.model.IJiveProject;

public class JiveSocketAttachConnector extends SocketAttachConnector
{
  private static IDebugTarget createDebugTarget(final ILaunch launch, final VirtualMachine vm,
      final String vmLabel, final boolean allowTerminate)
  {
    if (vm != null && vm.version() != null && "1.6".compareTo(vm.version()) > 0)
    {
      System.err.format(JiveLaunchPlugin.VERSION_WARNING, vm.version());
    }
    final IJiveLaunchFactory factory = LaunchFactory.createFactory(launch.getLaunchConfiguration());
    final IJiveProject project = factory.createJiveProject(launch);
    return JiveDebugPlugin.createDebugTarget(launch, vm, vmLabel, null, true, true, false, project);
  }

  /**
   * TODO: reduce the cyclomatic complexity.
   */
  @SuppressWarnings(
  { "rawtypes", "unchecked" })
  @Override
  public void connect(final Map arguments, IProgressMonitor monitor, final ILaunch launch)
      throws CoreException
  {
    if (monitor == null)
    {
      monitor = new NullProgressMonitor();
    }
    final IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
    subMonitor.beginTask(LaunchingMessages.SocketAttachConnector_Connecting____1, 2);
    subMonitor.subTask(LaunchingMessages.SocketAttachConnector_Configuring_connection____1);
    final AttachingConnector connector = SocketAttachConnector.getAttachingConnector();
    final String portNumberString = (String) arguments.get("port"); //$NON-NLS-1$
    if (portNumberString == null)
    {
      SocketAttachConnector.abort(
          LaunchingMessages.SocketAttachConnector_Port_unspecified_for_remote_connection__2, null,
          IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_PORT);
    }
    final String host = (String) arguments.get("hostname"); //$NON-NLS-1$
    if (host == null)
    {
      SocketAttachConnector.abort(
          LaunchingMessages.SocketAttachConnector_Hostname_unspecified_for_remote_connection__4,
          null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_HOSTNAME);
    }
    final Map map = connector.defaultArguments();
    Connector.Argument param = (Connector.Argument) map.get("hostname"); //$NON-NLS-1$
    param.setValue(host);
    param = (Connector.Argument) map.get("port"); //$NON-NLS-1$
    param.setValue(portNumberString);
    final String timeoutString = (String) arguments.get("timeout"); //$NON-NLS-1$
    if (timeoutString != null)
    {
      param = (Connector.Argument) map.get("timeout"); //$NON-NLS-1$
      param.setValue(timeoutString);
    }
    final ILaunchConfiguration configuration = launch.getLaunchConfiguration();
    boolean allowTerminate = false;
    if (configuration != null)
    {
      allowTerminate = configuration.getAttribute(
          IJavaLaunchConfigurationConstants.ATTR_ALLOW_TERMINATE, false);
    }
    subMonitor.worked(1);
    subMonitor.subTask(LaunchingMessages.SocketAttachConnector_Establishing_connection____2);
    try
    {
      final VirtualMachine vm = connector.attach(map);
      final String vmLabel = constructVMLabel(vm, host, portNumberString, configuration);
      // hijack the debug target creation
      final IDebugTarget debugTarget = JiveSocketAttachConnector.createDebugTarget(launch, vm,
          vmLabel, allowTerminate);
      launch.addDebugTarget(debugTarget);
      subMonitor.worked(1);
      subMonitor.done();
    }
    catch (final TimeoutException e)
    {
      SocketAttachConnector.abort(LaunchingMessages.SocketAttachConnector_0, e,
          IJavaLaunchConfigurationConstants.ERR_REMOTE_VM_CONNECTION_FAILED);
    }
    catch (final UnknownHostException e)
    {
      SocketAttachConnector
          .abort(
              MessageFormat
                  .format(
                      LaunchingMessages.SocketAttachConnector_Failed_to_connect_to_remote_VM_because_of_unknown_host____0___1,
                      (Object[]) new String[]
                      { host }), e,
              IJavaLaunchConfigurationConstants.ERR_REMOTE_VM_CONNECTION_FAILED);
    }
    catch (final ConnectException e)
    {
      SocketAttachConnector
          .abort(
              LaunchingMessages.SocketAttachConnector_Failed_to_connect_to_remote_VM_as_connection_was_refused_2,
              e, IJavaLaunchConfigurationConstants.ERR_REMOTE_VM_CONNECTION_FAILED);
    }
    catch (final IOException e)
    {
      SocketAttachConnector.abort(
          LaunchingMessages.SocketAttachConnector_Failed_to_connect_to_remote_VM_1, e,
          IJavaLaunchConfigurationConstants.ERR_REMOTE_VM_CONNECTION_FAILED);
    }
    catch (final IllegalConnectorArgumentsException e)
    {
      SocketAttachConnector.abort(
          LaunchingMessages.SocketAttachConnector_Failed_to_connect_to_remote_VM_1, e,
          IJavaLaunchConfigurationConstants.ERR_REMOTE_VM_CONNECTION_FAILED);
    }
  }
}
