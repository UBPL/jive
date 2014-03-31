package edu.buffalo.cse.jive.debug.model;

import java.util.List;

public interface IJiveLaunchManager
{
  public IJiveDebugTarget activeTarget();

  public IJiveDebugTarget lookupTarget(int targetId);

  public IJiveDebugTarget lookupTarget(Object launch);

  public List<IJiveDebugTarget> lookupTargets();

  public void registerListener(IJiveLaunchListener listener);

  public void unregisterListener(IJiveLaunchListener listener);
}