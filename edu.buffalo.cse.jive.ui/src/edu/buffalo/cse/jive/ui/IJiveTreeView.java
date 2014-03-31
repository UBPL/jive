package edu.buffalo.cse.jive.ui;

import org.eclipse.jface.viewers.TreeViewer;

public interface IJiveTreeView extends IJiveStructuredView
{
  @Override
  public TreeViewer getViewer();
}