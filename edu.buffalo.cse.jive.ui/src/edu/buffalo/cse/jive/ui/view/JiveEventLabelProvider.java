package edu.buffalo.cse.jive.ui.view;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_SM_EXECUTIONS_FILTERED_METHOD_ACTIVATION;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_SM_EXECUTIONS_METHOD_ACTIVATION;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.preferences.ImageInfo;

public class JiveEventLabelProvider extends LabelProvider
{
  @Override
  public Image getImage(final Object element)
  {
    if (element instanceof IJiveEvent)
    {
      final IJiveEvent event = (IJiveEvent) element;
      if (event instanceof IMethodCallEvent)
      {
        final IMethodCallEvent mca = (IMethodCallEvent) event;
        if (!mca.inModel())
        {
          return IM_SM_EXECUTIONS_FILTERED_METHOD_ACTIVATION.enabledImage();
        }
        else
        {
          return IM_SM_EXECUTIONS_METHOD_ACTIVATION.enabledImage();
        }
      }
      return (event == null) ? null : ImageInfo.imageInfo(event.kind()).enabledImage();
    }
    throw new IllegalStateException("Element " + element + " has an invalid type.");
  }

  public ImageDescriptor getImageDescriptor(final Object element)
  {
    if (element instanceof IJiveEvent)
    {
      final IJiveEvent event = (IJiveEvent) element;
      if (event instanceof IMethodCallEvent)
      {
        final IMethodCallEvent mca = (IMethodCallEvent) event;
        if (!mca.inModel())
        {
          return IM_SM_EXECUTIONS_FILTERED_METHOD_ACTIVATION.enabledDescriptor();
        }
        else
        {
          return IM_SM_EXECUTIONS_METHOD_ACTIVATION.enabledDescriptor();
        }
      }
      return (event == null) ? null : ImageInfo.imageInfo(event.kind()).enabledDescriptor();
    }
    throw new IllegalStateException("Element " + element + " has an invalid type.");
  }

  @Override
  public String getText(final Object element)
  {
    if (element instanceof IJiveEvent)
    {
      return (element == null) ? "" : element.toString();
    }
    throw new IllegalStateException("Element " + element + " has an invalid type.");
  }
}
