package edu.buffalo.cse.jive.internal.ui.view.contour.figures;

import org.eclipse.draw2d.IFigure;

public final class ContourFigureFactory
{
  public static IFigure createHierarchicalContourFigure()
  {
    return new HierarchicalContourDiagramFigure();
  }

  public static IFigure createTabularContourFigure()
  {
    return new TabularContourDiagramFigure();
  }
}
