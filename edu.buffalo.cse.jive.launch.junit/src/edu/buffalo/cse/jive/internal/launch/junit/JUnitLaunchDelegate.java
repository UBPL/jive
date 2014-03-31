package edu.buffalo.cse.jive.internal.launch.junit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMInstall;

import edu.buffalo.cse.jive.launch.IJiveLaunchFactory;
import edu.buffalo.cse.jive.launch.LaunchFactory;

public class JUnitLaunchDelegate extends JUnitLaunchConfigurationDelegate
{
  @Override
  public IVMInstall verifyVMInstall(final ILaunchConfiguration configuration) throws CoreException
  {
    final IJiveLaunchFactory factory = LaunchFactory.createFactory(configuration);
    return factory.createVMInstall(super.verifyVMInstall(configuration));
  }
}