package edu.buffalo.cse.jive.internal.ui.view.contour.editparts;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IModel.IContourReference;
import edu.buffalo.cse.jive.model.IModel.IOutOfModelMethodReference;
import edu.buffalo.cse.jive.model.IModel.IValue;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.ui.IJiveEditPart;
import edu.buffalo.cse.jive.ui.IThreadColorManager;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;

public class ContourConnectionEditPart extends AbstractConnectionEditPart implements IJiveEditPart
{
  public ContourConnectionEditPart(final Object model)
  {
    super();
    setModel(model);
  }

  @Override
  protected void createEditPolicies()
  {
    // TODO Determine if anything should be done here
  }

  @Override
  protected IFigure createFigure()
  {
    final PolylineConnection connection = new PolylineConnection();
    // connection.setAntialias(SWT.ON);
    connection.setLineStyle(SWT.LINE_SOLID);
    // connection.setLineWidthFloat(2.5f);
    connection.setLineWidth(2);
    connection.setTargetDecoration(new PolygonDecoration());
    final IContourMember instance = (IContourMember) getModel();
    // return point reference
    if (instance.schema().modifiers().contains(NodeModifier.NM_RPDL))
    {
      final ContourDiagramEditPart contents = (ContourDiagramEditPart) getRoot().getContents();
      final IJiveDebugTarget target = contents.getModel();
      IMethodContour contour = null;
      final IValue value = instance.value();
      if (value.isMethodContourReference())
      {
        contour = (IMethodContour) ((IContourReference) value).contour();
      }
      else if (value.isOutOfModelMethodReference())
      {
        final IOutOfModelMethodReference targetValue = (IOutOfModelMethodReference) value;
        contour = targetValue.method();
        connection.setLineStyle(SWT.LINE_CUSTOM); // LINE_DOT
        connection.setLineDash(new float[]
        { 5.0f, 2.0f });
        // connection.setLineWidthFloat(2.25f);
        connection.setLineWidth(2);
      }
      final IThreadColorManager manager = JiveUIPlugin.getDefault().getThreadColorManager();
      final Color c = manager.threadColor(target, contour.thread());
      connection.setForegroundColor(c);
    }
    // local variable reference
    else if (instance.schema().kind() != NodeKind.NK_FIELD)
    {
      connection.setLineStyle(SWT.LINE_CUSTOM); // LINE_DOT
      connection.setLineDash(new float[]
      { 5.0f, 2.0f });
      connection.setLineWidth(2);
      // connection.setLineWidthFloat(2.25f);
      connection.setLineStyle(SWT.LINE_DASH);
      connection.setForegroundColor(ColorConstants.gray);
    }
    // field reference
    else
    {
      connection.setLineWidth(1);
      // connection.setLineWidthFloat(1.25f);
      connection.setForegroundColor(ColorConstants.darkGray);
    }
    return connection;
  }
}
