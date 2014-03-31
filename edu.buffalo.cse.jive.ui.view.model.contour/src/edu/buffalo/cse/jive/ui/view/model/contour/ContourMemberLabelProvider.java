package edu.buffalo.cse.jive.ui.view.model.contour;

import edu.buffalo.cse.jive.model.IContourModel.IContourMember;

class ContourMemberLabelProvider
{
  private static final int NAME_COLUMN = 0;
  private static final int TYPE_COLUMN = 1;
  private static final int VALUE_COLUMN = 2;
  private IContourMember member;

  void setMember(final IContourMember member)
  {
    this.member = member;
  }

  String text(final int columnIndex)
  {
    switch (columnIndex)
    {
      case NAME_COLUMN:
        return member.schema().name();
      case TYPE_COLUMN:
        return member.schema().type().node().name();
      case VALUE_COLUMN:
        return member.value().toString();
      default:
        throw new IllegalArgumentException("Invalid column index:  " + columnIndex);
    }
  }
}