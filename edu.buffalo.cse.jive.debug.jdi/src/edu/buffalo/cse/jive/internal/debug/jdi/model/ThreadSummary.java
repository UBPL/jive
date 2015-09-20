package edu.buffalo.cse.jive.internal.debug.jdi.model;

import java.util.HashSet;
import java.util.Set;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;

import edu.buffalo.cse.jive.debug.jdi.model.IThreadSummary;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IEventModel.LockOperation;
import edu.buffalo.cse.jive.model.factory.IContourFactory;

@SuppressWarnings("restriction")
class ThreadSummary implements IThreadSummary
{
  private static final int STATUS_ACTIVE = 1;
  private static final int STATUS_OTHER = -1;
  private static final int STATUS_WAITING = 0;
  private final Set<String> acquiredLockDescriptions = new HashSet<String>();
  private final Set<IContour> acquiredLocks = new HashSet<IContour>();
  // private final long waitThreadId;
  // private final String waitThreadName;
  private final String lockDescription;
  private final IContour lock;
  private final Set<String> ownedLockDescriptions = new HashSet<String>();
  private final Set<IContour> ownedLocks = new HashSet<IContour>();
  private final Set<String> releasedLockDescriptions = new HashSet<String>();
  private final Set<IContour> releasedLocks = new HashSet<IContour>();
  private final boolean stateChanged;
  private final int status;
  private final ThreadReference thread;
  private final long threadId;

  /**
   * TODO: reduce the cyclomatic complexity.
   */
  ThreadSummary(final IContourFactory contourFactory, final ThreadReference thread,
      final ThreadSummary oldSummary)
  {
    this.thread = thread;
    this.threadId = thread.uniqueID();
    IContour monitor = null;
    ObjectReference waitMonitor = null;
    // this thread is in a wait state
    if (thread.status() == ThreadReference.THREAD_STATUS_RUNNING
        || thread.status() == ThreadReference.THREAD_STATUS_SLEEPING)
    {
      this.status = ThreadSummary.STATUS_ACTIVE;
    }
    else if (thread.status() == ThreadReference.THREAD_STATUS_MONITOR
        || thread.status() == ThreadReference.THREAD_STATUS_WAIT)
    {
      this.status = ThreadSummary.STATUS_WAITING;
      if (thread.isSuspended() && thread.virtualMachine().canGetCurrentContendedMonitor())
      {
        try
        {
          waitMonitor = thread.currentContendedMonitor();
          if (waitMonitor != null)
          {
            monitor = contourFactory.lookupInstanceContour(waitMonitor.type().name(),
                waitMonitor.uniqueID());
            // System.err.println("thread " + thread.name() + " waiting for object "
            // + (monitorId == null ? "none" : monitorId.toString()));
            // // waiting for in-model object, or the waiting thread is set
            // if (monitorId != null || waitMonitor.owningThread() != null) {
            // waitThread = waitMonitor.owningThread();
            // System.err.println("thread " + thread.name() + " waiting for object "
            // + (monitorId == null ? waitMonitor.toString() : monitorId.toString())
            // + " owned by thread " + (waitThread == null ? "none" : waitThread.name()));
            // }
          }
        }
        catch (final IncompatibleThreadStateException e)
        {
          e.printStackTrace();
        }
      }
    }
    else
    {
      status = ThreadSummary.STATUS_OTHER;
    }
    // compute the locks owned and newly acquired by this thread
    if (thread.virtualMachine().canGetOwnedMonitorInfo())
    {
      try
      {
        for (final Object object : thread.ownedMonitors())
        {
          final ObjectReference lockRef = (ObjectReference) object;
          final IContour lock = contourFactory.lookupInstanceContour(lockRef.type().name(),
              lockRef.uniqueID());
          if (lock != null)
          {
            ownedLocks.add(lock);
            if (oldSummary == null || !oldSummary.ownedLocks.contains(lock))
            {
              acquiredLocks.add(lock);
            }
          }
          else
          {
            ownedLockDescriptions.add(lockRef.toString());
            if (oldSummary == null
                || !oldSummary.ownedLockDescriptions.contains(lockRef.toString()))
            {
              acquiredLockDescriptions.add(lockRef.toString());
            }
          }
        }
      }
      catch (final IncompatibleThreadStateException e)
      {
        e.printStackTrace();
      }
    }
    // compute the locks released by this thread
    if (oldSummary != null)
    {
      for (final IContour lock : oldSummary.ownedLocks)
      {
        if (!ownedLocks.contains(lock))
        {
          releasedLocks.add(lock);
        }
      }
      for (final String lockDescription : oldSummary.ownedLockDescriptions)
      {
        if (!ownedLockDescriptions.contains(lockDescription))
        {
          releasedLockDescriptions.add(lockDescription);
        }
      }
    }
    lock = monitor;
    lockDescription = waitMonitor != null ? waitMonitor.toString() : "";
    stateChanged = !equals(oldSummary);
    // waitThreadId = waitThread != null ? waitThread.uniqueID() : -1;
    // waitThreadName = waitThread != null ? waitThread.name() : "";
  }

  /**
   * This thread acquired these locks since last inspection.
   */
  @Override
  public Set<String> acquiredLockDescriptions()
  {
    return acquiredLockDescriptions;
  }

  /**
   * This thread acquired these locks since last inspection.
   */
  @Override
  public Set<IContour> acquiredLocks()
  {
    return acquiredLocks;
  }

  @Override
  public boolean equals(final Object other)
  {
    if (!(other instanceof ThreadSummary))
    {
      return false;
    }
    final ThreadSummary old = (ThreadSummary) other;
    return old.threadId == threadId && old.status == status && old.lock == lock
        && old.lockDescription.equals(lockDescription);
  }

  @Override
  public int hashCode()
  {
    return (int) (31 * threadId + 23 * status + 47 * (lock != null ? lock : lockDescription)
        .hashCode());
  }

  @Override
  public boolean inconsistentWait()
  {
    return this.status == ThreadSummary.STATUS_WAITING && lock == null
        && lockDescription.length() == 0;
  }

  /**
   * Determine if the wait state of the thread has changed.
   */
  @Override
  public boolean isWaiting()
  {
    return this.stateChanged && this.status == ThreadSummary.STATUS_WAITING;
  }

  /**
   * This thread is waiting for this lock to be released.
   */
  @Override
  public IContour lock()
  {
    return this.lock;
  }

  /**
   * This thread is waiting for this lock to be released.
   */
  @Override
  public String lockDescription()
  {
    return this.lockDescription;
  }

  @Override
  public LockOperation operation()
  {
    return status == ThreadSummary.STATUS_WAITING ? LockOperation.LOCK_WAIT : null;
  }

  /**
   * This thread released these locks since last inspection.
   */
  @Override
  public Set<String> releasedLockDescriptions()
  {
    return releasedLockDescriptions;
  }

  /**
   * This thread released these locks since last inspection.
   */
  @Override
  public Set<IContour> releasedLocks()
  {
    return releasedLocks;
  }

  @Override
  public int status()
  {
    return this.status;
  }

  @Override
  public ThreadReference thread()
  {
    return this.thread;
  }
  // long waitThreadId() {
  //
  // return this.waitThreadId;
  // }
  //
  // String waitThreadName() {
  //
  // return this.waitThreadName;
  // }
}