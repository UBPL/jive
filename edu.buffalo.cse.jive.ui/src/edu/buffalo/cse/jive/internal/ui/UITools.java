package edu.buffalo.cse.jive.internal.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public final class UITools
{
  public static Composite createComposite(final Composite parent, final int columns)
  {
    final Composite control = new Composite(parent, SWT.NONE);
    control.setLayout(new GridLayout(columns, true));
    final GridData layoutData = new GridData(GridData.FILL, GridData.FILL, true, true);
    layoutData.horizontalSpan = 1;
    control.setLayoutData(layoutData);
    return control;
  }

  private UITools()
  {
    // block instantiation
  }
}