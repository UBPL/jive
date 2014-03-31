package edu.buffalo.cse.jive.internal.debug.jdi.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.AccessWatchpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.ModificationWatchpointEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;

import edu.buffalo.cse.jive.debug.JiveDebugPlugin;
import edu.buffalo.cse.jive.debug.jdi.model.IJDIEventHandler;
import edu.buffalo.cse.jive.debug.jdi.model.IJDIManager;
import edu.buffalo.cse.jive.debug.jdi.model.IJiveEventDispatcher;
import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;

public class JDIEventHandler implements IJDIEventHandler
{
  private boolean override;
  private final IJiveDebugTarget owner;
  private IThreadValue vmThreadId;
  private final JDIEventHandlerDelegate delegate;

  public JDIEventHandler(final IJiveDebugTarget owner)
  {
    this.owner = owner;
    this.override = false;
    this.delegate = new JDIEventHandlerDelegate(this.owner);
  }

  /**
   * Creates all JIVE model elements that are relevant at the current execution state, assuming that
   * all prior execution states are irrelevant. The snapshot provides the necessary model elements
   * at a particular point in execution in order to be able to trace consistently from that point.
   */
  @Override
  public void createSnapshot(final Object jvm)
  {
    if (jvm == null)
    {
      return;
    }
    final VirtualMachine vm = (VirtualMachine) jvm;
    // allow event processing for now
    override = true;
    // initialize the snapshot: system, thread, and run
    final StackFrame syntheticFrame = Synthetics.createSyntheticFrame(vm);
    // load the snapshot thread type
    dispatcher().dispatchSyntheticTypeLoad(syntheticFrame.thread());
    // call the run() method on the snapshot thread
    dispatcher().dispatchSyntheticMethodCall(syntheticFrame);
    // traverse loaded classes to create type load and object new events
    for (final Object c : vm.allClasses())
    {
      // might need to skip array types
      if (c instanceof ArrayType && !manager().generateArrayEvents())
      {
        continue;
      }
      // reference type
      final ReferenceType refType = (ReferenceType) c;
      // do not load out-of-model types explicitly
      if (!delegate.isInModel(refType))
      {
        continue;
      }
      // load the type and its super types
      delegate.handleTypeLoad(refType, syntheticFrame.thread());
      // create all instances but do not assign field values
      for (final Object o : refType.instances(0))
      {
        if (o instanceof ArrayReference)
        {
          delegate.handleNewArray((ArrayReference) o, syntheticFrame.location(), syntheticFrame);
        }
        else
        {
          delegate.handleNewObject((ObjectReference) o, syntheticFrame.thread());
        }
      }
    }
    // traverse loaded classes to create field write events
    for (final Object c : vm.allClasses())
    {
      // skip array types
      if (c instanceof ArrayType)
      {
        continue;
      }
      // reference type
      final ReferenceType refType = (ReferenceType) c;
      // do not load out-of-model types and their objects
      if (!delegate.isInModel(refType))
      {
        continue;
      }
      // static fields (all objects available)
      for (final Object f : refType.fields())
      {
        final Field field = (Field) f;
        // create an assignment for this field
        if (field.isStatic() && !field.isSynthetic())
        {
          dispatcher().dispatchSyntheticFieldWrite(syntheticFrame.thread(), null, field,
              refType.getValue(field));
        }
      }
      // instance fields (all objects available)
      for (final Object o : refType.instances(0))
      {
        final ObjectReference object = (ObjectReference) o;
        for (final Object f : object.referenceType().fields())
        {
          final Field field = (Field) f;
          // create an assignment for this field
          if (!field.isStatic() && !field.isSynthetic())
          {
            dispatcher().dispatchSyntheticFieldWrite(syntheticFrame.thread(), object, field,
                object.getValue(field));
          }
        }
      }
      // System.err.println(refType.name() + " processed correctly.");
    }
    // return from the run() method on the snapshot thread
    dispatcher().dispatchSyntheticMethodExit(syntheticFrame);
    // terminate the snapshot thread
    dispatcher().dispatchThreadDeath(syntheticFrame.thread());
    // traverse call stacks
    final List<ThreadReference> threads = vm.allThreads();
    Collections.sort(threads, new Comparator<ThreadReference>()
      {
        @Override
        public int compare(final ThreadReference o1, final ThreadReference o2)
        {
          return (int) (o1.uniqueID() - o2.uniqueID());
        }
      });
    for (int i = 0; i < threads.size(); i++)
    {
      final ThreadReference thread = threads.get(i);
      try
      {
        // not a thread of interest, move to the next
        if (!manager().modelFilter().acceptsThread(thread))
        {
          continue;
        }
        boolean isInModel = false;
        final Map<StackFrame, Boolean> frameToModel = new LinkedHashMap<StackFrame, Boolean>();
        // traverse the thread to determine if we have to process it
        for (int index = thread.frames().size() - 1; index >= 0; index--)
        {
          // obtain the frame
          final StackFrame frame = thread.frames().get(index);
          // the caller's type reference
          final ReferenceType callerType = frame.location().method().declaringType();
          // map the frame to its in-model status
          frameToModel.put(frame, delegate.isInModel(callerType));
          // mark as in-model
          if (delegate.isInModel(callerType))
          {
            isInModel = true;
          }
        }
        // if the thread is not in-model, ignore it
        if (!isInModel)
        {
          continue;
        }
        // create the thread
        // dispatcher().dispatchThreadStart(thread);
        // traverse the thread
        for (final StackFrame frame : frameToModel.keySet())
        {
          // create an out-of-model call and continue
          if (!frameToModel.get(frame))
          {
            dispatcher().dispatchOutOfModelCallEvent(frame);
            continue;
          }
          // resolve the method
          delegate.resolveMethod(frame, frame.location().method());
          // create an in-model call
          dispatcher().dispatchInModelCallEvent(null, frame);
          // process local variables
          if (manager().generateLocalEvents())
          {
            try
            {
              // first visit the arguments
              delegate.handleLocals(null, frame, frame.location());
              // then visit the remaining locals
              delegate.handleLocals(null, frame, frame.location());
            }
            catch (final AbsentInformationException e)
            {
              // cowardly ignore
            }
          }
        }
      }
      catch (final IncompatibleThreadStateException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  @Override
  public void jdiAccessWatchpoint(final AccessWatchpointEvent event)
  {
    if (!override && !owner.isStarted())
    {
      return;
    }
    try
    {
      delegate.handleFieldRead(event, manager().generateLocalEvents());
    }
    catch (final Throwable e)
    {
      JiveDebugPlugin.log(e);
    }
  }

  @Override
  public void jdiClassPrepare(final ClassPrepareEvent event)
  {
    if (!override && !owner.isStarted())
    {
      return;
    }
    try
    {
      delegate.handleTypeLoad(event.referenceType(), event.thread());
    }
    catch (final Throwable e)
    {
      JiveDebugPlugin.log(e);
    }
  }

  @Override
  public synchronized void jdiExceptionThrown(final ExceptionEvent event)
  {
    if (!override && !owner.isStarted())
    {
      return;
    }
    try
    {
      delegate.handleExceptionThrown(event);
    }
    catch (final Throwable e)
    {
      JiveDebugPlugin.log(e);
    }
  }

  @Override
  public void jdiMethodEntry(final MethodEntryEvent event)
  {
    if (!override && !owner.isStarted())
    {
      return;
    }
    try
    {
      // if (manager().generateLockEvents()) {
      // inspectThreads();
      // }
      delegate.handleMethodEntry(event, manager().generateLocalEvents());
    }
    catch (final Throwable e)
    {
      JiveDebugPlugin.log(e);
    }
  }

  @Override
  public synchronized void jdiMethodExit(final MethodExitEvent event)
  {
    if (!override && !owner.isStarted())
    {
      return;
    }
    try
    {
      delegate.handleMethodExit(event, manager().generateLocalEvents());
      // if (manager().generateLockEvents()) {
      // inspectThreads();
      // }
    }
    catch (final Throwable e)
    {
      JiveDebugPlugin.log(e);
    }
  }

  @Override
  public void jdiModificationWatchpoint(final ModificationWatchpointEvent event)
  {
    if (!override && !owner.isStarted())
    {
      return;
    }
    try
    {
      delegate.handleFieldWrite(event);
    }
    catch (final Throwable e)
    {
      JiveDebugPlugin.log(e);
    }
  }

  @Override
  public void jdiStep(final StepEvent event)
  {
    if (!override && !owner.isStarted())
    {
      return;
    }
    try
    {
      delegate.handleStep(event, manager().generateLocalEvents());
      // if (manager().generateLockEvents()) {
      // inspectThreads();
      // }
    }
    catch (final Throwable e)
    {
      JiveDebugPlugin.log(e);
    }
  }

  @Override
  public synchronized void jdiThreadDeath(final ThreadDeathEvent event)
  {
    if (!override && !owner.isStarted())
    {
      return;
    }
    try
    {
      if (vmThreadId != null)
      {
        delegate.handleThreadDeath(event);
      }
    }
    catch (final Throwable e)
    {
      JiveDebugPlugin.log(e);
    }
  }

  /**
   * All thread start events are inferred.
   */
  @Override
  public void jdiThreadStart(final com.sun.jdi.event.ThreadStartEvent event)
  {
    if (!override && !owner.isStarted())
    {
      return;
    }
    try
    {
      // dispatcher().dispatchThreadStart(event.thread());
    }
    catch (final Throwable e)
    {
      JiveDebugPlugin.log(e);
    }
  }

  @Override
  public synchronized void jdiVMDeath(final VMDeathEvent event)
  {
    if (!override && !owner.isStarted())
    {
      return;
    }
    systemExit();
  }

  @Override
  public synchronized void jdiVMDisconnect(final VMDisconnectEvent event)
  {
    if (!override && !owner.isStarted())
    {
      return;
    }
    systemExit();
  }

  @Override
  public void jdiVMStart(final VMStartEvent event)
  {
    if (!override && !owner.isStarted())
    {
      return;
    }
    try
    {
      vmThreadId = manager().executionModel().valueFactory()
          .createThread(event.thread().uniqueID(), event.thread().name());
      // dispatcher().dispatchSystemStart(vmThreadId);
    }
    catch (final Throwable e)
    {
      JiveDebugPlugin.log(e);
    }
  }

  private IJiveEventDispatcher dispatcher()
  {
    return manager().jiveDispatcher();
  }

  private IJDIManager manager()
  {
    return JiveDebugPlugin.getDefault().jdiManager(owner);
  }

  private void systemExit()
  {
    if (!override && !owner.isStarted())
    {
      return;
    }
    try
    {
      // when the main thread has already died, e.g., due to an exception, vmThreadId is null
      if (owner.model().lookupRoot().terminator() == null)
      {
        dispatcher().dispatchSystemExitEvent();
        vmThreadId = null;
      }
    }
    catch (final Throwable e)
    {
      JiveDebugPlugin.log(e);
    }
  }

  // private void inspectThreadLockAcquires(final List<IThreadSummary> summaries) {
  //
  // for (final IThreadSummary summary : summaries) {
  // // acquired out-of-model lock
  // for (final String lockDescription : summary.acquiredLockDescriptions()) {
  // dispatchEvent(eventFactoryAdapter().createLockEvent(summary.thread(),
  // LockOperation.LOCK_ACQUIRE, null, lockDescription));
  // }
  // // acquired in-model lock
  // for (final Contour lock : summary.acquiredLocks()) {
  // dispatchEvent(eventFactoryAdapter().createLockEvent(summary.thread(),
  // LockOperation.LOCK_ACQUIRE, lock, null));
  // }
  // }
  // }
  //
  // private void inspectThreadLockReleases(final List<IThreadSummary> summaries) {
  //
  // for (final IThreadSummary summary : summaries) {
  // // released out-of-model lock
  // for (final String lockDescription : summary.releasedLockDescriptions()) {
  // dispatchEvent(eventFactoryAdapter().createLockEvent(summary.thread(),
  // LockOperation.LOCK_RELEASE, null, lockDescription));
  // }
  // // released in-model lock
  // for (final Contour lock : summary.releasedLocks()) {
  // dispatchEvent(eventFactoryAdapter().createLockEvent(summary.thread(),
  // LockOperation.LOCK_RELEASE, lock, null));
  // }
  // }
  // }
  //
  // private void inspectThreads() {
  //
  // final List<?> threads = vm.allThreads();
  // final List<IThreadSummary> summaries = new ArrayList<IThreadSummary>();
  // for (int i = 0; i < threads.size(); i++) {
  // IThreadSummary summary = manager().executionManager().inspectThread(
  // (ThreadReference) threads.get(i));
  // if (summary != null) {
  // summaries.add(summary);
  // }
  // summary = null;
  // }
  // // all lock releases
  // inspectThreadLockReleases(summaries);
  // // all lock acquires
  // inspectThreadLockAcquires(summaries);
  // // all state changes
  // inspectThreadStateChange(summaries);
  // }
  //
  // private void inspectThreadStateChange(final List<IThreadSummary> summaries) {
  //
  // for (final IThreadSummary summary : summaries) {
  // // wait --> active | other
  // // active | other --> wait
  // if (!summary.inconsistentWait() && summary.isWaiting()) {
  // dispatchEvent(eventFactoryAdapter().createLockEvent(summary.thread(), summary.operation(),
  // summary.lock(), summary.lockDescription()));
  // }
  // }
  // }
  @Override
  public void reset()
  {
    override = false;
    vmThreadId = null;
    delegate.reset();
  }
}
