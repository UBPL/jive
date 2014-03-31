package edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.actions;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_COLLAPSE_ALL;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_EXPAND_ALL;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_SM_ACTION_EXPAND_LIFELINES;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_SM_ACTION_SHOW_THREAD_ACTIVATIONS;

import java.util.List;

import org.eclipse.gef.RootEditPart;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.SequenceDiagramEditPart;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IThreadStartEvent;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;
import edu.buffalo.cse.jive.preferences.PreferenceKeys;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;
import edu.buffalo.cse.jive.ui.IJiveGraphicalView;

public final class SequenceDiagramActionFactory
{
  private final IJiveGraphicalView view;

  public SequenceDiagramActionFactory(final IJiveGraphicalView viewer)
  {
    this.view = viewer;
  }

  public IAction createCollapseAfterAction(final IInitiatorEvent initiator)
  {
    return new ByTimeAction("After", true, initiator, false);
  }

  public IAction createCollapseBeforeAction(final IInitiatorEvent initiator)
  {
    return new ByTimeAction("Before", true, initiator, true);
  }

  public IAction createCollapseChildrenAction(final IInitiatorEvent initiator)
  {
    return new ChildrenAction("Children", true, initiator);
  }

  public IAction createCollapseLifeline(final Object model)
  {
    return new LifelineAction("Lifeline", getContour(model), getInitiator(model), true, false);
  }

  public IAction createCollapseLifelineChildren(final Object model)
  {
    return new LifelineAction("Children", getContour(model), getInitiator(model), true, true);
  }

  public IAction createCollapseNodeAction(final IInitiatorEvent initiator)
  {
    return new NodeAction("Here", true, initiator);
  }

  public IAction createExpandAfterAction(final IInitiatorEvent initiator)
  {
    return new ByTimeAction("After", false, initiator, false);
  }

  public IAction createExpandBeforeAction(final IInitiatorEvent initiator)
  {
    return new ByTimeAction("Before", false, initiator, true);
  }

  public IAction createExpandChildrenAction(final IInitiatorEvent initiator)
  {
    return new ChildrenAction("Children", false, initiator);
  }

  public IAction createExpandLifeline(final Object model)
  {
    return new LifelineAction("Lifeline", getContour(model), getInitiator(model), false, false);
  }

  public IAction createExpandLifelineChildren(final Object model)
  {
    return new LifelineAction("Children", getContour(model), getInitiator(model), false, true);
  }

  public IAction createExpandLifelinesAction()
  {
    return new ExpandLifelinesAction();
  }

  public IAction createExpandNodeAction(final IInitiatorEvent initiator)
  {
    return new NodeAction("Here", false, initiator);
  }

  public IAction createFocusAction(final IInitiatorEvent initiator)
  {
    return new FocusAction("Focus Here", true, initiator);
  }

  public IAction createShowThreadActivationsAction()
  {
    return new ShowThreadActivationsAction();
  }

  public IAction createThreadVisibilityToggleAction(IThreadValue thread)
  {
    return new ThreadVisibilityToggleAction(thread);
  }

  public IAction createVisibleThreadsAction()
  {
    return new VisibleThreadsAction();
  }

  private SequenceDiagramEditPart contents()
  {
    final RootEditPart root = view.getViewer().getRootEditPart();
    return (SequenceDiagramEditPart) root.getContents();
  }

  private IContextContour getContour(final Object model)
  {
    return model instanceof IContextContour ? (IContextContour) model : null;
  }

  private IInitiatorEvent getInitiator(final Object model)
  {
    return model instanceof IThreadValue ? ((IThreadValue) model).model().lookupThread(
        (IThreadValue) model) : model instanceof IInitiatorEvent ? (IInitiatorEvent) model : null;
  }

  private final class ByTimeAction extends NodeAction
  {
    private final boolean isBefore;

    private ByTimeAction(final String text, final boolean isCollapse,
        final IInitiatorEvent execution, final boolean isBefore)
    {
      super(text, isCollapse, execution);
      this.isBefore = isBefore;
      setEnabled(isBefore || execution.terminator() != null);
    }

    @Override
    public void run()
    {
      if (isCollapse())
      {
        contents().collapseByTime(initiator(), isBefore);
      }
      else
      {
        contents().expandExecutionByTime(initiator(), isBefore);
      }
    }
  }

  private final class ChildrenAction extends NodeAction
  {
    private ChildrenAction(final String text, final boolean isCollapse,
        final IInitiatorEvent initiator)
    {
      super(text, isCollapse, initiator);
    }

    @Override
    public void run()
    {
      if (isCollapse())
      {
        contents().collapseChildren(initiator());
      }
      else
      {
        contents().expandChildren(initiator());
      }
    }
  }

  private class ExpandLifelinesAction extends Action
  {
    ExpandLifelinesAction()
    {
      super("Expand Lifelines", IAction.AS_CHECK_BOX);
      setImageDescriptor(IM_SM_ACTION_EXPAND_LIFELINES.enabledDescriptor());
      final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
      setChecked(store.getBoolean(PreferenceKeys.PREF_SD_EXPAND_LIFELINES));
    }

    @Override
    public void run()
    {
      final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
      store.setValue(PreferenceKeys.PREF_SD_EXPAND_LIFELINES, isChecked());
    }
  }

  private class FocusAction extends NodeAction
  {
    private FocusAction(final String text, final boolean isCollapse, final IInitiatorEvent execution)
    {
      super(text, isCollapse, execution);
      setEnabled(true);
    }

    @Override
    public void run()
    {
      contents().focus(initiator());
    }
  }

  private final class LifelineAction extends Action
  {
    private final boolean isCollapse;
    private final IInitiatorEvent threadEvent;
    private final IContextContour contour;
    private final boolean isChildren;

    private LifelineAction(final String text, final IContextContour contour,
        final IInitiatorEvent threadEvent, final boolean isCollapse, final boolean isChildren)
    {
      super(text);
      this.contour = contour;
      this.threadEvent = threadEvent;
      this.isCollapse = isCollapse;
      this.isChildren = isChildren;
      if (isCollapse)
      {
        setImageDescriptor(IM_BASE_COLLAPSE_ALL.enabledDescriptor());
        setDisabledImageDescriptor(IM_BASE_COLLAPSE_ALL.disabledDescriptor());
      }
      else
      {
        setImageDescriptor(IM_BASE_EXPAND_ALL.enabledDescriptor());
        setDisabledImageDescriptor(IM_BASE_EXPAND_ALL.disabledDescriptor());
      }
      setEnabled(true);
    }

    @Override
    public void run()
    {
      if (threadEvent != null)
      {
        processThread();
      }
      else
      {
        processContour();
      }
    }

    private void processContour()
    {
      if (!isChildren)
      {
        if (isCollapse)
        {
          contents().collapseLifeline(contour);
        }
        else
        {
          contents().expandLifeline(contour);
        }
      }
      else
      {
        if (isCollapse)
        {
          contents().collapseLifelineChildren(contour);
        }
        else
        {
          contents().expandLifelineChildren(contour);
        }
      }
    }

    private void processThread()
    {
      if (!isChildren)
      {
        if (isCollapse)
        {
          contents().collapseExecution(threadEvent);
        }
        else
        {
          contents().expandExecution(threadEvent);
        }
      }
      else
      {
        if (isCollapse)
        {
          contents().collapseChildren(threadEvent);
        }
        else
        {
          contents().expandChildren(threadEvent);
        }
      }
    }
  }

  private class NodeAction extends SequenceDiagramAction
  {
    private NodeAction(final String text, final boolean isCollapse, final IInitiatorEvent initiator)
    {
      super(text, isCollapse, initiator);
    }

    @Override
    public void run()
    {
      if (isCollapse())
      {
        contents().collapseExecution(initiator());
      }
      else
      {
        contents().expandExecution(initiator());
      }
    }
  }

  private class SequenceDiagramAction extends Action
  {
    private final IInitiatorEvent initiator;
    private final boolean isCollapse;

    private SequenceDiagramAction(final String text, final boolean isCollapse,
        final IInitiatorEvent initiator)
    {
      super(text);
      this.initiator = initiator;
      this.isCollapse = isCollapse;
      if (isCollapse)
      {
        setImageDescriptor(IM_BASE_COLLAPSE_ALL.enabledDescriptor());
        setDisabledImageDescriptor(IM_BASE_COLLAPSE_ALL.disabledDescriptor());
      }
      else
      {
        setImageDescriptor(IM_BASE_EXPAND_ALL.enabledDescriptor());
        setDisabledImageDescriptor(IM_BASE_EXPAND_ALL.disabledDescriptor());
      }
      setEnabled(isCollapseEnabled() || isExpandEnabled());
    }

    protected IInitiatorEvent initiator()
    {
      return this.initiator;
    }

    protected boolean isCollapse()
    {
      return this.isCollapse;
    }

    protected boolean isCollapseEnabled()
    {
      return this.isCollapse && !contents().isCollapsed(initiator())
          && !initiator().nestedInitiators().isEmpty();
    }

    protected boolean isExpandEnabled()
    {
      return !this.isCollapse && !initiator().nestedInitiators().isEmpty();
    }
  }

  private final class ShowThreadActivationsAction extends Action
  {
    ShowThreadActivationsAction()
    {
      super("Show Thread Activations", IAction.AS_CHECK_BOX);
      setImageDescriptor(IM_SM_ACTION_SHOW_THREAD_ACTIVATIONS.enabledDescriptor());
      final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
      setChecked(store.getBoolean(PreferenceKeys.PREF_SD_SHOW_THREAD_ACTIVATIONS));
    }

    @Override
    public void run()
    {
      final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
      store.setValue(PreferenceKeys.PREF_SD_SHOW_THREAD_ACTIVATIONS, isChecked());
    }
  }

  private final class ThreadVisibilityToggleAction extends Action
  {
    private final IThreadValue thread;

    ThreadVisibilityToggleAction(final IThreadValue thread)
    {
      super(thread.name() + " (" + thread.id() + ")", IAction.AS_CHECK_BOX);
      this.thread = thread;
      this.setChecked(contents().isHiddenThread(thread));
    }

    @Override
    public void run()
    {
      contents().toggleHiddenThread(thread, isChecked());
    }
  }

  private final class VisibleThreadsAction extends Action implements IMenuCreator
  {
    private Menu menu;

    VisibleThreadsAction()
    {
      super("Hidden Threads");
      setMenuCreator(this);
    }

    @Override
    public void dispose()
    {
      if (menu != null)
      {
        menu.dispose();
      }
      menu = null;
    }

    @Override
    public Menu getMenu(Control parent)
    {
      dispose();
      List<IThreadStartEvent> threads = contents().executionModel().lookupThreads();
      menu = new Menu(parent);
      for (final IThreadStartEvent thread : threads)
      {
        new ActionContributionItem(new ThreadVisibilityToggleAction(thread.thread()))
            .fill(menu, -1);
      }
      return menu;
    }

    @Override
    public Menu getMenu(Menu parent)
    {
      return null;
    }

    @Override
    public void run()
    {
    }
  }
}