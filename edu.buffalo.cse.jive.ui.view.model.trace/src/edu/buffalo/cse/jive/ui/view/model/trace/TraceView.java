package edu.buffalo.cse.jive.ui.view.model.trace;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_LIST;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.ui.IDiagramOutputActionFactory;
import edu.buffalo.cse.jive.ui.IStepAction;
import edu.buffalo.cse.jive.ui.IStepManager;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;
import edu.buffalo.cse.jive.ui.view.AbstractStructuredJiveView;
import edu.buffalo.cse.jive.ui.view.JiveTableViewer;

/**
 * The view is in tabular form with columns for the thread in which the event occurs, the event
 * number, the event name, and the details of the event.
 */
public class TraceView extends AbstractStructuredJiveView
{
  private static final IJiveDebugTarget activeTarget()
  {
    return JiveLaunchPlugin.getDefault().getLaunchManager().activeTarget();
  }

  public TraceView()
  {
    super();
  }

  @Override
  public TableViewer getViewer()
  {
    return (TableViewer) super.getViewer();
  }

  @Override
  protected IStructuredContentProvider createContentProvider()
  {
    return new TraceContentProvider(getViewer());
  }

  @Override
  protected StructuredViewer createViewer(final Composite parent)
  {
    return JiveTableViewer.configureTraceTable(new JiveTableViewer(parent));
  }

  @Override
  protected void fillContextMenu(final IMenuManager manager)
  {
    final IDiagramOutputActionFactory factory = JiveUIPlugin.getDefault()
        .getDiagramOutputActionFactory();
    manager.add(factory.createTraceCopyEventsAction(getViewer(), false));
    manager.add(factory.createTraceCopyEventsAction(getViewer(), true));
    // Add run to event action
    final IStructuredSelection selection = (IStructuredSelection) getViewer().getSelection();
    final Object element = selection.getFirstElement();
    if (element instanceof IJiveEvent)
    {
      final IJiveEvent event = (IJiveEvent) element;
      final IJiveDebugTarget target = TraceView.activeTarget();
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

  @Override
  protected String getDefaultContentDescription()
  {
    return "No trace events to display at this time.";
  }

  @Override
  protected ImageDescriptor getDisplayDropDownDisabledImageDescriptor()
  {
    return IM_BASE_LIST.disabledDescriptor();
  }

  @Override
  protected ImageDescriptor getDisplayDropDownEnabledImageDescriptor()
  {
    return IM_BASE_LIST.enabledDescriptor();
  }

  @Override
  protected String getDisplayTargetDropDownText()
  {
    return "Display Trace";
  }
}