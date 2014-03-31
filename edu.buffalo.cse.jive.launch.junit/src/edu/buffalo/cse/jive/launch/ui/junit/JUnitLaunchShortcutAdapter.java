package edu.buffalo.cse.jive.launch.ui.junit;

import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;

import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;

/**
 * TODO: This shortcut is unable to identify an existing launch configuration similar to the one
 * being launched via the shortcut. This seems to be a problem with the JUnitLaunchShortcut class
 * but requires a more careful investigation.
 * 
 */
public class JUnitLaunchShortcutAdapter extends JUnitLaunchShortcut
{
  @Override
  protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(final IJavaElement element)
      throws CoreException
  {
    // adapt the work done by the superclass
    final ILaunchConfigurationWorkingCopy wc = super.createLaunchConfiguration(element);
    // Initialize the manual start flag
    wc.setAttribute(PreferencesPlugin.getDefault().getManualStartEventsKey(), false);
    // Initialize the local events flag
    wc.setAttribute(PreferencesPlugin.getDefault().getGenerateLocalEventsKey(), true);
    // Initialize the lock events flag
    wc.setAttribute(PreferencesPlugin.getDefault().getGenerateArrayEventsKey(), false);
    // Initialize the exclusion filters list
    PreferencesPlugin.getDefault().updateConfiguration(wc,
        PreferencesPlugin.getDefault().getExclusionFiltersKey());
    // Initialize the Jive modes
    final HashSet<String> jiveModes = new HashSet<String>();
    jiveModes.add(JiveLaunchPlugin.JIVE_MODE);
    wc.addModes(jiveModes);
    // save the launch configuration
    wc.doSave();
    // return the updated working copy
    return wc;
  }
}