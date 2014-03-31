package edu.buffalo.cse.jive.console;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class JiveConsolePlugin implements BundleActivator
{
  private static JiveConsolePlugin plugin;
  private CommandProvider service;

  public static JiveConsolePlugin getDefault()
  {
    return JiveConsolePlugin.plugin;
  }

  @Override
  public void start(final BundleContext context) throws Exception
  {
    service = new JiveCommandProvider();
    context.registerService(CommandProvider.class.getName(), service, null);
  }

  @Override
  public void stop(final BundleContext context) throws Exception
  {
    service = null;
  }
}
