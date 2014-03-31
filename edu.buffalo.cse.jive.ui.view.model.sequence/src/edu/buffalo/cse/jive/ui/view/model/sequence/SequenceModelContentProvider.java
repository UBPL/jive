package edu.buffalo.cse.jive.ui.view.model.sequence;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IExecutionModel.ITraceViewListener;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;
import edu.buffalo.cse.jive.ui.view.AbstractJiveContentProvider;

/**
 * An implementation of an {@code AbstractJiveContentProvider} that provides {@code InitiatorEvent}s
 * and {@code DataEvent}s as model elements.
 */
final class SequenceModelContentProvider extends AbstractJiveContentProvider implements
    ITreeContentProvider, ITraceViewListener
{
  private final SequenceModelView owner;

  SequenceModelContentProvider(final SequenceModelView owner)
  {
    this.owner = owner;
  }

  @Override
  public void dispose()
  {
    // do nothing
  }

  @Override
  public void eventsInserted(final List<IJiveEvent> events)
  {
    final Display display = JiveUIPlugin.getStandardDisplay();
    if (display.getThread() == Thread.currentThread())
    {
      processChanges(events);
    }
    else
    {
      display.asyncExec(new Runnable()
        {
          @Override
          public void run()
          {
            processChanges(events);
          }
        });
    }
  }

  @Override
  public Object[] getChildren(final Object element)
  {
    // Children of an execution occurrence are its events
    if (element instanceof IInitiatorEvent)
    {
      final IInitiatorEvent parent = (IInitiatorEvent) element;
      return parent.events().toArray();
    }
    // Other event occurrences do not have children
    else if (element instanceof IJiveEvent)
    {
      return new Object[0];
    }
    // This case should not be reached unless there is a programming error
    throw new IllegalStateException("Element " + element + " has an invalid type.");
  }

  @Override
  public Object getParent(final Object element)
  {
    // The parent of an event occurrence is its containing execution
    if (element instanceof IJiveEvent)
    {
      final IJiveEvent child = (IJiveEvent) element;
      return child.parent();
    }
    // This case should not be reached unless there is a programming error
    throw new IllegalStateException("Element " + element + " has an invalid type.");
  }

  @Override
  public boolean hasChildren(final Object element)
  {
    // An execution occurrence has children if it has any events
    if (element instanceof IInitiatorEvent)
    {
      final IInitiatorEvent parent = (IInitiatorEvent) element;
      return parent.hasChildren();
    }
    // Other event occurrences do not have children
    else if (element instanceof IJiveEvent)
    {
      return false;
    }
    // This case should not be reached unless there is a programming error
    throw new IllegalStateException("Element " + element + " has an invalid type.");
  }

  @Override
  public void traceVirtualized(final boolean isVirtual)
  {
    final Display display = JiveUIPlugin.getStandardDisplay();
    if (display.getThread() == Thread.currentThread())
    {
      owner.getViewer().refresh();
    }
    else
    {
      display.asyncExec(new Runnable()
        {
          @Override
          public void run()
          {
            owner.getViewer().refresh();
          }
        });
    }
  }

  /**
   * Creates a {@code TreePath} for the supplied execution occurrence, so that it may be used to
   * efficiently add (remove) an execution or event occurrence to (from) the tree viewer.
   */
  private TreePath createTreePath(IInitiatorEvent execution)
  {
    final LinkedList<Object> segmentList = new LinkedList<Object>();
    segmentList.addFirst(execution);
    IInitiatorEvent initiator = execution.parent();
    while (initiator != null)
    {
      segmentList.addFirst(initiator);
      execution = initiator;
      segmentList.addFirst(execution);
      initiator = execution.parent();
    }
    return new TreePath(segmentList.toArray());
  }

  @Override
  protected Object[] getModelElements(final IJiveDebugTarget target)
  {
    final IInitiatorEvent root = (target == null ? null : target.model().lookupRoot());
    return root == null ? new Object[0] : new Object[]
    { root };
  }

  protected void processChanges(final List<IJiveEvent> events)
  {
    final TreeViewer viewer = owner.getViewer();
    for (final IJiveEvent added : events)
    {
      if (added instanceof IInitiatorEvent)
      {
        final IInitiatorEvent execution = (IInitiatorEvent) added;
        if (execution.parent() == null)
        {
          viewer.add(viewer.getInput(), execution);
        }
        else
        {
          final TreePath path = createTreePath(execution);
          viewer.add(path.getParentPath(), execution);
        }
      }
      else
      {
        final TreePath path = createTreePath(added.parent());
        viewer.add(path, added);
      }
    }
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