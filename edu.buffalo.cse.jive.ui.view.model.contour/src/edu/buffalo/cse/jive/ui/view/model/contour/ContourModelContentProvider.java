package edu.buffalo.cse.jive.ui.view.model.contour;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IExecutionModel.AtomicDelete;
import edu.buffalo.cse.jive.model.IExecutionModel.AtomicInsert;
import edu.buffalo.cse.jive.model.IExecutionModel.AtomicUpdate;
import edu.buffalo.cse.jive.model.IExecutionModel.IStateChange;
import edu.buffalo.cse.jive.model.IExecutionModel.ITraceViewListener;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;
import edu.buffalo.cse.jive.ui.view.AbstractJiveContentProvider;

class ContourModelContentProvider extends AbstractJiveContentProvider implements
    ITreeContentProvider, ITraceViewListener
{
  private final ContourModelView owner;

  ContourModelContentProvider(final ContourModelView contourModelView)
  {
    this.owner = contourModelView;
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
      processEvents(events);
    }
    else
    {
      display.asyncExec(new Runnable()
        {
          @Override
          public void run()
          {
            processEvents(events);
          }
        });
    }
  }

  @Override
  public Object[] getChildren(final Object parentElement)
  {
    final IContour parent = (IContour) parentElement;
    final IExecutionModel model = owner.contourModel();
    if (model != null)
    {
      model.readLock();
      try
      {
        if (model.contourView().contains(parent))
        {
          return parent.children().toArray();
        }
      }
      finally
      {
        model.readUnlock();
      }
    }
    return new Object[0];
  }

  @Override
  public Object getParent(final Object element)
  {
    final IContour child = (IContour) element;
    final IExecutionModel model = this.owner.contourModel();
    if (model != null)
    {
      model.readLock();
      try
      {
        if (model.contourView().contains(child))
        {
          final IContour parent = child.parent();
          return parent == null ? this.owner.getViewer().getInput() : parent;
        }
      }
      finally
      {
        model.readUnlock();
      }
    }
    return null;
  }

  @Override
  public boolean hasChildren(final Object element)
  {
    final IContour contour = (IContour) element;
    final IExecutionModel model = this.owner.contourModel();
    if (model != null)
    {
      if (model.contourView().contains(contour))
      { // read-locks
        return contour.children().size() > 0;
      }
    }
    return false;
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
   * Adds the supplied contour to the tree viewer. This method is called by
   * {@link #contourAdded(IExecutionModel, AtomicInsert)}.
   * 
   * @param model
   *          the model in which the contour was added
   * @param contour
   *          the contour that was added
   * @param parent
   *          the parent of the added contour
   */
  private void addContour(final AtomicInsert change)
  {
    final TreeViewer viewer = this.owner.getViewer();
    if (!viewer.getControl().isDisposed())
    {
      if (change.contour().parent() == null)
      {
        viewer.add(viewer.getInput(), change.contour());
      }
      else
      {
        if (change.contour().model().contourView().contains(change.contour().parent()))
        {
          final TreePath path = createTreePath(change.contour().parent());
          viewer.add(path, change.contour());
          viewer.expandToLevel(path, 1);
        }
        else
        {
          viewer.add(change.contour().parent(), change.contour());
        }
      }
    }
  }

  /**
   * Creates a {@code TreePath} for the supplied contour, so that it may be used to efficiently add
   * (remove) a contour to (from) the tree viewer.
   * 
   * @param model
   *          the model containing the contour
   * @param contour
   *          the contour for which to construct a path
   * @return a tree path from the root to the contour, inclusive
   */
  private TreePath createTreePath(IContour contour)
  {
    final IExecutionModel model = contour.model();
    model.readLock();
    try
    {
      final LinkedList<IContour> segmentList = new LinkedList<IContour>();
      do
      {
        segmentList.addFirst(contour);
        contour = contour.parent();
      } while (contour != null);
      return new TreePath(segmentList.toArray());
    }
    finally
    {
      model.readUnlock();
    }
  }

  private void processEvents(final List<IJiveEvent> events)
  {
    for (final IJiveEvent event : events)
    {
      for (final IStateChange change : event.transaction().changes())
      {
        if (change instanceof AtomicDelete)
        {
          removeContour((AtomicDelete) change);
        }
        else if (change instanceof AtomicInsert)
        {
          addContour((AtomicInsert) change);
        }
        else if (change instanceof AtomicUpdate)
        {
          updateContourMemberTable((AtomicUpdate) change);
        }
      }
    }
  }

  /**
   * Removes the supplied contour from the tree viewer. This method is called by
   * {@link #removeContour(IExecutionModel, AtomicDelete)}.
   * 
   * @param model
   *          the model in which the contour was removed
   * @param contour
   *          the contour that was removed
   * @param oldParent
   *          the parent of the removed contour
   */
  private void removeContour(final AtomicDelete removed)
  {
    final TreeViewer viewer = this.owner.getViewer();
    if (!viewer.getControl().isDisposed())
    {
      final Object selection = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
      if (removed.contour().equals(selection))
      {
        this.owner.fMemberTableViewer.setInput(null);
      }
      // old parent might not be in the model if the remove was a result of a rollback
      if (removed.contour().model().contourView().contains(removed.contour().parent()))
      { // read-locks
        final TreePath path = createTreePath(removed.contour().parent());
        viewer.remove(path.createChildPath(removed.contour()));
      }
      else
      {
        viewer.remove(removed.contour());
      }
    }
  }

  /**
   * Updates the contour member table if the supplied contour is selected.
   * 
   * @param contour
   *          the contour whose member table has been updated
   */
  private void updateContourMemberTable(final AtomicUpdate change)
  {
    final TreeViewer viewer = this.owner.getViewer();
    if (!viewer.getControl().isDisposed())
    {
      final Object selection = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
      if (change.contour().equals(selection))
      {
        this.owner.fMemberTableViewer.refresh();
      }
    }
  }

  @Override
  protected Object[] getModelElements(final IJiveDebugTarget target)
  {
    if (target != null)
    {
      final IExecutionModel model = target.model();
      if (model != null)
      {
        return model.contourView().lookupRoots().toArray();
      }
    }
    return new Object[0];
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
    if (!owner.fMemberTableViewer.getControl().isDisposed())
    {
      this.owner.fMemberTableViewer.setInput(null);
    }
  }
}