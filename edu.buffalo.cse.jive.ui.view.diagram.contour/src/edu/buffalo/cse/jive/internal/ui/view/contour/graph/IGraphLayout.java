package edu.buffalo.cse.jive.internal.ui.view.contour.graph;

import org.eclipse.draw2d.graph.Node;

import edu.buffalo.cse.jive.model.IContourModel.IContour;

public interface IGraphLayout
{
  public void addContourNode(IContour c, Node node);

  public void addEdge(Node v, Node u);

  public Node getNode(IContour contour);

  public INodePosition getPosition(IContour c);

  public void updatePositions();
}