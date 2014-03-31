package edu.buffalo.cse.jive.internal.ui.view.contour.figures;

import java.util.Map.Entry;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FlowLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.OrderedLayout;

import edu.buffalo.cse.jive.internal.ui.view.contour.graph.INodePosition;
import edu.buffalo.cse.jive.internal.ui.view.contour.graph.INodePosition.DiagramSection;
import edu.buffalo.cse.jive.internal.ui.view.contour.graph.INodePosition.IHierarchicalPosition;

class HierarchicalContourDiagramFigure extends AbstractContourDiagramFigure
{
  HierarchicalContourDiagramFigure()
  {
    super();
  }

  private void ensurePositionExists(final IHierarchicalPosition position)
  {
    // retrieve the position's section
    final DiagramSection section = position.section();
    // retrieve the position's column
    final int column = position.column();
    // retrieve the position's parent
    final IHierarchicalPosition parent = position.parent();
    // retrieve the container figure associated with this position
    final IFigure containerFigure;
    if (parent != null)
    {
      // make sure the parent figure exists
      ensurePositionExists(parent);
      // the container is the bottom part of the parent figure's container
      containerFigure = (IFigure) positionToFigure().get(parent).getParent().getChildren().get(1);
    }
    else
    {
      // the container is the section figure
      containerFigure = (section == DiagramSection.DS_REACHABLE ? sectionReachable()
          : section == DiagramSection.DS_UNREACHABLE ? sectionUnreachable() : sectionSingletons());
    }
    // make sure the container figure has enough column figures
    while (column >= containerFigure.getChildren().size())
    {
      final IFigure columnFigure = new Figure();
      // columnFigure.setBorder(new LineBorder(1));
      final FlowLayout columnLayout = new FlowLayout(false); // vertical orientation
      columnLayout.setMinorSpacing(20);
      columnLayout.setStretchMinorAxis(true); // all layers have same height
      columnFigure.setLayoutManager(columnLayout);
      containerFigure.add(columnFigure);
      // top part, where the contour goes-- child @0
      final IFigure topFigure = new Figure();
      // topFigure.setBorder(new LineBorder(1));
      final FlowLayout topLayout = new FlowLayout(true); // horizontal orientation
      topLayout.setMinorSpacing(20);
      topLayout.setMajorAlignment(OrderedLayout.ALIGN_CENTER); // center w.r.t. parent
      topFigure.setLayoutManager(topLayout);
      columnFigure.add(topFigure);
      // bottom part, where the subgraph goes-- child @1
      final IFigure bottomFigure = new Figure();
      // bottomFigure.setBorder(new LineBorder(1));
      final FlowLayout bottomLayout = new FlowLayout(true); // horizontal orientation
      bottomLayout.setMinorSpacing(20);
      bottomLayout.setMajorAlignment(OrderedLayout.ALIGN_CENTER); // center w.r.t. parent
      bottomFigure.setLayoutManager(bottomLayout);
      columnFigure.add(bottomFigure);
    }
    // container --> column --> top figure
    final IFigure cellFigure = (IFigure) ((IFigure) containerFigure.getChildren().get(column))
        .getChildren().get(0);
    // make sure the cell figure exists in the map
    positionToFigure().put(position, cellFigure);
  }

  @Override
  protected void addNonStaticFigure(final ContourFigure figure, final INodePosition position)
  {
    // make sure a cell exists on which to place the figure
    ensurePositionExists((IHierarchicalPosition) position);
    assert positionToFigure().containsKey(position) : "No position for:  " + position;
    final IFigure cellFigure = positionToFigure().get(position);
    cellFigure.add(figure);
  }

  @Override
  protected void removeFigure(final ContourFigure figure)
  {
    final IFigure topFigure = figure.getParent();
    if (topFigure != null)
    {
      topFigure.remove(figure);
    }
    if (topFigure != sectionStatic())
    {
      // retrieve the container
      final IFigure containerFigure = topFigure.getParent();
      assert containerFigure != null : "Cannot have a top figure without a column figure.";
      containerFigure.remove(topFigure);
      // remove this figure's entry from the map
      INodePosition position = null;
      for (final Entry<INodePosition, IFigure> e : positionToFigure().entrySet())
      {
        if (e.getValue() == topFigure)
        {
          position = e.getKey();
          break;
        }
      }
      if (position != null)
      {
        positionToFigure().remove(position);
      }
      else
      {
        throw new IllegalStateException("No cell exists for the position:  " + position);
      }
      // remove this the bottom figure if it is empty
      final IFigure bottomFigure = (IFigure) containerFigure.getChildren().remove(0);
      // the container figure can be removed if the bottom figure is empty
      if (bottomFigure.getChildren().isEmpty())
      {
        containerFigure.remove(bottomFigure);
      }
      // remove an empty container that is not one of the diagram sections
      if (containerFigure.getChildren().isEmpty() && containerFigure != sectionReachable()
          && containerFigure != sectionUnreachable() && containerFigure != sectionStatic())
      {
        final IFigure parentContainer = containerFigure.getParent();
        if (parentContainer != null)
        {
          parentContainer.remove(containerFigure);
        }
      }
    }
  }
}