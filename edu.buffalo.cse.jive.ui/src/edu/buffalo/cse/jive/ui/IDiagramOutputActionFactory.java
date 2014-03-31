package edu.buffalo.cse.jive.ui;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.viewers.StructuredViewer;

import edu.buffalo.cse.jive.ui.view.AbstractStructuredJiveView;

public interface IDiagramOutputActionFactory
{
  public IUpdatableAction createDiagramExportAction(final EditPartViewer viewer);

  public IUpdatableAction createPrintAction(final EditPartViewer viewer);

  public IUpdatableAction createTraceCopyEventsAction(final StructuredViewer viewer,
      final boolean isXML);

  // TODO: decouple the parameter type
  public IUpdatableAction createTraceExportAction(final AbstractStructuredJiveView view);
}