package edu.buffalo.cse.jive.internal.model.store.memory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.ITransaction;
import edu.buffalo.cse.jive.model.IExecutionModel.AtomicDelete;
import edu.buffalo.cse.jive.model.IExecutionModel.AtomicInsert;
import edu.buffalo.cse.jive.model.IExecutionModel.AtomicUpdate;
import edu.buffalo.cse.jive.model.IExecutionModel.IStateChange;
import edu.buffalo.cse.jive.model.IModel.IValue;
import edu.buffalo.cse.jive.model.ITransactionLog;
import edu.buffalo.cse.jive.model.IVisitor;

/**
 * Thread-safe transaction log implementation that delegates fine-grained state management to a
 * {@code SnapshotState} instance. Atomic operations supported by the transaction log correspond to
 * thread-safe store mutator methods. However, transactions may perform multiple changes, all of
 * which must be applied as one atomic unit. This class provides exactly this service-- that is, a
 * list of atomic operations can be applied and/or reverted atomically to the underlying snapshot.
 * This class also maintains a replay event cursor that allows reverting back to any previous
 * snapshot state, then forward again, etc. No new transactions can begin until the cursor points to
 * the most recent committed transaction in the log.
 * 
 * Subclassing ReentrantReadWriteLock opportunistically simplifies locking and avoids separate
 * construction.
 */
class TransactionLog extends ReentrantReadWriteLock implements ITransactionLog
{
  private static final long serialVersionUID = 7974054832211319199L;

  private static IStateChange atomicDelete(final IContour removed)
  {
    return new AtomicDelete()
      {
        @Override
        public IContour contour()
        {
          return removed;
        }
      };
  }

  private static IStateChange atomicInsert(final IContour added)
  {
    return new AtomicInsert()
      {
        @Override
        public IContour contour()
        {
          return added;
        }
      };
  }

  private static IStateChange atomicUpdate(final IContour contour, final IContourMember member,
      final IValue newValue, final IValue oldValue)
  {
    return new AtomicUpdate()
      {
        @Override
        public IContour contour()
        {
          return contour;
        }

        @Override
        public IContourMember member()
        {
          return member;
        }

        @Override
        public IValue newValue()
        {
          return newValue;
        }

        @Override
        public IValue oldValue()
        {
          return oldValue;
        }
      };
  }

  protected final ExecutionModel model;

  TransactionLog(final ExecutionModel model)
  {
    this.model = model;
  }

  // TODO: push to the event
  @Override
  public void atomicObjectDestroy(final IJiveEvent event, final IContextContour contour)
  {
    writeLock().lock();
    try
    {
      // makes sure a new transaction can begin
      assertStatus(checkBegin());
      // create the list of changes
      final List<IStateChange> changes = new LinkedList<IStateChange>();
      IContextContour instance = contour;
      // virtual contour removed first, most generic contour last
      while (instance != null)
      {
        // System.err.println("ATOMIC_DESTROY[" + instance.toString() + "]");
        changes.add(TransactionLog.atomicDelete(instance));
        instance = instance.parent();
      }
      // encapsulates changes in a transaction and commits
      storeTransaction(event, changes);
    }
    finally
    {
      writeLock().unlock();
    }
  }

  private void assertStatus(final String error)
  {
    readLock().lock();
    try
    {
      if (error != null)
      {
        throw new IllegalStateException(error);
      }
    }
    finally
    {
      readLock().unlock();
    }
  }

  private String checkBegin()
  {
    readLock().lock();
    try
    {
      final IJiveEvent replayEvent = store().transactionLogCursor();
      // cannot begin in "replay" mode
      if (replayEvent != null)
      {
        return "Cannot begin transaction: the log is currently in replay mode.";
      }
      if (store().isVirtual())
      {
        return "Cannot begin transaction: the log is currently in virtual mode.";
      }
      // cannot begin if the current transaction is dirty (uncommitted)
      if (store().transactionCount() > 0 && !store().lastTransaction().isCommitted())
      {
        return "Cannot begin transaction: the transaction log has an outstanding uncommitted transaction.";
      }
      // all checks passed
      return null;
    }
    finally
    {
      readLock().unlock();
    }
  }

  private String checkCommit()
  {
    readLock().lock();
    try
    {
      // cannot commit when no outstanding transactions exist
      if (store().transactionCount() == 0)
      {
        return "Cannot commit: no transaction in the transaction log.";
      }
      final IJiveEvent replayEvent = store().transactionLogCursor();
      // cannot commit in normal mode if the current transaction is committed
      if (replayEvent == null && store().lastTransaction().isCommitted())
      {
        return "Cannot commit: current transaction is committed.";
      }
      // check for a prior transaction
      final ITransaction replayTransaction = replayEvent == null ? null : replayEvent.transaction();
      // cannot commit in "replay" mode if the replay transaction is committed
      if (replayTransaction != null && replayTransaction.isCommitted())
      {
        return "Cannot commit: replay transaction is committed.";
      }
      // all checks passed
      return null;
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * TODO: reduce the cyclomatic complexity.
   */
  private String checkRollback()
  {
    readLock().lock();
    try
    {
      // cannot roll back when no transactions exist
      if (store().transactionCount() == 0)
      {
        return "Cannot roll back: no transaction in the log.";
      }
      final IJiveEvent replayEvent = store().transactionLogCursor();
      // cannot roll back in "replay" mode after rolling back the last transaction
      if (replayEvent == model.lookupRoot())
      {
        return "Cannot roll back: the entire log is uncommitted.";
      }
      // cannot roll back in normal mode if the current transaction is dirty (uncommitted)
      if (replayEvent == null && !store().lastTransaction().isCommitted())
      {
        return "Cannot roll back: current transaction is uncommitted.";
      }
      // check for a prior event
      final IJiveEvent prior = replayEvent == null ? null : replayEvent.prior();
      // check for a prior transaction
      final ITransaction priorTransaction = prior == null ? null : prior.transaction();
      // cannot roll back in "replay" mode if the transaction at the cursor is dirty (uncommitted)
      if (replayEvent != null && replayEvent != model.lookupRoot() && priorTransaction != null
          && !priorTransaction.isCommitted())
      {
        return "Cannot roll back: replay transaction is uncommitted.";
      }
      // all checks passed
      return null;
    }
    finally
    {
      readLock().unlock();
    }
  }

  private Store store()
  {
    return model.store();
  }

  /**
   * Encapsulates a list of changes into a transaction, appends the transaction to the log, maps it
   * to an event, and commits.
   * 
   * @requires a write lock to be obtained by the caller
   */
  private void storeTransaction(final IJiveEvent event, final List<IStateChange> changes)
  {
    store().storeTransaction(event, changes);
  }

  /**
   * Called by {@code visitDepthFirst(final Visitor<E> visitor)} and protected by a read lock.
   * 
   * @throws IllegalArgumentException
   *           when element is null
   * 
   * @requires a read lock to be obtained by the caller
   */
  private void visitDepthFirst(final IContour element, final IVisitor<IContour> visitor)
  {
    visitor.visit(element);
    for (final IContour child : getChildren(element))
    {
      visitDepthFirst(child, visitor);
    }
  }

  // TODO: push to the event
  @Override
  public void atomicEmpty(final IJiveEvent event)
  {
    writeLock().lock();
    try
    {
      // makes sure a new transaction can begin
      assertStatus(checkBegin());
      // encapsulates changes in a transaction and commits
      storeTransaction(event, null);
    }
    finally
    {
      writeLock().unlock();
    }
  }

  @Override
  public void atomicMethodEnter(final IJiveEvent event, final IValue caller,
      final IMethodContour method)
  {
    writeLock().lock();
    try
    {
      // makes sure a new transaction can begin
      assertStatus(checkBegin());
      // create the list of changes
      final List<IStateChange> changes = new LinkedList<IStateChange>();
      // method contour inserted
      changes.add(TransactionLog.atomicInsert(method));
      // retrieve the rpdl variable instance
      final IContourMember rpdl = method.lookupRPDLMember();
      // return point member of the method contour updated
      changes.add(TransactionLog.atomicUpdate(method, rpdl, caller, rpdl.value()));
      // encapsulates changes in a transaction and commits
      storeTransaction(event, changes);
    }
    finally
    {
      writeLock().unlock();
    }
  }

  // TODO: push to the event
  @Override
  public void atomicMethodExit(final IJiveEvent event, final IMethodContour method)
  {
    writeLock().lock();
    try
    {
      // makes sure a new transaction can begin
      assertStatus(checkBegin());
      // create the list of changes
      final List<IStateChange> changes = new LinkedList<IStateChange>();
      // method contour deleted
      changes.add(TransactionLog.atomicDelete(method));
      // encapsulates changes in a transaction and commits
      storeTransaction(event, changes);
    }
    finally
    {
      writeLock().unlock();
    }
  }

  // TODO: push to the event
  @Override
  public void atomicObjectNew(final IJiveEvent event, final IContextContour contour)
  {
    writeLock().lock();
    try
    {
      // makes sure a new transaction can begin
      assertStatus(checkBegin());
      // create the list of changes
      final List<IStateChange> changes = new LinkedList<IStateChange>();
      final Stack<IContextContour> stack = new Stack<IContextContour>();
      IContextContour instance = contour;
      while (instance != null)
      {
        stack.push(instance);
        instance = instance.parent();
      }
      // most generic contour added first, virtual contour last
      while (!stack.isEmpty())
      {
        changes.add(TransactionLog.atomicInsert(stack.pop()));
      }
      // encapsulates changes in a transaction and commits
      storeTransaction(event, changes);
    }
    finally
    {
      writeLock().unlock();
    }
  }

  // TODO: push to the event
  @Override
  public void atomicRemoveContour(final IJiveEvent event, final IMethodContour method)
  {
    writeLock().lock();
    try
    {
      // makes sure a new transaction can begin
      assertStatus(checkBegin());
      // create the list of changes
      final List<IStateChange> changes = new LinkedList<IStateChange>();
      // method contour deleted
      changes.add(TransactionLog.atomicDelete(method));
      // encapsulates changes in a transaction and commits
      storeTransaction(event, changes);
    }
    finally
    {
      writeLock().unlock();
    }
  }

  // TODO: push to the event
  @Override
  public void atomicTypeLoad(final IJiveEvent event, final IContextContour contour)
  {
    writeLock().lock();
    try
    {
      // makes sure a new transaction can begin
      assertStatus(checkBegin());
      // create the list of changes
      final List<IStateChange> changes = new LinkedList<IStateChange>();
      // static contour inserted
      changes.add(TransactionLog.atomicInsert(contour));
      // encapsulates changes in a transaction and commits
      storeTransaction(event, changes);
    }
    finally
    {
      writeLock().unlock();
    }
  }

  // TODO: push to the event
  @Override
  public void atomicValueSet(final IJiveEvent event, final IContour contour,
      final IContourMember member, final IValue newValue)
  {
    writeLock().lock();
    try
    {
      // makes sure a new transaction can begin
      assertStatus(checkBegin());
      // create the list of changes
      final List<IStateChange> changes = new LinkedList<IStateChange>();
      if (contour == null)
      {
        System.err.println("Unexpected condition: null contour for " + contour);
      }
      changes.add(TransactionLog.atomicUpdate(contour, member, newValue, member.value()));
      // encapsulates changes in a transaction and commits
      storeTransaction(event, changes);
    }
    finally
    {
      writeLock().unlock();
    }
  }

  boolean canCommit()
  {
    readLock().lock();
    try
    {
      return checkCommit() == null;
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * Can commit a particular event number if (a) replay commit is allowed and (2) the last rolled
   * back transaction is smaller or equal to than event number.
   */
  boolean canCommit(final IJiveEvent event)
  {
    readLock().lock();
    try
    {
      /**
       * Since replayEvent contains the last rolled back transaction, its id must be smaller or
       * equal to the given event number.
       */
      final IJiveEvent replayEvent = store().transactionLogCursor();
      return event != null && canReplayCommit() && replayEvent.eventId() <= event.eventId();
    }
    finally
    {
      readLock().unlock();
    }
  }

  boolean canReplayCommit()
  {
    readLock().lock();
    try
    {
      final IJiveEvent replayEvent = store().transactionLogCursor();
      return replayEvent != null && checkCommit() == null;
    }
    finally
    {
      readLock().unlock();
    }
  }

  boolean canRollback()
  {
    readLock().lock();
    try
    {
      return checkRollback() == null;
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * Can roll back to a particular event if (a) roll back is allowed and (2) the last rolled back
   * transaction is larger than event number.
   */
  boolean canRollback(final IJiveEvent event)
  {
    readLock().lock();
    try
    {
      /**
       * Since replayEvent contains the last rolled back transaction, its id must be larger than the
       * given event number.
       */
      final IJiveEvent replayEvent = store().transactionLogCursor();
      return event != null && checkRollback() == null
          && (replayEvent == null || replayEvent.eventId() > event.eventId());
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * When commit completes successfully, the transaction log may be in either normal or replay mode.
   */
  void commit()
  {
    writeLock().lock();
    try
    {
      // makes sure the current transaction can commit
      assertStatus(checkCommit());
      // delegate to the store
      store().transactionCommitReplay();
    }
    finally
    {
      writeLock().unlock();
    }
  }

  /**
   * Returns true if this element is either a child or a parent in the model. This method is an
   * accessor.
   */
  boolean contains(final IContour element)
  {
    readLock().lock();
    try
    {
      return model.store().contourInModel(element);
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * Thread-safe retrieval of the children of the given element. Returns an immutable list of child
   * contour elements, which may be empty if the element has no children in the model. This method
   * is an accessor.
   * 
   * @return immutable list of contour elements, where each contour in the list is a child of the
   *         given element
   * @throws IllegalArgumentException
   *           when element is null
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<IContour> getChildren(final IContour element)
  {
    readLock().lock();
    try
    {
      // element must be non-null
      if (element == null)
      {
        throw new IllegalArgumentException("Cannot retrieve child list of a null element.");
      }
      final List<IContour> children = model.store().contourChildren(element);
      return Collections.unmodifiableList(children != null ? children
          : (List<IContour>) Collections.EMPTY_LIST);
    }
    finally
    {
      readLock().unlock();
    }
  }

  IJiveEvent getEvent()
  {
    readLock().lock();
    try
    {
      final IJiveEvent replayEvent = store().transactionLogCursor();
      if (replayEvent != null)
      {
        return replayEvent;
      }
      return store().lastTransactionEvent();
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * Thread-safe retrieval of the root elements in the model. This method is an accessor.
   * 
   * @ensures returned list is non-null
   */
  @SuppressWarnings("unchecked")
  List<IContour> getRoots()
  {
    readLock().lock();
    try
    {
      final List<IContour> children = model.store().contourRoots();
      return Collections.unmodifiableList(children != null ? children
          : (List<IContour>) Collections.EMPTY_LIST);
    }
    finally
    {
      readLock().unlock();
    }
  }

  boolean isReady()
  {
    readLock().lock();
    try
    {
      return checkBegin() == null;
    }
    finally
    {
      readLock().unlock();
    }
  }

  boolean isReplayMode()
  {
    readLock().lock();
    try
    {
      final IJiveEvent replayEvent = store().transactionLogCursor();
      // return replayCursor >= 0;
      return replayEvent != null;
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * When roll back completes successfully, the transaction log must be in replay mode.
   */
  void rollback()
  {
    writeLock().lock();
    try
    {
      // makes sure the current transaction can roll back
      assertStatus(checkRollback());
      // delegate to the store
      store().transactionRollback();
    }
    finally
    {
      writeLock().unlock();
    }
  }

  void setFinalState()
  {
    writeLock().lock();
    try
    {
      while (canCommit())
      {
        commit();
      }
      // post-condition: all states have been committed
    }
    finally
    {
      writeLock().unlock();
    }
  }

  void setInitialState()
  {
    writeLock().lock();
    try
    {
      while (canRollback())
      {
        rollback();
      }
      // post-condition: all states have been rolled back
    }
    finally
    {
      writeLock().unlock();
    }
  }

  /**
   * Given the input event, the transaction log places the replay cursor exactly on this event. This
   * amounts to setting this event as the last uncommitted event in the log.
   */
  void setLastUncommittedEvent(final IJiveEvent event)
  {
    writeLock().lock();
    try
    {
      store().transactionSetLastUncommittedEvent(event);
    }
    finally
    {
      writeLock().unlock();
    }
  }

  long size()
  {
    readLock().lock();
    try
    {
      return store().transactionCount();
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * Thread-safe, depth-first visitation. This method is an accessor.
   * 
   * @throws IllegalArgumentException
   *           when element is null
   * 
   * @requires non-null visitor
   */
  void visitDepthFirst(final IVisitor<IContour> visitor)
  {
    readLock().lock();
    try
    {
      // visitor must be non-null
      if (visitor == null)
      {
        throw new IllegalArgumentException("Cannot visit: visitor is null.");
      }
      for (final IContour element : getRoots())
      {
        visitDepthFirst(element, visitor);
      }
    }
    finally
    {
      readLock().unlock();
    }
  }
}