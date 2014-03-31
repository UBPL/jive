package edu.buffalo.cse.jive.internal.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IEventModel.IThreadStartEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;
import edu.buffalo.cse.jive.preferences.PreferenceKeys;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;
import edu.buffalo.cse.jive.ui.IThreadColorListener;
import edu.buffalo.cse.jive.ui.IThreadColorManager;
import edu.buffalo.cse.jive.ui.JiveUIPlugin;

final class ThreadColorManager implements IThreadColorManager
{
  private final Map<IJiveDebugTarget, TargetThreadColors> targetToThreadColorsMap;
  private final List<Color> threadColors;
  private final List<IThreadColorListener> updateList;
  // fixed color threads-- these do not cycle
  private Color mainThreadColor;
  private Color snapshotThreadColor;

  ThreadColorManager()
  {
    updateList = new LinkedList<IThreadColorListener>();
    threadColors = new ArrayList<Color>();
    targetToThreadColorsMap = new HashMap<IJiveDebugTarget, TargetThreadColors>();
    final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
    store.addPropertyChangeListener(this);
    final ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    for (final ILaunch launch : manager.getLaunches())
    {
      launchChanged(launch);
    }
    manager.addLaunchListener(this);
  }

  @Override
  public void addThreadColorListener(final IThreadColorListener listener)
  {
    if (!updateList.contains(listener))
    {
      updateList.add(listener);
    }
  }

  @Override
  public void launchAdded(final ILaunch launch)
  {
    targetAdd(launch.getDebugTarget());
  }

  @Override
  public void launchChanged(final ILaunch launch)
  {
    targetAdd(launch.getDebugTarget());
  }

  @Override
  public void launchRemoved(final ILaunch launch)
  {
    final IDebugTarget target = launch.getDebugTarget();
    if (target instanceof IJiveDebugTarget)
    {
      targetToThreadColorsMap.remove(target);
    }
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event)
  {
    final String property = event.getProperty();
    if (property.startsWith(PreferenceKeys.PREF_THREAD_COLOR))
    {
      threadColorsDispose();
      for (final TargetThreadColors targetColors : targetToThreadColorsMap.values())
      {
        targetColors.clear();
      }
      for (final IJiveDebugTarget target : targetToThreadColorsMap.keySet())
      {
        fireThreadColorsChanged(target);
      }
    }
  }

  @Override
  public void removeThreadColorListener(final IThreadColorListener listener)
  {
    if (updateList.contains(listener))
    {
      updateList.remove(listener);
    }
  }

  @Override
  public Color threadColor(final IJiveDebugTarget target, final IThreadValue thread)
  {
    targetAdd((IJavaDebugTarget) target);
    // System.out.println(target);
    if (threadColors.isEmpty())
    {
      threadColorsUpdate();
    }
    final TargetThreadColors targetColors = targetToThreadColorsMap.get(target);
    return targetColors.getThreadColor(thread);
  }

  private void fireThreadColorsChanged(final IJiveDebugTarget target)
  {
    for (final IThreadColorListener listener : updateList)
    {
      listener.threadColorsChanged(target);
    }
  }

  private void targetAdd(final IDebugTarget target)
  {
    if (target instanceof IJiveDebugTarget)
    {
      if (!targetToThreadColorsMap.containsKey(target))
      {
        targetToThreadColorsMap.put((IJiveDebugTarget) target, new TargetThreadColors(
            (IJiveDebugTarget) target));
      }
    }
  }

  private void threadColorsDispose()
  {
    if (snapshotThreadColor != null)
    {
      snapshotThreadColor.dispose();
    }
    if (mainThreadColor != null)
    {
      mainThreadColor.dispose();
    }
    for (final Color c : threadColors)
    {
      c.dispose();
    }
    snapshotThreadColor = null;
    mainThreadColor = null;
    threadColors.clear();
  }

  /**
   * Reload the color list from the preference store.
   */
  private void threadColorsUpdate()
  {
    final Display display = JiveUIPlugin.getStandardDisplay();
    final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
    // snapshot thread color
    snapshotThreadColor = new Color(display, PreferenceConverter.getColor(store,
        PreferenceKeys.PREF_THREAD_COLOR_SNAPSHOT));
    // add the main thread color to the list
    mainThreadColor = new Color(display, PreferenceConverter.getColor(store,
        PreferenceKeys.PREF_THREAD_COLOR_MAIN));
    // add the seven thread colors to the list
    for (int i = 1; i <= 7; i++)
    {
      threadColors.add(new Color(display, PreferenceConverter.getColor(store,
          PreferenceKeys.PREF_THREAD_COLOR + i)));
    }
  }

  void dispose()
  {
    final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
    store.removePropertyChangeListener(this);
    final ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    manager.removeLaunchListener(this);
    updateList.clear();
    targetToThreadColorsMap.clear();
    threadColorsDispose();
  }

  private final class TargetThreadColors
  {
    private final IJiveDebugTarget target;
    private final Map<IThreadValue, Color> threadColorsMap;

    TargetThreadColors(final IJiveDebugTarget target)
    {
      this.target = target;
      this.threadColorsMap = new HashMap<IThreadValue, Color>();
    }

    public Color getThreadColor(final IThreadValue thread)
    {
      if (!thread.name().equals("SYSTEM") && !threadColorsMap.containsKey(thread))
      {
        updateThreadToColorMap();
      }
      return threadColorsMap.get(thread);
    }

    private void updateThreadToColorMap()
    {
      threadColorsMap.clear();
      final IExecutionModel model = target.model();
      model.readLock();
      try
      {
        final LinkedList<Color> temp = new LinkedList<Color>(threadColors);
        for (final IThreadStartEvent thread : model.lookupThreads())
        {
          if (temp.isEmpty())
          {
            temp.addAll(threadColors);
          }
          // special treatment for the snapshot thread
          if (thread.thread().name().equals("JIVE Snapshot"))
          {
            threadColorsMap.put(thread.thread(), snapshotThreadColor);
          }
          // special treatment for the main thread
          else if (thread.thread().name().equals("main"))
          {
            threadColorsMap.put(thread.thread(), mainThreadColor);
          }
          // cycle through thread colors
          else
          {
            threadColorsMap.put(thread.thread(), temp.removeFirst());
          }
        }
      }
      finally
      {
        model.readUnlock();
      }
    }

    void clear()
    {
      threadColorsMap.clear();
    }
  }
}