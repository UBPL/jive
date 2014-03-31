package edu.buffalo.cse.jive.ui.view;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;

/**
 * An abstract content provider used to provide model elements associated with
 * {@code IJiveDebugTarget}s. Methods for providing model elements to the viewer, registering with a
 * model, and unregistering from a model are to be implemented by derived classes. These methods
 * will be called at the appropriate time by this class.
 * 
 * @see #getModelElements(IJiveDebugTarget)
 * @see #subscribeToModel(IJiveDebugTarget)
 * @see #unsubscribeFromModel(IJiveDebugTarget)
 */
public abstract class AbstractJiveContentProvider implements IStructuredContentProvider
{
  @Override
  public final Object[] getElements(final Object inputElement)
  {
    return getModelElements((IJiveDebugTarget) inputElement);
  }

  @Override
  public final void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput)
  {
    if (oldInput != null)
    {
      unsubscribeFromModel((IJiveDebugTarget) oldInput);
    }
    if (newInput != null)
    {
      subscribeToModel((IJiveDebugTarget) newInput);
    }
  }

  /**
   * Returns the model elements that should be provided to the viewer when the input has changed.
   * 
   * @param target
   *          the input of the content provider
   * @return the model elements to provide
   * @see #getElements(Object)
   * @see #inputChanged(Viewer, Object, Object)
   */
  protected abstract Object[] getModelElements(IJiveDebugTarget target);

  /**
   * Registers itself as a listener to a model of the supplied {@code IJiveDebugTarget}. This method
   * is called by {@link #inputChanged(Viewer, Object, Object)} after calling
   * {@link #unsubscribeFromModel(IJiveDebugTarget)} with the old input as the parameter.
   * 
   * @param newInput
   *          the new input of the content provider
   * @see #inputChanged(Viewer, Object, Object)
   */
  protected abstract void subscribeToModel(IJiveDebugTarget newInput);

  /**
   * Unregisters itself from being a listener to a model of the supplied {@code IJiveDebugTarget}.
   * This method is called by {@link #inputChanged(Viewer, Object, Object)} before calling
   * {@link #subscribeToModel(IJiveDebugTarget)} with the new input as the parameter.
   * 
   * @param oldInput
   *          the new input of the content provider
   * @see #inputChanged(Viewer, Object, Object)
   */
  protected abstract void unsubscribeFromModel(IJiveDebugTarget oldInput);
}