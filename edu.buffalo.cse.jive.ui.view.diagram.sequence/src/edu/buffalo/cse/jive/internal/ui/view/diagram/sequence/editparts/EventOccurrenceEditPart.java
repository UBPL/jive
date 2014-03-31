package edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.jface.preference.IPreferenceStore;

import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.figures.EventOccurrenceFigure;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.preferences.PreferenceKeys;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;
import edu.buffalo.cse.jive.ui.IJiveEditPart;
import edu.buffalo.cse.jive.ui.view.JiveEventLabelProvider;

public class EventOccurrenceEditPart extends AbstractGraphicalEditPart implements IJiveEditPart
{
  private final JiveEventLabelProvider labelProvider = new JiveEventLabelProvider();

  public IJiveEvent getEvent()
  {
    final ExecutionOccurrenceEditPart parent = (ExecutionOccurrenceEditPart) getParent();
    return parent.getModel().model().lookupEvent(getModel());
  }

  @Override
  public Long getModel()
  {
    return (Long) super.getModel();
  }

  @Override
  protected void createEditPolicies()
  {
    // installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new NonResizableEditPolicy());
    // TODO Determine what other edit policies should be added, if any
  }

  @Override
  protected IFigure createFigure()
  {
    final IJiveEvent event = getEvent();
    return new EventOccurrenceFigure(labelProvider.getText(event), labelProvider.getImage(event));
  }

  @Override
  protected void refreshVisuals()
  {
    // final SequenceDiagramEditPart contents = (SequenceDiagramEditPart) getRoot().getContents();
    final IJiveEvent event = getEvent();
    final ExecutionOccurrenceEditPart parent = (ExecutionOccurrenceEditPart) getParent();
    final EventOccurrenceFigure figure = (EventOccurrenceFigure) getFigure();
    final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
    final int eventHeight = store.getInt(PreferenceKeys.PREF_SD_EVENT_HEIGHT);
    final int eventPosition = (int) event.eventId();
    // "-1" because the first event on the activation is actually an "entered" event
    final int delta = event.parent() instanceof IMethodCallEvent ? 1 : 0;
    final int y = (int) (eventHeight * (eventPosition - parent.getModel().eventId() - delta));
    final Rectangle constraint = new Rectangle(0, y, -1, -1);
    parent.setLayoutConstraint(this, figure, constraint);
  }
}
