package edu.buffalo.cse.jive.ui.view.model.contour;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import edu.buffalo.cse.jive.model.IContourModel.IContourMember;

class ContourMemberTableLabelProvider extends LabelProvider implements ITableLabelProvider
{
  private final ContourMemberLabelProvider fVariableLabel = new ContourMemberLabelProvider();

  @Override
  public Image getColumnImage(final Object element, final int columnIndex)
  {
    // do nothing
    return null;
  }

  @Override
  public String getColumnText(final Object element, final int columnIndex)
  {
    // Export the contour member once
    if (columnIndex == 0)
    {
      fVariableLabel.setMember((IContourMember) element);
    }
    return fVariableLabel.text(columnIndex);
  }
}