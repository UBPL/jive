package edu.buffalo.cse.jive.ui;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

public interface IContourAttributes
{
  // light gray (instance contour title bars)
  public final Color BACKGROUND_COLOR_INTERFACE = new Color(null, 212, 212, 255);
  // light orange (class contour title bars and lifeline headers)
  public final Color BACKGROUND_COLOR_CLASS = new Color(null, 255, 236, 212);
  // light yellow (object contour title bars and lifeline headers)
  public final Color BACKGROUND_COLOR_OBJECT = new Color(null, 255, 255, 176);
  // light green (arguments in method contours)
  public final Color BACKGROUND_COLOR_ARGUMENT = new Color(null, 212, 255, 212);
  // light olive-gray
  public final Color BACKGROUND_COLOR_JIVE_VARS = new Color(null, 236, 236, 184);
  // light purple (out-of-model values)
  public final Color BACKGROUND_COLOR_OUT_OF_MODEL = new Color(null, 212, 176, 176);
  // light violet blue (garbage collected object reference values)
  public final Color BACKGROUND_COLOR_GARBAGE_COLLECTED = new Color(null, 108, 108, 255);
  // light gray (out-of-scope local variables)
  public final Color BACKGROUND_COLOR_OUT_OF_SCOPE = new Color(null, 224, 224, 224);

  public Image getIcon();

  public Color getLabelBackgroundColor();

  public String getText();

  public Image getToolTipIcon();

  public String getToolTipText();
}