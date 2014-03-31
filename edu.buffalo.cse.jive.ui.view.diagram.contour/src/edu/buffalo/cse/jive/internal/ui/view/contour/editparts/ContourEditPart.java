package edu.buffalo.cse.jive.internal.ui.view.contour.editparts;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.NonResizableEditPolicy;

import edu.buffalo.cse.jive.internal.ui.view.contour.figures.ContourFigure;
import edu.buffalo.cse.jive.internal.ui.view.contour.figures.ContourFigure.LabeledContourFigure;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IModel.IContourReference;
import edu.buffalo.cse.jive.model.IModel.IOutOfModelMethodReference;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IVisitor;
import edu.buffalo.cse.jive.ui.IJiveEditPart;

/**
 * An {@code EditPart} serving as a controller for a {@code Contour}. It is responsible for creating
 * the appropriate {@code ContourFigure} as well as supplying the children and connections for the
 * contour.
 * 
 * @see ContourDiagramEditPart
 * @see ContourConnectionEditPart
 * @see ContourFigure
 * @see LabeledContourFigure
 */
public class ContourEditPart extends AbstractGraphicalEditPart implements IJiveEditPart,
    NodeEditPart
{
  private ContourState contourState;
  private boolean focusCallPath;
  private final ContourState minimizedState;
  private final ContourState objectState;
  private boolean showMemberTables;
  private final ContourState stackedState;

  public ContourEditPart(final IContour model)
  {
    objectState = ContourState.createObjectState(this);
    stackedState = ContourState.createStackedState(this);
    minimizedState = ContourState.createMinimizedState(this);
    contourState = stackedState;
    setModel(model);
  }

  public IExecutionModel executionModel()
  {
    final ContourDiagramEditPart contents = (ContourDiagramEditPart) getViewer().getContents();
    return contents != null ? contents.executionModel() : null;
  }

  public boolean focusCallPath()
  {
    return focusCallPath;
  }

  @Override
  public ConnectionAnchor getSourceConnectionAnchor(final ConnectionEditPart connection)
  {
    setContourState();
    return contourState.getSourceConnectionAnchor(connection);
  }

  @Override
  public ConnectionAnchor getSourceConnectionAnchor(final Request request)
  {
    // TODO Determine if this should be implemented
    System.out.println("ContourEditPart#getSourceConnectionAnchor(Request) - " + request);
    return null;
  }

  @Override
  public ConnectionAnchor getTargetConnectionAnchor(final ConnectionEditPart connection)
  {
    return new ChopboxAnchor(getFigure());
  }

  @Override
  public ConnectionAnchor getTargetConnectionAnchor(final Request request)
  {
    // TODO Determine if this should be implemented
    System.out.println("ContourEditPart#getTargetConnectionAnchor(Request) - " + request);
    return null;
  }

  public boolean showMemberTables()
  {
    return showMemberTables;
  }

  private void setContourState()
  {
    final ContourDiagramEditPart contents = (ContourDiagramEditPart) getViewer().getContents();
    focusCallPath = contents.isCallPathFocused();
    showMemberTables = false;
    switch (contents.getContourState())
    {
      case MINIMIZED:
        contourState = minimizedState;
        break;
      case OBJECTS:
        contourState = objectState;
        break;
      case OBJECTS_MEMBERS:
        showMemberTables = true;
        contourState = objectState;
        break;
      case STACKED:
        contourState = stackedState;
        break;
      case STACKED_MEMBERS:
        showMemberTables = true;
        contourState = stackedState;
        break;
      default:
        contourState = stackedState;
    }
  }

  @Override
  protected void createEditPolicies()
  {
    installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new NonResizableEditPolicy());
  }

  @Override
  protected IFigure createFigure()
  {
    setContourState();
    return contourState.createFigure();
  }

  /**
   * Returns the {@code ContourEditPart} associated with the supplied contour.
   * 
   * @param contour
   *          the contour whose edit part will be returned
   * @return the contour edit part for the contour, or <code>null</code> if none exist
   */
  protected ContourEditPart getContourEditPart(final IContour contour)
  {
    return (ContourEditPart) getViewer().getEditPartRegistry().get(contour);
  }

  /**
   * Returns the {@code ContourDiagramEditPart} for the edit part. This corresponds to the contents
   * of the {@code RootEditPart}.
   * 
   * @return the contour diagram edit part of the edit part
   */
  protected ContourDiagramEditPart getDiagramEditPart()
  {
    return (ContourDiagramEditPart) getRoot().getContents();
  }

  @Override
  protected List<IContour> getModelChildren()
  {
    final IContour contour = (IContour) getModel();
    return contour.children();
  }

  /**
   * Returns a list of variable instances that reference some contour other than this edit part's
   * contour. Note that variable instances are unique, as opposed to the actual reference values,
   * which are not necessarily unique and, therefore, may be cached by the underlying factory.
   */
  @Override
  protected List<IContourMember> getModelSourceConnections()
  {
    final IContour sourceContour = (IContour) getModel();
    final List<IContourMember> sourceFor = new ArrayList<IContourMember>();
    final IExecutionModel model = executionModel();
    /**
     * Traverse this contour's variable members and, if some variable's value references another
     * contour, this edit part's contour (source) must "point to" the contour (target). Hence, this
     * edit part is a source for all such (target) values. The respective variable instance is thus
     * added to the list.
     */
    model.readLock();
    try
    {
      for (final IContourMember target : sourceContour.members())
      {
        if (target.value().isContourReference())
        {
          final IContourReference targetValue = (IContourReference) target.value();
          // do not add part if the target has been garbage collected
          // if (targetValue.isGarbageCollected(model.temporalState().event().eventId()))
          // {
          // // System.err.println("CONTOUR_EDIT_PART[omit edge to " + target.value().toString() +
          // // "]");
          // continue;
          // }
          /**
           * The member is a local variable, the contour reference is a method contour reference,
           * and the context of the method contour in the reference is the source contour. In other
           * words, the local variable references the method's enclosing environment.
           */
          if (target.schema().kind() == NodeKind.NK_VARIABLE
              && targetValue.contour().equals(sourceContour.parent()))
          {
            continue;
          }
          if (!((IContourReference) target.value()).contour().equals(sourceContour))
          {
            sourceFor.add(target);
          }
        }
        else if (target.value().isOutOfModelMethodReference()
            && !((IOutOfModelMethodReference) target.value()).method().equals(sourceContour))
        {
          sourceFor.add(target);
        }
      }
    }
    finally
    {
      model.readUnlock();
    }
    return sourceFor;
  }

  /**
   * Returns a list of variable instances that reference this edit part's contour. Note that
   * variable instances are unique, as opposed to the actual reference values, which are not
   * necessarily unique and, therefore, may be cached by the underlying factory.
   * 
   * TODO takes O(n) to determine targets-- provide a model service to perform this as a lookup
   */
  @Override
  protected List<IContourMember> getModelTargetConnections()
  {
    final IContour targetContour = (IContour) getModel();
    final List<IContourMember> targetFor = new LinkedList<IContourMember>();
    final IVisitor<IContour> modelVisitor = new IVisitor<IContour>()
      {
        /**
         * Otherwise, traverse each of the contour's variable members and, if value represents this
         * edit part's contour, the contour (source) must "point to" this edit part's contour
         * (target). Hence, the edit part is a target for all such (source) values.
         */
        @Override
        public void visit(final IContour sourceContour)
        {
          // If the contour is the same one represented by this edit part, skip.
          if (sourceContour.equals(targetContour))
          {
            return;
          }
          for (final IContourMember source : sourceContour.members())
          {
            if (source.value().isContourReference())
            {
              final IContourReference sourceValue = (IContourReference) source.value();
              /**
               * The member is a local variable, the contour reference is a method contour
               * reference, and the context of the method contour in the reference is the source
               * contour. In other words, the local variable references the method's enclosing
               * environment.
               */
              if (source.schema().kind() == NodeKind.NK_VARIABLE
                  && sourceContour.parent().equals(targetContour)
                  && targetContour.equals(sourceValue.contour()))
              {
                continue;
              }
              if (!targetFor.contains(source) && sourceValue.contour().equals(targetContour))
              {
                targetFor.add(source);
              }
            }
            else if (source.value().isOutOfModelMethodReference())
            {
              final IOutOfModelMethodReference sourceValue = (IOutOfModelMethodReference) source
                  .value();
              if (!targetFor.contains(source) && sourceValue.method().equals(targetContour))
              {
                targetFor.add(source);
              }
            }
          }
        }
      };
    executionModel().contourView().visit(modelVisitor);
    return targetFor;
  }

  @Override
  protected void refreshVisuals()
  {
    setContourState();
    contourState.refreshVisuals();
  }

  /**
   * Adds a child {@code EditPart} for the supplied {@code Contour} if one does not already exist.
   * 
   * @param contour
   *          the child contour
   */
  void addChildContour(final IContour contour)
  {
    // Create a new child edit part if one does not already exist. Instance
    // contours for an object are created together, so during normal
    // execution this case will not be reached. However, while replaying
    // recorded states it will.
    if (getContourEditPart(contour) == null)
    {
      final EditPart childPart = createChild(contour);
      addChild(childPart, -1);
    }
  }

  /**
   * Removes the {@code EditPart} associated with the supplied {@code Contour} if one exists. For
   * {@code MethodContour}s, connections are first updated to reflect the fact that the contour has
   * been removed from the model.
   * 
   * @param contour
   *          the child contour
   */
  void removeChildContour(final IContour contour)
  {
    // Remove the edit part if it exists
    final EditPart childPart = getContourEditPart(contour);
    if (childPart != null)
    {
      removeChild(childPart);
    }
  }
}