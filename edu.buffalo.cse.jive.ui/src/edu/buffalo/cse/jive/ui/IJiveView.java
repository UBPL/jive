package edu.buffalo.cse.jive.ui;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;

/**
 * A view part capable of presenting an <code>IJiveDebugTarget</code>. Typically, a target is
 * presented in terms of one of its underlying models (e.g. the event log, contour model, or
 * sequence model).
 */
public interface IJiveView extends IViewPart
{
  /**
   * Changes the view to present the supplied <code>IJiveDebugTarget</code>, making it the new
   * active target. Implementers may determine if the target should actually be presented.
   * <p>
   * This method should only be called on the UI thread. In order for non-UI threads to invoke this
   * method, the following can be done.
   * <p>
   * 
   * <pre>
   *     IJiveView view = ... ;
   *     IJiveDebugTarget target = ... ;
   *     
   *     Display display = JiveUITools.getStandardDisplay();
   *     display.syncExec(new Runnable() {
   *         public void run() {
   *             view.display(target);
   *         }
   *     });
   * </pre>
   * 
   * @param target
   *          the target to display in the view
   * @see JiveUITools#getStandardDisplay()
   * @see Display#asyncExec(Runnable)
   * @see Display#syncExec(Runnable)
   */
  public void display(IJiveDebugTarget target);
}
