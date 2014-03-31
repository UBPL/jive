package edu.buffalo.cse.jive.command;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.RegistryFactory;
import org.osgi.framework.BundleContext;

public class JiveCommandPlugin extends Plugin
{
  private static JiveCommandPlugin plugin;
  private final Map<String, IJiveCommand> CMDS = new HashMap<String, IJiveCommand>();

  public JiveCommandPlugin()
  {
    if (JiveCommandPlugin.plugin != null)
    {
      throw new IllegalStateException("The JIVE console plug-in class already exists.");
    }
    JiveCommandPlugin.plugin = this;
  }

  public static JiveCommandPlugin getDefault()
  {
    return JiveCommandPlugin.plugin;
  }

  public void registerJiveCommand(IJiveCommand command)
  {
    if (command != null)
    {
      CMDS.put(command.getCommand(), command);
    }
  }

  public Map<String, IJiveCommand> getJiveCommands()
  {
    return CMDS;
  }

  @Override
  public void start(final BundleContext context) throws Exception
  {
    super.start(context);
    registerJiveCommands();
  }

  private void registerJiveCommands()
  {
    final IExtensionRegistry registry = RegistryFactory.getRegistry();
    final IExtensionPoint[] extpts = registry.getExtensionPoints("edu.buffalo.cse.jive.command");
    for (final IExtensionPoint ep : extpts)
    {
      if (ep.getSimpleIdentifier().equals("jiveConsoleCommands"))
      {
        for (final IExtension e : ep.getExtensions())
        {
          for (final IConfigurationElement ce : e.getConfigurationElements())
          {
            try
            {
              IJiveCommand jc = (IJiveCommand) Class.forName(ce.getAttribute("class"))
                  .newInstance();
              CMDS.put(jc.getCommand(), jc);
            }
            catch (final InvalidRegistryObjectException ex)
            {
              throw new RuntimeException(ex);
            }
            catch (final InstantiationException ex)
            {
              throw new RuntimeException(ex);
            }
            catch (final IllegalAccessException ex)
            {
              throw new RuntimeException(ex);
            }
            catch (final ClassNotFoundException ex)
            {
              throw new RuntimeException(ex);
            }
          }
        }
      }
    }
  }
}
