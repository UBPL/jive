package edu.buffalo.cse.jive.debug.model;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;

import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IExecutionModel.ITraceViewListener;
import edu.buffalo.cse.jive.model.IJiveProject;

/**
 * An <code>IJavaDebugTarget</code> monitored by JIVE. The target is monitored for particular events
 * of interest which are used to produce JIVE events. It maintains two models representing the
 * execution state and call history of the running program.
 */
public interface IJiveDebugTarget extends ILaunchListener, ITraceViewListener
{
  /**
   * Returns whether or not the the debug target can replay past states. It can if all non-system
   * threads are suspended.
   * 
   * @return <code>true</code> if past states can be replayed, <code>false</code> otherwise
   */
  public boolean canReplay();

  public boolean canSuspend();

  public int disableViews();

  public int enableViews();

  public Object getJVM();

  /**
   * Launch associated with the debug target.
   */
  public ILaunch getLaunch();

  public String getModelIdentifier();

  /**
   * A name to identify the debug target.
   */
  public String getName();

  /**
   * Project associated with the debug target.
   */
  public IJiveProject getProject();

  /**
   * Determines if this target is actively collecting trace events.
   */
  public boolean isActive();

  public boolean isDisconnected();

  /**
   * Determines whether this target is initialized manually.
   */
  public boolean isManualStart();

  /**
   * Determines if this target is generated from an offline source.
   */
  public boolean isOffline();

  /**
   * Determines if this target has started event collection. This returns false only for manually
   * starting targets, before the start command has been issued from the console.
   */
  public boolean isStarted();

  /**
   * Determines if this target has stopped event collection. This returns true if the target has
   * been manually stopped.
   */
  public boolean isStopped();

  public boolean isSuspended();

  public boolean isTerminated();

  /**
   * Execution model associated with the debug target.
   */
  public IExecutionModel model();

  /**
   * Manually starts this target, generating a state snapshot before proceeding with JDI processing.
   * If start is called on an already started or a stopped target, nothing happens.
   */
  public void start();

  /**
   * Manually stops this target. If stop is called on an already stopped target or on a target not
   * yet started, nothing happens.
   */
  public void stop();

  public void suspend() throws Exception;

  /**
   * Identifier of this debug target.
   */
  public int targetId();

  public boolean viewsEnabled();
}