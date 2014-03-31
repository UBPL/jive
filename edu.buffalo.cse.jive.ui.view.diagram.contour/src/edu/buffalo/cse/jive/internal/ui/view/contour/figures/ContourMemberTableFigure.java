package edu.buffalo.cse.jive.internal.ui.view.contour.figures;

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.BorderLayout;
import org.eclipse.draw2d.CompoundBorder;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import edu.buffalo.cse.jive.internal.ui.CustomLineBorder;
import edu.buffalo.cse.jive.ui.IContourAttributes;
import edu.buffalo.cse.jive.ui.IMemberAttributes;

public class ContourMemberTableFigure extends Figure
{
  private static final Border CELL_BORDER = new CompoundBorder(new CustomLineBorder(0, 0, 1, 0),
      new MarginBorder(1, 2, 1, 6));
  private static final Border IDENTIFIER_COLUMN_BORDER = new CompoundBorder(new CustomLineBorder(0,
      0, 0, 0), new MarginBorder(0));
  private static final Border TYPE_COLUMN_BORDER = new CompoundBorder(new CustomLineBorder(0, 1, 0,
      0), new MarginBorder(0));
  private static final Border VALUE_COLUMN_BORDER = new CompoundBorder(new CustomLineBorder(0, 1,
      0, 0), new MarginBorder(0));
  private final IFigure identifierColumn;
  private final IFigure typeColumn;
  private final IFigure valueColumn;

  public ContourMemberTableFigure()
  {
    identifierColumn = new Figure();
    typeColumn = new Figure();
    valueColumn = new Figure();
    identifierColumn.setBorder(ContourMemberTableFigure.IDENTIFIER_COLUMN_BORDER);
    typeColumn.setBorder(ContourMemberTableFigure.TYPE_COLUMN_BORDER);
    valueColumn.setBorder(ContourMemberTableFigure.VALUE_COLUMN_BORDER);
    final ToolbarLayout identifierLayout = new ToolbarLayout(false);
    identifierLayout.setStretchMinorAxis(true);
    identifierColumn.setLayoutManager(identifierLayout);
    final ToolbarLayout typeLayout = new ToolbarLayout(false);
    typeLayout.setStretchMinorAxis(true);
    typeColumn.setLayoutManager(typeLayout);
    final ToolbarLayout valueLayout = new ToolbarLayout(false);
    valueLayout.setStretchMinorAxis(true);
    valueColumn.setLayoutManager(valueLayout);
    final BorderLayout layout = new BorderLayout();
    setLayoutManager(layout);
    add(identifierColumn, BorderLayout.LEFT);
    add(typeColumn, BorderLayout.CENTER);
    add(valueColumn, BorderLayout.RIGHT);
  }

  public void addMember(final IMemberAttributes attributes)
  {
    identifierColumn.add(createIdentifierLabel(attributes));
    typeColumn.add(createTypeLabel(attributes));
    valueColumn.add(createValueLabel(attributes));
  }

  @Override
  public void paint(final Graphics graphics)
  {
    graphics.setAntialias(SWT.ON);
    graphics.setTextAntialias(SWT.ON);
    super.paint(graphics);
    graphics.setAntialias(SWT.OFF);
    graphics.setTextAntialias(SWT.OFF);
  }

  private Label createIdentifierLabel(final IMemberAttributes attributes)
  {
    final Label result = new Label(attributes.getIdentifierText(), attributes.getIdentifierIcon());
    result.setBorder(ContourMemberTableFigure.CELL_BORDER);
    result.setLabelAlignment(PositionConstants.LEFT);
    result.setToolTip(new Label(attributes.getIdentifierToolTipText(), null));
    result.setOpaque(getLineOpaque(attributes));
    result.setBackgroundColor(getLineColor(attributes));
    return result;
  }

  private Label createTypeLabel(final IMemberAttributes attributes)
  {
    final Label result = new Label(attributes.getTypeText(), attributes.getTypeIcon());
    result.setBorder(ContourMemberTableFigure.CELL_BORDER);
    result.setLabelAlignment(PositionConstants.LEFT);
    result.setToolTip(new Label(attributes.getTypeToolTipText(), attributes.getTypeToolTipIcon()));
    result.setOpaque(getLineOpaque(attributes));
    result.setBackgroundColor(getLineColor(attributes));
    return result;
  }

  private Label createValueLabel(final IMemberAttributes attributes)
  {
    final Label result = new Label(attributes.getValueText(), attributes.getValueIcon());
    result.setBorder(ContourMemberTableFigure.CELL_BORDER);
    result.setLabelAlignment(PositionConstants.LEFT);
    result
        .setToolTip(new Label(attributes.getValueToolTipText(), attributes.getValueToolTipIcon()));
    result.setTextPlacement(PositionConstants.WEST);
    result.setOpaque(getLineOpaque(attributes) || attributes.isOutOfModel());
    if (!attributes.isVarRpdl() && !attributes.isVarResult())
    {
      if (attributes.isOutOfModel())
      {
        result.setBackgroundColor(IContourAttributes.BACKGROUND_COLOR_OUT_OF_MODEL);
      }
      // else if (attributes.isGarbageCollected())
      // {
      // result.setBackgroundColor(IContourAttributes.BACKGROUND_COLOR_GARBAGE_COLLECTED);
      // }
      else
      {
        result.setBackgroundColor(getLineColor(attributes));
      }
    }
    else
    {
      result.setBackgroundColor(getLineColor(attributes));
    }
    return result;
  }

  /**
   * Returns a color only if the **line** is opaque; otherwise, returns null.
   */
  private Color getLineColor(final IMemberAttributes attributes)
  {
    if (attributes.isVarRpdl() || attributes.isVarResult())
    {
      return IContourAttributes.BACKGROUND_COLOR_JIVE_VARS;
    }
    if (attributes.isVarArgument())
    {
      return IContourAttributes.BACKGROUND_COLOR_ARGUMENT;
    }
    if (attributes.isVarOutOfScope())
    {
      return IContourAttributes.BACKGROUND_COLOR_OUT_OF_SCOPE;
    }
    return null;
  }

  /**
   * Returns true if the **line** should be opaque; otherwise returns false. Note that only for the
   * value that should be opaque (out-of-model), this method returns false.
   */
  private boolean getLineOpaque(final IMemberAttributes attributes)
  {
    return attributes.isVarArgument() || attributes.isVarOutOfScope() || attributes.isVarRpdl()
        || attributes.isVarResult();
  }
}