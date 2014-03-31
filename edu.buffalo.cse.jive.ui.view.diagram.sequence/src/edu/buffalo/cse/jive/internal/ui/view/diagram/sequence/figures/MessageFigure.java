package edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.figures;

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.ConnectionEndpointLocator;
import org.eclipse.draw2d.Ellipse;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.MidpointLocator;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.PolylineDecoration;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;

import edu.buffalo.cse.jive.internal.ui.CustomLineBorder;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;

public class MessageFigure
{
  private static final Border MESSAGE_BREAK_BORDER = new CustomLineBorder(0, 1, 0, 1);

  public static PolylineConnection createBrokenInitiatorMessageFigure(final boolean isTransparent)
  {
    return new BrokenInitiatorMessageFigure(isTransparent);
  }

  public static PolylineConnection createBrokenTerminatorMessageFigure(final boolean isTransparent,
      final boolean isException)
  {
    return new BrokenTerminatorMessageFigure(isTransparent, isException);
  }

  public static PolylineConnection createFoundMessageFigure(final boolean isTransparent)
  {
    return new FoundMessageFigure(isTransparent);
  }

  public static PolylineConnection createInitiatorMessageFigure(final boolean isTransparent)
  {
    return new InitiatorMessageFigure(isTransparent);
  }

  public static PolylineConnection createLostMessageFigure(final boolean isTransparent,
      final boolean isException)
  {
    return new LostMessageFigure(isTransparent, isException);
  }

  public static PolylineConnection createTerminatorMessageFigure(final boolean isTransparent,
      final boolean isException)
  {
    return new TerminatorMessageFigure(isTransparent, isException);
  }

  private abstract static class AbstractMessageFigure extends PolylineConnection
  {
    AbstractMessageFigure()
    {
      super();
    }

    @Override
    public void paint(final Graphics graphics)
    {
      graphics.setAntialias(SWT.ON);
      graphics.setTextAntialias(SWT.ON);
      super.paint(graphics);
      graphics.setAntialias(SWT.OFF);
      graphics.setTextAntialias(SWT.OFF);
    }

    protected void addBreak()
    {
      final RectangleFigure messageBreak = new RectangleFigure();
      messageBreak.setBackgroundColor(ColorConstants.white);
      messageBreak.setOutline(false);
      final Rectangle bounds = Rectangle.SINGLETON;
      bounds.x = -1;
      bounds.y = -1;
      bounds.width = 4;
      bounds.height = 13;
      messageBreak.setBounds(bounds);
      messageBreak.setBorder(MessageFigure.MESSAGE_BREAK_BORDER);
      add(messageBreak, new MidpointLocator(this, 0));
    }
  }

  private static class BrokenInitiatorMessageFigure extends InitiatorMessageFigure
  {
    BrokenInitiatorMessageFigure(final boolean isTransparent)
    {
      super(isTransparent);
      addBreak();
    }
  }

  private static class BrokenTerminatorMessageFigure extends TerminatorMessageFigure
  {
    BrokenTerminatorMessageFigure(final boolean isTransparent, final boolean isException)
    {
      super(isTransparent, isException);
      addBreak();
    }
  }

  private static class FoundMessageFigure extends InitiatorMessageFigure
  {
    FoundMessageFigure(final boolean isTransparent)
    {
      super(isTransparent);
      final Ellipse circle = new Ellipse();
      circle.setBackgroundColor(ColorConstants.lightGray);
      circle.setLineWidth(1);
      final Rectangle bounds = Rectangle.SINGLETON;
      final int diameter = PreferencesPlugin.getDefault().eventHeight();
      bounds.x = 0;
      bounds.y = 0;
      bounds.width = diameter;
      bounds.height = diameter;
      circle.setBounds(bounds);
      final ConnectionEndpointLocator locator = new ConnectionEndpointLocator(this, false);
      locator.setUDistance(0);
      locator.setVDistance(0);
      add(circle, locator);
    }
  }

  private static class InitiatorMessageFigure extends AbstractMessageFigure
  {
    InitiatorMessageFigure(final boolean isTransparent)
    {
      super();
      setOpaque(!isTransparent);
      setLineStyle(SWT.LINE_SOLID);
      setForegroundColor(isTransparent ? ColorConstants.white : ColorConstants.gray);
      setLineWidthFloat(1.15f);
      // omit decoration for transparent figures
      if (!isTransparent)
      {
        final PolygonDecoration decoration = new PolygonDecoration();
        decoration.setScale(8, 3);
        setTargetDecoration(decoration);
      }
    }

    @Override
    public void paint(final Graphics graphics)
    {
      super.paint(graphics);
    }
  }

  private static class LostMessageFigure extends TerminatorMessageFigure
  {
    LostMessageFigure(final boolean isTransparent, final boolean isException)
    {
      super(isTransparent, isException);
      final Ellipse circle = new Ellipse();
      circle.setBackgroundColor(ColorConstants.lightGray);
      circle.setLineWidth(1);
      final Rectangle bounds = Rectangle.SINGLETON;
      final int diameter = PreferencesPlugin.getDefault().eventHeight();
      bounds.x = 0;
      bounds.y = 0;
      bounds.width = diameter;
      bounds.height = diameter;
      circle.setBounds(bounds);
      final ConnectionEndpointLocator locator = new ConnectionEndpointLocator(this, true);
      locator.setUDistance(0);
      locator.setVDistance(0);
      add(circle, locator);
    }
  }

  private static class TerminatorMessageFigure extends AbstractMessageFigure
  {
    TerminatorMessageFigure(final boolean isTransparent, final boolean isException)
    {
      super();
      setOpaque(!isTransparent);
      setLineStyle(SWT.LINE_DASH);
      setForegroundColor(isException ? ColorConstants.red : (isTransparent ? ColorConstants.white
          : ColorConstants.gray));
      setLineWidthFloat(1.15f);
      // omit decoration for transparent figures that are not exceptions
      if (isException || !isTransparent)
      {
        final PolylineDecoration decoration = new PolylineDecoration();
        decoration.setScale(8, 3);
        setTargetDecoration(decoration);
      }
    }
  }
}
