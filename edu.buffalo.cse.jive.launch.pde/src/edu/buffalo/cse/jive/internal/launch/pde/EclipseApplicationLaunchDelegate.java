package edu.buffalo.cse.jive.internal.launch.pde;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.pde.internal.launching.launcher.VMHelper;
import org.eclipse.pde.launching.EclipseApplicationLaunchConfiguration;

import edu.buffalo.cse.jive.launch.IJiveLaunchFactory;
import edu.buffalo.cse.jive.launch.LaunchFactory;

public class EclipseApplicationLaunchDelegate extends EclipseApplicationLaunchConfiguration
{
  @Override
  public IVMRunner getVMRunner(final ILaunchConfiguration configuration, final String mode)
      throws CoreException
  {
    final IJiveLaunchFactory factory = LaunchFactory.createFactory(configuration);
    // if (factory == null) {
    // factory = new JavaApplicationLaunchFactory();
    // LaunchFactory.registerFactory(configuration.getType(), factory);
    // }
    return factory.createVMRunner(mode, VMHelper.createLauncher(configuration));
  }
}