package edu.buffalo.cse.jive.internal.launch.tomcat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jst.server.tomcat.core.internal.TomcatLaunchConfigurationDelegate;

import edu.buffalo.cse.jive.launch.IJiveLaunchFactory;
import edu.buffalo.cse.jive.launch.LaunchFactory;

@SuppressWarnings("restriction")
public class TomcatApplicationLaunchDelegate extends TomcatLaunchConfigurationDelegate
{
  @Override
  public IVMInstall verifyVMInstall(final ILaunchConfiguration configuration) throws CoreException
  {
    final IJiveLaunchFactory factory = LaunchFactory.createFactory(configuration);
    return factory.createVMInstall(super.verifyVMInstall(configuration));
  }
}