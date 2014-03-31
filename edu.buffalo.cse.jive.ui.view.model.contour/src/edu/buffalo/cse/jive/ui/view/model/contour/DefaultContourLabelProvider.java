package edu.buffalo.cse.jive.ui.view.model.contour;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_OM_CONTOUR_INSTANCE;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_OM_CONTOUR_METHOD;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_OM_CONTOUR_STATIC;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import edu.buffalo.cse.jive.model.IContourModel.IContour;

/**
 * An abstract <code>Contour</code> exporter used to provide labels to an
 * <code>IStructuredContentProvider</code>.
 */
abstract class DefaultContourLabelProvider extends LabelProvider
{
  private static final Image INSTANCE_CONTOUR_IMAGE = IM_OM_CONTOUR_INSTANCE.enabledImage();
  private static final Image METHOD_CONTOUR_IMAGE = IM_OM_CONTOUR_METHOD.enabledImage();
  private static final Image STATIC_CONTOUR_IMAGE = IM_OM_CONTOUR_STATIC.enabledImage();

  static DefaultContourLabelProvider createInstanceContourLabelProvider()
  {
    return new DefaultContourLabelProvider()
      {
        @Override
        Image image()
        {
          return DefaultContourLabelProvider.INSTANCE_CONTOUR_IMAGE;
        }
      };
  }

  static DefaultContourLabelProvider createMethodContourLabelProvider()
  {
    return new DefaultContourLabelProvider()
      {
        @Override
        Image image()
        {
          return DefaultContourLabelProvider.METHOD_CONTOUR_IMAGE;
        }
      };
  }

  static DefaultContourLabelProvider createStaticContourLabelProvider()
  {
    return new DefaultContourLabelProvider()
      {
        @Override
        Image image()
        {
          return DefaultContourLabelProvider.STATIC_CONTOUR_IMAGE;
        }
      };
  }

  private IContour contour;

  private DefaultContourLabelProvider()
  {
  }

  abstract Image image();

  void setContour(final IContour contour)
  {
    this.contour = contour;
  }

  String text()
  {
    if (contour == null)
    {
      throw new IllegalStateException();
    }
    return contour.signature().toString();
  }
}