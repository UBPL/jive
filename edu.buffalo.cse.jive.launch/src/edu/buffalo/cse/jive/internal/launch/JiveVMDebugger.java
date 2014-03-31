package edu.buffalo.cse.jive.internal.launch;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.internal.launching.StandardVMDebugger;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import com.sun.jdi.VirtualMachine;

import edu.buffalo.cse.jive.debug.JiveDebugPlugin;
import edu.buffalo.cse.jive.launch.IJiveLaunchFactory;
import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;
import edu.buffalo.cse.jive.launch.LaunchFactory;
import edu.buffalo.cse.jive.model.IJiveProject;

@SuppressWarnings("restriction")
public class JiveVMDebugger extends StandardVMDebugger
{
  public JiveVMDebugger(final IVMInstall vmInstance)
  {
    super(vmInstance);
  }

  @Override
  protected IDebugTarget createDebugTarget(final VMRunnerConfiguration config,
      final ILaunch launch, final int port, final IProcess process, final VirtualMachine vm)
  {
    if (vm != null && vm.version() != null && "1.6".compareTo(vm.version()) > 0)
    {
      System.err.format(JiveLaunchPlugin.VERSION_WARNING, vm.version());
    }
    final IJiveLaunchFactory factory = LaunchFactory.createFactory(launch.getLaunchConfiguration());
    final IJiveProject project = factory.createJiveProject(launch);
    return JiveDebugPlugin.createDebugTarget(launch, vm,
        renderDebugTarget(config.getClassToLaunch(), port), process, true, false,
        config.isResumeOnStartup(), project);
  }
}