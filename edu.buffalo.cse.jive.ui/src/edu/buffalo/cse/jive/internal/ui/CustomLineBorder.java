package edu.buffalo.cse.jive.internal.ui;

import org.eclipse.draw2d.AbstractBorder;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

public class CustomLineBorder extends AbstractBorder
{
  private final int bottom;
  private final int left;
  private final int right;
  private final int top;

  public CustomLineBorder(final int top, final int left, final int bottom, final int right)
  {
    // super(Math.max(Math.max(top, bottom), Math.max(left, right)));
    this.top = top;
    this.left = left;
    this.bottom = bottom;
    this.right = right;
  }

  @Override
  public Insets getInsets(final IFigure figure)
  {
    return new Insets(top, left, bottom, right);
  }

  @Override
  public void paint(final IFigure figure, final Graphics graphics, final Insets insets)
  {
    final Rectangle paintArea = AbstractBorder.getPaintRectangle(figure, insets);
    graphics.setForegroundColor(ColorConstants.gray);
    if (top > 0)
    {
      graphics.setLineWidth(top);
      graphics.drawLine(paintArea.getTopLeft(), paintArea.getTopRight());
    }
    if (left > 0)
    {
      graphics.setLineWidth(left);
      graphics.drawLine(paintArea.getTopLeft(), paintArea.getBottomLeft());
    }
    if (bottom > 0)
    {
      graphics.setLineWidth(top);
      final Point bottomLeft = paintArea.getBottomLeft();
      bottomLeft.y--;
      final Point bottomRight = paintArea.getBottomRight();
      bottomRight.y--;
      graphics.drawLine(bottomLeft, bottomRight);
    }
    if (right > 0)
    {
      graphics.setLineWidth(right);
      final Point topRight = paintArea.getTopRight();
      topRight.x--;
      final Point bottomRight = paintArea.getBottomRight();
      bottomRight.x--;
      graphics.drawLine(topRight, bottomRight);
    }
  }
}
