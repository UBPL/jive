package edu.buffalo.cse.jive.internal.ui.view.contour.actions;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_BASE_SCROLL_LOCK;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_OM_ACTION_MINIMIZED;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_OM_ACTION_OBJECTS_MEMBERS;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_OM_ACTION_STACKED;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_OM_ACTION_STACKED_MEMBERS;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;

import edu.buffalo.cse.jive.preferences.PreferenceKeys;
import edu.buffalo.cse.jive.preferences.PreferencesPlugin;

public final class ContourStateActionFactory
{
  public static IAction createCallPathStateAction()
  {
    return new CallPathStateAction();
  }

  public static IAction createMinimizedStateAction()
  {
    return new MinimizedStateAction();
  }

  public static IAction createObjectExpandedStateAction()
  {
    return new ObjectExpandedStateAction();
  }

  public static IAction createObjectStateAction()
  {
    return new ObjectStateAction();
  }

  public static IAction createScrollLockAction()
  {
    return new ScrollLockAction();
  }

  public static IAction createStackedExpandedStateAction()
  {
    return new StackedExpandedStateAction();
  }

  public static IAction createStackedStateAction()
  {
    return new StackedStateAction();
  }

  private ContourStateActionFactory()
  {
    // this factory should not be instantiated
  }

  private static final class CallPathStateAction extends Action
  {
    private CallPathStateAction()
    {
      super("Focus on Call Paths", IAction.AS_CHECK_BOX);
      setImageDescriptor(IM_OM_ACTION_MINIMIZED.enabledDescriptor());
      final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
      setChecked(store.getBoolean(PreferenceKeys.PREF_OD_CALLPATH_FOCUS));
    }

    @Override
    public void run()
    {
      final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
      store.setValue(PreferenceKeys.PREF_OD_CALLPATH_FOCUS, isChecked());
    }
  }

  private static class ContourStateAction extends Action
  {
    private final String state;

    private ContourStateAction(final String text, final String state)
    {
      super(text, IAction.AS_RADIO_BUTTON);
      this.state = state;
      final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
      final String actualState = store.getString(PreferenceKeys.PREF_OD_STATE);
      setChecked(actualState.equals(state));
    }

    @Override
    public void run()
    {
      if (isChecked())
      {
        final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
        store.setValue(PreferenceKeys.PREF_OD_STATE, state);
      }
    }
  }

  private static final class MinimizedStateAction extends ContourStateAction
  {
    private MinimizedStateAction()
    {
      super("Minimized", PreferenceKeys.PREF_OD_MINIMIZED);
      setImageDescriptor(IM_OM_ACTION_MINIMIZED.enabledDescriptor());
    }
  }

  private static final class ObjectExpandedStateAction extends ContourStateAction
  {
    private ObjectExpandedStateAction()
    {
      super("Objects with Tables", PreferenceKeys.PREF_OD_OBJECTS_MEMBERS);
      setImageDescriptor(IM_OM_ACTION_OBJECTS_MEMBERS.enabledDescriptor());
    }
  }

  private static final class ObjectStateAction extends ContourStateAction
  {
    private ObjectStateAction()
    {
      super("Objects", PreferenceKeys.PREF_OD_OBJECTS);
      setImageDescriptor(IM_OM_ACTION_OBJECTS_MEMBERS.enabledDescriptor());
    }
  }

  private static final class ScrollLockAction extends Action
  {
    private ScrollLockAction()
    {
      super("Scroll Lock", IAction.AS_CHECK_BOX);
      setImageDescriptor(IM_BASE_SCROLL_LOCK.enabledDescriptor());
      final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
      setChecked(store.getBoolean(PreferenceKeys.PREF_SCROLL_LOCK));
    }

    @Override
    public void run()
    {
      final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
      store.setValue(PreferenceKeys.PREF_SCROLL_LOCK, isChecked());
    }
  }

  private static final class StackedExpandedStateAction extends ContourStateAction
  {
    private StackedExpandedStateAction()
    {
      super("Stacked with Tables", PreferenceKeys.PREF_OD_STACKED_MEMBERS);
      setImageDescriptor(IM_OM_ACTION_STACKED_MEMBERS.enabledDescriptor());
    }
  }

  private static final class StackedStateAction extends ContourStateAction
  {
    private StackedStateAction()
    {
      super("Stacked", PreferenceKeys.PREF_OD_STACKED);
      setImageDescriptor(IM_OM_ACTION_STACKED.enabledDescriptor());
    }
  }
}