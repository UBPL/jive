package edu.buffalo.cse.jive.ui.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IPluginContribution;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import edu.buffalo.cse.jive.ui.JiveUIPlugin;

public class JiveSearchPageDescriptor implements IPluginContribution
{
  public static final String CLASS_ATTRIBUTE = "class";
  public static final String ICON_ATTRIBUTE = "icon";
  public static final String ID_ATTRIBUTE = "id";
  public static final String NAME_ATTRIBUTE = "name";
  public static final String PAGE_TAG = "page";
  private final IConfigurationElement pageElement;
  private IJiveSearchPage searchPage;

  public JiveSearchPageDescriptor(final IConfigurationElement element)
  {
    pageElement = element;
  }

  @Override
  public String getLocalId()
  {
    return pageElement.getAttribute(JiveSearchPageDescriptor.ID_ATTRIBUTE);
  }

  @Override
  public String getPluginId()
  {
    return pageElement.getContributor().getName();
  }

  public IJiveSearchPage getSearchPage() throws CoreException
  {
    if (searchPage == null)
    {
      searchPage = (IJiveSearchPage) pageElement
          .createExecutableExtension(JiveSearchPageDescriptor.CLASS_ATTRIBUTE);
    }
    return searchPage;
  }

  public Image getSearchQueryIcon()
  {
    final ImageRegistry registry = JiveUIPlugin.getDefault().getImageRegistry();
    final String key = getPluginId() + "/" + getLocalId();
    Image icon = registry.get(key);
    if (icon == null)
    {
      try
      {
        final ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
            getPluginId(), getIconPath());
        registry.put(key, descriptor);
        icon = registry.get(key);
      }
      // TODO Catch the right exception and log accordingly.
      // Also, handle the case when no icon is given better.
      catch (final Exception e)
      {
        // JiveUIPlugin.log(e);
        return null;
      }
    }
    return icon;
  }

  public String getSearchQueryName()
  {
    return pageElement.getAttribute(JiveSearchPageDescriptor.NAME_ATTRIBUTE);
  }

  private String getIconPath()
  {
    return pageElement.getAttribute(JiveSearchPageDescriptor.ICON_ATTRIBUTE);
  }
}
