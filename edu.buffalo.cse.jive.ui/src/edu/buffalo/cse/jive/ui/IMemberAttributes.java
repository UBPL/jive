package edu.buffalo.cse.jive.ui;

import org.eclipse.swt.graphics.Image;

public interface IMemberAttributes
{
  public Image getIdentifierIcon();

  public String getIdentifierText();

  public String getIdentifierToolTipText();

  public Image getTypeIcon();

  public String getTypeText();

  public Image getTypeToolTipIcon();

  public String getTypeToolTipText();

  public Image getValueIcon();

  public String getValueText();

  public Image getValueToolTipIcon();

  public String getValueToolTipText();

  public boolean isField();

  // public boolean isGarbageCollected();

  public boolean isOutOfModel();

  public boolean isVarArgument();

  public boolean isVarOutOfScope();

  public boolean isVarResult();

  public boolean isVarRpdl();
}