package edu.buffalo.cse.jive.ui;

import org.eclipse.debug.core.ILaunchListener;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;

public interface ISourceLookupFacility extends ILaunchListener, IStepListener
{
  /**
   * Selects the source line on corresponding to the given event in the source editor window.
   */
  public void selectLine(IJiveDebugTarget target, IJiveEvent event);
}