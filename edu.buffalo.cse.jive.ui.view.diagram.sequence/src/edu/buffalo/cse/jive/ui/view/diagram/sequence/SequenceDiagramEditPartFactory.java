package edu.buffalo.cse.jive.ui.view.diagram.sequence;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.EventOccurrenceEditPart;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.ExecutionOccurrenceEditPart;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.Gutter;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.InitiatorMessageEditPart;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.LifelineEditPart;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.Message.InitiatorMessage;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.Message.TerminatorMessage;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.SequenceDiagramEditPart;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.TerminatorMessageEditPart;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;

class SequenceDiagramEditPartFactory implements EditPartFactory
{
  private static final EditPart EMPTY_EDIT_PART = new AbstractGraphicalEditPart()
    {
      @Override
      protected void createEditPolicies()
      {
        // TODO Determine if this should be implemented
      }

      @Override
      protected IFigure createFigure()
      {
        return new Figure();
      }
    };

  @Override
  public EditPart createEditPart(final EditPart context, final Object model)
  {
    if (model == null)
    {
      // System.err.println("returning empty edit part for context: " + context);
      return SequenceDiagramEditPartFactory.EMPTY_EDIT_PART;
    }
    if (model instanceof IJiveDebugTarget)
    {
      final EditPart editPart = new SequenceDiagramEditPart();
      editPart.setModel(model);
      return editPart;
    }
    if (model instanceof IContextContour || model instanceof IThreadValue
        || model instanceof Gutter)
    {
      final EditPart editPart = new LifelineEditPart();
      editPart.setModel(model);
      return editPart;
    }
    if (model instanceof IInitiatorEvent)
    {
      final EditPart editPart = new ExecutionOccurrenceEditPart();
      editPart.setModel(model);
      return editPart;
    }
    if (model instanceof Long)
    {
      final EditPart editPart = new EventOccurrenceEditPart();
      editPart.setModel(model);
      return editPart;
    }
    if (model instanceof InitiatorMessage)
    {
      final EditPart editPart = new InitiatorMessageEditPart();
      editPart.setModel(model);
      return editPart;
    }
    if (model instanceof TerminatorMessage)
    {
      final EditPart editPart = new TerminatorMessageEditPart();
      editPart.setModel(model);
      return editPart;
    }
    throw new IllegalArgumentException("Unknown element type:  " + model.getClass());
  }
}