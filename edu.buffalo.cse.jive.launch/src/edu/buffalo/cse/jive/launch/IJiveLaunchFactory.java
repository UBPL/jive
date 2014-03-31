package edu.buffalo.cse.jive.launch;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;

import edu.buffalo.cse.jive.model.IJiveProject;

public interface IJiveLaunchFactory
{
  public IJiveProject createJiveProject(final ILaunch launch);

  public IVMInstall createVMInstall(final IVMInstall subject);

  public IVMRunner createVMRunner(final String mode, final IVMInstall subject);
}
