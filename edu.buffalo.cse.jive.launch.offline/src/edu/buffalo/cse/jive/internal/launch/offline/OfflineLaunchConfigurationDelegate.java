package edu.buffalo.cse.jive.internal.launch.offline;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.launch.IJiveLaunchFactory;
import edu.buffalo.cse.jive.launch.LaunchFactory;
import edu.buffalo.cse.jive.model.IJiveProject;

public class OfflineLaunchConfigurationDelegate extends LaunchConfigurationDelegate
{
  @Override
  public void launch(final ILaunchConfiguration configuration, final String mode,
      final ILaunch launch, final IProgressMonitor monitor) throws CoreException
  {
    final IJiveLaunchFactory factory = LaunchFactory.createFactory(configuration);
    final IJiveProject project = factory.createJiveProject(launch);
    final IJiveDebugTarget target = new OfflineDebugTarget(launch, project);
    launch.addDebugTarget((IJavaDebugTarget) target);
    target.start();
  }
}