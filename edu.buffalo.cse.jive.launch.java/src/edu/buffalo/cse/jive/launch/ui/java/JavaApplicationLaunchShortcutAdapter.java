package edu.buffalo.cse.jive.launch.ui.java;

import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaApplicationLaunchShortcut;

import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;

public class JavaApplicationLaunchShortcutAdapter extends JavaApplicationLaunchShortcut
{
  @Override
  protected ILaunchConfiguration createConfiguration(final IType type)
  {
    // adapt the work done by the superclass
    final ILaunchConfiguration config = super.createConfiguration(type);
    try
    {
      // try to create a new launch configuration
      final ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
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
    }
    catch (final CoreException exception)
    {
      // cowardly ignore
      // MessageDialog.openError(JDIDebugUIPlugin.getActiveWorkbenchShell(),
      // LauncherMessages.JavaLaunchShortcut_3, exception.getStatus().getMessage());
    }
    return config;
  }

  @Override
  protected ILaunchConfiguration findLaunchConfiguration(final IType type,
      final ILaunchConfigurationType configType)
  {
    // adapt the work done by the superclass
    final ILaunchConfiguration config = super.findLaunchConfiguration(type, configType);
    if (config != null)
    {
      try
      {
        // make sure the launch uses Jive
        final ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
        // Initialize the Jive modes
        final HashSet<String> jiveModes = new HashSet<String>();
        jiveModes.add(JiveLaunchPlugin.JIVE_MODE);
        wc.addModes(jiveModes);
        // save the launch configuration
        wc.doSave();
        // return modified config
        return config;
      }
      catch (final CoreException e)
      {
        // cowardly ignore
      }
    }
    return null;
  }
}