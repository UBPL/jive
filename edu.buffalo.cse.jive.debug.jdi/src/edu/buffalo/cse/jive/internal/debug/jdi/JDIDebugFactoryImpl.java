package edu.buffalo.cse.jive.internal.debug.jdi;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;

import com.sun.jdi.VirtualMachine;

import edu.buffalo.cse.jive.debug.JiveDebugPlugin;
import edu.buffalo.cse.jive.model.IJiveProject;

public enum JDIDebugFactoryImpl
{
  INSTANCE;
  public static IDebugTarget createDebugTarget(final ILaunch launch, final VirtualMachine vm,
      final String name, final IProcess process, final boolean allowTerminate,
      final boolean allowDisconnect, final boolean resume, final IJiveProject project)
  {
    return INSTANCE.newDebugTarget(launch, vm, name, process, allowTerminate, allowDisconnect,
        resume, project);
  }

  /**
   * Creates and returns a debug target for the given VM, with the specified name, and associates
   * the debug target with the given process for console I/O. The allow terminate flag specifies
   * whether the debug target will support termination (<code>ITerminate</code>). The allow
   * disconnect flag specifies whether the debug target will support disconnection (
   * <code>IDisconnect</code>). The resume flag specifies if the target VM should be resumed on
   * startup (has no effect if the VM was already running when the connection to the VM was
   * established). Launching the actual VM is a client responsibility. The debug target is added to
   * the given launch.
   * <p>
   * The debug target is monitored by JIVE for JDI events.
   * 
   * @param launch
   *          the launch the new debug target will be contained in
   * @param vm
   *          the VM to create a debug target for
   * @param name
   *          the name to associate with the VM, which will be returned from
   *          <code>IDebugTarget.getName</code>. If <code>null</code> the name will be retrieved
   *          from the underlying VM.
   * @param process
   *          the process to associate with the debug target, which will be returned from
   *          <code>IDebugTarget.getProcess</code>
   * @param allowTerminate
   *          whether the target will support termination
   * @param allowDisconnect
   *          whether the target will support disconnection
   * @param resume
   *          whether the target is to be resumed on startup. Has no effect if the target was
   *          already running when the connection to the VM was established.
   * @return a debug target
   * @see org.eclipse.debug.core.model.ITerminate
   * @see org.eclipse.debug.core.model.IDisconnect
   */
  private IDebugTarget newDebugTarget(final ILaunch launch, final VirtualMachine vm,
      final String name, final IProcess process, final boolean allowTerminate,
      final boolean allowDisconnect, final boolean resume, final IJiveProject project)
  {
    final IJavaDebugTarget[] target = new IJavaDebugTarget[1];
    final IWorkspaceRunnable r = new IWorkspaceRunnable()
      {
        @Override
        public void run(final IProgressMonitor m)
        {
          target[0] = new JiveDebugTarget(launch, vm, name, allowTerminate, allowDisconnect,
              process, resume, project);
        }
      };
    try
    {
      ResourcesPlugin.getWorkspace().run(r, null, 0, null);
    }
    catch (final CoreException e)
    {
      JiveDebugPlugin.log(e);
    }
    return target[0];
  }
}
