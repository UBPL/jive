package edu.buffalo.cse.jive.launch.ui.offline;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

public class OfflineLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup
{
  @Override
  public void createTabs(final ILaunchConfigurationDialog dialog, final String mode)
  {
    setTabs(new ILaunchConfigurationTab[]
    { new OfflineTab() });
  }
}
