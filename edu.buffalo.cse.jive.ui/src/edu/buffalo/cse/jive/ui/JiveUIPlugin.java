package edu.buffalo.cse.jive.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import edu.buffalo.cse.jive.internal.ui.UICoreFactoryImpl;
import edu.buffalo.cse.jive.ui.search.JiveSearchPageDescriptor;

/**
 * The activator class that controls the plug-in life cycle and provides utility methods.
 */
public class JiveUIPlugin extends AbstractUIPlugin
{
  public static final String PLUGIN_ID = "edu.buffalo.cse.jive.ui";
  // IDs of the elements contributed to the Eclipse UI
  public static final String ID_CONTOUR_DIAGRAM_VIEW = JiveUIPlugin.PLUGIN_ID
      + ".contourDiagramView";
  public static final String ID_CONTOUR_MODEL_VIEW = JiveUIPlugin.PLUGIN_ID + ".contourModelView";
  public static final String ID_JIVE_CATEGORY = JiveUIPlugin.PLUGIN_ID + ".jiveCategory";
  public static final String ID_JIVE_PERSPECTIVE = JiveUIPlugin.PLUGIN_ID + ".jivePerspective";
  public static final String ID_JIVE_TAB = JiveUIPlugin.PLUGIN_ID + ".jiveTab";
  public static final String ID_SEARCH_PAGES_EXTENSION_POINT = JiveUIPlugin.PLUGIN_ID
      + ".searchPages";
  public static final String ID_SEQUENCE_DIAGRAM_VIEW = JiveUIPlugin.PLUGIN_ID
      + ".sequenceDiagramView";
  public static final String ID_SEQUENCE_MODEL_VIEW = JiveUIPlugin.PLUGIN_ID + ".sequenceModelView";
  public static final String ID_TRACE_VIEW = JiveUIPlugin.PLUGIN_ID + ".traceView";
  /**
   * Shared instance of the plug-in.
   */
  private static JiveUIPlugin plugin;

  /**
   * Returns the shared instance of the JIVE UI plug-in.
   * 
   * @return the shared instance
   */
  public static JiveUIPlugin getDefault()
  {
    return JiveUIPlugin.plugin;
  }

  /**
   * Returns the standard display to be used. The method first checks, if the thread calling this
   * method has an associated display. If so, this display is returned. Otherwise the method returns
   * the default display.
   */
  public static Display getStandardDisplay()
  {
    Display display = Display.getCurrent();
    if (display == null)
    {
      display = Display.getDefault();
    }
    return display;
  }

  /**
   * Logs a status object to the Eclipse error log.
   * 
   * @param status
   *          the status object to record
   */
  public static void log(final IStatus status)
  {
    JiveUIPlugin.getDefault().getLog().log(status);
  }

  /**
   * Logs a string to the Eclipse error log as an {@code IStatus.ERROR} object.
   * 
   * @param message
   *          the message to be recorded
   */
  public static void log(final String message)
  {
    JiveUIPlugin
        .log(new Status(IStatus.ERROR, JiveUIPlugin.PLUGIN_ID, IStatus.ERROR, message, null));
  }

  /**
   * Logs the message associated with a {@code Throwable} object to the Eclipse error log as an
   * {@code IStatus.ERROR} object.
   * 
   * @param e
   *          the {@code Throwable} object whose message is recorded
   */
  public static void log(final Throwable e)
  {
    JiveUIPlugin.log(new Status(IStatus.ERROR, JiveUIPlugin.PLUGIN_ID, IStatus.ERROR, e
        .getMessage(), e));
  }

  /**
   * Delegate factory responsible for creating all UI support factories.
   */
  private UICoreFactory coreFactory;

  /**
   * Constructs the JIVE UI plug-in. This constructor is called by the Eclipse platform and should
   * not be called by clients.
   * 
   * @throws IllegalStateException
   *           if the plug-in has already been instantiated
   */
  public JiveUIPlugin()
  {
    if (JiveUIPlugin.plugin != null)
    {
      // TODO Add log message and internationalize the string literal
      throw new IllegalStateException("The JIVE UI plug-in class already exists.");
    }
  }

  public UICoreFactory coreFactory()
  {
    return this.coreFactory;
  }

  public IJumpMenuManager createJumpMenuManager(final EditPartViewer viewer)
  {
    return coreFactory().createJumpMenuManager(viewer);
  }

  public ISliceMenuManager createSliceMenuManager()
  {
    return coreFactory().createSliceMenuManager();
  }

  public IStepActionFactory createStepActionFactory(final IJiveView view)
  {
    return coreFactory().createStepActionFactory(view);
  }

  public IDiagramOutputActionFactory getDiagramOutputActionFactory()
  {
    return coreFactory().diagramOutputFactory();
  }

  /**
   * Returns a list of search page descriptors available to the plug-in.
   * 
   * @see JiveSearchPageDescriptor
   * @return a list of search page descriptors
   */
  public List<JiveSearchPageDescriptor> getSearchPageDescriptors()
  {
    final List<JiveSearchPageDescriptor> result = new ArrayList<JiveSearchPageDescriptor>();
    final IExtensionRegistry registry = Platform.getExtensionRegistry();
    final IConfigurationElement[] elements = registry
        .getConfigurationElementsFor(JiveUIPlugin.ID_SEARCH_PAGES_EXTENSION_POINT);
    for (final IConfigurationElement element : elements)
    {
      if (JiveSearchPageDescriptor.PAGE_TAG.equals(element.getName()))
      {
        result.add(new JiveSearchPageDescriptor(element));
      }
    }
    return result;
  }

  public IThreadColorManager getThreadColorManager()
  {
    return coreFactory().threadColorManager();
  }

  @Override
  public void start(final BundleContext context) throws Exception
  {
    super.start(context);
    JiveUIPlugin.plugin = this;
    this.coreFactory = new UICoreFactoryImpl();
  }

  public IStepManager stepManager()
  {
    return coreFactory().stepManager();
  }

  @Override
  public void stop(final BundleContext context) throws Exception
  {
    coreFactory().dispose();
    JiveUIPlugin.plugin = null;
    super.stop(context);
  }
}