package edu.buffalo.cse.jive.preferences;

public interface PreferenceKeys
{
  // Attribute key used to obtain the class exclusion filters from the launch configuration.
  // final String PREF_EXCLUSION_FILTERS = ID_BASE + ".exclusionFilters";
  // Attribute key used to obtain the generate local events flag from the launch configuration.
  final String PREF_GENERATE_LOCAL_EVENTS = PreferencesPlugin.ID_BASE + ".generateLocalEvents";
  // Attribute key used to obtain the generate lock events flag from the launch configuration.
  final String PREF_GENERATE_ARRAY_EVENTS = PreferencesPlugin.ID_BASE + ".generateArrayEvents";
  // Attribute key used to obtain the manual start flag from the launch configuration.
  final String PREF_MANUAL_START = PreferencesPlugin.ID_BASE + ".manualStart";
  // Attribute key used to obtain the offline launch URL.
  final String PREF_OFFLINE_URL = PreferencesPlugin.ID_BASE + ".offlineURL";
  // common package/class filter keys
  final String PREF_FILTERS_COMMON = PreferencesPlugin.ID_BASE + ".common_filters";
  // object diagram view state-- call path focus flag
  final String PREF_OD_CALLPATH_FOCUS = PreferencesPlugin.ID_BASE + "contour_callpath_focus";
  // object diagram view state-- minimized objects (no internal structure)
  final String PREF_OD_MINIMIZED = PreferencesPlugin.ID_BASE + "contour_minimized";
  // object diagram view state-- objects with inheritance structure
  final String PREF_OD_OBJECTS = PreferencesPlugin.ID_BASE + "contour_objects";
  // object diagram view state-- objects with inheritance structure and member tables
  final String PREF_OD_OBJECTS_MEMBERS = PreferencesPlugin.ID_BASE + "contour_objects_members";
  // object diagram view state-- objects without inheritance structure
  final String PREF_OD_STACKED = PreferencesPlugin.ID_BASE + "contour_stacked";
  // object diagram view state-- stacked objects without inheritance and member tables
  final String PREF_OD_STACKED_MEMBERS = PreferencesPlugin.ID_BASE + "contour_stacked_members";
  // object diagram-- state preference
  final String PREF_OD_STATE = PreferencesPlugin.ID_BASE + ".contour_state";
  // Contour diagram scroll lock preference.
  final String PREF_SCROLL_LOCK = PreferencesPlugin.ID_BASE + ".scroll_lock";
  // sequence diagram-- activation width
  final String PREF_SD_ACTIVATION_WIDTH = PreferencesPlugin.ID_BASE + ".activation_width";
  // sequence diagram event height preference.
  final String PREF_SD_EVENT_HEIGHT = PreferencesPlugin.ID_BASE + ".event_height";
  // Sequence diagram expand lifelines preference.
  final String PREF_SD_EXPAND_LIFELINES = PreferencesPlugin.ID_BASE + ".expand_lifelines";
  // Sequence diagram show thread activation preference.
  final String PREF_SD_SHOW_THREAD_ACTIVATIONS = PreferencesPlugin.ID_BASE
      + ".show_thread_activations";
  // thread color preference prefix
  final String PREF_THREAD_COLOR = PreferencesPlugin.ID_BASE + ".thread_color_";
  // thread color 0 preference
  final String PREF_THREAD_COLOR_SNAPSHOT = PreferenceKeys.PREF_THREAD_COLOR + "snapshot";
  // thread color 1 preference
  final String PREF_THREAD_COLOR_MAIN = PreferenceKeys.PREF_THREAD_COLOR + "main";
  // thread color 2 preference
  final String PREF_THREAD_COLOR_1 = PreferenceKeys.PREF_THREAD_COLOR + "1";
  // thread color 3 preference
  final String PREF_THREAD_COLOR_2 = PreferenceKeys.PREF_THREAD_COLOR + "2";
  // thread color 4 preference
  final String PREF_THREAD_COLOR_3 = PreferenceKeys.PREF_THREAD_COLOR + "3";
  // thread color 5 preference
  final String PREF_THREAD_COLOR_4 = PreferenceKeys.PREF_THREAD_COLOR + "4";
  // thread color 5 preference
  final String PREF_THREAD_COLOR_5 = PreferenceKeys.PREF_THREAD_COLOR + "5";
  // thread color 6 preference
  final String PREF_THREAD_COLOR_6 = PreferenceKeys.PREF_THREAD_COLOR + "6";
  // thread color 7 preference
  final String PREF_THREAD_COLOR_7 = PreferenceKeys.PREF_THREAD_COLOR + "7";
  // diagram update interval (in milliseconds)
  final String PREF_UPDATE_INTERVAL = PreferencesPlugin.ID_BASE + ".update_interval";
}