package edu.buffalo.cse.jive.ui;

import org.eclipse.jface.action.IMenuManager;

import edu.buffalo.cse.jive.model.IContourModel.IContour;

public interface ISliceMenuManager
{
  public void createSliceMenu(IContour contour, IMenuManager manager);
}
