package edu.buffalo.cse.jive.launch.ui.offline;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_JIVE;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import edu.buffalo.cse.jive.preferences.PreferencesPlugin;

public class OfflineTab extends AbstractLaunchConfigurationTab
{
  // Offline tab identifier
  public final String ID_OFFLINE_TAB = PreferencesPlugin.ID_BASE + ".offlineTab";
  /**
   * The name appearing on the tab.
   */
  public static final String OFFLINE_TAB_NAME = "JIVE (offline)"; //$NON-NLS-1$
  private OfflineTabForm tabForm;

  @Override
  public void createControl(final Composite parent)
  {
    // create the form
    tabForm = new OfflineTabForm(parent);
    // Set the controls for the tab
    setControl(tabForm.control());
  }

  @Override
  public Image getImage()
  {
    return IM_BASE_JIVE.enabledImage();
  }

  @Override
  public String getName()
  {
    return OfflineTab.OFFLINE_TAB_NAME;
  }

  public String getTabId()
  {
    return ID_OFFLINE_TAB;
  }

  @Override
  public void initializeFrom(final ILaunchConfiguration configuration)
  {
    // Initialize the controls based on the launch configuration
    try
    {
      // Initialize the project name
      tabForm.projectText.setText(configuration.getAttribute(
          IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""));
      // Initialize the URL
      tabForm.urlText.setText(configuration.getAttribute(PreferencesPlugin.getDefault()
          .getOfflineURLKey(), ""));
    }
    catch (final CoreException e)
    {
      // cowardly ignore for now
    }
  }

  @Override
  public void performApply(final ILaunchConfigurationWorkingCopy configuration)
  {
    if (isDirty())
    {
      // project name
      configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
          tabForm.projectText.getText());
      // offline target
      configuration.setAttribute(PreferencesPlugin.getDefault().getOfflineURLKey(),
          tabForm.urlText.getText());
    }
  }

  @Override
  public void setDefaults(final ILaunchConfigurationWorkingCopy configuration)
  {
    String projectName = null;
    final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    final IWorkbenchPart part = window.getPartService().getActivePart();
    // try to resolve the project for the active editor window
    if (part instanceof IEditorPart)
    {
      final IEditorPart editor = (IEditorPart) part;
      final IEditorInput input = editor.getEditorInput();
      if (input instanceof IFileEditorInput)
      {
        projectName = ((IFileEditorInput) input).getFile().getProject().getName();
      }
    }
    // try to resolve the project for the current selection in the package explorer
    if (projectName == null)
    {
      final ISelection selection = window.getSelectionService().getSelection(
          "org.eclipse.jdt.ui.PackageExplorer");
      // try to use the selection on the
      if (selection instanceof IStructuredSelection)
      {
        final Object o = ((IStructuredSelection) selection).getFirstElement();
        if (o instanceof IResource)
        {
          projectName = ((IResource) o).getProject().getName();
        }
        else if (o instanceof IProject)
        {
          projectName = ((IProject) o).getName();
        }
        else if (o instanceof IJavaElement)
        {
          projectName = ((IJavaElement) o).getJavaProject().getProject().getName();
        }
        else if (o instanceof IAdaptable)
        {
          projectName = ((IProject) ((IAdaptable) o)
              .getAdapter(org.eclipse.core.resources.IProject.class)).getName();
        }
      }
    }
    // Initialize the project name
    if (projectName != null)
    {
      configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
    }
  }

  public void widgetModified()
  {
    setDirty(true);
    updateLaunchConfigurationDialog();
  }

  private final class OfflineTabForm
  {
    private final Composite control;
    /**
     * The text field used to set the project name.
     */
    private Text projectText;
    /**
     * The text field used to set the offline source URL.
     */
    private Text urlText;

    OfflineTabForm(final Composite parent)
    {
      control = createComposite(parent, 1);
      createProjectText();
      createURLText();
      projectText.setFocus();
    }

    public Control control()
    {
      return control;
    }

    private Composite createComposite(final Composite parent, final int columns)
    {
      final Composite control = new Composite(parent, SWT.NONE);
      control.setLayout(new GridLayout(columns, true));
      final GridData layoutData = new GridData(GridData.FILL, GridData.FILL, true, true);
      layoutData.horizontalSpan = 1;
      control.setLayoutData(layoutData);
      return control;
    }

    private void createProjectText()
    {
      final Label l = new Label(control, SWT.LEFT);
      l.setText("Project Name:");
      projectText = new Text(control, SWT.BORDER);
      projectText.setEnabled(true);
      projectText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      projectText.setToolTipText("Project Name");
      projectText.addKeyListener(new KeyListener()
        {
          @Override
          public void keyPressed(final KeyEvent e)
          {
          }

          @Override
          public void keyReleased(final KeyEvent e)
          {
            widgetModified();
          }
        });
    }

    private void createURLText()
    {
      final Label l = new Label(control, SWT.LEFT);
      l.setText("Trace URL:");
      urlText = new Text(control, SWT.BORDER);
      urlText.setEnabled(true);
      urlText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      urlText.setToolTipText("Trace URL");
      urlText.addKeyListener(new KeyListener()
        {
          @Override
          public void keyPressed(final KeyEvent e)
          {
          }

          @Override
          public void keyReleased(final KeyEvent e)
          {
            widgetModified();
          }
        });
    }
  }
}