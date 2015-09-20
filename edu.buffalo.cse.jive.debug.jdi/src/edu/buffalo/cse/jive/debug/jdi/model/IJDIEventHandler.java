package edu.buffalo.cse.jive.debug.jdi.model;

import com.sun.jdi.event.AccessWatchpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.ModificationWatchpointEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;

/**
 * Observation: new objects must be inferred as there is no corresponding event in JDI.
 * 
 */
@SuppressWarnings("restriction")
public interface IJDIEventHandler
{
  /**
   * Create a snapshot of the JDI state.
   */
  void createSnapshot(final Object jvm);

  /**
   * Notification of a field access in the target VM.
   */
  void jdiAccessWatchpoint(AccessWatchpointEvent event);

  /**
   * Notification of a class prepare in the target VM.
   */
  void jdiClassPrepare(ClassPrepareEvent event);

  /**
   * Notification of an exception in the target VM.
   */
  void jdiExceptionThrown(ExceptionEvent event);

  /**
   * Notification of a method invocation in the target VM.
   */
  void jdiMethodEntry(MethodEntryEvent event);

  /**
   * Notification of a method return in the target VM.
   */
  void jdiMethodExit(MethodExitEvent event);

  /**
   * Notification of a field modification in the target VM.
   */
  void jdiModificationWatchpoint(ModificationWatchpointEvent event);

  /**
   * A step event is generated immediately before the code at its location is executed; thus, if the
   * step is entering a new method (as might occur with StepRequest.STEP_INTO) the location of the
   * event is the first instruction of the method. When a step leaves a method, the location of the
   * event will be the first instruction after the call in the calling method; note that this
   * location may not be at a line boundary, even if StepRequest.STEP_LINE was used.
   */
  void jdiStep(StepEvent event);

  /**
   * Notification of a completed thread in the target VM.
   */
  void jdiThreadDeath(ThreadDeathEvent event);

  /**
   * Notification of a new running thread in the target VM.
   */
  void jdiThreadStart(ThreadStartEvent event);

  /**
   * Notification of target VM termination.
   */
  void jdiVMDeath(VMDeathEvent event);

  /**
   * Notification of disconnection from target VM.
   */
  void jdiVMDisconnect(VMDisconnectEvent event);

  /**
   * Notification of initialization of a target VM.
   */
  void jdiVMStart(VMStartEvent event);

  /**
   * Reset the handler-- all internal state is cleared.
   */
  void reset();
}
