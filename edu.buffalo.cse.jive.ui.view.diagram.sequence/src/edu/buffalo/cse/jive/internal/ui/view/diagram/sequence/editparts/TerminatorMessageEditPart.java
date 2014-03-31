package edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionEndpointLocator;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.jface.preference.IPreferenceStore;

import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.Message.MessageKind;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.Message.TerminatorMessage;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.figures.MessageFigure;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IEventModel.IExceptionThrowEvent;
import edu.buffalo.cse.jive.model.IEventModel.IThreadStartEvent;
import edu.buffalo.cse.jive.preferences.PreferenceKeys;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;
import edu.buffalo.cse.jive.ui.IJiveEditPart;

public class TerminatorMessageEditPart extends AbstractConnectionEditPart implements IJiveEditPart
{
  private void addTargetLabel(final Connection connection)
  {
    final SequenceDiagramEditPart contents = (SequenceDiagramEditPart) getRoot().getContents();
    final TerminatorMessage message = (TerminatorMessage) getModel();
    if (contents.isCollapsed(message.target()))
    {
      final Label label = new Label("+");
      label.setForegroundColor(ColorConstants.gray);
      final ConnectionEndpointLocator endpointLocator = new ConnectionEndpointLocator(connection,
          true);
      endpointLocator.setUDistance(5);
      endpointLocator.setVDistance(-4);
      connection.add(label, endpointLocator);
    }
  }

  @Override
  protected void createEditPolicies()
  {
    // installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new NonResizableEditPolicy());
  }

  @Override
  protected IFigure createFigure()
  {
    final TerminatorMessage message = (TerminatorMessage) getModel();
    final boolean isException = message.source().terminator() instanceof IExceptionThrowEvent;
    PolylineConnection figure = null;
    if (message.kind() == MessageKind.MK_LOST_BROKEN)
    {
      figure = MessageFigure.createBrokenTerminatorMessageFigure(false, isException);
    }
    if (message.kind() == MessageKind.MK_LOST)
    {
      figure = MessageFigure.createLostMessageFigure(false, isException);
    }
    final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
    final boolean showExpandedLifeLines = store.getBoolean(PreferenceKeys.PREF_SD_EXPAND_LIFELINES);
    if (message.target() instanceof IThreadStartEvent)
    {
      // target is a thread but thread activations are not visible
      if (!store.getBoolean(PreferenceKeys.PREF_SD_SHOW_THREAD_ACTIVATIONS))
      {
        figure = MessageFigure.createLostMessageFigure(false, isException);
      }
    }
    // default-- source and target are visible and in-model
    if (figure == null)
    {
      final IContextContour sourceContext = message.source().executionContext();
      final IContextContour targetContext = message.target().executionContext();
      // messages between activations on the same context need not explicit arrows
      final boolean isTransparent = !showExpandedLifeLines ? (sourceContext == null ? sourceContext
          : sourceContext.concreteContour()) == (targetContext == null ? targetContext
          : targetContext.concreteContour()) : (sourceContext == targetContext);
      figure = MessageFigure.createTerminatorMessageFigure(isTransparent, isException);
    }
    // place the target label on the figure
    addTargetLabel(figure);
    return figure;
  }
}