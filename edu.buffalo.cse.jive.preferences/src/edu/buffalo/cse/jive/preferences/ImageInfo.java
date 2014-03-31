package edu.buffalo.cse.jive.preferences;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

import edu.buffalo.cse.jive.model.IEventModel.EventKind;

public enum ImageInfo
{
  // general purpose images
  IM_BASE_COLLAPSE_ALL(ImagePaths.PATH_ICONS_GENERAL, "collapse_all.gif"),
  IM_BASE_EXPAND_ALL(ImagePaths.PATH_ICONS_GENERAL, "expand_all.gif"),
  IM_BASE_JIVE(ImagePaths.PATH_ICONS_GENERAL, "jive.gif"),
  IM_BASE_LIST(ImagePaths.PATH_ICONS_GENERAL, "list.gif"),
  IM_BASE_PRINT(ImagePaths.PATH_ICONS_GENERAL, "print.gif"),
  IM_BASE_REMOVE(ImagePaths.PATH_ICONS_GENERAL, "remove.gif"),
  IM_BASE_REMOVE_ALL(ImagePaths.PATH_ICONS_GENERAL, "remove_all.gif"),
  IM_BASE_SAVE(ImagePaths.PATH_ICONS_GENERAL, "save.gif"),
  IM_BASE_SCROLL_LOCK(ImagePaths.PATH_ICONS_GENERAL, "scroll_lock.gif"),
  IM_BASE_SLICE_CLEAR(ImagePaths.PATH_ICONS_GENERAL, "slice_clear.gif"),
  IM_BASE_SEARCH(ImagePaths.PATH_ICONS_GENERAL, "search.gif"),
  IM_BASE_TRACE_START(ImagePaths.PATH_ICONS_GENERAL, "trace_start.gif"),
  IM_BASE_TRACE_STOP(ImagePaths.PATH_ICONS_GENERAL, "trace_stop.gif"),
  IM_BASE_TREE(ImagePaths.PATH_ICONS_GENERAL, "tree.gif"),
  // event images
  IM_EVENT(ImagePaths.PATH_ICONS_EVENTS, "event.gif"),
  IM_EVENT_ASSIGN(ImagePaths.PATH_ICONS_EVENTS, "var_write.gif"),
  IM_EVENT_STEP(ImagePaths.PATH_ICONS_EVENTS, "bos.gif"),
  IM_EVENT_CATCH(ImagePaths.PATH_ICONS_EVENTS, "catch.gif"),
  IM_EVENT_EXIT(ImagePaths.PATH_ICONS_EVENTS, "exit.gif"),
  IM_EVENT_FIELD_READ(ImagePaths.PATH_ICONS_EVENTS, "var_read.gif"),
  IM_EVENT_LOAD(ImagePaths.PATH_ICONS_EVENTS, "load.gif"),
  IM_EVENT_LOCK(ImagePaths.PATH_ICONS_EVENTS, "lock.png"),
  IM_EVENT_NEW(ImagePaths.PATH_ICONS_EVENTS, "new.gif"),
  IM_EVENT_METHOD_CALL(ImagePaths.PATH_ICONS_EVENTS, "call.gif"),
  IM_EVENT_METHOD_ENTER(ImagePaths.PATH_ICONS_EVENTS, "call.gif"),
  IM_EVENT_METHOD_EXIT(ImagePaths.PATH_ICONS_EVENTS, "return.gif"),
  IM_EVENT_METHOD_RETURN(ImagePaths.PATH_ICONS_EVENTS, "return.gif"),
  IM_EVENT_START(ImagePaths.PATH_ICONS_EVENTS, "start.gif"),
  IM_EVENT_THREAD_EXIT(ImagePaths.PATH_ICONS_EVENTS, "thread_exit.gif"),
  IM_EVENT_THREAD_START(ImagePaths.PATH_ICONS_EVENTS, "thread_start.gif"),
  IM_EVENT_THROW(ImagePaths.PATH_ICONS_EVENTS, "throw.gif"),
  // object model images
  IM_OM_ACTION_EXPANDED(ImagePaths.PATH_ICONS_CONTOUR_MODEL_ACTIONS, "expand_contours.gif"),
  IM_OM_ACTION_MINIMIZED(ImagePaths.PATH_ICONS_CONTOUR_MODEL_ACTIONS, "minimize_contours.gif"),
  IM_OM_ACTION_OBJECTS_MEMBERS(
      ImagePaths.PATH_ICONS_CONTOUR_MODEL_ACTIONS,
      "show_member_tables.gif"),
  IM_OM_ACTION_STACKED(ImagePaths.PATH_ICONS_CONTOUR_MODEL_ACTIONS, "stack_instance_contours.gif"),
  IM_OM_ACTION_STACKED_MEMBERS(
      ImagePaths.PATH_ICONS_CONTOUR_MODEL_ACTIONS,
      "stack_instance_contours.gif"),
  IM_OM_CONTOUR_INSTANCE(ImagePaths.PATH_ICONS_CONTOUR_MODEL_CONTOURS, "object.gif"), // instance.gif
  IM_OM_CONTOUR_INTERFACE(ImagePaths.PATH_ICONS_CONTOUR_MODEL_CONTOURS, "interface.gif"),
  IM_OM_CONTOUR_METHOD(ImagePaths.PATH_ICONS_CONTOUR_MODEL_CONTOURS, "method.gif"),
  IM_OM_CONTOUR_STATIC(ImagePaths.PATH_ICONS_CONTOUR_MODEL_CONTOURS, "class.gif"), // static.gif
  IM_OM_CONTOUR_THREAD(ImagePaths.PATH_ICONS_CONTOUR_MODEL_CONTOURS, "thread.gif"),
  // sequence model images
  IM_SEARCH_INVARIANT_VIOLATED(ImagePaths.PATH_ICONS_SEARCH, "invariant_violated.gif"),
  // sequence model images
  IM_SM_ACTION_EXPAND_LIFELINES(
      ImagePaths.PATH_ICONS_SEQUENCE_MODEL_ACTIONS,
      "expand_lifelines.gif"),
  IM_SM_ACTION_HIDE_RECEIVE_MESSAGES(
      ImagePaths.PATH_ICONS_SEQUENCE_MODEL_ACTIONS,
      "hide_message_receives.gif"),
  IM_SM_ACTION_SHOW_THREAD_ACTIVATIONS(
      ImagePaths.PATH_ICONS_SEQUENCE_MODEL_ACTIONS,
      "show_thread_activations.gif"),
  IM_SM_EXECUTIONS_FILTERED_METHOD_ACTIVATION(
      ImagePaths.PATH_ICONS_SEQUENCE_MODEL_EXECUTIONS,
      "filtered_method.gif"),
  IM_SM_EXECUTIONS_METHOD_ACTIVATION(ImagePaths.PATH_ICONS_SEQUENCE_MODEL_EXECUTIONS, "method.gif"),
  IM_SM_EXECUTIONS_THREAD_ACTIVATION(ImagePaths.PATH_ICONS_SEQUENCE_MODEL_EXECUTIONS, "thread.gif"),
  // temporal model images
  IM_TM_PAUSE(ImagePaths.PATH_ICONS_TEMPORAL_MODEL, "pause.gif"),
  IM_TM_RUN_BACKWARD(ImagePaths.PATH_ICONS_TEMPORAL_MODEL, "run_backward.gif"),
  IM_TM_RUN_FORWARD(ImagePaths.PATH_ICONS_TEMPORAL_MODEL, "run_forward.gif"),
  IM_TM_STEP_BACKWARD(ImagePaths.PATH_ICONS_TEMPORAL_MODEL, "step_backward.gif"),
  IM_TM_STEP_FORWARD(ImagePaths.PATH_ICONS_TEMPORAL_MODEL, "step_forward.gif");
  private static final String KEY_DISABLED = "_DISABLED";
  private static final String KEY_ENABLED = "_ENABLED";
  private static final String PATH_DISABLED = "disabled/";
  private static final String PATH_ENABLED = "enabled/";
  private static final Map<EventKind, ImageInfo> eventKindMap = new HashMap<EventKind, ImageInfo>();

  public static ImageInfo imageInfo(final EventKind kind)
  {
    if (ImageInfo.eventKindMap.isEmpty())
    {
      ImageInfo.initializerEventMap();
    }
    final ImageInfo result = ImageInfo.eventKindMap.get(kind);
    return result == null ? ImageInfo.IM_EVENT : result;
  }

  private static void initializerEventMap()
  {
    ImageInfo.eventKindMap.put(EventKind.EXCEPTION_CATCH, ImageInfo.IM_EVENT_CATCH);
    ImageInfo.eventKindMap.put(EventKind.EXCEPTION_THROW, ImageInfo.IM_EVENT_THROW);
    ImageInfo.eventKindMap.put(EventKind.FIELD_READ, ImageInfo.IM_EVENT_FIELD_READ);
    ImageInfo.eventKindMap.put(EventKind.FIELD_WRITE, ImageInfo.IM_EVENT_ASSIGN);
    ImageInfo.eventKindMap.put(EventKind.LINE_STEP, ImageInfo.IM_EVENT_STEP);
    ImageInfo.eventKindMap.put(EventKind.THREAD_LOCK, ImageInfo.IM_EVENT_LOCK);
    ImageInfo.eventKindMap.put(EventKind.METHOD_CALL, ImageInfo.IM_EVENT_METHOD_CALL);
    ImageInfo.eventKindMap.put(EventKind.METHOD_ENTERED, ImageInfo.IM_EVENT_METHOD_ENTER);
    ImageInfo.eventKindMap.put(EventKind.METHOD_EXIT, ImageInfo.IM_EVENT_METHOD_EXIT);
    ImageInfo.eventKindMap.put(EventKind.METHOD_RETURNED, ImageInfo.IM_EVENT_METHOD_RETURN);
    ImageInfo.eventKindMap.put(EventKind.OBJECT_NEW, ImageInfo.IM_EVENT_NEW);
    ImageInfo.eventKindMap.put(EventKind.SYSTEM_END, ImageInfo.IM_EVENT_EXIT);
    ImageInfo.eventKindMap.put(EventKind.SYSTEM_START, ImageInfo.IM_EVENT_START);
    ImageInfo.eventKindMap.put(EventKind.THREAD_END, ImageInfo.IM_EVENT_THREAD_EXIT);
    ImageInfo.eventKindMap.put(EventKind.THREAD_START, ImageInfo.IM_EVENT_THREAD_START);
    ImageInfo.eventKindMap.put(EventKind.TYPE_LOAD, ImageInfo.IM_EVENT_LOAD);
    ImageInfo.eventKindMap.put(EventKind.VAR_ASSIGN, ImageInfo.IM_EVENT_ASSIGN);
    ImageInfo.eventKindMap.put(EventKind.VAR_DELETE, ImageInfo.IM_EVENT_ASSIGN);
  }

  private final String basePath;
  private final String fileName;

  private ImageInfo(final String basePath, final String fileName)
  {
    this.basePath = basePath;
    this.fileName = fileName;
  }

  public final ImageDescriptor disabledDescriptor()
  {
    return imageRegistry().getDescriptor(disabledKey());
  }

  public final Image disabledImage()
  {
    return imageRegistry().get(disabledKey());
  }

  public String disabledKey()
  {
    return name() + ImageInfo.KEY_DISABLED;
  }

  public String disabledPath()
  {
    return this.basePath + ImageInfo.PATH_DISABLED + this.fileName;
  }

  public final ImageDescriptor enabledDescriptor()
  {
    return imageRegistry().getDescriptor(enabledKey());
  }

  public final Image enabledImage()
  {
    return imageRegistry().get(enabledKey());
  }

  public String enabledKey()
  {
    return name() + ImageInfo.KEY_ENABLED;
  }

  public String enabledPath()
  {
    return this.basePath + ImageInfo.PATH_ENABLED + this.fileName;
  }

  private final ImageRegistry imageRegistry()
  {
    return PreferencesPlugin.getDefault().getImageRegistry();
  }
}