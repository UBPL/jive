package edu.buffalo.cse.jive.internal.ui.view.model.sequence.actions;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_COLLAPSE_ALL;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_EXPAND_ALL;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;
import edu.buffalo.cse.jive.model.IEventModel.EventKind;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.ui.IJiveTreeView;
import edu.buffalo.cse.jive.ui.IUpdatableAction;

public class SequenceModelActionFactory
{
  private static final IJiveDebugTarget activeTarget()
  {
    return JiveLaunchPlugin.getDefault().getLaunchManager().activeTarget();
  }

  private final IJiveTreeView viewer;

  public SequenceModelActionFactory(final IJiveTreeView viewer)
  {
    this.viewer = viewer;
  }

  public IUpdatableAction createCollapseAllAction()
  {
    return new CollapseAllAction(viewer);
  }

  public IAction createCollapseAllSelectedAction()
  {
    return new CollapseAllSelectedAction(viewer);
  }

  public IAction createEventFilterAction(final EventKind kind)
  {
    return new EventFilterAction(viewer, kind);
  }

  public IUpdatableAction createExpandAllAction()
  {
    return new ExpandAllAction(viewer);
  }

  public IAction createExpandAllSelectedAction()
  {
    return new ExpandAllSelectedAction(viewer);
  }

  private final static class CollapseAllAction extends Action implements IUpdatableAction
  {
    private final IJiveTreeView owner;

    CollapseAllAction(final IJiveTreeView owner)
    {
      super("Collapse All");
      this.owner = owner;
      setImageDescriptor(IM_BASE_COLLAPSE_ALL.enabledDescriptor());
      setDisabledImageDescriptor(IM_BASE_COLLAPSE_ALL.disabledDescriptor());
    }

    @Override
    public void run()
    {
      owner.getViewer().collapseAll();
    }

    @Override
    public void update()
    {
      setEnabled(SequenceModelActionFactory.activeTarget() != null);
    }
  }

  /**
   * An action that recursively collapses all nodes rooted at the selected node. This action
   * supports collapsing multiple selections.
   */
  private final static class CollapseAllSelectedAction extends Action
  {
    private final IJiveTreeView owner;

    CollapseAllSelectedAction(final IJiveTreeView owner)
    {
      super("Collapse All");
      this.owner = owner;
      setImageDescriptor(IM_BASE_COLLAPSE_ALL.enabledDescriptor());
      setDisabledImageDescriptor(IM_BASE_COLLAPSE_ALL.disabledDescriptor());
    }

    @Override
    public void run()
    {
      final TreeViewer viewer = owner.getViewer();
      final ITreeSelection selection = (ITreeSelection) viewer.getSelection();
      for (final TreePath path : selection.getPaths())
      {
        viewer.collapseToLevel(path, AbstractTreeViewer.ALL_LEVELS);
      }
    }
  }

  /**
   * An action that adds or removes an event exclusion filter based on the checked status. When the
   * action is checked, the exclusion filter is added to the viewer. When it is unchecked, the
   * filter is removed.
   */
  private static final class EventFilterAction extends Action
  {
    private final EventKind eventKind;
    private final IJiveTreeView owner;
    private final ViewerFilter filter = new ViewerFilter()
      {
        @Override
        public boolean select(final Viewer viewer, final Object parentElement, final Object element)
        {
          if (element instanceof IJiveEvent)
          {
            final IJiveEvent event = ((IJiveEvent) element);
            if (eventKind == event.kind())
            {
              return false;
            }
          }
          return true;
        }
      };

    EventFilterAction(final IJiveTreeView owner, final EventKind eventKind)
    {
      super("", IAction.AS_CHECK_BOX);
      this.owner = owner;
      this.eventKind = eventKind;
      setText("Hide " + eventKind);
      // setImageDescriptor(JiveResourceManager.descriptorFor(eventKind));
      setChecked(false);
    }

    @Override
    public void run()
    {
      if (isChecked())
      {
        owner.getViewer().addFilter(filter);
      }
      else
      {
        owner.getViewer().removeFilter(filter);
      }
    }
  }

  private final static class ExpandAllAction extends Action implements IUpdatableAction
  {
    private final IJiveTreeView owner;

    ExpandAllAction(final IJiveTreeView owner)
    {
      super("Expand All");
      this.owner = owner;
      setImageDescriptor(IM_BASE_EXPAND_ALL.enabledDescriptor());
      setDisabledImageDescriptor(IM_BASE_EXPAND_ALL.disabledDescriptor());
    }

    @Override
    public void run()
    {
      owner.getViewer().expandAll();
    }

    @Override
    public void update()
    {
      setEnabled(SequenceModelActionFactory.activeTarget() != null);
    }
  }

  /**
   * An action that recursively expanding all nodes rooted at the selected node. This action
   * supports expanding multiple selections.
   */
  private final static class ExpandAllSelectedAction extends Action
  {
    private final IJiveTreeView owner;

    ExpandAllSelectedAction(final IJiveTreeView owner)
    {
      super("Expand All");
      this.owner = owner;
      setImageDescriptor(IM_BASE_EXPAND_ALL.enabledDescriptor());
      setDisabledImageDescriptor(IM_BASE_EXPAND_ALL.disabledDescriptor());
    }

    @Override
    public void run()
    {
      final TreeViewer viewer = owner.getViewer();
      final ITreeSelection selection = (ITreeSelection) viewer.getSelection();
      for (final TreePath path : selection.getPaths())
      {
        viewer.expandToLevel(path, AbstractTreeViewer.ALL_LEVELS);
      }
    }
  }
}