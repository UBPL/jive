package edu.buffalo.cse.jive.ui.view.model.contour;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import edu.buffalo.cse.jive.model.IContourModel.IContour;

class ContourMemberTableContentProvider implements IStructuredContentProvider
{
  @Override
  public void dispose()
  {
    // do nothing
  }

  @Override
  public Object[] getElements(final Object inputElement)
  {
    return ((IContour) inputElement).members().toArray();
  }

  @Override
  public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput)
  {
    // do nothing
  }
}