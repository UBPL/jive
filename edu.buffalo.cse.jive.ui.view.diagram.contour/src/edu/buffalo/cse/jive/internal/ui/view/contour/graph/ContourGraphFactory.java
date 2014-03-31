package edu.buffalo.cse.jive.internal.ui.view.contour.graph;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.graph.DirectedGraph;
import org.eclipse.draw2d.graph.Edge;
import org.eclipse.draw2d.graph.Node;
import org.eclipse.draw2d.graph.NodeList;

import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.internal.ui.view.contour.graph.INodePosition.DiagramSection;
import edu.buffalo.cse.jive.internal.ui.view.contour.graph.INodePosition.IHierarchicalPosition;
import edu.buffalo.cse.jive.internal.ui.view.contour.graph.INodePosition.ITabularPosition;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContour;

public final class ContourGraphFactory
{
  public static IGraphLayout createHierarchicalLayout()
  {
    return new HierarchicalLayout();
  }

  public static IGraphLayout createTabularLayout()
  {
    return new TabularLayout();
  }

  private static void sortEdges(final List<Edge> edges)
  {
    Collections.sort(edges, new Comparator<Edge>()
      {
        @Override
        public int compare(final Edge e1, final Edge e2)
        {
          final Node v1 = e1.target;
          final Node v2 = e2.target;
          final Long id1 = (Long) v1.data * (v1.incoming.size() == 0 ? -1 : 1);
          final Long id2 = (Long) v2.data * (v2.incoming.size() == 0 ? -1 : 1);
          return id1 < 0 && id2 < 0 ? (int) (id2 - id1) : (int) (id1 - id2);
        }
      });
  }

  @SuppressWarnings("unchecked")
  private static void sortNodes(final NodeList nodes)
  {
    Collections.sort(nodes, new Comparator<Node>()
      {
        @Override
        public int compare(final Node v1, final Node v2)
        {
          final Long id1 = (Long) v1.data;
          final Long id2 = (Long) v2.data;
          final int isz1 = v1.incoming.size();
          final int isz2 = v2.incoming.size();
          final int osz1 = v1.outgoing.size();
          final int osz2 = v2.outgoing.size();
          // at least one node is a root
          if (isz1 == 0 || isz2 == 0)
          {
            // if both are root nodes, oldest first, otherwise, root first
            return isz1 == isz2 ? (int) (id1 - id2) : (isz1 - isz2);
          }
          // at least one node is a leaf
          if (osz1 == 0 || osz2 == 0)
          {
            // if both are leaf nodes, oldest first, otherwise, internal first
            return osz1 == osz2 ? (int) (id1 - id2) : (osz2 - osz1);
          }
          // both nodes are internal
          return (int) (id1 - id2);
        }
      });
  }

  static IHierarchicalPosition createHierarchicalPosition(final DiagramSection section,
      final IHierarchicalPosition parent, final int column)
  {
    return new HierarchicalPosition(section, parent, column);
  }

  static ITabularPosition createTabularPosition(final DiagramSection section, final int column,
      final int layer, final int cell)
  {
    return new TabularPosition(section, column, layer, cell);
  }

  private static enum Color
  {
    BLACK,
    WHITE
  }

  private static class HierarchicalLayout implements IGraphLayout
  {
    private final Map<IContour, Node> contourToNode;
    private final Map<Node, IHierarchicalPosition> nodeToPosition;
    private final DirectedGraph graph;
    private final Map<Long, Color> nodeColors;
    private final Map<Node, Integer> staticNodes;

    public HierarchicalLayout()
    {
      this.contourToNode = TypeTools.newHashMap();
      this.graph = new DirectedGraph();
      this.nodeColors = TypeTools.newHashMap();
      this.nodeToPosition = TypeTools.newHashMap();
      this.staticNodes = TypeTools.newHashMap();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addContourNode(final IContour c, final Node v)
    {
      graph.nodes.add(v);
      if (c instanceof IContextContour && ((IContextContour) c).isStatic())
      {
        staticNodes.put(v, graph.nodes.size() - 1);
      }
      updateMappings(c, v);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addEdge(final Node x, final Node y)
    {
      final Edge e = new Edge(x, y);
      graph.edges.add(e);
      x.outgoing.add(e);
      y.incoming.add(e);
    }

    @Override
    public Node getNode(final IContour contour)
    {
      return contourToNode.get(contour);
    }

    @Override
    public INodePosition getPosition(final IContour c)
    {
      assert contourToNode.containsKey(c) : "Undefined contour " + c.signature();
      final Node u = contourToNode.get(c);
      assert nodeToPosition.containsKey(u) : "Undefined position for contour " + c.signature();
      return nodeToPosition.get(u);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void updatePositions()
    {
      final NodeList nodes = new NodeList(graph.nodes);
      if (nodes.isEmpty())
      {
        return;
      }
      // process all static nodes and object nodes reachable from them
      int staticColumn = 0;
      for (final Node staticNode : staticNodes.keySet())
      {
        // remove from the nodes list
        nodes.remove(staticNodes.get(staticNode));
        // create a new column for this node and update for the next static node
        setPosition(staticNode, DiagramSection.DS_STATIC, null, staticColumn++);
        // mark node
        setColor(staticNode, Color.BLACK);
        // sort outgoing edges
        ContourGraphFactory.sortEdges(staticNode.outgoing);
        // process the object nodes reachable from the current static node
        final DiagramSection section = DiagramSection.DS_REACHABLE;
        int instanceColumn = 0;
        // traverse each of the static node's edges
        for (final Object o : staticNode.outgoing)
        {
          final Edge e = (Edge) o;
          final Node u = e.target;
          // create columns for the unvisited node and its descendants, recursively
          if (getColor(u) == Color.WHITE)
          {
            final IHierarchicalPosition position = setPosition(u, section, null, instanceColumn++);
            visitBF(u, section, position);
          }
        }
      }
      // process all unreachable object nodes that have some association
      DiagramSection section = DiagramSection.DS_UNREACHABLE;
      int unreachableColumn = 0;
      ContourGraphFactory.sortNodes(nodes);
      for (final Object o : nodes)
      {
        final Node u = (Node) o;
        // only object nodes that were not visited remain white
        if (getColor(u) == Color.WHITE && u.outgoing.size() > 0)
        {
          final IHierarchicalPosition position = setPosition(u, section, null, unreachableColumn++);
          visitBF(u, section, position);
        }
        // only object nodes that were not visited remain white
        // if (getColor(u) == Color.WHITE && u.incoming.size() > 0)
        // {
        // final IHierarchicalPosition position = setPosition(u, section, null,
        // unreachableColumn++);
        // visitBF(u, section, position);
        // }
      }
      // process all unreachable object nodes that have no associations
      section = DiagramSection.DS_SINGLETONS;
      int singletonColumn = 0;
      for (final Object o : nodes)
      {
        final Node u = (Node) o;
        // only object nodes that were not visited remain white
        if (getColor(u) == Color.WHITE)
        {
          setPosition(u, section, null, singletonColumn++);
        }
      }
    }

    private Color getColor(final Node u)
    {
      return nodeColors.get(u.data);
    }

    private void setColor(final Node u, final Color c)
    {
      nodeColors.put((Long) u.data, c);
    }

    private IHierarchicalPosition setPosition(final Node u, final DiagramSection section,
        final IHierarchicalPosition parent, final int column)
    {
      final IHierarchicalPosition position = ContourGraphFactory.createHierarchicalPosition(
          section, parent, column);
      nodeToPosition.put(u, position);
      return position;
    }

    private void updateMappings(final IContour c, final Node v)
    {
      // Color the node white for graph traversal algorithm
      nodeColors.put((Long) v.data, Color.WHITE);
      // Add a contour-node mapping for the contour and its children
      contourToNode.put(c, v);
      // update child mappings
      for (final IContour child : c.children())
      {
        updateMappings(child, v);
      }
    }

    @SuppressWarnings("unchecked")
    private void visitBF(final Node u, final DiagramSection section,
        final IHierarchicalPosition parent)
    {
      setColor(u, Color.BLACK);
      int column = 0;
      // subtrees of this object
      ContourGraphFactory.sortEdges(u.outgoing);
      for (final Object o : u.outgoing)
      {
        final Edge e = (Edge) o;
        final Node v = e.target;
        if (getColor(v) == Color.WHITE)
        {
          final IHierarchicalPosition position = setPosition(v, section, parent, column++);
          visitBF(v, section, position);
        }
      }
      // subtrees leading into this object
      // ContourGraphFactory.sortEdges(u.incoming);
      // for (final Object o : u.incoming)
      // {
      // final Edge e = (Edge) o;
      // final Node v = e.source;
      // if (getColor(v) == Color.WHITE)
      // {
      // final IHierarchicalPosition position = setPosition(v, section, parent, column++);
      // visitBF(v, section, position);
      // }
      // }
    }
  }

  /**
   * Tabular position of graph nodes within the contour graph.
   */
  private final static class HierarchicalPosition implements IHierarchicalPosition
  {
    private final int column;
    private final IHierarchicalPosition parent;
    private final DiagramSection section;

    HierarchicalPosition(final DiagramSection section, final IHierarchicalPosition parent,
        final int column)
    {
      this.section = section;
      this.parent = parent;
      this.column = column;
    }

    @Override
    public int column()
    {
      return column;
    }

    @Override
    public boolean equals(final Object o)
    {
      if (o instanceof HierarchicalPosition)
      {
        final HierarchicalPosition position = (HierarchicalPosition) o;
        return (position.section() == section && position.column() == column && position.parent() == parent);
      }
      return false;
    }

    @Override
    public int hashCode()
    {
      int hash = 1;
      hash = hash * 31 + section.hashCode();
      hash = hash * 31 + column;
      hash = hash * 31 + (parent == null ? 7 : parent.hashCode());
      return hash;
    }

    @Override
    public IHierarchicalPosition parent()
    {
      return parent;
    }

    @Override
    public DiagramSection section()
    {
      return section;
    }

    @Override
    public String toString()
    {
      return "[section = " + section.toString() + ", parent = "
          + (parent == null ? "root" : parent.toString()) + ", column = " + column + "]";
    }
  }

  private static class TabularLayout implements IGraphLayout
  {
    private final Map<IContour, Node> contourToNode;
    private final DirectedGraph graph;
    private final Map<Long, Integer> nodeCells;
    private final Map<Long, Color> nodeColors;
    private final Map<Long, Integer> nodeColumns;
    private final Map<Long, Integer> nodeLayers;
    private final Map<Long, DiagramSection> nodeSections;

    public TabularLayout()
    {
      this.contourToNode = TypeTools.newHashMap();
      this.graph = new DirectedGraph();
      this.nodeCells = TypeTools.newHashMap();
      this.nodeColors = TypeTools.newHashMap();
      this.nodeColumns = TypeTools.newHashMap();
      this.nodeLayers = TypeTools.newHashMap();
      this.nodeSections = TypeTools.newHashMap();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addContourNode(final IContour c, final Node v)
    {
      graph.nodes.add(v);
      updateMappings(c, v);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addEdge(final Node x, final Node y)
    {
      final Edge e = new Edge(x, y);
      graph.edges.add(e);
      x.outgoing.add(e);
      y.incoming.add(e);
    }

    @Override
    public Node getNode(final IContour contour)
    {
      return contourToNode.get(contour);
    }

    @Override
    public INodePosition getPosition(final IContour c)
    {
      assert contourToNode.containsKey(c) : "Undefined position for contour " + c.signature();
      final Node u = contourToNode.get(c);
      final Long id = (Long) u.data;
      return ContourGraphFactory.createTabularPosition(nodeSections.get(id), nodeColumns.get(id),
          nodeLayers.get(id), nodeCells.get(id));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void updatePositions()
    {
      final NodeList nodes = new NodeList(graph.nodes);
      if (nodes.isEmpty())
      {
        return;
      }
      final Node staticNode = (Node) nodes.remove(0);
      setPosition(staticNode, DiagramSection.DS_STATIC, 0, 0, 0);
      setColor(staticNode, Color.BLACK);
      ContourGraphFactory.sortEdges(staticNode.outgoing);
      DiagramSection section = DiagramSection.DS_REACHABLE;
      int column = 0;
      int layer = 0;
      for (final Object o : staticNode.outgoing)
      {
        final Edge e = (Edge) o;
        final Node u = e.target;
        if (getColor(u) == Color.WHITE)
        {
          // sortRootOutgoingEdges(u);
          final Map<Integer, Integer> layerToNextCellMapping = TypeTools.newHashMap();
          visitDF(u, section, column, layer, layerToNextCellMapping);
          column++;
        }
      }
      section = DiagramSection.DS_UNREACHABLE;
      column = 0;
      layer = 0;
      ContourGraphFactory.sortNodes(nodes);
      for (final Object o : nodes)
      {
        final Node u = (Node) o;
        if (getColor(u) == Color.WHITE)
        {
          // sortOutgoingEdges(u);
          final Map<Integer, Integer> layerToNextCellMapping = TypeTools.newHashMap();
          visitDF(u, section, column, layer, layerToNextCellMapping);
          column++;
        }
      }
    }

    private Color getColor(final Node u)
    {
      return nodeColors.get(u.data);
    }

    private void setColor(final Node u, final Color c)
    {
      nodeColors.put((Long) u.data, c);
    }

    private void setPosition(final Node u, final DiagramSection section, final int column,
        final int layer, final int cell)
    {
      final Long id = (Long) u.data;
      nodeSections.put(id, section);
      nodeColumns.put(id, column);
      nodeLayers.put(id, layer);
      nodeCells.put(id, cell);
    }

    private void updateMappings(final IContour c, final Node v)
    {
      // Color the node white for graph traversal algorithm
      nodeColors.put((Long) v.data, Color.WHITE);
      // Add a contour-node mapping for the contour and its children
      contourToNode.put(c, v);
      // update child mappings
      for (final IContour child : c.children())
      {
        updateMappings(child, v);
      }
    }

    private void visitDF(final Node u, final DiagramSection section, final int column, int layer,
        final Map<Integer, Integer> layerToNextCellMapping)
    {
      if (layerToNextCellMapping.containsKey(layer))
      {
        final int cell = layerToNextCellMapping.get(layer);
        setPosition(u, section, column, layer, cell);
        layerToNextCellMapping.put(layer, cell + 1);
      }
      else
      {
        setPosition(u, section, column, layer, 0);
        layerToNextCellMapping.put(layer, 1);
      }
      setColor(u, Color.BLACK);
      layer++;
      for (final Object o : u.outgoing)
      {
        final Edge e = (Edge) o;
        final Node v = e.target;
        if (getColor(v) == Color.WHITE)
        {
          visitDF(v, section, column, layer, layerToNextCellMapping);
        }
      }
      setColor(u, Color.BLACK);
    }
  }

  /**
   * Tabular position of graph nodes within the contour graph.
   */
  private final static class TabularPosition implements ITabularPosition
  {
    private final int cell;
    private final int column;
    private final int layer;
    private final DiagramSection section;

    private TabularPosition(final DiagramSection section, final int column, final int layer,
        final int cell)
    {
      this.section = section;
      this.column = column;
      this.layer = layer;
      this.cell = cell;
    }

    @Override
    public int cell()
    {
      return cell;
    }

    @Override
    public int column()
    {
      return column;
    }

    @Override
    public boolean equals(final Object o)
    {
      if (o instanceof TabularPosition)
      {
        final TabularPosition position = (TabularPosition) o;
        if (position.section() == section && position.column() == column
            && position.layer() == layer && position.cell() == cell)
        {
          return true;
        }
      }
      return false;
    }

    @Override
    public int hashCode()
    {
      int hash = 1;
      hash = hash * 31 + section.hashCode();
      hash = hash * 31 + column;
      hash = hash * 31 + layer;
      hash = hash * 31 + cell;
      return hash;
    }

    @Override
    public int layer()
    {
      return layer;
    }

    @Override
    public DiagramSection section()
    {
      return section;
    }

    @Override
    public String toString()
    {
      return "[section = " + section.toString() + ", column = " + column + ", layer = " + layer
          + ", cell = " + cell + "]";
    }
  }
}
