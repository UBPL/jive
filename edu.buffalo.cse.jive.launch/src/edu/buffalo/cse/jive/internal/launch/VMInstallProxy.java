package edu.buffalo.cse.jive.internal.launch;

import java.io.File;
import java.net.URL;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.AbstractVMInstall;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.LibraryLocation;

/**
 * An implementation of an <code>IVMInstall</code> that delegates nearly all requests to a subject
 * <code>IVMInstall</code>. The only exception is <code>getVMRunner()</code>, which is overridden to
 * return a specialized <code>IVMRunner</code> needed to debug with JIVE enabled.
 * 
 * @see VMInstallProxy#getVMRunner(String)
 */
public class VMInstallProxy implements IVMInstall
{
  /**
   * The subject to which the proxy delegates requests.
   */
  private final AbstractVMInstall subject;

  /**
   * Constructs a proxy for the supplied <code>IVMInstall</code>.
   * 
   * @param subject
   *          the object used as a delegate
   */
  public VMInstallProxy(final IVMInstall subject)
  {
    this.subject = (AbstractVMInstall) subject;
  }

  @Override
  public String getId()
  {
    return subject.getId();
  }

  @Override
  public File getInstallLocation()
  {
    return subject.getInstallLocation();
  }

  @Override
  public URL getJavadocLocation()
  {
    return subject.getJavadocLocation();
  }

  @Override
  public LibraryLocation[] getLibraryLocations()
  {
    return subject.getLibraryLocations();
  }

  @Override
  public String getName()
  {
    return subject.getName();
  }

  @Override
  public String[] getVMArguments()
  {
    return subject.getVMArguments();
  }

  @Override
  public IVMInstallType getVMInstallType()
  {
    return subject.getVMInstallType();
  }

  /**
   * Returns a VM runner that runs this installed VM with JIVE debugging enabled if the given mode
   * is <code>org.eclipse.debug.core.ILaunchManager.DEBUG_MODE</code>. Otherwise, an exception is
   * thrown.
   * 
   * @param mode
   *          the mode the VM should be launched in; only
   *          <code>org.eclipse.debug.core.ILaunchManager.DEBUG_MODE</code> is applicable
   * @return a VM runner used to debug with JIVE enabled
   * @throws IllegalStateException
   *           if a mode other than <code>org.eclipse.debug.core.ILaunchManager.DEBUG_MODE</code> is
   *           supplied
   * @see org.eclipse.jdt.launching.IVMInstall#getVMRunner(java.lang.String)
   * @see org.eclipse.debug.core.ILaunchManager.DEBUG_MODE
   */
  @Override
  public IVMRunner getVMRunner(final String mode)
  {
    if (ILaunchManager.DEBUG_MODE.equals(mode))
    {
      return new JiveVMDebugger(this);
    }
    throw new IllegalStateException("JIVE must be used in debug mode.");
  }

  @Override
  public void setInstallLocation(final File installLocation)
  {
    subject.setInstallLocation(installLocation);
  }

  @Override
  public void setJavadocLocation(final URL url)
  {
    subject.setJavadocLocation(url);
  }

  @Override
  public void setLibraryLocations(final LibraryLocation[] locations)
  {
    subject.setLibraryLocations(locations);
  }

  @Override
  public void setName(final String name)
  {
    subject.setName(name);
  }

  @Override
  public void setVMArguments(final String[] vmArgs)
  {
    subject.setVMArguments(vmArgs);
  }
}