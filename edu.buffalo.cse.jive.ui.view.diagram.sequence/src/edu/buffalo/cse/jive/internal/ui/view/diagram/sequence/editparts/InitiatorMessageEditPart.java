package edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts;

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.ConnectionEndpointLocator;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;

import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.Message.InitiatorMessage;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.Message.MessageKind;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.figures.MessageFigure;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IThreadStartEvent;
import edu.buffalo.cse.jive.preferences.PreferenceKeys;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;
import edu.buffalo.cse.jive.ui.IJiveEditPart;

public class InitiatorMessageEditPart extends AbstractConnectionEditPart implements IJiveEditPart
{
  private static final Border LABEL_BORDER = new MarginBorder(1, 1, 1, 2);
  private static final Font FIGURE_FONT = new Font(null, new FontData("Lucida Sans, Arial", 10,
      SWT.NONE));

  private void addTargetLabel(final PolylineConnection connection, final boolean showExpandedLifeLines)
  {
    final InitiatorMessage message = (InitiatorMessage) getModel();
    final IInitiatorEvent execution = message.target();
    Label label = new Label(determineTargetLabel(execution));
    label.setForegroundColor(ColorConstants.black);
    label.setBorder(InitiatorMessageEditPart.LABEL_BORDER);
    Figure container = new Figure();
    container.setBorder(new MarginBorder(5));
    container.setForegroundColor(ColorConstants.yellow);
    container.add(label);
    //
    // must figure right_to_left arrows
    boolean isEndPoint = true;
    ConnectionEndpointLocator epl = new ConnectionEndpointLocator(connection, isEndPoint);
    epl.setUDistance(8);
    epl.setVDistance(-2);
    connection.add(label, epl);
    final SequenceDiagramEditPart contents = (SequenceDiagramEditPart) getRoot().getContents();
    if (contents.isCollapsed(execution))
    {
      label = new Label("+");
      label.setForegroundColor(ColorConstants.darkGray);
      epl = new ConnectionEndpointLocator(connection, true);
      epl.setUDistance(-16);
      epl.setVDistance(-1);
      connection.add(label, epl);
    }
  }

  private String determineTargetLabel(final IInitiatorEvent execution)
  {
    if (!execution.inModel())
    {
      return "";
    }
    if (execution instanceof IThreadStartEvent)
    {
      return execution.thread().toString();
    }
    final IMethodContour method = execution.execution();
    if (method != null && method.signature().contains("#"))
    {
      final String[] tokens = method.signature().split("#");
      String result = tokens[1];
      if (result.contains("."))
      {
        result = result.substring(result.lastIndexOf('.') + 1);
      }
      if (result.contains("$"))
      {
        result = result.substring(result.lastIndexOf('$') + 1);
      }
      return result;
    }
    return execution.toString();
  }

  @Override
  protected void createEditPolicies()
  {
    // installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new NonResizableEditPolicy());
  }

  @Override
  protected IFigure createFigure()
  {
    final InitiatorMessage message = (InitiatorMessage) getModel();
    PolylineConnection figure = null;
    if (message.kind() == MessageKind.MK_FOUND_BROKEN)
    {
      figure = MessageFigure.createBrokenInitiatorMessageFigure(false);
    }
    if (message.kind() == MessageKind.MK_FOUND)
    {
      figure = MessageFigure.createFoundMessageFigure(false);
    }
    final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
    final boolean showExpandedLifeLines = store.getBoolean(PreferenceKeys.PREF_SD_EXPAND_LIFELINES);
    if (message.source() instanceof IThreadStartEvent)
    {
      // source is a thread but thread activations are not visible
      if (!store.getBoolean(PreferenceKeys.PREF_SD_SHOW_THREAD_ACTIVATIONS))
      {
        figure = MessageFigure.createFoundMessageFigure(false);
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
      figure = MessageFigure.createInitiatorMessageFigure(isTransparent);
    }
    figure.setFont(FIGURE_FONT);
    // figure.setClippingStrategy(new IClippingStrategy()
    // {
    // @Override
    // public Rectangle[] getClip(IFigure childFigure)
    // {
    // if (childFigure instanceof Label)
    // {
    // Rectangle r = childFigure.getBounds();
    // r.width = r.width + 30;
    // return new Rectangle[]
    // { r };
    // }
    // return new Rectangle[]
    // { childFigure.getBounds() };
    // }
    // });
    // place the target label on the figure
    addTargetLabel(figure, showExpandedLifeLines);
    return figure;
  }
}
