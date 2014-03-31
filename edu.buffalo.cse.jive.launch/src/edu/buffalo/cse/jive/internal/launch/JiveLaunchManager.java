package edu.buffalo.cse.jive.internal.launch;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.debug.model.IJiveLaunchListener;
import edu.buffalo.cse.jive.debug.model.IJiveLaunchManager;
import edu.buffalo.cse.jive.internal.launch.ui.JiveLaunchNotifier;

public enum JiveLaunchManager implements ILaunchListener, IJiveLaunchManager
{
  INSTANCE;
  // MRU list of targets-- index 0 is active
  private final static List<IJiveDebugTarget> targets = Collections
      .synchronizedList(new LinkedList<IJiveDebugTarget>());
  private final static Map<ILaunch, IJiveDebugTarget> launchToTarget = Collections
      .synchronizedMap(new LinkedHashMap<ILaunch, IJiveDebugTarget>());
  private final static Set<IJiveLaunchListener> listeners = Collections
      .newSetFromMap(new ConcurrentHashMap<IJiveLaunchListener, Boolean>());
  private JiveLaunchNotifier notifier;

  private JiveLaunchManager()
  {
    DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
  }

  @Override
  public IJiveDebugTarget activeTarget()
  {
    if (!JiveLaunchManager.targets.isEmpty())
    {
      return JiveLaunchManager.targets.get(0);
    }
    return null;
  }

  public void dispose()
  {
    DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
  }

  @Override
  public void launchAdded(final ILaunch launch)
  {
    processLaunchChildren(launch, true);
  }

  @Override
  public void launchChanged(final ILaunch launch)
  {
    // only process newly added targets to the launch
    processLaunchChildren(launch, true);
  }

  @Override
  public void launchRemoved(final ILaunch launch)
  {
    processLaunchChildren(launch, false);
  }

  @Override
  public IJiveDebugTarget lookupTarget(final int targetId)
  {
    final int index = indexOf(targetId);
    if (index != -1)
    {
      return JiveLaunchManager.targets.get(index);
    }
    return null;
  }

  @Override
  public IJiveDebugTarget lookupTarget(final Object launch)
  {
    return JiveLaunchManager.launchToTarget.get(launch);
  }

  @Override
  public List<IJiveDebugTarget> lookupTargets()
  {
    return Collections.unmodifiableList(JiveLaunchManager.targets);
  }

  @Override
  public void registerListener(final IJiveLaunchListener listener)
  {
    if (listener != null)
    {
      JiveLaunchManager.listeners.add(listener);
    }
  }

  public void targetSelected(final IJiveDebugTarget target)
  {
    if (JiveLaunchManager.targets.contains(target) && JiveLaunchManager.targets.get(0) != target)
    {
      removeTarget(target);
      JiveLaunchManager.targets.add(0, target);
      // System.err.println("New selection: " + target.getName());
      for (final IJiveLaunchListener listener : JiveLaunchManager.listeners)
      {
        listener.targetSelected(target);
      }
    }
  }

  @Override
  public void unregisterListener(final IJiveLaunchListener listener)
  {
    JiveLaunchManager.listeners.remove(listener);
  }

  /**
   * Lazily creates the selection listener after the first launch event notification is received but
   * before it is processed.
   */
  private void createNotifier()
  {
    if (notifier == null)
    {
      notifier = new JiveLaunchNotifier();
    }
  }

  private int indexOf(final int targetId)
  {
    int result = -1;
    for (int i = 0; i < JiveLaunchManager.targets.size(); i++)
    {
      if (JiveLaunchManager.targets.get(i).targetId() == targetId)
      {
        result = i;
        break;
      }
    }
    return result;
  }

  private void notifyListeners()
  {
    for (final IJiveLaunchListener listener : JiveLaunchManager.listeners)
    {
      listener.targetSelected(JiveLaunchManager.targets.isEmpty() ? null
          : JiveLaunchManager.targets.get(0));
    }
  }

  private void processLaunchChildren(final ILaunch launch, final boolean added)
  {
    createNotifier();
    for (final Object child : launch.getChildren())
    {
      if (child instanceof IJiveDebugTarget)
      {
        final IJiveDebugTarget target = (IJiveDebugTarget) child;
        if (added && !JiveLaunchManager.targets.contains(target))
        {
          JiveLaunchManager.targets.add(0, target);
          JiveLaunchManager.launchToTarget.put(launch, target);
          // System.err.println("TARGET (+): " + target.getName());
          notifyListeners();
        }
        else if (!added && removeTarget(target))
        {
          JiveLaunchManager.launchToTarget.remove(launch);
          // System.err.println("TARGET (-): " + target.getName());
          notifyListeners();
          // now release resources
          target.model().done();
        }
      }
    }
  }

  private boolean removeTarget(final IJiveDebugTarget target)
  {
    final int index = JiveLaunchManager.targets.indexOf(target);
    if (index != -1)
    {
      JiveLaunchManager.targets.remove(index);
      return true;
    }
    return false;
  }
}