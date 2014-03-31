package edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.figures;

import java.util.List;

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;

import edu.buffalo.cse.jive.preferences.PreferenceKeys;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;

public class SequenceDiagramFigure extends Figure
{
  private static final Border DIAGRAM_BORDER = new MarginBorder(10);
  private static final int[] LINE_DASH_PATTERN = new int[]
  { 10, 5 };
  private int currentEventNumber;

  /**
   * Constructs the sequence diagram.
   */
  public SequenceDiagramFigure()
  {
    super();
    currentEventNumber = 1;
    final ToolbarLayout layout = new ToolbarLayout(true);
    layout.setStretchMinorAxis(true);
    layout.setSpacing(10);
    setLayoutManager(layout);
    setBorder(SequenceDiagramFigure.DIAGRAM_BORDER);
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

  public int setCurrentEventNumber(final int eventId)
  {
    final int oldEventNumber = currentEventNumber;
    currentEventNumber = (eventId <= 0 ? 1 : eventId);
    return (currentEventNumber - oldEventNumber);
  }

  @Override
  protected void paintBorder(final Graphics graphics)
  {
    super.paintBorder(graphics);
    final List<?> children = getChildren();
    if (children.size() > 1)
    {
      final LifelineFigure child = (LifelineFigure) children.get(1);
      final Dimension headDimension = child.getLifelineHeadSize();
      final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
      final int eventHeight = store.getInt(PreferenceKeys.PREF_SD_EVENT_HEIGHT);
      final int y = eventHeight * currentEventNumber + headDimension.height + 10;
      final Point p1 = getClientArea().getTopLeft();
      p1.y += y;
      final Point p2 = getClientArea().getTopRight();
      p2.y += y;
      graphics.setLineWidthFloat(1.25f);
      graphics.setLineDash(SequenceDiagramFigure.LINE_DASH_PATTERN);
      graphics.drawLine(p1, p2);
    }
  }
}
