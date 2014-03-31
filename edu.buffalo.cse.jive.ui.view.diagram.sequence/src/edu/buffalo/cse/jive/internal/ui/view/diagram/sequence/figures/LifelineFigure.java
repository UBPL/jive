package edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.figures;

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.CompoundBorder;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;

import edu.buffalo.cse.jive.ui.IContourAttributes;

public class LifelineFigure extends Figure
{
  private static final Border HEAD_BORDER = new CompoundBorder(new LineBorder(1), new MarginBorder(
      4, 3, 4, 8));
  private static final int[] LINE_DASH_PATTERN = new int[]
  { 10, 5 };
  private final Label head;
  private final boolean isGutter;

  public LifelineFigure(final IContourAttributes attributes)
  {
    isGutter = (attributes == null);
    if (isGutter)
    {
      head = new Label("");
      head.setOpaque(false);
      head.setBorder(new MarginBorder(5));
    }
    else
    {
      head = new Label(attributes.getText(), attributes.getIcon());
      final Label tooltip = new Label(attributes.getToolTipText(), attributes.getToolTipIcon());
      head.setToolTip(tooltip);
      head.setToolTip(tooltip);
      head.setOpaque(true);
      head.setBorder(LifelineFigure.HEAD_BORDER);
      head.setIconAlignment(PositionConstants.BOTTOM);
      head.setLabelAlignment(PositionConstants.LEFT);
      head.setBackgroundColor(attributes.getLabelBackgroundColor());
    }
    setOpaque(false);
    setLayoutManager(new XYLayout());
    add(head, new Rectangle(0, 0, -1, -1));
  }

  public Dimension getLifelineHeadSize()
  {
    return head.getPreferredSize();
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

  @Override
  protected void paintFigure(final Graphics graphics)
  {
    super.paintFigure(graphics);
    graphics.setLineWidthFloat(1.25f);
    if (!isGutter)
    {
      // graphics.setLineWidth(1);
      graphics.setLineDash(LifelineFigure.LINE_DASH_PATTERN);
      graphics.setForegroundColor(ColorConstants.lightGray);
      final Rectangle bounds = getBounds();
      final Point top = head.getBounds().getBottom();
      final Point bottom = bounds.getBottom();
      bottom.x = top.x;
      graphics.drawLine(top, bottom);
    }
  }
}