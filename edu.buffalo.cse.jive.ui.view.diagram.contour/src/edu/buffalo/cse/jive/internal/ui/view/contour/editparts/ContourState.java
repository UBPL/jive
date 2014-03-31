package edu.buffalo.cse.jive.internal.ui.view.contour.editparts;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.swt.graphics.Color;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.internal.ui.ContourAttributesFactory;
import edu.buffalo.cse.jive.internal.ui.view.contour.figures.ContourFigure;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.ui.IContourAttributes;
import edu.buffalo.cse.jive.ui.IThreadColorManager;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;
import edu.buffalo.cse.jive.ui.VisualStatus;

/**
 * An interface representing the visual state of a contour.
 * 
 * @see ContourEditPart
 */
abstract class ContourState
{
  static ContourState createMinimizedState(final ContourEditPart editPart)
  {
    return new ContourStateMinimized(editPart);
  }

  static ContourState createObjectState(final ContourEditPart editPart)
  {
    return new ContourStateObject(editPart);
  }

  static ContourState createStackedState(final ContourEditPart editPart)
  {
    return new ContourStateStacked(editPart);
  }

  private final ContourEditPart editPart;

  private ContourState(final ContourEditPart editPart)
  {
    this.editPart = editPart;
  }

  protected IContourAttributes contourFigureAttributes(final IContour contour)
  {
    if (contour instanceof IContextContour)
    {
      final IContextContour cc = (IContextContour) contour;
      if (cc.isStatic())
      {
        return ContourAttributesFactory.createStaticAttributes(cc);
      }
      return ContourAttributesFactory.createInstanceAttributes(cc);
    }
    if (contour instanceof IMethodContour)
    {
      final IMethodContour methodContour = (IMethodContour) contour;
      final IJiveDebugTarget target = editPart.getDiagramEditPart().getModel();
      final IThreadColorManager manager = JiveUIPlugin.getDefault().getThreadColorManager();
      final Color color = manager.threadColor(target, methodContour.thread());
      return ContourAttributesFactory.createMethodAttributes((IMethodContour) contour, color);
    }
    return null;
  }

  protected ContourFigure createFullBuilder(final IContour contour)
  {
    final IContourAttributes attributes = contourFigureAttributes(contour);
    final ContourFigure figure = new ContourFigure(VisualStatus.FULL, attributes);
    return figure;
  }

  protected IExecutionModel executionModel()
  {
    return editPart.executionModel();
  }

  protected void exposeMembers(final IContour contour, final ContourFigure figure)
  {
    for (final IContourMember member : contour.members())
    {
      figure.getMemberTable().addMember(
          ContourAttributesFactory.createMemberAttributes(member,
              contour.schema().kind() == NodeKind.NK_ARRAY, executionModel().temporalState()
                  .event().eventId()));
    }
  }

  protected boolean focusCallPath()
  {
    return editPart.focusCallPath();
  }

  protected ContourEditPart getEditPart()
  {
    return editPart;
  }

  // Recursive method to determine if contour contains a method
  protected boolean hasMethod(final IContour contour)
  {
    if (contour == null)
    {
      return true;
    }
    if (contour instanceof IMethodContour)
    {
      return true;
    }
    for (final IContour child : contour.children())
    {
      if (hasMethod(child))
      {
        return true;
      }
    }
    return false;
  }

  protected boolean showMemberTables()
  {
    return editPart.showMemberTables();
  }

  abstract IFigure createFigure();

  IFigure getFigure()
  {
    return editPart.getFigure();
  }

  Object getModel()
  {
    return editPart.getModel();
  }

  ConnectionAnchor getSourceConnectionAnchor(final ConnectionEditPart connection)
  {
    return new ChopboxAnchor(getFigure());
  }

  void refreshVisuals()
  {
    // do nothing
  }

  /**
   * A class representing an object contour. It displays the contour as a labeled dot.
   * 
   * @see ContourState
   * @see ContourEditPart
   */
  private static class ContourStateMinimized extends ContourState
  {
    private ContourStateMinimized(final ContourEditPart editPart)
    {
      super(editPart);
    }

    private VisualStatus contourFigureState(final IContour contour)
    {
      if (contour instanceof IContextContour)
      {
        final IContextContour cc = (IContextContour) contour;
        if (cc.isStatic())
        {
          return VisualStatus.FULL;
        }
        if (cc.isVirtual())
        {
          return VisualStatus.EMPTY;
        }
        return VisualStatus.NODE;
      }
      if (contour instanceof IMethodContour)
      {
        return VisualStatus.EMPTY;
      }
      return null;
    }

    @Override
    IFigure createFigure()
    {
      final IContour contour = (IContour) getModel();
      final VisualStatus figureStatus = contourFigureState(contour);
      if (figureStatus == VisualStatus.EMPTY)
      {
        return new ContourFigure(figureStatus, null);
      }
      else
      {
        final IExecutionModel model = executionModel();
        model.readLock();
        try
        {
          final IContourAttributes attributes = contourFigureAttributes(contour);
          final ContourFigure figure = new ContourFigure(figureStatus, attributes);
          return figure;
        }
        finally
        {
          model.readUnlock();
        }
      }
    }
  }

  /**
   * A class representing an object contour. It supports showing member tables and focusing on the
   * call path. Hence, this class encapsulates four different states.
   * 
   * @see ContourState
   * @see ContourEditPart
   */
  private static class ContourStateObject extends ContourState
  {
    private ContourStateObject(final ContourEditPart editPart)
    {
      super(editPart);
    }

    @Override
    IFigure createFigure()
    {
      final IContour contour = (IContour) getModel();
      final IExecutionModel model = executionModel();
      model.readLock();
      try
      {
        boolean renderFull = false;
        final ContourFigure figure;
        // no focusing on the call path-- build a full contour
        if (!focusCallPath())
        {
          figure = createFullBuilder(contour);
        }
        else
        {
          // when focusing on the call path we must determine how much to draw
          final IContourAttributes attributes = contourFigureAttributes(contour);
          // we render full contours if the contour is static or has some pending method call
          renderFull = (contour instanceof IContextContour && ((IContextContour) contour)
              .isStatic()) || hasMethod(contour);
          // focusing on the call path-- build a full contour
          if (!renderFull)
          {
            // show the innermost contour only
            if (contour.children().size() == 0)
            {
              figure = new ContourFigure(VisualStatus.NODE, attributes);
            }
            // when minimized, do not show contours for containing types
            else
            {
              figure = new ContourFigure(VisualStatus.EMPTY, attributes);
            }
          }
          else
          {
            figure = new ContourFigure(VisualStatus.FULL, attributes);
          }
        }
        // we only show member tables for full renderings
        if (showMemberTables() && (renderFull || !focusCallPath()))
        {
          exposeMembers(contour, figure);
        }
        return figure;
      }
      finally
      {
        model.readUnlock();
      }
    }
  }

  /**
   * A class representing a stacked contour. It supports showing member tables and focusing on the
   * call path. Hence, this class encapsulates four different states.
   * 
   * @see ContourState
   * @see ContourEditPart
   */
  private static class ContourStateStacked extends ContourState
  {
    private ContourStateStacked(final ContourEditPart editPart)
    {
      super(editPart);
    }

    private IFigure createBuilder(final IContour contour)
    {
      boolean renderFull = false;
      final ContourFigure figure;
      // no focusing on the call path-- build a full contour
      if (!focusCallPath())
      {
        figure = createFullBuilder(contour);
      }
      else
      {
        // when focusing on the call path we must determine how much to draw
        final IContourAttributes attributes = contourFigureAttributes(contour);
        // we render full contours if the contour is static or has some pending method call
        renderFull = (contour instanceof IContextContour && ((IContextContour) contour).isStatic())
            || hasMethod(contour);
        if (!renderFull)
        {
          figure = new ContourFigure(VisualStatus.NODE, attributes);
        }
        else
        {
          figure = new ContourFigure(VisualStatus.FULL, attributes);
        }
      }
      // we only show member tables for full renderings
      if (showMemberTables() && (renderFull || !focusCallPath()))
      {
        exposeMembers(contour, figure);
      }
      return figure;
    }

    private IFigure figure(final IContour contour)
    {
      if (contour instanceof IContextContour)
      {
        final IContextContour cc = (IContextContour) contour;
        if (cc.isStatic())
        {
          final ContourFigure figure = createFullBuilder(contour);
          // we show member tables for static contours
          if (showMemberTables())
          {
            exposeMembers(contour, figure);
          }
          return figure;
        }
        // virtual contours are all but the "innermost" contours
        if (cc.isVirtual())
        {
          for (final IContour child : contour.children())
          {
            if (child instanceof IMethodContour)
            {
              return new ContourFigure(VisualStatus.OUTLINE, null);
            }
          }
          return new ContourFigure(VisualStatus.EMPTY, null);
        }
        return createBuilder(contour);
      }
      if (contour instanceof IMethodContour)
      {
        return createBuilder(contour);
      }
      return null;
    }

    @Override
    IFigure createFigure()
    {
      final IContour contour = (IContour) getModel();
      final IExecutionModel model = executionModel();
      model.readLock();
      try
      {
        return figure(contour);
      }
      finally
      {
        model.readUnlock();
      }
    }
  }
}
