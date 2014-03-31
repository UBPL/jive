package edu.buffalo.cse.jive.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

/**
 * SEE: http://www.colorsontheweb.com/colorwizard.asp
 * 
 * SEE: http://www.color-wheel-pro.com/color-schemes.html
 * 
 * SEE: http://www.javascripter.net/faq/hextorgb.htm
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer
{
  /**
   * The delimiter used between event filters.
   */
  public static final String EVENT_FILTER_DELIMITER = ",";
  /**
   * The default event filters.
   */
  private static final String[] COMMON_FILTERS =
  { "antlr.*", "com.apple.*", "com.mysql.*", "com.sun.*", "java.*", "javax.*", "org.apache.*",
      "org.eclipse.*", "org.hibernate.*", "org.jcp.*", "org.junit.*", "org.postgresql.*",
      "org.w3c.*", "org.xml.*", "sun.*", "$Proxy*" };
  // gray: cececeff
  public final static RGB COLOR_THREAD_SNAPSHOT = new RGB(206, 206, 206);
  // blue: a6caf0ff
  public final static RGB COLOR_THREAD_MAIN = new RGB(166, 202, 240);
  // teal: 40C0B0
  public final static RGB COLOR_THREAD_1 = new RGB(64, 192, 176);// new RGB(255, 244, 65); // yellow
  // violet: ba7dffff
  public final static RGB COLOR_THREAD_2 = new RGB(186, 125, 255);
  // green
  public final static RGB COLOR_THREAD_3 = ColorConstants.lightGreen.getRGB();
  // orange: ffb931ff
  public final static RGB COLOR_THREAD_4 = new RGB(255, 185, 49);
  // acqua: 80ffffff
  public final static RGB COLOR_THREAD_5 = new RGB(128, 255, 255);
  // light blue
  public final static RGB COLOR_THREAD_6 = ColorConstants.lightBlue.getRGB();
  // red: ff7f65ff
  public final static RGB COLOR_THREAD_7 = new RGB(255, 127, 101);

  /**
   * Converts an array of filters into a single string which is saved as a preference.
   * 
   * @param filters
   *          an array of filters
   * @return a single string containing the input filters
   */
  public static String convertFilters(final String[] filters)
  {
    String result = "";
    for (final String filter : filters)
    {
      result += filter;
      result += PreferenceInitializer.EVENT_FILTER_DELIMITER;
    }
    return result;
  }

  @Override
  public void initializeDefaultPreferences()
  {
    final IPreferenceStore store = PreferencesPlugin.getDefault().getPreferenceStore();
    store.setDefault(PreferenceKeys.PREF_UPDATE_INTERVAL, 2500L);
    store.setDefault(PreferenceKeys.PREF_OD_STATE, PreferenceKeys.PREF_OD_STACKED);
    store.setDefault(PreferenceKeys.PREF_OD_CALLPATH_FOCUS, false);
    store.setDefault(PreferenceKeys.PREF_SCROLL_LOCK, false);
    store.setDefault(PreferenceKeys.PREF_SD_SHOW_THREAD_ACTIVATIONS, false);
    store.setDefault(PreferenceKeys.PREF_SD_EXPAND_LIFELINES, false);
    // new
    PreferenceConverter.setDefault(store, PreferenceKeys.PREF_THREAD_COLOR_SNAPSHOT,
        PreferenceInitializer.COLOR_THREAD_SNAPSHOT);
    // new
    PreferenceConverter.setDefault(store, PreferenceKeys.PREF_THREAD_COLOR_MAIN,
        PreferenceInitializer.COLOR_THREAD_MAIN);
    // used to be: ColorConstants.lightBlue.getRGB()
    PreferenceConverter.setDefault(store, PreferenceKeys.PREF_THREAD_COLOR_1,
        PreferenceInitializer.COLOR_THREAD_1);
    // used to be: ColorConstants.lightGreen.getRGB()
    PreferenceConverter.setDefault(store, PreferenceKeys.PREF_THREAD_COLOR_2,
        PreferenceInitializer.COLOR_THREAD_2);
    // used to be: ColorConstants.lightGray.getRGB()
    PreferenceConverter.setDefault(store, PreferenceKeys.PREF_THREAD_COLOR_3,
        PreferenceInitializer.COLOR_THREAD_3);
    // used to be: ColorConstants.orange.getRGB()
    PreferenceConverter.setDefault(store, PreferenceKeys.PREF_THREAD_COLOR_4,
        PreferenceInitializer.COLOR_THREAD_4);
    // new
    PreferenceConverter.setDefault(store, PreferenceKeys.PREF_THREAD_COLOR_5,
        PreferenceInitializer.COLOR_THREAD_5);
    // new
    PreferenceConverter.setDefault(store, PreferenceKeys.PREF_THREAD_COLOR_6,
        PreferenceInitializer.COLOR_THREAD_6);
    // new
    PreferenceConverter.setDefault(store, PreferenceKeys.PREF_THREAD_COLOR_7,
        PreferenceInitializer.COLOR_THREAD_7);
    store.setDefault(PreferenceKeys.PREF_SD_ACTIVATION_WIDTH, 8);
    store.setDefault(PreferenceKeys.PREF_SD_EVENT_HEIGHT, 4);
    initializeDefaultEventFilterPreferences(store);
  }

  /**
   * Initializes the default event filter preferences.
   * 
   * @param prefs
   *          the preferences to initialize
   */
  private void initializeDefaultEventFilterPreferences(final IPreferenceStore store)
  {
    store.setDefault(PreferenceKeys.PREF_FILTERS_COMMON,
        PreferenceInitializer.convertFilters(PreferenceInitializer.COMMON_FILTERS));
  }
}
