package org.eclipse.jdt.internal.debug.core.model;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;

import com.sun.jdi.VirtualMachine;

public class JDIDebugTargetAdapter extends JDIDebugTarget
{
  public JDIDebugTargetAdapter(final ILaunch launch, final VirtualMachine jvm, final String name,
      final boolean supportTerminate, final boolean supportDisconnect, final IProcess process,
      final boolean resume)
  {
    super(launch, jvm, name, supportTerminate, supportDisconnect, process, resume);
  }

  protected class ThreadDeathHandlerAdapter extends JDIDebugTarget.ThreadDeathHandler
  {
    protected ThreadDeathHandlerAdapter()
    {
      super();
    }
  }

  protected class ThreadStartHandlerAdapter extends JDIDebugTarget.ThreadStartHandler
  {
    protected ThreadStartHandlerAdapter()
    {
      super();
    }
  }
}