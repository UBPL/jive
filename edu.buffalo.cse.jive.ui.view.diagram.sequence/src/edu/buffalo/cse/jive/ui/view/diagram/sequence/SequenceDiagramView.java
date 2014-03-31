package edu.buffalo.cse.jive.ui.view.diagram.sequence;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_TREE;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.ImageDescriptor;

import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.actions.SequenceDiagramActionFactory;
import edu.buffalo.cse.jive.ui.IJiveGraphicalView;
import edu.buffalo.cse.jive.ui.view.AbstractGraphicalJiveView;

public class SequenceDiagramView extends AbstractGraphicalJiveView implements IJiveGraphicalView
{
  private IAction expandLifelinesAction;
  private IAction showThreadActivationsAction;
  private final SequenceDiagramActionFactory factory;

  public SequenceDiagramView()
  {
    super();
    this.factory = new SequenceDiagramActionFactory(this);
  }

  @Override
  protected void configurePullDownMenu(final IMenuManager manager)
  {
    manager.add(showThreadActivationsAction);
    manager.add(expandLifelinesAction);
  }

  @Override
  protected void createActions()
  {
    super.createActions();
    showThreadActivationsAction = factory.createShowThreadActivationsAction();
    expandLifelinesAction = factory.createExpandLifelinesAction();
  }

  @Override
  protected ContextMenuProvider createContextMenuProvider()
  {
    return new SequenceDiagramContextMenuProvider(this);
  }

  @Override
  protected EditPartFactory createEditPartFactory()
  {
    return new SequenceDiagramEditPartFactory();
  }

  @Override
  protected GraphicalViewer createGraphicalViewer()
  {
    return new ScrollingGraphicalViewer();
  }

  @Override
  protected String getDefaultContentDescription()
  {
    return "No sequence diagrams to display at this time.";
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
    return "Display Sequence Diagram";
  }
}