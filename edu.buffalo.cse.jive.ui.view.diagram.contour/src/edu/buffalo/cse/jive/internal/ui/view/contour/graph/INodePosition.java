package edu.buffalo.cse.jive.internal.ui.view.contour.graph;

public interface INodePosition
{
  /**
   * Section to which this node position references.
   */
  public DiagramSection section();

  public enum DiagramSection
  {
    // section that holds all reachable instance contours
    DS_REACHABLE,
    // section that holds all static contours
    DS_STATIC,
    // section that holds all orphan instance contours that have *some* association
    DS_UNREACHABLE,
    // section that holds all orphan instance contours that have no association
    DS_SINGLETONS
  }

  /**
   * This position supports a hierarchical layout of the Object Diagram:
   * 
   * <pre>
   *   a) Three horizontal sections-- static, reachable, and unreachable.
   *   
   *   b) The section figure is broken into one column figure per connected component;
   *      the column contains an upper layer with the contour figure corresponding to
   *      the root of the connected component; a bottom layer contains one column figure 
   *      for each child of the root figure.
   *      
   *   c) The scheme above is repeated for every depth-level of the traversal of the 
   *      connected component.
   * </pre>
   * 
   * As a consequence of the strategy above, all children of a given node are placed below it and
   * are centered with respect to the parent. This means that the contour graph has no horizontal
   * layout arrangement based on the maximum height of the siblings at a given depth level. Also, it
   * should be noted that the parent-child relationship is determined based on earliest traversal,
   * therefore, some back edges may exist in the graph.
   */
  public static interface IHierarchicalPosition extends INodePosition
  {
    public int column();

    public IHierarchicalPosition parent();
  }

  /**
   * This position supports a tabular layout of the Object Diagram:
   * 
   * <pre>
   *   a) Three horizontal sections-- static, reachable, and unreachable.
   *   
   *   b) In each section, a column is created for each connected component of the 
   *      contour graph. The column has a vertical flow layout.
   * 
   *   c) within each column, a layer is created at every depth level to hold all 
   *      sibling contours at that depth. All layers have the same height and the 
   *      figures within the layer are centered with respect to the layer above.
   *   
   *   d) within each layer, a cell is created to hold the actual contour figure.
   * </pre>
   * 
   * As a consequence of the strategy above, all nodes at a particular depth are placed on a layer
   * that guarantees a fixed height. Hence, all figures in a layer are centered with respect to all
   * figures in the layer above. This *does not* imply that figures are centered below one of their
   * parents.
   */
  public static interface ITabularPosition extends INodePosition
  {
    public int cell();

    public int column();

    public int layer();
  }
}