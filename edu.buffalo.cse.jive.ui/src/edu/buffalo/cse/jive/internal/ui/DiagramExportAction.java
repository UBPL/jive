package edu.buffalo.cse.jive.internal.ui;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_SAVE;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import edu.buffalo.cse.jive.ui.IJiveDiagramEditPart;
import edu.buffalo.cse.jive.ui.IUpdatableAction;

final class DiagramExportAction extends Action implements IUpdatableAction
{
  public static final String ACTION_ID = "ExportImageActionId";
  private final EditPartViewer viewer;

  DiagramExportAction(final EditPartViewer viewer)
  {
    super("Save As...");
    this.viewer = viewer;
    setId(DiagramExportAction.ACTION_ID);
    setImageDescriptor(IM_BASE_SAVE.enabledDescriptor());
    setDisabledImageDescriptor(IM_BASE_SAVE.disabledDescriptor());
  }

  @Override
  public void run()
  {
    final Shell shell = viewer.getControl().getShell();
    final FileDialog saveDialog = new FileDialog(shell, SWT.SAVE);
    saveDialog.setFilterExtensions(new String[]
    { "*.png", "*.jpg", "*.bmp" });
    saveDialog.setFilterNames(new String[]
    { "PNG format (*.png)", "JPEG format (*.jpg)", "Bitmap format (*.bmp)" });
    final String filePath = saveDialog.open();
    if (filePath == null || filePath.trim().length() == 0)
    {
      return;
    }
    int imageType = SWT.IMAGE_PNG;
    if (filePath.toLowerCase().endsWith(".jpg"))
    {
      imageType = SWT.IMAGE_JPEG;
    }
    else if (filePath.toLowerCase().endsWith(".bmp"))
    {
      imageType = SWT.IMAGE_BMP_RLE;
    }
    // REF: http://dev.eclipse.org/newslists/news.eclipse.tools.gef/msg14680.html
    final LayerManager lm = (LayerManager) viewer.getEditPartRegistry().get(LayerManager.ID);
    final IFigure printableFigure = lm.getLayer(LayerConstants.PRINTABLE_LAYERS);
    // HINT: http://blog.cypal-solutions.com/2008/03/exporting-gef-figure-to-image.html
    // TODO: http://eclipseo.blogspot.com/2008/11/print-your-gef-editor.html
    Image image = null;
    GC gc = null;
    SWTGraphics graphics = null;
    try
    {
      final Rectangle r = printableFigure.getBounds();
      image = new Image(viewer.getControl().getDisplay(), r.width, r.height);
      // System.err.println("Viewer: " + viewer);
      // System.err.println("Figure: " + printableFigure);
      // System.err.println("Child figures: " + printableFigure);
      // for (final Object o1 : printableFigure.getChildren()) {
      // System.err.println("child: " + o1.toString());
      // if (o1 instanceof IFigure) {
      // System.err.println("child: " + ((IFigure) o1).getBounds());
      // }
      // if (o1 instanceof Layer) {
      // for (final Object o2 : ((Layer) o1).getChildren()) {
      // System.err.println("grand child: " + o2.toString());
      // if (o2 instanceof SequenceDiagramFigure) {
      // for (final Object o3 : ((SequenceDiagramFigure) o2).getChildren()) {
      // System.err.println("great grand child: " + o3.toString());
      // }
      // }
      // }
      // }
      // if (o1 instanceof ConnectionLayer) {
      // for (final Object o2 : ((ConnectionLayer) o1).getChildren()) {
      // System.err.println("grand child: " + o2.toString());
      // if (o2 instanceof IFigure) {
      // System.err.println("child: " + ((IFigure) o2).getBounds());
      // }
      // }
      // }
      // }
      // System.err.println("Export dimensions: " + r);
      gc = new GC(image);
      gc.setAdvanced(true);
      gc.setAntialias(SWT.ON);
      gc.setTextAntialias(SWT.ON);
      // viewer.getControl().print(gc);
      graphics = new SWTGraphics(gc);
      graphics.setAdvanced(true);
      graphics.setAntialias(SWT.ON);
      graphics.setTextAntialias(SWT.ON);
      graphics.translate(-r.x, -r.y);
      printableFigure.paint(graphics);
    }
    finally
    {
      if (graphics != null)
      {
        graphics.dispose();
      }
      if (gc != null)
      {
        gc.dispose();
      }
    }
    if (saveImage(image, imageType, filePath))
    {
      MessageDialog.openInformation(shell, "Export Complete", "Diagram has been exported to "
          + filePath);
    }
  }

  @Override
  public void update()
  {
    setEnabled(checkEnabled());
  }

  private boolean checkEnabled()
  {
    return viewer != null && (viewer.getContents() instanceof IJiveDiagramEditPart);
  }

  private boolean saveImage(final Image image, final int imageType, final String filePath)
  {
    OutputStream stream = null;
    try
    {
      stream = new FileOutputStream(filePath);
      final ImageLoader imageLoader = new ImageLoader();
      imageLoader.data = new ImageData[]
      { image.getImageData() };
      imageLoader.save(stream, imageType);
    }
    catch (final IOException e)
    {
      e.printStackTrace();
      return false;
    }
    finally
    {
      if (stream != null)
      {
        try
        {
          stream.close();
        }
        catch (final IOException e)
        {
          e.printStackTrace();
        }
      }
      if (image != null)
      {
        image.dispose();
      }
    }
    return true;
  }
}