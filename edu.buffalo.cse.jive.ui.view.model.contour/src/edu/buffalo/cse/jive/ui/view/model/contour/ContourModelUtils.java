package edu.buffalo.cse.jive.ui.view.model.contour;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class ContourModelUtils
{
  public static TableViewer appendTableViewer(final SashForm parent, final TreeViewer treeViewer)
  {
    final TableViewer viewer = ContourModelUtils.createTableViewer(parent);
    // Customize the member table
    final Table table = viewer.getTable();
    table.setHeaderVisible(true);
    final String[] columnNames =
    { "Name", "Type", "Value" };
    final int[] columnAlignments =
    { SWT.LEFT, SWT.LEFT, SWT.LEFT };
    final int[] columnWidths =
    { 120, 180, 180 };
    for (int i = 0; i < columnNames.length; i++)
    {
      final TableColumn column = new TableColumn(table, columnAlignments[i]);
      column.setText(columnNames[i]);
      column.setWidth(columnWidths[i]);
    }
    // Customize the splitter
    parent.setWeights(new int[]
    { 70, 30 });
    parent.setOrientation(SWT.VERTICAL);
    return viewer;
  }

  public static TreeViewer appendTreeViewer(final SashForm parent)
  {
    // Create a tree viewer for the contours
    final TreeViewer contourViewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL);
    contourViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
    return contourViewer;
  }

  private static TableViewer createTableViewer(final SashForm parent)
  {
    // Create a table viewer for the contour member tables
    final TableViewer viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL
        | SWT.FULL_SELECTION);
    viewer.setContentProvider(new ContourMemberTableContentProvider());
    viewer.setLabelProvider(new ContourMemberTableLabelProvider());
    return viewer;
  }
}
