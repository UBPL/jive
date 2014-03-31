package edu.buffalo.cse.jive.ui.view.model.sequence;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_TREE;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.internal.ui.view.model.sequence.actions.SequenceModelActionFactory;
import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;
import edu.buffalo.cse.jive.model.IEventModel.EventKind;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.ui.IJiveTreeView;
import edu.buffalo.cse.jive.ui.IStepAction;
import edu.buffalo.cse.jive.ui.IStepManager;
import edu.buffalo.cse.jive.ui.IUpdatableAction;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;
import edu.buffalo.cse.jive.ui.view.AbstractJiveView;
import edu.buffalo.cse.jive.ui.view.AbstractStructuredJiveView;
import edu.buffalo.cse.jive.ui.view.JiveEventLabelProvider;

/**
 * A view part to present {@code SequenceView}s associated with {@code IJiveDebugTarget}s. The
 * content provider used by this view is specific to Java sequence models. The view uses a JFace
 * {@code TreeViewer} to display the execution and event occurrences. The tree contains multiple
 * roots, where each is the call tree of a thread. Controls are available to expand or collapse
 * portions of the call trees.
 */
public class SequenceModelView extends AbstractStructuredJiveView implements IJiveTreeView
{
  protected static final String EXPAND_COLLAPSE_GROUP = "expandCollapseGroup";

  private static final IJiveDebugTarget activeTarget()
  {
    return JiveLaunchPlugin.getDefault().getLaunchManager().activeTarget();
  }

  private IUpdatableAction collapseAllAction;
  private IAction collapseAllSelectedAction;
  private IUpdatableAction expandAllAction;
  private IAction expandAllSelectedAction;
  private final SequenceModelActionFactory factory;

  public SequenceModelView()
  {
    super();
    this.factory = new SequenceModelActionFactory(this);
  }

  @Override
  public TreeViewer getViewer()
  {
    return (TreeViewer) super.getViewer();
  }

  @Override
  protected void configurePullDownMenu(final IMenuManager manager)
  {
    manager.add(factory.createEventFilterAction(EventKind.EXCEPTION_CATCH));
    manager.add(factory.createEventFilterAction(EventKind.EXCEPTION_THROW));
    manager.add(factory.createEventFilterAction(EventKind.FIELD_WRITE));
    manager.add(factory.createEventFilterAction(EventKind.LINE_STEP));
    manager.add(factory.createEventFilterAction(EventKind.VAR_ASSIGN));
    manager.add(factory.createEventFilterAction(EventKind.VAR_DELETE));
    manager.add(factory.createEventFilterAction(EventKind.METHOD_RETURNED));
    manager.add(factory.createEventFilterAction(EventKind.OBJECT_NEW));
    manager.add(factory.createEventFilterAction(EventKind.TYPE_LOAD));
  }

  @Override
  protected void configureToolBar(final IToolBarManager manager)
  {
    super.configureToolBar(manager);
    manager.insertAfter(AbstractJiveView.GROUP_STEP_CONTROLS, new Separator(
        SequenceModelView.EXPAND_COLLAPSE_GROUP));
    manager.appendToGroup(SequenceModelView.EXPAND_COLLAPSE_GROUP, expandAllAction);
    manager.appendToGroup(SequenceModelView.EXPAND_COLLAPSE_GROUP, collapseAllAction);
  }

  @Override
  protected void createActions()
  {
    // Make sure the super class's method is called
    super.createActions();
    // Create the tool bar actions
    expandAllAction = factory.createExpandAllAction();
    collapseAllAction = factory.createCollapseAllAction();
    // Register the tool bar actions to be included in updates
    addUpdatableAction(expandAllAction);
    addUpdatableAction(collapseAllAction);
    // Create the context menu actions
    expandAllSelectedAction = factory.createExpandAllSelectedAction();
    collapseAllSelectedAction = factory.createCollapseAllSelectedAction();
  }

  @Override
  protected IStructuredContentProvider createContentProvider()
  {
    return new SequenceModelContentProvider(this);
  }

  @Override
  protected IBaseLabelProvider createLabelProvider()
  {
    return new JiveEventLabelProvider();
  }

  @Override
  protected StructuredViewer createViewer(final Composite parent)
  {
    return new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
  }

  @Override
  protected void fillContextMenu(final IMenuManager manager)
  {
    // Fill the context menu with disabled actions
    expandAllSelectedAction.setEnabled(false);
    collapseAllSelectedAction.setEnabled(false);
    manager.add(expandAllSelectedAction);
    manager.add(collapseAllSelectedAction);
    final TreeViewer viewer = getViewer();
    final ITreeSelection selection = (ITreeSelection) viewer.getSelection();
    if (!selection.isEmpty())
    {
      // Determine if the actions should be enabled
      for (final TreePath path : selection.getPaths())
      {
        if (viewer.isExpandable(path))
        {
          expandAllSelectedAction.setEnabled(true);
          collapseAllSelectedAction.setEnabled(true);
          break;
        }
      }
      // Add run to event action
      final Object element = selection.getFirstElement();
      if (element instanceof IJiveEvent)
      {
        final IJiveEvent event = (IJiveEvent) element;
        final IJiveDebugTarget target = SequenceModelView.activeTarget();
        final IStepManager stepManager = JiveUIPlugin.getDefault().stepManager();
        if (target.canReplay() && !stepManager.isStepping(target))
        {
          final IStepAction action = JiveUIPlugin.getDefault().coreFactory()
              .createRunToEventAction(target, event);
          action.setText("Run to Event");
          manager.add(action);
        }
      }
    }
  }

  @Override
  protected String getDefaultContentDescription()
  {
    return "No sequence models to display at this time.";
  }

  @Override
  protected ImageDescriptor getDisplayDropDownDisabledImageDescriptor()
  {
    return IM_BASE_TREE.disabledDescriptor();
  }

  @Override
  protected ImageDescriptor getDisplayDropDownEnabledImageDescriptor()
  {
    return IM_BASE_TREE.enabledDescriptor();
  }

  @Override
  protected String getDisplayTargetDropDownText()
  {
    return "Display Sequence Model";
  }
}