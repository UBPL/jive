package edu.buffalo.cse.jive.internal.ui.view.contour.graph;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.draw2d.graph.Node;

import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IModel.IContourReference;
import edu.buffalo.cse.jive.model.IModel.IOutOfModelMethodReference;
import edu.buffalo.cse.jive.model.IModel.IValue;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.model.IVisitor;
import edu.buffalo.cse.jive.ui.view.diagram.contour.ContourDiagramFactory;

/**
 * Representation of the contour diagram as a graph.
 */
public class ContourGraph
{
  public static final int STATIC_SECTION = -1;
  private final IGraphLayout layout;

  public ContourGraph(final IExecutionModel model)
  {
    assert (model != null);
    this.layout = ContourDiagramFactory.createLayout();
    model.readLock();
    try
    {
      // vertices
      for (final IContour c : model.contourView().lookupRoots())
      {
        layout.addContourNode(c, new Node(c.id()));
      }
      // edges
      model.contourView().visit(new IVisitor<IContour>()
        {
          @Override
          public void visit(final IContour contour)
          {
            addGraphEdges(model, contour);
          }
        });
      // let the layout update all node positions
      layout.updatePositions();
    }
    finally
    {
      model.readUnlock();
    }
  }

  public INodePosition getPosition(final IContour c)
  {
    return layout.getPosition(c);
  }

  private void addGraphEdges(final IExecutionModel model, final IContour contour)
  {
    final Node u = layout.getNode(contour);
    final Set<IContourReference> targets = new HashSet<IContourReference>();
    for (final IContourMember member : contour.members())
    {
      final boolean isRpdl = member.schema().modifiers().contains(NodeModifier.NM_RPDL);
      final IValue value = member.value();
      if (value instanceof IContourReference)
      {
        // do not add edge if the target has been garbage collected
        // if (member.value().isGarbageCollected(model.temporalState().event().eventId()))
        // {
        // // System.err.println("CONTOUR_GRAPH[omit edge to " + member.value().toString() + "]");
        // continue;
        // }
        final IContourReference reference = (IContourReference) value;
        final IContour d = reference.contour();
        final Node v = layout.getNode(d);
        if (v == null || u == null)
        {
          continue;
        }
        // in-model RPDL at the contour or member level
        if (isRpdl)
        {
          layout.addEdge(v, u);
        }
        else if (!targets.contains(reference))
        {
          layout.addEdge(u, v);
          targets.add(reference);
        }
      }
      // RPDL of an in-model call with an out-of-model return; we link this call to the eventual
      // in-model activation that this thread of execution returns to
      else if (value.isOutOfModelMethodReference())
      {
        final IOutOfModelMethodReference targetValue = (IOutOfModelMethodReference) value;
        final IContour d = targetValue.method();
        if (isRpdl)
        {
          final Node v = layout.getNode(d);
          layout.addEdge(v, u);
        }
      }
    }
  }
}