package edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.figures;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import edu.buffalo.cse.jive.internal.ui.ContourAttributesFactory;
import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.ISystemStartEvent;
import edu.buffalo.cse.jive.model.IEventModel.IThreadStartEvent;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;
import edu.buffalo.cse.jive.ui.IContourAttributes;

public class ExecutionOccurrenceFigure extends RectangleFigure
{
  private final boolean isGutter;

  public ExecutionOccurrenceFigure(final IInitiatorEvent execution, final Color color)
  {
    super();
    this.isGutter = execution instanceof ISystemStartEvent;
    final int width = isGutter ? 0 : PreferencesPlugin.getDefault().getActivationWidth();
    final int height = (int) execution.duration() * PreferencesPlugin.getDefault().eventHeight();
    if (!isGutter)
    {
      final IContourAttributes attributes = createAttributes(execution, color);
      setToolTip(new Label(attributes.getToolTipText(), attributes.getToolTipIcon()));
      setBackgroundColor(color);
    }
    setBounds(new Rectangle(-1, -1, width, height));
    setOpaque(false);
    setOutline(false);
    setLayoutManager(new XYLayout());
    final Rectangle constraint = new Rectangle(width, height, -1, -1);
    add(new Figure(), constraint); // Ensures the figure has the correct dimensions
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

  private IContourAttributes createAttributes(final IInitiatorEvent initiator, final Color color)
  {
    if (initiator instanceof IThreadStartEvent)
    {
      return ContourAttributesFactory.createThreadAttributes(
          ((IThreadStartEvent) initiator).thread(), color);
    }
    return ContourAttributesFactory.createMethodAttributes(initiator.execution(), color);
  }
}