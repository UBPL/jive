package edu.buffalo.cse.jive.internal.ui;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IStep;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.core.model.ITerminate;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.InstructionPointerManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.sourcelookup.ISourceLookupResult;
import org.eclipse.jdt.debug.core.IJavaClassType;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.ui.ISourceLookupFacility;
import edu.buffalo.cse.jive.ui.IStepAction;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;

@SuppressWarnings("restriction")
final class SourceLookupFacility implements ISourceLookupFacility
{
  @Override
  public void launchAdded(final ILaunch launch)
  {
  }

  @Override
  public void launchChanged(final ILaunch launch)
  {
  }

  @Override
  public void launchRemoved(final ILaunch launch)
  {
    final IDebugTarget target = launch.getDebugTarget();
    if (target instanceof IJiveDebugTarget)
    {
      removeAnnotations(target);
    }
  }

  @Override
  public void selectLine(final IJiveDebugTarget target, final IJiveEvent event)
  {
    // new line, remove current annotations
    removeAnnotations((IJavaDebugTarget) target);
    // situations in which we cannot synchronize
    if (event == null || event.line() == event.model().valueFactory().createUnavailableLine())
    {
      return;
    }
    // set up the mock objects for the lookup
    final IStackFrame stackFrame = createStackFrame(target, event);
    final ISourceLookupResult result = DebugUITools.lookupSource(stackFrame, target.getLaunch()
        .getSourceLocator());
    // synchronize the source
    final IWorkbench workbench = PlatformUI.getWorkbench();
    DebugUITools.displaySource(result, workbench.getActiveWorkbenchWindow().getActivePage());
  }

  @Override
  public void steppingCompleted(final IJiveDebugTarget target, final IStepAction action)
  {
    final IJiveEvent event = target.model().temporalState().event();
    if (event == null || event.line() == event.model().valueFactory().createUnavailableLine())
    {
      removeAnnotations((IJavaDebugTarget) target);
      return;
    }
    if (action != null && action.lineChanged())
    {
      selectLine(target, event);
    }
  }

  @Override
  public void steppingInitiated(final IJiveDebugTarget target)
  {
  }

  private IStackFrame createStackFrame(final IJiveDebugTarget target, final IJiveEvent event)
  {
    try
    {
      final IThread thread = new MockThread(target, event);
      return thread.getTopStackFrame();
    }
    catch (final DebugException e)
    {
      throw new IllegalStateException("This should never occur since a mock object is returned.");
    }
  }

  private void removeAnnotations(final IDebugTarget target)
  {
    InstructionPointerManager.getDefault().removeAnnotations(target);
  }

  // TODO Determine if we should cache previous MockStackFrame in order to
  // access the MockThread and be able to use this method
  // private void removeAnnotations(IThread thread) {
  //
  // InstructionPointerManager.getDefault().removeAnnotations(thread);
  // }
  private abstract class MockDebugElement implements IDebugElement, ISuspendResume, ITerminate,
      IStep
  {
    private final IJiveDebugTarget target;

    public MockDebugElement(final IJiveDebugTarget target)
    {
      this.target = target;
    }

    @Override
    public boolean canResume()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canStepInto()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canStepOver()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canStepReturn()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canSuspend()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canTerminate()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdapter(final Class adapter)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public IDebugTarget getDebugTarget()
    {
      return (IJavaDebugTarget) target;
    }

    @Override
    public ILaunch getLaunch()
    {
      return target.getLaunch();
    }

    @Override
    public String getModelIdentifier()
    {
      return target.getModelIdentifier();
    }

    @Override
    public boolean isStepping()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSuspended()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTerminated()
    {
      return false;
    }

    @Override
    public void resume() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void stepInto() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void stepOver() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void stepReturn() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void suspend() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void terminate() throws DebugException
    {
      throw new UnsupportedOperationException();
    }
  }

  private class MockStackFrame extends MockDebugElement implements IStackFrame, IJavaStackFrame
  {
    private final IJiveEvent event;
    private final IThread thread;

    public MockStackFrame(final IJiveDebugTarget target, final IJiveEvent event,
        final IThread thread)
    {
      super(target);
      this.event = event;
      this.thread = thread;
    }

    @Override
    public boolean canDropToFrame()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canForceReturn()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canStepWithFilters()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void dropToFrame() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(final Object o)
    {
      if (o instanceof MockStackFrame)
      {
        final MockStackFrame other = (MockStackFrame) o;
        return event.parent().equals(other.event.parent());
      }
      else
      {
        return false;
      }
    }

    @Override
    public IJavaVariable findVariable(final String variableName) throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void forceReturn(final IJavaValue value) throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdapter(final Class adapter)
    {
      if (adapter == IJavaStackFrame.class)
      {
        return this;
      }
      else
      {
        throw new UnsupportedOperationException();
      }
    }

    @Override
    public List<String> getArgumentTypeNames() throws DebugException
    {
      throw new DebugException(new Status(IStatus.OK, JiveUIPlugin.PLUGIN_ID,
          "Unknown argument type names"));
    }

    @Override
    public int getCharEnd() throws DebugException
    {
      return -1;
    }

    @Override
    public int getCharStart() throws DebugException
    {
      return -1;
    }

    @Override
    public IJavaClassType getDeclaringType() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getDeclaringTypeName() throws DebugException
    {
      // TODO: figure out what's really being done in here
      final String context = event.parent().toString();
      String result = "";
      if (context != null)
      {
        final int index = result.indexOf(':');
        if (index != -1)
        {
          result = result.substring(0, index);
        }
      }
      return result;
    }

    @Override
    public int getLineNumber() throws DebugException
    {
      return event.line().lineNumber();
    }

    @Override
    public int getLineNumber(final String stratum) throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public IJavaVariable[] getLocalVariables() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getMethodName() throws DebugException
    {
      throw new DebugException(
          new Status(IStatus.OK, JiveUIPlugin.PLUGIN_ID, "Unknown method name"));
    }

    @Override
    public String getName() throws DebugException
    {
      return event.parent().toString();
    }

    @Override
    public String getReceivingTypeName() throws DebugException
    {
      throw new DebugException(new Status(IStatus.OK, JiveUIPlugin.PLUGIN_ID,
          "Unknown receiving type name"));
    }

    @Override
    public IJavaReferenceType getReferenceType() throws DebugException
    {
      throw new DebugException(new Status(IStatus.OK, JiveUIPlugin.PLUGIN_ID,
          "Unknown reference type"));
    }

    @Override
    public IRegisterGroup[] getRegisterGroups() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getSignature() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getSourceName() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getSourceName(final String stratum) throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getSourcePath() throws DebugException
    {
      return event.line().file().name();
    }

    @Override
    public String getSourcePath(final String stratum) throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public IJavaObject getThis() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public IThread getThread()
    {
      return thread;
    }

    @Override
    public IVariable[] getVariables() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasRegisterGroups() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasVariables() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConstructor() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFinal() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNative() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isObsolete() throws DebugException
    {
      return false;
    }

    @Override
    public boolean isOutOfSynch() throws DebugException
    {
      return false;
    }

    @Override
    public boolean isPackagePrivate() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPrivate() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isProtected() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPublic() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isStatic() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isStaticInitializer() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSynchronized() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSynthetic() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVarArgs() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void stepWithFilters() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean supportsDropToFrame()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean wereLocalsAvailable()
    {
      return false;
    }
  }

  private class MockThread extends MockDebugElement implements IThread
  {
    private final IJiveEvent event;
    private final IStackFrame stackFrame;

    public MockThread(final IJiveDebugTarget target, final IJiveEvent event)
    {
      super(target);
      this.event = event;
      stackFrame = new MockStackFrame(target, event, this);
    }

    @Override
    public boolean equals(final Object o)
    {
      if (o instanceof MockThread)
      {
        final MockThread other = (MockThread) o;
        return event.thread().equals(other.event.thread());
      }
      else
      {
        return false;
      }
    }

    @Override
    public IBreakpoint[] getBreakpoints()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getName() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getPriority() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public IStackFrame[] getStackFrames() throws DebugException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public IStackFrame getTopStackFrame() throws DebugException
    {
      return stackFrame;
    }

    @Override
    public int hashCode()
    {
      return event.thread().hashCode();
    }

    @Override
    public boolean hasStackFrames() throws DebugException
    {
      throw new UnsupportedOperationException();
    }
  }
}