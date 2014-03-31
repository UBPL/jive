package edu.buffalo.cse.jive.internal.ui;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.viewers.StructuredViewer;

import edu.buffalo.cse.jive.ui.IDiagramOutputActionFactory;
import edu.buffalo.cse.jive.ui.IUpdatableAction;
import edu.buffalo.cse.jive.ui.view.AbstractStructuredJiveView;

final class DiagramOutputActionFactory implements IDiagramOutputActionFactory
{
  @Override
  public IUpdatableAction createDiagramExportAction(final EditPartViewer viewer)
  {
    return new DiagramExportAction(viewer);
  }

  @Override
  public IUpdatableAction createPrintAction(final EditPartViewer viewer)
  {
    return new DiagramPrintAction(viewer);
  }

  @Override
  public IUpdatableAction createTraceCopyEventsAction(final StructuredViewer viewer,
      final boolean isXML)
  {
    return new TraceCopyEventsAction(viewer, isXML);
  }

  @Override
  public IUpdatableAction createTraceExportAction(final AbstractStructuredJiveView view)
  {
    return new TraceExportAction(view);
  }
}
