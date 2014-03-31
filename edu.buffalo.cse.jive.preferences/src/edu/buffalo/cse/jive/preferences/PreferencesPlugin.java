package edu.buffalo.cse.jive.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class that controls the plug-in life cycle and provides utility methods.
 */
public class PreferencesPlugin extends AbstractUIPlugin implements IPropertyChangeListener
{
  // prefix of all non-internal constants
  public static final String ID_BASE = "edu.buffalo.cse.jive.preferences";
  // plugin identifier
  public static final String ID_PLUGIN = PreferencesPlugin.ID_BASE + ".PreferencesPlugin";
  /**
   * Shared instance of the plug-in.
   */
  private static PreferencesPlugin plugin;

  /**
   * Returns the shared instance of the JIVE UI plug-in.
   * 
   * @return the shared instance
   */
  public static PreferencesPlugin getDefault()
  {
    return PreferencesPlugin.plugin;
  }

  /**
   * Logs a status object to the Eclipse error log.
   * 
   * @param status
   *          the status object to record
   */
  public static void log(final IStatus status)
  {
    PreferencesPlugin.getDefault().getLog().log(status);
  }

  /**
   * Logs a string to the Eclipse error log as an {@code IStatus.ERROR} object.
   * 
   * @param message
   *          the message to be recorded
   */
  public static void log(final String message)
  {
    PreferencesPlugin.log(new Status(IStatus.ERROR, PreferencesPlugin.ID_PLUGIN, IStatus.ERROR,
        message, null));
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
    PreferencesPlugin.log(new Status(IStatus.ERROR, PreferencesPlugin.ID_PLUGIN, IStatus.ERROR, e
        .getMessage(), e));
  }

  /**
   * Width in pixels of activations on the sequence diagram.
   */
  private int activationWidth;
  /**
   * Height in pixels of events on the sequence diagram.
   */
  private int eventHeight;

  /**
   * Constructs the JIVE UI plug-in. This constructor is called by the Eclipse platform and should
   * not be called by clients.
   * 
   * @throws IllegalStateException
   *           if the plug-in has already been instantiated
   */
  public PreferencesPlugin()
  {
    if (PreferencesPlugin.plugin != null)
    {
      // TODO Add log message and internationalize the string literal
      throw new IllegalStateException("The JIVE Preferences plug-in class already exists.");
    }
  }

  public int eventHeight()
  {
    return eventHeight;
  }

  public int getActivationWidth()
  {
    return activationWidth;
  }

  public String getExclusionFiltersKey()
  {
    return PreferenceKeys.PREF_FILTERS_COMMON;
  }

  public String getGenerateLocalEventsKey()
  {
    return PreferenceKeys.PREF_GENERATE_LOCAL_EVENTS;
  }

  public String getGenerateArrayEventsKey()
  {
    return PreferenceKeys.PREF_GENERATE_ARRAY_EVENTS;
  }

  public String getManualStartEventsKey()
  {
    return PreferenceKeys.PREF_MANUAL_START;
  }

  public String getOfflineURLKey()
  {
    return PreferenceKeys.PREF_OFFLINE_URL;
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event)
  {
    final String property = event.getProperty();
    if (property.equals(PreferenceKeys.PREF_SD_ACTIVATION_WIDTH))
    {
      final IPreferenceStore store = getPreferenceStore();
      // Preferences prefs = JiveUIPlugin.getDefault().getPluginPreferences();
      activationWidth = store.getInt(PreferenceKeys.PREF_SD_ACTIVATION_WIDTH);
    }
    else if (property.equals(PreferenceKeys.PREF_SD_EVENT_HEIGHT))
    {
      final IPreferenceStore store = getPreferenceStore();
      // Preferences prefs = JiveUIPlugin.getDefault().getPluginPreferences();
      eventHeight = store.getInt(PreferenceKeys.PREF_SD_EVENT_HEIGHT);
    }
  }

  @Override
  public void start(final BundleContext context) throws Exception
  {
    super.start(context);
    PreferencesPlugin.plugin = this;
    // Initialize the sequence diagram attributes
    final IPreferenceStore store = getPreferenceStore();
    activationWidth = store.getInt(PreferenceKeys.PREF_SD_ACTIVATION_WIDTH);
    eventHeight = store.getInt(PreferenceKeys.PREF_SD_EVENT_HEIGHT);
    store.addPropertyChangeListener(this);
  }

  @Override
  public void stop(final BundleContext context) throws Exception
  {
    getPreferenceStore().removePropertyChangeListener(this);
    PreferencesPlugin.plugin = null;
    super.stop(context);
  }

  public void updateConfiguration(final ILaunchConfigurationWorkingCopy configuration,
      final String preferenceName)
  {
    final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
    final List<String> filterList = updateDefaultFilters(store.getString(preferenceName));
    configuration.setAttribute(getExclusionFiltersKey(), filterList);
  }

  private ArrayList<String> updateDefaultFilters(final String javaPreferenceFilter)
  {
    final ArrayList<String> filterList = new ArrayList<String>();
    for (String filter : javaPreferenceFilter.split(PreferenceInitializer.EVENT_FILTER_DELIMITER))
    {
      filter = filter.trim();
      if (!filter.isEmpty())
      {
        filterList.add(filter);
      }
    }
    return filterList;
  }

  @Override
  protected void initializeImageRegistry(final ImageRegistry reg)
  {
    for (final ImageInfo info : ImageInfo.values())
    {
      reg.put(info.enabledKey(),
          ImageDescriptor.createFromURL(getBundle().getEntry(info.enabledPath())));
      reg.put(info.disabledKey(),
          ImageDescriptor.createFromURL(getBundle().getEntry(info.disabledPath())));
    }
  }
}