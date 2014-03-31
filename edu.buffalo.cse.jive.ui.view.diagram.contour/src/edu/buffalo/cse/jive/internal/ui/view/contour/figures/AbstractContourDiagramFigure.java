package edu.buffalo.cse.jive.internal.ui.view.contour.figures;

import java.util.Map;

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.ToolbarLayout;

import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.internal.ui.view.contour.graph.INodePosition;

abstract class AbstractContourDiagramFigure extends Figure
{
  /**
   * The border used around the contour diagram.
   */
  private static final Border CLIENT_AREA_BORDER = new MarginBorder(8);
  private final Map<INodePosition, IFigure> positionToFigure;
  private final IFigure reachableSection;
  private final IFigure staticSection;
  private final IFigure unreachableSection;
  private final IFigure singletonsSection;

  AbstractContourDiagramFigure()
  {
    super();
    staticSection = new Figure();
    final ToolbarLayout staticSectionLayout = new ToolbarLayout(true);
    staticSectionLayout.setSpacing(30);
    staticSection.setLayoutManager(staticSectionLayout);
    reachableSection = new Figure();
    final ToolbarLayout reachableSectionLayout = new ToolbarLayout(true);
    reachableSectionLayout.setSpacing(30);
    reachableSection.setLayoutManager(reachableSectionLayout);
    unreachableSection = new Figure();
    final ToolbarLayout unreachableSectionLayout = new ToolbarLayout(true);
    unreachableSectionLayout.setSpacing(30);
    unreachableSection.setLayoutManager(unreachableSectionLayout);
    singletonsSection = new Figure();
    final ToolbarLayout singletonsSectionLayout = new ToolbarLayout(true);
    singletonsSectionLayout.setSpacing(30);
    singletonsSection.setLayoutManager(singletonsSectionLayout);
    positionToFigure = TypeTools.newHashMap();
    final ToolbarLayout layout = new ToolbarLayout(false);
    layout.setSpacing(30);
    setLayoutManager(layout);
    setBorder(AbstractContourDiagramFigure.CLIENT_AREA_BORDER);
    add(staticSection);
    add(reachableSection);
    add(unreachableSection);
    add(singletonsSection);
  }

  @Override
  public void add(final IFigure f, final Object constraint, final int index)
  {
    if (f instanceof ContourFigure)
    {
      assert constraint instanceof INodePosition;
      final ContourFigure figure = (ContourFigure) f;
      final INodePosition position = (INodePosition) constraint;
      switch (position.section())
      {
        case DS_STATIC:
          addStaticFigure(figure, position);
          break;
        case DS_REACHABLE:
          addNonStaticFigure(figure, position);
          break;
        case DS_UNREACHABLE:
          addNonStaticFigure(figure, position);
          break;
        case DS_SINGLETONS:
          addNonStaticFigure(figure, position);
          break;
      }
    }
    else
    {
      super.add(f, constraint, index);
    }
  }

  @Override
  public void remove(final IFigure figure)
  {
    if (figure instanceof ContourFigure)
    {
      removeFigure((ContourFigure) figure);
    }
    else
    {
      super.remove(figure);
    }
  }

  protected abstract void addNonStaticFigure(final ContourFigure figure,
      final INodePosition position);

  protected void addStaticFigure(final ContourFigure figure, final INodePosition position)
  {
    sectionStatic().add(figure);
  }

  protected Map<INodePosition, IFigure> positionToFigure()
  {
    return positionToFigure;
  }

  protected abstract void removeFigure(final ContourFigure figure);

  protected IFigure sectionReachable()
  {
    return reachableSection;
  }

  protected IFigure sectionSingletons()
  {
    return singletonsSection;
  }

  protected IFigure sectionStatic()
  {
    return staticSection;
  }

  protected IFigure sectionUnreachable()
  {
    return unreachableSection;
  }
}
