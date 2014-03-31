package edu.buffalo.cse.jive.ui.view.diagram.contour;

import org.eclipse.draw2d.IFigure;

import edu.buffalo.cse.jive.internal.ui.view.contour.figures.ContourFigureFactory;
import edu.buffalo.cse.jive.internal.ui.view.contour.graph.ContourGraphFactory;
import edu.buffalo.cse.jive.internal.ui.view.contour.graph.IGraphLayout;

public enum ContourDiagramFactory
{
  HIERARCHICAL,
  TABULAR;
  private static ContourDiagramFactory INSTANCE = HIERARCHICAL;

  public static IFigure createContourDigramFigure()
  {
    if (ContourDiagramFactory.INSTANCE == HIERARCHICAL)
    {
      return ContourFigureFactory.createHierarchicalContourFigure();
    }
    return ContourFigureFactory.createTabularContourFigure();
  }

  public static IGraphLayout createLayout()
  {
    if (ContourDiagramFactory.INSTANCE == HIERARCHICAL)
    {
      return ContourGraphFactory.createHierarchicalLayout();
    }
    return ContourGraphFactory.createTabularLayout();
  }

  //
  // public IHierarchicalPosition createPosition(final DiagramSection section,
  // final IHierarchicalPosition parent, final int column) {
  //
  // return ContourGraphFactory.createHierarchicalPosition(section, parent, column);
  // }
  // public INodePosition createPosition(final DiagramSection section, final int column,
  // final int layer, final int cell) {
  //
  // return ContourGraphFactory.createTabularPosition(section, column, layer, cell);
  // }
  public static void setHierarchical()
  {
    ContourDiagramFactory.INSTANCE = HIERARCHICAL;
  }

  public static void setTabular()
  {
    ContourDiagramFactory.INSTANCE = TABULAR;
  }
}
