package edu.buffalo.cse.jive.ui.view.model.contour;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;

class ContourModelLabelProvider extends LabelProvider
{
  /**
   * Label provider of the contour being processed.
   */
  private DefaultContourLabelProvider fLabelProvider = null;
  /**
   * Last contour for which a label was provided, used so a contour is not exported more than once.
   */
  private IContour fLastContour = null;
  private final DefaultContourLabelProvider fInstanceContourLabelProvider = DefaultContourLabelProvider
      .createInstanceContourLabelProvider();
  private final DefaultContourLabelProvider fMethodContourLabelProvider = DefaultContourLabelProvider
      .createMethodContourLabelProvider();
  private final DefaultContourLabelProvider fStaticContourLabelProvider = DefaultContourLabelProvider
      .createStaticContourLabelProvider();

  @Override
  public Image getImage(final Object element)
  {
    exportContour((IContour) element);
    return fLabelProvider.image();
  }

  @Override
  public String getText(final Object element)
  {
    exportContour((IContour) element);
    return fLabelProvider.text();
  }

  /**
   * Uses the provided contour as the data provider for the label.
   */
  private void exportContour(final IContour contour)
  {
    if (contour != fLastContour)
    {
      setLabel(contour);
      fLabelProvider.setContour(contour);
      fLastContour = contour;
    }
  }

  private void setLabel(final IContour contour)
  {
    if (contour instanceof IContextContour)
    {
      if (((IContextContour) contour).isStatic())
      {
        fLabelProvider = fStaticContourLabelProvider;
      }
      else
      {
        fLabelProvider = fInstanceContourLabelProvider;
      }
    }
    if (contour instanceof IMethodContour)
    {
      fLabelProvider = fMethodContourLabelProvider;
    }
  }
}