package edu.buffalo.cse.jive.core.ast;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;

public class ASTPluginProxy
{
  private static Plugin OWNER;

  public static void log(final IStatus status)
  {
    if (ASTPluginProxy.getLog() != null)
    {
      ASTPluginProxy.getLog().log(status);
    }
  }

  public static void log(final String message)
  {
    ASTPluginProxy.log(message, null);
  }

  public static void log(final Throwable e)
  {
    ASTPluginProxy.log(e.getMessage(), e);
  }

  public static void setOwner(final Plugin owner)
  {
    ASTPluginProxy.OWNER = owner;
  }

  private static ILog getLog()
  {
    return ASTPluginProxy.OWNER == null ? null : ASTPluginProxy.OWNER.getLog();
  }

  private static void log(final String message, final Throwable e)
  {
    if (ASTPluginProxy.getLog() != null)
    {
      ASTPluginProxy.log(new Status(IStatus.ERROR, "edu.buffalo.cse.jive.debug.core",
          IStatus.ERROR, message, e));
    }
  }
}