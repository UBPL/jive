package edu.buffalo.cse.jive.internal.ui.view.contour.figures;

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.CompoundBorder;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FlowLayout;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import edu.buffalo.cse.jive.internal.ui.CustomLineBorder;
import edu.buffalo.cse.jive.ui.IContourAttributes;
import edu.buffalo.cse.jive.ui.VisualStatus;

/**
 * A Draw2d {@code Figure} used to visualize a {@code Contour} from a {@code ContourModel}.
 * 
 * @see edu.buffalo.cse.jive.model.IContour
 */
public class ContourFigure extends Figure
{
  /**
   * The border used around the contour's child compartment.
   */
  protected static final Border CHILD_COMPARTMENT_BORDER = new MarginBorder(4);
  /**
   * The background color for the contour.
   */
  protected static final Color CONTOUR_BACKGROUND_COLOR = new Color(null, 255, 255, 236);
  /**
   * The border used for the overall contour figure.
   */
  protected static final Border CONTOUR_BORDER = new LineBorder(1);
  /**
   * The border used around the contour's label.
   */
  protected static final Border LABEL_BORDER = new CompoundBorder(new CustomLineBorder(0, 0, 1, 0),
      new MarginBorder(1, 2, 1, 6));
  /**
   * The figure containing visualizations of the contour's children.
   */
  private Figure childCompartment;
  /**
   * The contour's label, which typically represents the contour ID.
   */
  private Label label;
  /**
   * The figure which visualizes the contour's members.
   */
  private ContourMemberTableFigure memberTable;

  // TODO Remove this and JavaContourFigure (and subclasses)
  public ContourFigure()
  {
  }

  /**
   * Constructs the contour figure.
   */
  public ContourFigure(final VisualStatus state, final IContourAttributes attributes)
  {
    super();
    switch (state)
    {
      case FULL:
        initializeFull(attributes);
        return;
      case NODE:
        initializeNode(attributes);
        return;
      case OUTLINE:
        initializeOutline(attributes);
        return;
      case EMPTY:
        initializeEmpty(attributes);
        return;
    }
    throw new IllegalStateException("State " + state + " is not implemented.");
  }

  @Override
  public void add(final IFigure figure, final Object constraint, final int index)
  {
    if (figure == label || figure == memberTable || figure == childCompartment)
    {
      super.add(figure, constraint, index);
    }
    else
    {
      childCompartment.add(figure, constraint, index);
    }
  }

  public ContourMemberTableFigure getMemberTable()
  {
    return memberTable;
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

  @Override
  public void remove(final IFigure figure)
  {
    if (figure == label || figure == memberTable || figure == childCompartment)
    {
      super.remove(figure);
    }
    else
    {
      childCompartment.remove(figure);
    }
  }

  private void initializeEmpty(final IContourAttributes attributes)
  {
    final FlowLayout layout = new FlowLayout();
    setLayoutManager(layout);
    initializeCompartment();
    childCompartment.setBorder(null);
    add(childCompartment);
  }

  private void initializeFull(final IContourAttributes attributes)
  {
    initializeContour();
    initializeLabel(attributes);
    initializeMemberTable();
    initializeCompartment();
    add(label);
    add(memberTable);
    add(childCompartment);
  }

  private void initializeNode(final IContourAttributes attributes)
  {
    final FlowLayout layout = new FlowLayout(false);
    setLayoutManager(layout);
    initializeLabel(attributes);
    label.setBorder(null);
    label.setBackgroundColor(null);
    initializeCompartment();
    childCompartment.setBorder(null);
    add(label);
    add(childCompartment);
  }

  private void initializeOutline(final IContourAttributes attributes)
  {
    final FlowLayout layout = new FlowLayout();
    setLayoutManager(layout);
    setBorder(ContourFigure.CONTOUR_BORDER);
    setBackgroundColor(ContourFigure.CONTOUR_BACKGROUND_COLOR);
    setOpaque(true);
    initializeCompartment();
    add(childCompartment);
  }

  /**
   * Initializes the contour figure's child compartment. This method is called from
   * {@link #initialize()}.
   */
  protected void initializeCompartment()
  {
    final ToolbarLayout layout = new ToolbarLayout(true); // TODO determine if we want horizontal or
                                                          // vertical arrangement
    layout.setSpacing(4);
    childCompartment = new Figure();
    childCompartment.setLayoutManager(layout);
    childCompartment.setOpaque(false);
    childCompartment.setBorder(ContourFigure.CHILD_COMPARTMENT_BORDER);
  }

  /**
   * Initializes the overall contour figure. This method is called from {@link #initialize()}.
   */
  protected void initializeContour()
  {
    final ToolbarLayout layout = new ToolbarLayout(false);
    layout.setStretchMinorAxis(true);
    setLayoutManager(layout);
    setOpaque(true);
    setBorder(ContourFigure.CONTOUR_BORDER);
    setBackgroundColor(ContourFigure.CONTOUR_BACKGROUND_COLOR);
  }

  /**
   * Initializes the contour figure's label. This method is called from {@link #initialize()}.
   */
  protected void initializeLabel(final IContourAttributes attributes)
  {
    label = new Label(attributes.getText(), attributes.getIcon());
    label.setOpaque(true);
    label.setToolTip(new Label(attributes.getToolTipText(), attributes.getToolTipIcon()));
    label.setBorder(ContourFigure.LABEL_BORDER);
    label.setBackgroundColor(attributes.getLabelBackgroundColor());
    label.setIconAlignment(PositionConstants.BOTTOM);
    label.setLabelAlignment(PositionConstants.LEFT);
  }

  /**
   * Initializes the contour figure's member table. This method is called from {@link #initialize()}
   * .
   */
  protected void initializeMemberTable()
  {
    memberTable = new ContourMemberTableFigure();
  }

  /**
   * A Draw2d figure used to visualize {@code JavaContour}s.
   * 
   * @see edu.buffalo.cse.jive.model.IContour.StaticContour
   */
  public static abstract class LabeledContourFigure extends ContourFigure
  {
    /**
     * The string used for the contour figure's label.
     */
    private final String labelText;
    /**
     * The string used for the tool tip of the contour figure's label.
     */
    private final String toolTipText;

    /**
     * Constructs the contour figure with the supplied identifier to be used by the contour figure's
     * label. The last portion of the identifier is used for the label, and the complete identifier
     * is used for the label's tool tip. The last portion is defined by all text after the last
     * occurrence of the delimiter returned by {@link #getContourTextDelimiter()}.
     * 
     * @param id
     *          the string for the contour figure's label
     */
    protected LabeledContourFigure(final String id)
    {
      super();
      final int index = id.lastIndexOf(getContourTextDelimiter());
      labelText = id.substring(index + 1);
      toolTipText = id;
    }

    /**
     * Returns the image used for the contour figure's label.
     * 
     * @return the image for the figure's label
     */
    protected abstract Image getContourImage();

    /**
     * Returns the delimiter to be used when determining the contour figure's label.
     * 
     * @return the delimiter for the contour figure's label
     * @see #JavaContourFigure(String)
     */
    protected abstract char getContourTextDelimiter();

    protected Image getLabelIcon()
    {
      return getContourImage();
    }

    protected String getLabelText()
    {
      return labelText;
    }

    protected Image getToolTipIcon()
    {
      return getContourImage();
    }

    protected String getToolTipText()
    {
      return toolTipText;
    }
  }
}