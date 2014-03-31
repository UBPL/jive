package edu.buffalo.cse.jive.internal.launch.ui;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.internal.launch.JiveLaunchManager;

/**
 * Interfaces with any IDebugView implementation (usually this is LaunchView) and listens for
 * selection changes. This runs independently from the JIVE views, therefore, it plays nicely with
 * all non-UI functionality in JIVE.
 * 
 */
public class JiveLaunchNotifier implements ISelectionListener
{
  public JiveLaunchNotifier()
  {
    // must run in the display thread
    Display.getDefault().asyncExec(new Runnable()
      {
        @Override
        public void run()
        {
          final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
          window.getSelectionService().addPostSelectionListener(JiveLaunchNotifier.this);
        }
      });
  }

  @Override
  public void selectionChanged(final IWorkbenchPart part, final ISelection selection)
  {
    // allow views to be selectively enabled/disabled
    if (part instanceof IDebugView && selection instanceof IStructuredSelection)
    {
      Object object = ((IStructuredSelection) selection).getFirstElement();
      if (!selection.isEmpty())
      {
        // Determine if an IJiveDebugTarget is associated with the selection
        if (object instanceof IStackFrame)
        {
          object = ((IStackFrame) object).getThread();
        }
        if (object instanceof IThread)
        {
          object = ((IThread) object).getDebugTarget();
        }
        if (object instanceof IProcess)
        {
          object = ((IProcess) object).getLaunch();
        }
        if (object instanceof ILaunch)
        {
          object = ((ILaunch) object).getDebugTarget();
        }
      }
      // Display the debug target
      if (object instanceof IJiveDebugTarget)
      {
        JiveLaunchManager.INSTANCE.targetSelected((IJiveDebugTarget) object);
      }
    }
  }
}
