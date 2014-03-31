package edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.swt.graphics.Color;

import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.internal.ui.ContourAttributesFactory;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.figures.LifelineFigure;
import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;
import edu.buffalo.cse.jive.ui.IJiveEditPart;
import edu.buffalo.cse.jive.ui.IThreadColorManager;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;

public class LifelineEditPart extends AbstractGraphicalEditPart implements IJiveEditPart,
    NodeEditPart
{
  private final Map<IInitiatorEvent, Integer> executionToBinMap = new HashMap<IInitiatorEvent, Integer>();
  private final List<IInitiatorEvent> topExecutionInBin = new ArrayList<IInitiatorEvent>(2);

  @Override
  public ConnectionAnchor getSourceConnectionAnchor(final ConnectionEditPart connection)
  {
    // source for a found message
    if (connection instanceof InitiatorMessageEditPart)
    {
      return new ChopboxAnchor(getFigure())
        {
          @Override
          public Point getLocation(final Point reference)
          {
            final Rectangle bounds = Rectangle.SINGLETON;
            bounds.setBounds(getBox());
            getOwner().translateToAbsolute(bounds);
            final Point result = bounds.getTopLeft();
            result.y = reference.y;
            return result;
          }
        };
    }
    throw new IllegalArgumentException("Unsupported connection:  " + connection);
  }

  @Override
  public ConnectionAnchor getSourceConnectionAnchor(final Request request)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public ConnectionAnchor getTargetConnectionAnchor(final ConnectionEditPart connection)
  {
    // target for a lost message
    if (connection instanceof TerminatorMessageEditPart)
    {
      return new ChopboxAnchor(getFigure())
        {
          @Override
          public Point getLocation(final Point reference)
          {
            final Rectangle bounds = Rectangle.SINGLETON;
            bounds.setBounds(getBox());
            getOwner().translateToAbsolute(bounds);
            final Point result = bounds.getBottomLeft(); // bounds.getTopRight();
            result.y = reference.y;
            return result;
          }
        };
    }
    throw new IllegalArgumentException("Unsupported connection:  " + connection);
  }

  @Override
  public ConnectionAnchor getTargetConnectionAnchor(final Request request)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Computes the relative horizontal position of an activation box with respect to its life line.
   */
  private void assignBin(final IInitiatorEvent execution)
  {
    final long executionStart = execution.eventId();
    final int binCount = topExecutionInBin.size();
    // determine if the current execution is nested on some top execution
    for (int bin = 0; bin < binCount; bin++)
    {
      final IInitiatorEvent lastExecution = topExecutionInBin.get(bin);
      final long lastExecutionEnd = lastExecution.eventId() + lastExecution.duration() - 1;
      if (executionStart > lastExecutionEnd)
      {
        executionToBinMap.put(execution, bin);
        topExecutionInBin.set(bin, execution);
        return;
      }
    }
    // the current execution is not nested
    executionToBinMap.put(execution, binCount);
    topExecutionInBin.add(execution);
  }

  private void updateBinMapping(final List<? extends IInitiatorEvent> executionList)
  {
    executionToBinMap.clear();
    topExecutionInBin.clear();
    for (final IInitiatorEvent execution : executionList)
    {
      assignBin(execution);
    }
  }

  @Override
  protected void createEditPolicies()
  {
    // installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new NonResizableEditPolicy());
  }

  @Override
  protected IFigure createFigure()
  {
    final Object model = getModel();
    if (model instanceof Gutter)
    {
      return new LifelineFigure(null);
    }
    else if (model instanceof IThreadValue)
    {
      final IThreadColorManager manager = JiveUIPlugin.getDefault().getThreadColorManager();
      final IJiveDebugTarget target = JiveLaunchPlugin.getDefault().getLaunchManager()
          .activeTarget();
      final Color color = manager.threadColor(target, (IThreadValue) model);
      return new LifelineFigure(ContourAttributesFactory.createThreadAttributes(
          (IThreadValue) model, color));
    }
    else if (model instanceof IContextContour)
    {
      final IContextContour cc = (IContextContour) model;
      if (cc.isStatic())
      {
        return new LifelineFigure(ContourAttributesFactory.createStaticAttributes(cc));
      }
      return new LifelineFigure(ContourAttributesFactory.createInstanceAttributes(cc));
    }
    throw new IllegalArgumentException("Cannot create a lifeline for object " + model.toString());
  }

  @Override
  protected List<? extends IInitiatorEvent> getModelChildren()
  {
    if (getModel() instanceof Gutter)
    {
      final List<IInitiatorEvent> adapter = new ArrayList<IInitiatorEvent>();
      adapter.add(((Gutter) getModel()).getSystemStart());
      return adapter;
    }
    List<? extends IInitiatorEvent> result = Collections.emptyList();
    final SequenceDiagramEditPart sd = (SequenceDiagramEditPart) getRoot().getContents();
    final UIAdapter uiAdapter = sd.getUIAdapter();
    if (getModel() instanceof IThreadValue)
    {
      result = uiAdapter.visibleInitiators((IThreadValue) getModel());
      executionToBinMap.put(result.get(0), 0);
    }
    else
    {
      final IContour contour = (IContour) getModel();
      if (contour instanceof IContextContour)
      {
        IContextContour c = (IContextContour) contour;
        final List<IMethodCallEvent> nested = TypeTools.newArrayList();
        nested.addAll(c.nestedInitiators());
        c = !sd.showExpandedLifeLines() ? c.parent() : null;
        while (c != null)
        {
          nested.addAll(c.nestedInitiators());
          c = c.parent();
        }
        result = nested;
        // remove null children-- these are out-of-model markers
        for (int i = result.size() - 1; i >= 0; i--)
        {
          if (result.get(i) == null || sd.isHiddenThread(result.get(i).execution().thread()))
          {
            result.remove(i);
          }
        }
        // sort the initiators if life lines are compressed
        if (!sd.showExpandedLifeLines())
        {
          Collections.sort(nested, new Comparator<IMethodCallEvent>()
            {
              @Override
              public int compare(final IMethodCallEvent o1, final IMethodCallEvent o2)
              {
                return (int) (o1.eventId() - o2.eventId());
              }
            });
        }
      }
      result = uiAdapter.visibleInitiators(result);
      updateBinMapping(result);
    }
    return result;
  }

  @Override
  protected List<Message> getModelSourceConnections()
  {
    final Object model = getModel();
    if (model instanceof IThreadValue || model instanceof Gutter)
    {
      return Collections.emptyList();
    }
    else
    {
      final SequenceDiagramEditPart contents = (SequenceDiagramEditPart) getRoot().getContents();
      return contents.getFoundMessages((IContour) getModel());
    }
  }

  @Override
  protected List<Message> getModelTargetConnections()
  {
    final Object model = getModel();
    if (model instanceof IThreadValue || model instanceof Gutter)
    {
      return Collections.emptyList();
    }
    else
    {
      final SequenceDiagramEditPart contents = (SequenceDiagramEditPart) getRoot().getContents();
      return contents.getLostMessages((IContour) getModel());
    }
  }

  int binNumber(final IInitiatorEvent execution)
  {
    if (executionToBinMap.containsKey(execution))
    {
      return executionToBinMap.get(execution);
    }
    throw new IllegalStateException("Bin numbers have not been assigned yet.");
  }
}
