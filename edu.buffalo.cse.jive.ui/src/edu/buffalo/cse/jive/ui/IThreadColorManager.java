package edu.buffalo.cse.jive.ui;

import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.graphics.Color;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;

public interface IThreadColorManager extends IPropertyChangeListener, ILaunchListener
{
  public void addThreadColorListener(IThreadColorListener listener);

  public void removeThreadColorListener(IThreadColorListener listener);

  public Color threadColor(IJiveDebugTarget target, IThreadValue threadId);
}
