package edu.buffalo.cse.jive.internal.ui;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

import edu.buffalo.cse.jive.ui.JiveUIPlugin;

/**
 * A factory for creating the JIVE perspective. The perspective contains views for the current
 * execution state and the execution history, as well as views for other common debugging tasks.
 */
public class JivePerspectiveFactory implements IPerspectiveFactory
{
  /**
   * The folder ID of the <code>IFolderLayout</code> used to hold views for the console or other
   * miscellaneous views. Used by {@code JivePerspectiveFactory}.
   */
  private static final String ID_FOLDER_CONSOLE = JiveUIPlugin.PLUGIN_ID + ".consoleFolder";
  /**
   * The folder ID of the <code>IFolderLayout</code> used to hold views related to the execution
   * history of the active target.vUsed by {@code JivePerspectiveFactory}.
   */
  private static final String ID_FOLDER_EXECUTION_HISTORY = JiveUIPlugin.PLUGIN_ID
      + ".executionHistoryFolder";
  /**
   * The folder ID of the <code>IFolderLayout</code> used to hold views related to the execution
   * state of the active target. Used by {@code JivePerspectiveFactory}.
   */
  private static final String ID_FOLDER_EXECUTION_STATE = JiveUIPlugin.PLUGIN_ID
      + ".executionStateFolder";
  /**
   * The folder ID of the <code>IFolderLayout</code> used to hold views related to the current
   * launches. Used by {@code JivePerspectiveFactory}.
   */
  private static final String ID_FOLDER_LAUNCH = JiveUIPlugin.PLUGIN_ID + ".launchFolder";

  @Override
  public void createInitialLayout(final IPageLayout layout)
  {
    final String editorArea = layout.getEditorArea();
    // Create and populate the execution state folder
    final IFolderLayout executionStateFolderLayout = layout.createFolder(
        JivePerspectiveFactory.ID_FOLDER_EXECUTION_STATE, IPageLayout.RIGHT, (float) 0.45,
        editorArea);
    setExecutionStateFolderViews(executionStateFolderLayout);
    // Create and populate the execution history folder
    final IFolderLayout executionHistoryFolderLayout = layout.createFolder(
        JivePerspectiveFactory.ID_FOLDER_EXECUTION_HISTORY, IPageLayout.BOTTOM, (float) 0.50,
        JivePerspectiveFactory.ID_FOLDER_EXECUTION_STATE);
    setExecutionHistoryFolderViews(executionHistoryFolderLayout);
    // Create and populate the launch folder
    final IFolderLayout launchFolderLayout = layout.createFolder(
        JivePerspectiveFactory.ID_FOLDER_LAUNCH, IPageLayout.TOP, (float) 0.30, editorArea);
    setLaunchFolderViews(launchFolderLayout);
    // Create and populate the console folder
    final IFolderLayout consoleFolderLayout = layout.createFolder(
        JivePerspectiveFactory.ID_FOLDER_CONSOLE, IPageLayout.BOTTOM, (float) 0.67, editorArea);
    setConsoleFolderViews(consoleFolderLayout);
    // Populate the "Window -> Open Perspective" and "Window -> Show View" menus
    setPerspectiveShortcuts(layout);
    setViewShortcuts(layout);
  }

  /**
   * Adds views and placeholders to the console folder.
   * 
   * @param layout
   *          the layout for the console folder
   */
  protected void setConsoleFolderViews(final IFolderLayout layout)
  {
    layout.addView(IConsoleConstants.ID_CONSOLE_VIEW);
    layout.addView(IPageLayout.ID_TASK_LIST);
    layout.addView(NewSearchUI.SEARCH_VIEW_ID);
    layout.addPlaceholder(IPageLayout.ID_BOOKMARKS);
    layout.addPlaceholder(IPageLayout.ID_PROP_SHEET);
  }

  /**
   * Adds views and placeholders to the execution history folder.
   * 
   * @param layout
   *          the layout for the execution history folder
   */
  protected void setExecutionHistoryFolderViews(final IFolderLayout layout)
  {
    layout.addView(JiveUIPlugin.ID_SEQUENCE_DIAGRAM_VIEW);
    layout.addView(JiveUIPlugin.ID_SEQUENCE_MODEL_VIEW);
    layout.addPlaceholder(JiveUIPlugin.ID_TRACE_VIEW);
  }

  /**
   * Adds views and placeholders to the execution state folder.
   * 
   * @param layout
   *          the layout for the execution state folder
   */
  protected void setExecutionStateFolderViews(final IFolderLayout layout)
  {
    layout.addView(JiveUIPlugin.ID_CONTOUR_DIAGRAM_VIEW);
    layout.addView(JiveUIPlugin.ID_CONTOUR_MODEL_VIEW);
    layout.addPlaceholder(IDebugUIConstants.ID_VARIABLE_VIEW);
    layout.addPlaceholder(IDebugUIConstants.ID_BREAKPOINT_VIEW);
    layout.addPlaceholder(IDebugUIConstants.ID_EXPRESSION_VIEW);
    layout.addPlaceholder(IDebugUIConstants.ID_REGISTER_VIEW);
  }

  /**
   * Adds views and placeholders to the launch folder.
   * 
   * @param layout
   *          the layout for the launch folder
   */
  protected void setLaunchFolderViews(final IFolderLayout layout)
  {
    layout.addView(IDebugUIConstants.ID_DEBUG_VIEW);
    layout.addView(JavaUI.ID_PACKAGES);
  }

  /**
   * Adds perspectives to the "Window -> Open Perspective" menu to be displayed when the JIVE
   * perspective is the current perspective.
   * 
   * @param layout
   *          the layout for the JIVE perspective
   */
  protected void setPerspectiveShortcuts(final IPageLayout layout)
  {
    layout.addPerspectiveShortcut(JavaUI.ID_PERSPECTIVE);
    layout.addPerspectiveShortcut(JavaUI.ID_BROWSING_PERSPECTIVE);
    layout.addPerspectiveShortcut(IDebugUIConstants.ID_DEBUG_PERSPECTIVE);
  }

  /**
   * Adds views to the "Window -> Show View" menu to be displayed when the JIVE perspective is the
   * current perspective.
   * 
   * @param layout
   *          the layout for the JIVE perspective
   */
  protected void setViewShortcuts(final IPageLayout layout)
  {
    layout.addShowViewShortcut(JiveUIPlugin.ID_TRACE_VIEW);
    layout.addShowViewShortcut(JiveUIPlugin.ID_CONTOUR_DIAGRAM_VIEW);
    layout.addShowViewShortcut(JiveUIPlugin.ID_CONTOUR_MODEL_VIEW);
    layout.addShowViewShortcut(JiveUIPlugin.ID_SEQUENCE_DIAGRAM_VIEW);
    layout.addShowViewShortcut(JiveUIPlugin.ID_SEQUENCE_MODEL_VIEW);
    layout.addShowViewShortcut(NewSearchUI.SEARCH_VIEW_ID);
  }
}
