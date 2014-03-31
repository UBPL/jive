package edu.buffalo.cse.jive.launch;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;

import edu.buffalo.cse.jive.internal.launch.JiveVMDebugger;
import edu.buffalo.cse.jive.internal.launch.VMInstallProxy;
import edu.buffalo.cse.jive.model.IJiveProject;

public abstract class LaunchFactory
{
  private static final ConcurrentMap<ILaunchConfigurationType, IJiveLaunchFactory> factoryMap = new ConcurrentHashMap<ILaunchConfigurationType, IJiveLaunchFactory>();
  private static final Random SOURCE = new Random();
  private static int CURRENT = LaunchFactory.SOURCE.nextInt(100);

  public static IJiveLaunchFactory createFactory(final ILaunchConfiguration configuration)
  {
    IJiveLaunchFactory factory = null;
    try
    {
      factory = LaunchFactory.factoryMap.get(configuration.getType());
      if (factory == null)
      {
        LaunchFactory.factoryMap.putIfAbsent(configuration.getType(), new DefaultLaunchFactory());
      }
      return LaunchFactory.factoryMap.get(configuration.getType());
    }
    catch (final CoreException e)
    {
      e.printStackTrace();
    }
    return null;
  }

  public static void registerFactory(final ILaunchConfigurationType type,
      final IJiveLaunchFactory factory)
  {
    LaunchFactory.factoryMap.put(type, factory);
  }

  public static void unregisterFactory(final ILaunchConfigurationType type,
      final IJiveLaunchFactory factory)
  {
    if (LaunchFactory.factoryMap.get(type) == factory)
    {
      LaunchFactory.factoryMap.remove(type);
    }
  }

  public static class DefaultLaunchFactory implements IJiveLaunchFactory
  {
    /**
     * The default behavior is to create a IJiveProject instance encapsulating a single JavaProject.
     * For server targets, e.g., Tomcat, there is no project associated with the launch. Thus, it is
     * the Tomcat Launch Plugin that should update the java projects in the jive project.
     */
    @Override
    public IJiveProject createJiveProject(final ILaunch launch)
    {
      final IJiveProject project = new JiveProject(launch);
      final ILaunchConfiguration lc = launch.getLaunchConfiguration();
      ILaunchConfigurationWorkingCopy wc;
      try
      {
        wc = lc.getWorkingCopy();
        final String projectName = wc.getAttribute(
            IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
        final IWorkspaceRoot wr = ResourcesPlugin.getWorkspace().getRoot();
        for (final IProject p : wr.getProjects())
        {
          if (p.isAccessible() && p.getName().equals(projectName))
          {
            project.addProject(p);
          }
        }
      }
      catch (final CoreException e)
      {
        e.printStackTrace();
      }
      return project;
    }

    @Override
    public final IVMInstall createVMInstall(final IVMInstall subject)
    {
      return new VMInstallProxy(subject);
    }

    @Override
    public final IVMRunner createVMRunner(final String mode, final IVMInstall subject)
    {
      if (ILaunchManager.DEBUG_MODE.equals(mode))
      {
        return new JiveVMDebugger(subject);
      }
      throw new IllegalStateException("JIVE must be used in debug mode.");
    }
  }

  protected static class JiveProject implements IJiveProject
  {
    protected static synchronized int nextTargetId()
    {
      LaunchFactory.CURRENT = LaunchFactory.CURRENT + 1 + LaunchFactory.SOURCE.nextInt(10);
      return LaunchFactory.CURRENT;
    }

    private final ILaunch launch;
    private final int targetId;
    private final Set<IJavaProject> javaProjects;

    private JiveProject(final ILaunch launch)
    {
      this.launch = launch;
      this.targetId = JiveProject.nextTargetId();
      this.javaProjects = new HashSet<IJavaProject>();
    }

    private JiveProject(final ILaunch launch, final IProject project)
    {
      this(launch);
      addProject(project);
    }

    @Override
    public void addProject(final Object project)
    {
      if (project instanceof IProject)
      {
        final IJavaProject jp = JavaCore.create((IProject) project);
        javaProjects.add(jp);
      }
    }

    @Override
    public Object findType(final String typeName)
    {
      try
      {
        // cycle through all java projects in this jive project
        for (final IJavaProject javaProject : javaProjects)
        {
          final Object result = javaProject.findType(typeName);
          if (result != null)
          {
            return result;
          }
        }
      }
      catch (final JavaModelException e)
      {
        // nothing we can do
      }
      return null;
    }

    @Override
    public Object launch()
    {
      return launch;
    }

    @Override
    public String name()
    {
      return launch.getLaunchConfiguration() == null ? launch.toString() : launch
          .getLaunchConfiguration().toString();
    }

    @Override
    public int targetId()
    {
      return targetId;
    }
  }
}