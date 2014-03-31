package edu.buffalo.cse.jive.internal.ui.view.contour.figures;

import java.util.Map.Entry;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FlowLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.OrderedLayout;

import edu.buffalo.cse.jive.internal.ui.view.contour.graph.INodePosition;
import edu.buffalo.cse.jive.internal.ui.view.contour.graph.INodePosition.DiagramSection;
import edu.buffalo.cse.jive.internal.ui.view.contour.graph.INodePosition.ITabularPosition;

class TabularContourDiagramFigure extends AbstractContourDiagramFigure
{
  TabularContourDiagramFigure()
  {
    super();
  }

  private void ensurePositionExists(final ITabularPosition position)
  {
    final DiagramSection section = position.section();
    // retrieve the position's column
    final int column = position.column();
    // retrieve the section figure where the column figure must exist
    final IFigure sectionFigure = (section == DiagramSection.DS_REACHABLE ? sectionReachable()
        : sectionUnreachable());
    // make sure the section figure has enough column figures
    while (column >= sectionFigure.getChildren().size())
    {
      final IFigure columnFigure = new Figure();
      final FlowLayout layout = new FlowLayout(false); // vertical orientation
      layout.setMinorSpacing(20);
      layout.setStretchMinorAxis(true); // all layers have same height
      columnFigure.setLayoutManager(layout);
      sectionFigure.add(columnFigure);
    }
    // retrieve the position's layer
    final int layer = position.layer();
    // retrieve the column figure where the layer figure must exist
    final IFigure columnFigure = (IFigure) sectionFigure.getChildren().get(column);
    // make sure the column figure has enough layer figures
    while (layer >= columnFigure.getChildren().size())
    {
      final IFigure layerFigure = new Figure();
      final FlowLayout layout = new FlowLayout(true); // horizontal orientation
      layout.setMinorSpacing(20);
      layout.setMajorAlignment(OrderedLayout.ALIGN_CENTER); // center w.r.t. parent
      layerFigure.setLayoutManager(layout);
      columnFigure.add(layerFigure);
    }
    // retrieve the position's cell
    final int cell = position.cell();
    // retrieve the layer figure where the cell figure must exist
    final IFigure layerFigure = (IFigure) columnFigure.getChildren().get(layer);
    // make sure the layer figure has enough cells
    while (cell >= layerFigure.getChildren().size())
    {
      final IFigure cellFigure = new Figure();
      cellFigure.setLayoutManager(new FlowLayout(true)); // horizontal orientation
      layerFigure.add(cellFigure);
    }
    // retrieve the cell figure
    final IFigure cellFigure = (IFigure) layerFigure.getChildren().get(cell);
    // make sure the cell figure exists in the map
    positionToFigure().put(position, cellFigure);
  }

  @Override
  protected void addNonStaticFigure(final ContourFigure figure, final INodePosition position)
  {
    // make sure a cell exists on which to place the figure
    ensurePositionExists((ITabularPosition) position);
    assert positionToFigure().containsKey(position) : "No position for:  " + position;
    final IFigure cellFigure = positionToFigure().get(position);
    cellFigure.add(figure);
  }

  @Override
  protected void removeFigure(final ContourFigure figure)
  {
    final IFigure parentFigure = figure.getParent();
    if (parentFigure != null)
    {
      parentFigure.remove(figure);
    }
    if (parentFigure != sectionStatic())
    {
      final IFigure cellFigure = parentFigure;
      final IFigure layerFigure = cellFigure.getParent();
      assert layerFigure != null : "Cannot have a cell figure without a containing layer figure.";
      layerFigure.remove(cellFigure);
      if (layerFigure.getChildren().size() == 0)
      {
        final IFigure columnFigure = layerFigure.getParent();
        columnFigure.remove(layerFigure);
        if (columnFigure.getChildren().size() == 0)
        {
          final IFigure sectionFigure = columnFigure.getParent();
          sectionFigure.remove(columnFigure);
        }
        INodePosition position = null;
        for (final Entry<INodePosition, IFigure> e : positionToFigure().entrySet())
        {
          if (e.getValue() == cellFigure)
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
          throw new IllegalStateException("No layer exists for the position:  " + position);
        }
      }
    }
  }
}