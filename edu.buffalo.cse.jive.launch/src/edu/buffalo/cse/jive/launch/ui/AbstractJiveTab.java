package edu.buffalo.cse.jive.launch.ui;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_JIVE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.core.LaunchConfiguration;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import edu.buffalo.cse.jive.debug.JiveDebugPlugin;
import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;
import edu.buffalo.cse.jive.preferences.PreferenceKeys;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;

/**
 * A configuration tab used to enable debugging with JIVE and to modify the default JIVE settings
 * for the corresponding launch.
 */
@SuppressWarnings("restriction")
public abstract class AbstractJiveTab extends AbstractLaunchConfigurationTab
{
  // JIVE tab identifier
  public final String ID_JIVE_TAB = PreferencesPlugin.ID_BASE + ".jiveTab";
  /**
   * The name appearing on the tab.
   */
  public static final String JIVE_TAB_NAME = "JIVE"; //$NON-NLS-1$
  /**
   * The set of modes relevant to JIVE.
   */
  private final HashSet<String> jiveModes;
  private JiveTabForm jiveTabForm;

  public AbstractJiveTab()
  {
    jiveModes = new HashSet<String>();
    jiveModes.add(JiveLaunchPlugin.JIVE_MODE);
  }

  @Override
  public void createControl(final Composite parent)
  {
    // create the form
    jiveTabForm = new JiveTabForm(this, parent);
    // Set the controls for the tab
    setControl(jiveTabForm.control());
    // Mark the tab as clean
    setDirty(false);
  }

  @Override
  public Image getImage()
  {
    return IM_BASE_JIVE.enabledImage();
  }

  @Override
  public String getName()
  {
    return AbstractJiveTab.JIVE_TAB_NAME;
  }

  public String getPreferenceName()
  {
    return PreferenceKeys.PREF_FILTERS_COMMON;
  }

  public String getTabId()
  {
    return ID_JIVE_TAB;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void initializeFrom(final ILaunchConfiguration configuration)
  {
    // Initialize the controls based on the launch configuration
    try
    {
      // Initialize the enable JIVE checkbox
      final Set<String> modes = configuration.getAttribute(LaunchConfiguration.ATTR_LAUNCH_MODES,
          Collections.EMPTY_SET);
      if (modes.isEmpty())
      {
        jiveTabForm.enableJive(false);
      }
      else
      {
        jiveTabForm.enableJive(modes.containsAll(jiveModes));
      }
      // Initialize the manual start flag
      final boolean manualStart = configuration.getAttribute(PreferencesPlugin.getDefault()
          .getManualStartEventsKey(), false);
      jiveTabForm.enableManualStart(manualStart);
      // Initialize the local events flag
      final boolean generateLocalEvents = configuration.getAttribute(PreferencesPlugin.getDefault()
          .getGenerateLocalEventsKey(), true);
      jiveTabForm.enableLocalEvents(generateLocalEvents);
      // Initialize the lock events flag
      final boolean generateLockEvents = configuration.getAttribute(PreferencesPlugin.getDefault()
          .getGenerateArrayEventsKey(), false);
      jiveTabForm.enableArrayEvents(generateLockEvents);
      // Initialize the exclusion filters list
      List<String> exclusionFilters = configuration.getAttribute(PreferencesPlugin.getDefault()
          .getExclusionFiltersKey(), (List<String>) null);
      // Set the default filters for launch configurations created before exclusion
      // filters were supported
      if (exclusionFilters == null)
      {
        final ILaunchConfigurationWorkingCopy workingCopy = configuration.getWorkingCopy();
        setDefaultFilters(workingCopy);
        exclusionFilters = workingCopy.getAttribute(PreferencesPlugin.getDefault()
            .getExclusionFiltersKey(), Collections.EMPTY_LIST);
        workingCopy.doSave();
      }
      jiveTabForm.updateFilters(exclusionFilters);
    }
    catch (final CoreException e)
    {
      JiveDebugPlugin.log(e);
    }
    // Enable the controls only if launching in debug mode
    final String mode = getLaunchConfigurationDialog().getMode();
    jiveTabForm.enableControls(mode.equals(ILaunchManager.DEBUG_MODE));
  }

  @Override
  public void performApply(final ILaunchConfigurationWorkingCopy configuration)
  {
    if (isDirty())
    {
      // Add or remove the relevant JIVE modes from the configuration
      if (jiveTabForm.isJiveEnabled())
      {
        configuration.addModes(jiveModes);
      }
      else
      {
        configuration.removeModes(jiveModes);
      }
      // control the manual start of JIVE
      configuration.setAttribute(PreferencesPlugin.getDefault().getManualStartEventsKey(),
          jiveTabForm.isManualStart());
      // control the generation of local events
      configuration.setAttribute(PreferencesPlugin.getDefault().getGenerateLocalEventsKey(),
          jiveTabForm.generateLocalEvents());
      // control the generation of lock events
      configuration.setAttribute(PreferencesPlugin.getDefault().getGenerateArrayEventsKey(),
          jiveTabForm.generateArrayEvents());
      // Add the exclusion filters to the configuration
      final List<String> filters = new ArrayList<String>();
      filters.addAll(jiveTabForm.filterList());
      configuration.setAttribute(PreferencesPlugin.getDefault().getExclusionFiltersKey(), filters);
    }
  }

  /**
   * Sets the default event filters.
   * 
   * @param configuration
   *          the launch configuration
   */
  public void setDefaultFilters(final ILaunchConfigurationWorkingCopy configuration)
  {
    if (jiveTabForm != null)
    {
      PreferencesPlugin.getDefault().updateConfiguration(configuration, getPreferenceName());
    }
  }

  @Override
  public void setDefaults(final ILaunchConfigurationWorkingCopy configuration)
  {
    setDefaultFilters(configuration);
  }

  public void widgetModified()
  {
    setDirty(true);
    updateLaunchConfigurationDialog();
  }
}