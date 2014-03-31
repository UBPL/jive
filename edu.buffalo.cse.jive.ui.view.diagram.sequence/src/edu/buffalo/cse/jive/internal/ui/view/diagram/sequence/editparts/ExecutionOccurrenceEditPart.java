package edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.NonResizableEditPolicy;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.Message.MessageKind;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.figures.ExecutionOccurrenceFigure;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.figures.LifelineFigure;
import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.ISystemStartEvent;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;
import edu.buffalo.cse.jive.ui.IJiveEditPart;
import edu.buffalo.cse.jive.ui.IThreadColorManager;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;

public class ExecutionOccurrenceEditPart extends AbstractGraphicalEditPart implements
    IJiveEditPart, NodeEditPart
{
  @Override
  public IInitiatorEvent getModel()
  {
    return (IInitiatorEvent) super.getModel();
  }

  @Override
  public ConnectionAnchor getSourceConnectionAnchor(final ConnectionEditPart connection)
  {
    final Message message = (Message) connection.getModel();
    // this execution can be the source for multiple initiators
    if (connection instanceof InitiatorMessageEditPart)
    {
      return new MethodCallSourceConnectionAnchor();
    }
    // this execution can be the source for at most one terminator
    else if (connection instanceof TerminatorMessageEditPart)
    {
      if (message.kind() == MessageKind.MK_LOST_BROKEN)
      {
        return new MethodBrokenReturnConnectionAnchor(message);
      }
      return new MethodReturnSourceConnectionAnchor();
    }
    throw new IllegalArgumentException("Unsupported connection:  " + connection);
  }

  @Override
  public ConnectionAnchor getSourceConnectionAnchor(final Request request)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public ConnectionAnchor getTargetConnectionAnchor(final ConnectionEditPart connection)
  {
    final Message message = (Message) connection.getModel();
    // this execution can be the target for one initiator
    if (connection instanceof InitiatorMessageEditPart)
    {
      if (message.kind() == MessageKind.MK_FOUND_BROKEN)
      {
        return new MethodBrokenCallTargetConnectionAnchor(message);
      }
      return new MethodCallTargetConnectionAnchor();
    }
    // this execution can be the target for multiple terminators
    else if (connection instanceof TerminatorMessageEditPart)
    {
      return new MethodReturnTargetConnectionAnchor();
    }
    throw new IllegalArgumentException("Unsupported connection:  " + connection);
  }

  @Override
  public ConnectionAnchor getTargetConnectionAnchor(final Request request)
  {
    throw new UnsupportedOperationException();
  }

  private SequenceDiagramEditPart contents()
  {
    return (SequenceDiagramEditPart) getRoot().getContents();
  }

  @Override
  protected void createEditPolicies()
  {
    installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new NonResizableEditPolicy());
  }

  @Override
  protected IFigure createFigure()
  {
    final IThreadColorManager manager = JiveUIPlugin.getDefault().getThreadColorManager();
    final IJiveDebugTarget target = contents().getModel();
    return new ExecutionOccurrenceFigure(getModel(), manager.threadColor(target, getModel()
        .thread()));
  }

  @Override
  protected List<Long> getModelChildren()
  {
    final List<IJiveEvent> events = contents().getSearchResults(getModel());
    final List<Long> result = new ArrayList<Long>();
    for (final IJiveEvent event : events)
    {
      result.add(event.eventId());
    }
    return result; // contents().getSearchResults(getModel());
  }

  @Override
  protected List<Message> getModelSourceConnections()
  {
    return contents().getSourceMessages(getModel());
  }

  @Override
  protected List<Message> getModelTargetConnections()
  {
    return contents().getTargetMessages(getModel());
  }

  @Override
  protected void refreshVisuals()
  {
    final IInitiatorEvent execution = getModel();
    final LifelineEditPart parent = (LifelineEditPart) getParent();
    final int width = PreferencesPlugin.getDefault().getActivationWidth();
    final int eventHeight = PreferencesPlugin.getDefault().eventHeight();
    final LifelineFigure parentFigure = (LifelineFigure) parent.getFigure();
    final ExecutionOccurrenceFigure figure = (ExecutionOccurrenceFigure) getFigure();
    final Dimension headDimension = parentFigure.getLifelineHeadSize();
    final int x = execution instanceof ISystemStartEvent ? 0 : (width + 3)
        * parent.binNumber(execution) + (headDimension.width / 2) - (width / 2);
    // "+1" on the execution because the activation is displayed one event below the initiator event
    final int delta = !(execution instanceof ISystemStartEvent) ? 1 : 0;
    // final ModelAdapter adapter = contents().getModelAdapter();
    final long unitTop;
    final long unitHeight;
    // if (adapter.isRealTime()) {
    // unitTop = 1 + (((IRealTimeEvent) execution).timestamp() - adapter.systemStartTime())
    // / (100 * 1000);
    // final IJiveEvent last = execution.model().lookupEvent(
    // execution.eventId() + execution.duration());
    // unitHeight = (((IRealTimeEvent) last).timestamp() - ((IRealTimeEvent) execution).timestamp())
    // / (100 * 1000);
    // }
    // else {
    unitTop = execution.eventId();
    unitHeight = execution.duration();
    // }
    final int y = (int) (eventHeight * (delta + unitTop) + headDimension.height + 10);
    final Rectangle constraint = new Rectangle(x, y, execution instanceof ISystemStartEvent ? 1
        : width, (int) (eventHeight * (unitHeight + (execution instanceof ISystemStartEvent ? 20
        : 0))));
    parent.setLayoutConstraint(this, figure, constraint);
  }

  /**
   * A target connection anchor for a broken SyncCall message. The anchor is placed at the top
   * center of the target. The reference point is located at the same point as the anchor only
   * shifted upward so that the line is angled.
   */
  class MethodBrokenCallTargetConnectionAnchor extends ChopboxAnchor
  {
    private final Message message;

    MethodBrokenCallTargetConnectionAnchor(final Message message)
    {
      super(getFigure());
      this.message = message;
    }

    @Override
    public Point getLocation(final Point reference)
    {
      final Rectangle bounds = Rectangle.SINGLETON;
      bounds.setBounds(getBox());
      getOwner().translateToAbsolute(bounds);
      return bounds.getTopLeft();
    }

    @Override
    public Point getReferencePoint()
    {
      final Point result = getBox().getTop();
      final long start = message.target().eventId() - 1;
      final long end = message.target().eventId();
      final int offset = ((int) (end - start)) * PreferencesPlugin.getDefault().eventHeight();
      result.y = result.y - offset;
      getOwner().translateToAbsolute(result);
      return result;
    }
  }

  /**
   * A source connection anchor for a broken Reply message. The anchor is placed on the bottom left
   * corner of the source. The reference point is located at the same point as the anchor only
   * shifted downward so that the line is angled.
   */
  class MethodBrokenReturnConnectionAnchor extends ChopboxAnchor
  {
    private final Message message;

    MethodBrokenReturnConnectionAnchor(final Message message)
    {
      super(getFigure());
      this.message = message;
    }

    @Override
    public Point getLocation(final Point reference)
    {
      final Rectangle bounds = Rectangle.SINGLETON;
      bounds.setBounds(getBox());
      getOwner().translateToAbsolute(bounds);
      return bounds.getBottomLeft();
    }

    @Override
    public Point getReferencePoint()
    {
      final Point result = getBox().getBottomLeft();
      // Calculate the vertical offset from the source to the target
      final long start = message.source().eventId();
      final long end = message.source().eventId() + 1;
      final int offset = ((int) (end - start)) * PreferencesPlugin.getDefault().eventHeight();
      result.y = result.y + offset;
      getOwner().translateToAbsolute(result);
      return result;
    }
  }

  /**
   * A source connection anchor for a SyncCall message. The anchor is placed at the point where the
   * call was made.
   */
  class MethodCallSourceConnectionAnchor extends ChopboxAnchor
  {
    MethodCallSourceConnectionAnchor()
    {
      super(getFigure());
    }

    @Override
    public Point getLocation(final Point reference)
    {
      final Rectangle bounds = Rectangle.SINGLETON;
      bounds.setBounds(getBox());
      getOwner().translateToAbsolute(bounds);
      final Point result = bounds.getTopRight();
      result.x = result.x - 1;
      result.y = reference.y;
      // result.y = reference.y + 1;
      return result;
    }
  }

  /**
   * A target connection anchor for a SyncCall message. The anchor is placed at the top center of
   * the target. The reference point is the same point as the anchor's location.
   */
  class MethodCallTargetConnectionAnchor extends ChopboxAnchor
  {
    MethodCallTargetConnectionAnchor()
    {
      super(getFigure());
    }

    @Override
    public Point getLocation(final Point reference)
    {
      final Rectangle bounds = Rectangle.SINGLETON;
      bounds.setBounds(getBox());
      getOwner().translateToAbsolute(bounds);
      final Point result = bounds.getTopLeft();
      result.x = result.x - 1;
      // result.y = result.y + 1;
      return result;
    }

    @Override
    public Point getReferencePoint()
    {
      final Point result = getBox().getTop();
      getOwner().translateToAbsolute(result);
      return result;
    }
  }

  /**
   * A source connection anchor for a Reply message. The anchor is placed on the bottom left corner
   * of the source. The reference point is the same point as the anchor's location.
   */
  class MethodReturnSourceConnectionAnchor extends ChopboxAnchor
  {
    MethodReturnSourceConnectionAnchor()
    {
      super(getFigure());
    }

    @Override
    public Point getLocation(final Point reference)
    {
      final Rectangle bounds = Rectangle.SINGLETON;
      bounds.setBounds(getBox());
      getOwner().translateToAbsolute(bounds);
      final Point result = bounds.getBottomLeft();
      result.y = result.y - 1;
      return result;
    }

    @Override
    public Point getReferencePoint()
    {
      final Point result = getBox().getBottomLeft();
      getOwner().translateToAbsolute(result);
      return result;
    }
  }

  /**
   * A target connection anchor for a Reply message. The anchor is placed at the point where the
   * reply was received.
   */
  class MethodReturnTargetConnectionAnchor extends ChopboxAnchor
  {
    MethodReturnTargetConnectionAnchor()
    {
      super(getFigure());
    }

    @Override
    public Point getLocation(final Point reference)
    {
      final Rectangle bounds = Rectangle.SINGLETON;
      bounds.setBounds(getBox());
      getOwner().translateToAbsolute(bounds);
      final Point result = bounds.getTopRight();
      result.x = result.x - 1;
      result.y = reference.y - 1;
      return result;
    }
  }
}
