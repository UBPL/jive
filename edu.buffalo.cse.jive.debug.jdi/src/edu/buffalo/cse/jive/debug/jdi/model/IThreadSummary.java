package edu.buffalo.cse.jive.debug.jdi.model;

import java.util.Set;

import com.sun.jdi.ThreadReference;

import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IEventModel.LockOperation;

public interface IThreadSummary
{
  /**
   * This thread acquired these locks since last inspection.
   */
  public Set<String> acquiredLockDescriptions();

  /**
   * This thread acquired these locks since last inspection.
   */
  public Set<IContour> acquiredLocks();

  public boolean inconsistentWait();

  /**
   * Determine if the wait state of the thread has changed.
   */
  public boolean isWaiting();

  /**
   * This thread is waiting for this lock to be released.
   */
  public IContour lock();

  /**
   * This thread is waiting for this lock to be released.
   */
  public String lockDescription();

  public LockOperation operation();

  /**
   * This thread released these locks since last inspection.
   */
  public Set<String> releasedLockDescriptions();

  /**
   * This thread released these locks since last inspection.
   */
  public Set<IContour> releasedLocks();

  public int status();

  public ThreadReference thread();
}