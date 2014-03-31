package edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

import edu.buffalo.cse.jive.preferences.PreferencesPlugin;

public class EventOccurrenceFigure extends Figure
{
  public EventOccurrenceFigure(final String toolTipText, final Image toolTipIcon)
  {
    super();
    final int width = PreferencesPlugin.getDefault().getActivationWidth();
    final int height = PreferencesPlugin.getDefault().eventHeight();
    setBounds(new Rectangle(-1, -1, width, height));
    setOpaque(true);
    setBackgroundColor(ColorConstants.red);
    final Label tooltip = new Label(toolTipText, toolTipIcon);
    setToolTip(tooltip);
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
}