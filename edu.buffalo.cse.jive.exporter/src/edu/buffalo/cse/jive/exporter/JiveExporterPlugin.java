package edu.buffalo.cse.jive.exporter;

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

public class JiveExporterPlugin extends Plugin
{
  private static JiveExporterPlugin plugin;
  private final Map<String, IJiveExporter> CMDS = new HashMap<String, IJiveExporter>();

  public JiveExporterPlugin()
  {
    if (JiveExporterPlugin.plugin != null)
    {
      throw new IllegalStateException("The JIVE exporter plug-in class already exists.");
    }
    JiveExporterPlugin.plugin = this;
  }

  public static JiveExporterPlugin getDefault()
  {
    return JiveExporterPlugin.plugin;
  }

  public void registerJiveExporter(IJiveExporter exporter)
  {
    if (exporter != null)
    {
      CMDS.put(exporter.getElement(), exporter);
    }
  }

  public Map<String, IJiveExporter> getJiveExporters()
  {
    return CMDS;
  }

  @Override
  public void start(final BundleContext context) throws Exception
  {
    super.start(context);
    registerJiveExporters();
  }

  private void registerJiveExporters()
  {
    final IExtensionRegistry registry = RegistryFactory.getRegistry();
    final IExtensionPoint[] extpts = registry.getExtensionPoints("edu.buffalo.cse.jive.exporter");
    for (final IExtensionPoint ep : extpts)
    {
      if (ep.getSimpleIdentifier().equals("jiveExporters"))
      {
        for (final IExtension e : ep.getExtensions())
        {
          for (final IConfigurationElement ce : e.getConfigurationElements())
          {
            try
            {
              IJiveExporter je = (IJiveExporter) Class.forName(ce.getAttribute("class"))
                  .newInstance();
              CMDS.put(je.getElement(), je);
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
