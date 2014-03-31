package edu.buffalo.cse.jive.ui.view.model.contour;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_TREE;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.ui.view.AbstractStructuredJiveView;

/**
 * A view part to present {@code Contour}s associated with {@code IJiveDebugTarget}s. The content
 * provider used by this view is specific to Java contour models. The view uses a JFace
 * {@code TreeViewer} to display the contours and a JFace {@code TableViewer} to display the member
 * table of the selected contour. Controls are also available to step and run through model
 * transactions in both the forward and reverse directions. However, these controls are currently
 * limited to target's that are already terminated.
 * 
 * @see IJiveDebugTarget
 * @see TreeViewer
 */
public class ContourModelView extends AbstractStructuredJiveView implements ISelectionListener
{
  private static final IJiveDebugTarget activeTarget()
  {
    return JiveLaunchPlugin.getDefault().getLaunchManager().activeTarget();
  }

  TableViewer fMemberTableViewer;

  public IExecutionModel contourModel()
  {
    final IJiveDebugTarget target = ContourModelView.activeTarget();
    return target != null ? target.model() : null;
  }

  @Override
  public void createPartControl(final Composite parent)
  {
    super.createPartControl(parent);
    getSite().getPage().addPostSelectionListener(this);
  }

  @Override
  public void dispose()
  {
    getSite().getPage().removePostSelectionListener(this);
    super.dispose();
  }

  @Override
  public TreeViewer getViewer()
  {
    return (TreeViewer) super.getViewer();
  }

  @Override
  public void selectionChanged(final IWorkbenchPart part, final ISelection selection)
  {
    // Update the member table if the selection is a Contour
    if (part instanceof ContourModelView && selection instanceof IStructuredSelection)
    {
      if (!selection.isEmpty())
      {
        final Object object = ((IStructuredSelection) selection).getFirstElement();
        if (object instanceof IContour)
        {
          fMemberTableViewer.setInput(object);
        }
      }
    }
  }

  @Override
  protected IStructuredContentProvider createContentProvider()
  {
    return new ContourModelContentProvider(this);
  }

  @Override
  protected IBaseLabelProvider createLabelProvider()
  {
    return new ContourModelLabelProvider();
  }

  @Override
  protected StructuredViewer createViewer(final Composite parent)
  {
    final SashForm form = new SashForm(parent, SWT.NONE);
    final TreeViewer treeViewer = ContourModelUtils.appendTreeViewer(form);
    fMemberTableViewer = ContourModelUtils.appendTableViewer(form, treeViewer);
    return treeViewer;
  }

  @Override
  protected String getDefaultContentDescription()
  {
    return "No contour models to display at this time.";
  }

  @Override
  protected ImageDescriptor getDisplayDropDownDisabledImageDescriptor()
  {
    return IM_BASE_TREE.disabledDescriptor();
  }

  @Override
  protected ImageDescriptor getDisplayDropDownEnabledImageDescriptor()
  {
    return IM_BASE_TREE.enabledDescriptor();
  }

  @Override
  protected String getDisplayTargetDropDownText()
  {
    return "Display Contour Model";
  }
}