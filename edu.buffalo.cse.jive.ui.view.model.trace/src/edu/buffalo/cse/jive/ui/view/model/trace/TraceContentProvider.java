package edu.buffalo.cse.jive.ui.view.model.trace;

import java.util.List;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Display;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IExecutionModel.ITraceViewListener;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;
import edu.buffalo.cse.jive.ui.view.AbstractJiveContentProvider;

class TraceContentProvider extends AbstractJiveContentProvider implements ITraceViewListener
{
  private final TableViewer viewer;

  TraceContentProvider(final TableViewer viewer)
  {
    this.viewer = viewer;
  }

  @Override
  public void dispose()
  {
    // do nothing
  }

  @Override
  public void eventsInserted(final List<IJiveEvent> events)
  {
    if (!viewer.getControl().isDisposed())
    {
      viewer.getTable().getDisplay().asyncExec(new Runnable()
        {
          @Override
          public void run()
          {
            if (viewer != null && !viewer.getControl().isDisposed())
            {
              viewer.add(events.toArray());
            }
          }
        });
    }
  }

  @Override
  public void traceVirtualized(final boolean isVirtual)
  {
    final Display display = JiveUIPlugin.getStandardDisplay();
    if (display.getThread() == Thread.currentThread())
    {
      viewer.refresh();
    }
    else
    {
      display.asyncExec(new Runnable()
        {
          @Override
          public void run()
          {
            viewer.refresh();
          }
        });
    }
  }

  @Override
  protected Object[] getModelElements(final IJiveDebugTarget target)
  {
    final List<? extends IJiveEvent> elements = (target == null ? null : target.model().traceView()
        .events());
    return elements == null ? new Object[0] : elements.toArray();
  }

  @Override
  protected void subscribeToModel(final IJiveDebugTarget newInput)
  {
    if (newInput.model() != null)
    {
      newInput.model().traceView().register(this);
    }
  }

  @Override
  protected void unsubscribeFromModel(final IJiveDebugTarget oldInput)
  {
    if (oldInput.model() != null)
    {
      oldInput.model().traceView().unregister(this);
    }
  }
}