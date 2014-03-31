package edu.buffalo.cse.jive.ui;

import org.eclipse.jface.viewers.StructuredViewer;

public interface IJiveStructuredView extends IJiveView
{
  public StructuredViewer getViewer();
}