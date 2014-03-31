package edu.buffalo.cse.jive.internal.model.store.memory;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArraySet;

import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.model.ast.StaticAnalysisFactory;
import edu.buffalo.cse.jive.model.contours.ContourFactory;
import edu.buffalo.cse.jive.model.contours.ContourFactory.ContextContour;
import edu.buffalo.cse.jive.model.events.EventFactory;
import edu.buffalo.cse.jive.model.events.EventFactory.JiveEvent;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IEventModel.IAssignEvent;
import edu.buffalo.cse.jive.model.IEventModel.IEventProducer;
import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodExitEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodTerminatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.INewThreadEvent;
import edu.buffalo.cse.jive.model.IEventModel.IRealTimeEvent;
import edu.buffalo.cse.jive.model.IEventModel.IScopeAllocEvent;
import edu.buffalo.cse.jive.model.IEventModel.ISystemExitEvent;
import edu.buffalo.cse.jive.model.IEventModel.ISystemStartEvent;
import edu.buffalo.cse.jive.model.IEventModel.ITerminatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IThreadEndEvent;
import edu.buffalo.cse.jive.model.IEventModel.IThreadStartEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;
import edu.buffalo.cse.jive.model.ITransactionLog;
import edu.buffalo.cse.jive.model.IVisitor;
import edu.buffalo.cse.jive.model.factory.IContourFactory;
import edu.buffalo.cse.jive.model.factory.IEventFactory;
import edu.buffalo.cse.jive.model.factory.IQueryFactory;
import edu.buffalo.cse.jive.model.factory.IStaticAnalysisFactory;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory;
import edu.buffalo.cse.jive.model.factory.IValueFactory;
import edu.buffalo.cse.jive.model.queries.QueryFactory;
import edu.buffalo.cse.jive.model.slicing.ProgramSlice;
import edu.buffalo.cse.jive.model.statics.StaticModelFactory;
import edu.buffalo.cse.jive.model.values.ValueFactory;

public class ExecutionModel implements IExecutionModel
{
  private final ContourFactory contourFactory;
  private final ContourView contourView;
  private final EventFactory eventFactory;
  private boolean hasSnapshot;
  private final IQueryFactory queryFactory;
  private ISystemStartEvent root;
  private final SliceView sliceView;
  private final StaticAnalysisFactory staticAnalysisFactory;
  private final StaticModelFactory staticModelFactory;
  private final Store store;
  private final ITemporalState temporalState;
  private final Map<IThreadValue, IThreadStartEvent> threads;
  private final Map<IThreadValue, Stack<IMethodCallEvent>> threadStacks;
  private final TraceView traceView;
  private final TransactionLog transactionLog;
  private final ValueFactory valueFactory;

  // private IMemoryStore memoryStore;
  public ExecutionModel()
  {
    this(true);
  }

  public ExecutionModel(final boolean createKnownTypes)
  {
    this.hasSnapshot = false;
    this.store = new Store(this);
    // this.memoryStore = new MemoryStore();
    this.transactionLog = new TransactionLog(this);
    this.temporalState = new TemporalState();
    this.contourView = new ContourView();
    this.traceView = new TraceView();
    this.threadStacks = TypeTools.newHashMap();
    this.threads = TypeTools.newLinkedHashMap();
    this.contourFactory = ContourFactory.getDefault(this);
    this.eventFactory = new EventFactory(this);
    this.valueFactory = new ValueFactory(this);
    this.queryFactory = new QueryFactory(this);
    this.sliceView = new SliceView();
    this.staticAnalysisFactory = new StaticAnalysisFactory(this);
    this.staticModelFactory = StaticModelFactory.getDefault(this);
  }

  @Override
  public IContourFactory contourFactory()
  {
    return this.contourFactory;
  }

  @Override
  public IContourView contourView()
  {
    return this.contourView;
  }

  @Override
  public void done()
  {
    // memory store
    // try
    // {
    // memoryStore.close();
    // }
    // catch (final IOException e)
    // {
    // e.printStackTrace();
    // }
  }

  @Override
  public IEventFactory eventFactory()
  {
    return this.eventFactory;
  }

  @Override
  public void eventOccurred(final IEventProducer source, final List<IJiveEvent> events)
  {
    // updates require a write lock
    transactionLog.writeLock().lock();
    try
    {
      // update the model
      updateModel(events);
      // commit the event's changes to the model
      for (final IJiveEvent event : events)
      {
        // commit the event's changes to the model
        ((JiveEvent) event).commit();
      }
    }
    finally
    {
      // downgrade lock so that notification code doesn't need to perform additional locking
      transactionLog.readLock().lock();
      transactionLog.writeLock().unlock();
      try
      {
        // notify trace listeners
        traceView.notifyListeners(events);
      }
      finally
      {
        transactionLog.readLock().unlock();
      }
    }
  }

  @Override
  public boolean hasSnapshot()
  {
    return hasSnapshot;
  }

  @Override
  public IJiveEvent lookupEvent(final long eventId)
  {
    readLock();
    try
    {
      return store.lookupEvent(eventId);
    }
    finally
    {
      readUnlock();
    }
  }

  @Override
  public ISystemStartEvent lookupRoot()
  {
    return this.root;
  }

  @Override
  public IThreadStartEvent lookupThread(final IThreadValue thread)
  {
    return threads.get(thread);
  }

  @Override
  public List<IThreadStartEvent> lookupThreads()
  {
    readLock();
    try
    {
      final List<IThreadStartEvent> result = TypeTools.newArrayList();
      for (final IThreadStartEvent event : threads.values())
      {
        if (((JiveEvent) event).isVisible())
        {
          result.add(event);
        }
      }
      return result;
    }
    finally
    {
      readUnlock();
    }
  }

  @Override
  public IMethodCallEvent lookupTop(final IThreadValue thread)
  {
    readLock();
    try
    {
      final Stack<IMethodCallEvent> stack = getStack(thread);
      return stack.isEmpty() ? null : stack.peek();
    }
    finally
    {
      readUnlock();
    }
  }

  @Override
  public IQueryFactory queryFactory()
  {
    return this.queryFactory;
  }

  @Override
  public void readLock()
  {
    transactionLog.readLock().lock();
  }

  @Override
  public void readUnlock()
  {
    transactionLog.readLock().unlock();
  }

  @Override
  public void reset()
  {
    readLock();
    try
    {
      // reset the system start
      root = null;
      // create a new store
      // memoryStore = new MemoryStore();
      // clear helper data structure
      threadStacks.clear();
      // clear helper data structure
      threads.clear();
      // clear all storage
      store.reset();
      // reinitialize known types
      staticModelFactory.createKnownTypes();
    }
    finally
    {
      readUnlock();
    }
  }

  @Override
  public ISliceView sliceView()
  {
    return this.sliceView;
  }

  @Override
  public IStaticAnalysisFactory staticAnalysisFactory()
  {
    return this.staticAnalysisFactory;
  }

  @Override
  public IStaticModelFactory staticModelFactory()
  {
    return this.staticModelFactory;
  }

  @Override
  public ITemporalState temporalState()
  {
    return this.temporalState;
  }

  @Override
  public void terminate()
  {
    // event batch
    final List<IJiveEvent> events = TypeTools.newArrayList();
    readLock();
    try
    {
      // traverse call stacks
      for (final IThreadStartEvent thread : threads.values())
      {
        if (thread.terminator() != null)
        {
          continue;
        }
        // peek at this thread's stack
        final Stack<IMethodCallEvent> stack = getStack(thread.thread());
        IMethodTerminatorEvent terminator = null;
        IMethodCallEvent top = null;
        for (int i = stack.size() - 1; i >= 0; i--)
        {
          // generate synthetic returned for last top
          if (terminator != null)
          {
            events.add(eventFactory.createMethodReturnedEvent(terminator));
            terminator = null;
          }
          // top of the stack
          top = stack.get(i);
          // generate synthetic exit for top
          terminator = (IMethodExitEvent) eventFactory.createMethodExitEvent(thread.thread(),
              top.line());
          events.add(terminator);
        }
        // generate synthetic returned for the thread
        if (terminator != null)
        {
          // terminate the thread
          events.add(eventFactory.createMethodReturnedEvent(terminator));
        }
        // terminate the thread
        events.add(eventFactory.createThreadEndEvent(thread.thread()));
      }
      // terminate the system
      events.add(eventFactory.createSystemExitEvent());
    }
    finally
    {
      readUnlock();
    }
    // process the batch at once
    eventOccurred(null, events);
  }

  @Override
  public ITraceView traceView()
  {
    return this.traceView;
  }

  @Override
  public IValueFactory valueFactory()
  {
    return this.valueFactory;
  }

  private Stack<IMethodCallEvent> getStack(final IThreadValue thread)
  {
    return threadStacks.get(thread);
  }

  /**
   * Outstanding execution associated with the given event, which is essentially the initiator event
   * corresponding to the top of the thread's stack frame. This method throws an execution if no
   * initiator is found.
   * 
   * Running time is O(1).
   */
  private IInitiatorEvent lookupParentExecution(final IJiveEvent event)
  {
    readLock();
    try
    {
      if (event instanceof ISystemStartEvent)
      {
        return null;
      }
      if (event instanceof ISystemExitEvent || event instanceof IThreadStartEvent)
      {
        return root;
      }
      IInitiatorEvent result = null;
      if (event instanceof IThreadEndEvent)
      {
        result = threads.get(event.thread());
      }
      else
      {
        // try to get a method call or a thread start if one is not available
        final Stack<IMethodCallEvent> stack = getStack(event.thread());
        result = stack == null ? null : stack.isEmpty() ? threads.get(event.thread()) : stack
            .peek();
      }
      // if not found, try to get the system start
      return (result == null ? root : result);
    }
    finally
    {
      readUnlock();
    }
  }

  /**
   * Pop the top of the stack of a specific thread.
   */
  private IMethodCallEvent pop(final IThreadValue thread)
  {
    readLock();
    try
    {
      // peek at this thread's stack
      final Stack<IMethodCallEvent> stack = getStack(thread);
      final IMethodCallEvent oldTop = stack.isEmpty() ? null : stack.pop();
      // the top must not be empty
      if (oldTop == null)
      {
        throw new IllegalStateException("Invalid (null) frame popped for thread '" + thread + "'.");
      }
      return oldTop;
    }
    finally
    {
      readUnlock();
    }
  }

  private void updateLastEvent(final IJiveEvent event)
  {
    // update the root's last event
    ((EventFactory.InitiatorEvent) root).setLastEvent(event);
    // update the duration of all threads
    for (final IThreadStartEvent threadEvent : threads.values())
    {
      if (threadEvent.terminator() == null)
      {
        ((EventFactory.InitiatorEvent) threadEvent).setLastEvent(event);
      }
    }
  }

  private void updateModel(final IJiveEvent event)
  {
    hasSnapshot = hasSnapshot
        || (event instanceof IThreadStartEvent && ((IThreadStartEvent) event).thread().id() == -1);
    // update relevant last events
    updateLastEvent(event);
    final IInitiatorEvent parent = lookupParentExecution(event);
    if (parent != null)
    {
      // nests this event under this parent execution and also under the corresponding contour
      updateParents(event, parent);
    }
    // updates calls stacks
    updateStacks(event);
    // update the event log
    store.storeEvent((JiveEvent) event);
  }

  /**
   * Makes sure the model is consistent before actually updating the event store.
   * 
   * TODO: reduce the cyclomatic complexity (20).
   */
  private void updateModel(final List<IJiveEvent> events)
  {
    ISystemExitEvent exit = null;
    final ListIterator<IJiveEvent> iterator = events.listIterator();
    while (iterator.hasNext())
    {
      final IJiveEvent event = iterator.next();
      // postpone the system exit-- cannot process anything after
      if (event instanceof ISystemExitEvent)
      {
        exit = (ISystemExitEvent) event;
        iterator.remove();
        continue;
      }
      // handle explicit system start events
      if (event instanceof ISystemStartEvent)
      {
        if (root == null)
        {
          // legitimate
          root = (ISystemStartEvent) event;
          updateModel(root);
        }
        else
        {
          // duplicate
          iterator.remove();
        }
        continue;
      }
      // handle allocation of the immortal memory scope at the SYSTEM level
      if (event instanceof IScopeAllocEvent && ((IScopeAllocEvent) event).isImmortal())
      {
        // update the event log-- don't update any structures
        store.storeEvent((JiveEvent) event);
        continue;
      }
      // handle explicit thread start events
      if (event instanceof IThreadStartEvent)
      {
        if (threads.get(event.thread()) == null)
        {
          // legitimate
          final IJiveEvent threadStart = event;
          threads.put(threadStart.thread(), (IThreadStartEvent) threadStart);
          threadStacks.put(threadStart.thread(), new Stack<IMethodCallEvent>());
          updateModel(threadStart);
        }
        else
        {
          // duplicate
          iterator.remove();
        }
        continue;
      }
      // ignore duplicate thread ends
      if (event instanceof IThreadEndEvent && threads.get(event.thread()).terminator() != null)
      {
        iterator.remove();
        continue;
      }
      // do not proceed without proper system start and thread start events
      if (root == null && !(event instanceof IThreadEndEvent)
          && !(event instanceof ISystemExitEvent))
      {
        root = (ISystemStartEvent) eventFactory.createSystemStartEvent();
        updateModel(root);
        final IJiveEvent threadStart = eventFactory.createThreadStartEvent(event.thread());
        threads.put(threadStart.thread(), (IThreadStartEvent) threadStart);
        threadStacks.put(threadStart.thread(), new Stack<IMethodCallEvent>());
        updateModel(threadStart);
        // reorder: <event, ...> --> <root, threadStart, event, ...>
        iterator.remove();
        iterator.add(root);
        iterator.add(threadStart);
        iterator.add(event);
      }
      // do not proceed without a proper thread start event
      else if (threads.get(event.thread()) == null && !(event instanceof IThreadEndEvent)
          && !(event instanceof ISystemExitEvent) && !(event instanceof IThreadStartEvent)
          && !(event instanceof INewThreadEvent))
      {
        final IJiveEvent threadStart = eventFactory.createThreadStartEvent(event.thread());
        threads.put(threadStart.thread(), (IThreadStartEvent) threadStart);
        threadStacks.put(threadStart.thread(), new Stack<IMethodCallEvent>());
        updateModel(threadStart);
        // reorder: <..., event, ...> --> <..., threadStart, event, ...>
        iterator.remove();
        iterator.add(threadStart);
        iterator.add(event);
      }
      // update the model for this single event
      updateModel(event);
    }
    // if an exit event was seen, terminate all outstanding threads
    if (exit != null)
    {
      // terminate dangling threads
      for (final IThreadValue thread : threads.keySet())
      {
        if (threads.get(thread).terminator() == null)
        {
          final IJiveEvent event = exit instanceof IRealTimeEvent ? eventFactory
              .createRTThreadEndEvent(((IRealTimeEvent) exit).timestamp(), thread) : eventFactory
              .createThreadEndEvent(thread);
          events.add(event);
          updateModel(event);
        }
      }
      // exit is the last event
      events.add(exit);
      // terminate execution
      updateModel(exit);
    }
  }

  private void updateParents(final IJiveEvent event, final IInitiatorEvent parent)
  {
    // set the event's parent
    ((EventFactory.JiveEvent) event).setParentInitiator(parent);
    // in-model initiators should be nested within their caller methods and callee environments
    if (event instanceof IInitiatorEvent && ((IInitiatorEvent) event).inModel())
    {
      // method --> nested method calls
      ((EventFactory.InitiatorEvent) parent).addInitiator((IInitiatorEvent) event);
      /**
       * if the parent is not in model, associate the event with the top in-model call on the
       * event's call stack; note that model clients must understand that this in not a true
       * parent-child relationship and must therefore check the caller/callee relationship between
       * the events to determine whether there is an out-of-model call path in between.
       * */
      if (!parent.inModel())
      {
        EventFactory.InitiatorEvent inModelParent = (EventFactory.InitiatorEvent) parent;
        while (inModelParent != null && !inModelParent.inModel())
        {
          inModelParent = (EventFactory.InitiatorEvent) inModelParent.parent();
        }
        if (inModelParent != null)
        {
          inModelParent.addInitiator((IInitiatorEvent) event);
        }
      }
      // context --> called methods
      final IContextContour context = ((IInitiatorEvent) event).executionContext();
      if (context != null)
      {
        ((ContextContour) context).addInitiator((IMethodCallEvent) event);
      }
    }
  }

  private void updateStacks(final IJiveEvent event)
  {
    // push call onto the stack
    if (event instanceof IMethodCallEvent)
    {
      final Stack<IMethodCallEvent> stack = getStack(event.thread());
      stack.push((IMethodCallEvent) event);
    }
    // set terminator, pop the call stack
    else if (event instanceof ITerminatorEvent)
    {
      // link this terminator with its initiator
      if (event.parent() != null)
      {
        ((EventFactory.InitiatorEvent) event.parent()).setTerminator((ITerminatorEvent) event);
      }
      // pop call from the stack
      if (event instanceof IMethodTerminatorEvent)
      {
        if (((IMethodTerminatorEvent) event).framePopped())
        {
          // System.err.println("terminator/frame popped");
          pop(event.thread());
        }
      }
    }
  }

  @Override
  public Store store()
  {
    return store;
  }

  @Override
  public ITransactionLog transactionLog()
  {
    return transactionLog;
  }

  /**
   * Exposes a read-only contour view of the transaction log.
   */
  private class ContourView implements IContourView
  {
    @Override
    public boolean contains(final IContour contour)
    {
      // read-locks
      return transactionLog.contains(contour);
    }

    @Override
    public List<IContour> lookupRoots()
    {
      // read-locks
      return transactionLog.getRoots();
    }

    @Override
    public void visit(final IVisitor<IContour> visitor)
    {
      // read-locks
      transactionLog.visitDepthFirst(visitor);
    }
  }

  private final class SliceView implements ISliceView
  {
    private ProgramSlice slice;
    private IJiveEvent initial;

    @Override
    public IProgramSlice activeSlice()
    {
      return slice;
    }

    @Override
    public void clearSlice()
    {
      if (slice == null && initial == null)
      {
        return;
      }
      transactionLog.writeLock().lock();
      try
      {
        boolean doCommit = false;
        IJiveEvent finalTarget = transactionLog.getEvent();
        if (finalTarget == null || finalTarget == initial)
        {
          doCommit = true;
          transactionLog.rollback();
          finalTarget = transactionLog.getEvent();
        }
        /**
         * The sliced view may have been replayed arbitrarily and its states would not match the
         * states of a full execution. Therefore, we need to reset the state to a safe point.
         */
        transactionLog.setInitialState();
        // remove the sliced view
        store.sliceFilterRemove();
        // synchronize the state back to its original point.
        transactionLog.setLastUncommittedEvent(finalTarget);
        if (doCommit)
        {
          transactionLog.commit();
        }
        slice = null;
        initial = null;
      }
      finally
      {
        // downgrade lock so that notification code doesn't need to perform additional locking
        transactionLog.readLock().lock();
        transactionLog.writeLock().unlock();
        try
        {
          traceView.notifyVirtualization(false);
        }
        finally
        {
          transactionLog.readLock().unlock();
        }
      }
    }

    @Override
    public IProgramSlice computeSlice(final IContourMember member)
    {
      if (slice != null || initial != null || member == null)
      {
        return null;
      }
      // start from the current temporal context
      initial = temporalState.event();
      // search for the nearest assign event involving the member
      while (initial != null
          && (!(initial instanceof IAssignEvent) || ((IAssignEvent) initial).member() != member))
      {
        initial = initial.prior();
      }
      // a valid slicing criterion
      if (initial instanceof IAssignEvent)
      {
        slice = new ProgramSlice((IAssignEvent) initial);
        computeSlice();
        return slice;
      }
      return null;
    }

    @Override
    public IProgramSlice computeSlice(final long eventId)
    {
      if (slice != null || initial != null)
      {
        return null;
      }
      initial = lookupEvent(eventId);
      if (initial instanceof IAssignEvent)
      {
        slice = new ProgramSlice((IAssignEvent) initial);
        computeSlice();
        return slice;
      }
      return null;
    }

    private void computeSlice()
    {
      if (slice == null || initial == null)
      {
        return;
      }
      transactionLog.writeLock().lock();
      try
      {
        if (initial.next() != null)
        {
          transactionLog.setLastUncommittedEvent(initial.next());
        }
        else
        {
          transactionLog.setFinalState();
        }
        // transactionLog.setInitialState();
        try
        {
          /**
           * This is a guaranteed way to get a consistent slice. The main reason is that, a field or
           * variable D may become relevant at a time T in execution but at slicing criterion time
           * the value of D is different, that is, there have been one or more intermediate
           * (non-relevant) assignments to D, which would be visible at slice time (i.e., the
           * respective transaction has not been rolled back).
           */
          slice.computeSlice();
        }
        catch (final Throwable t)
        {
          slice = null;
          initial = null;
          System.err
              .println("\nThis is embarrassing... The slicer found an unexpected condition." +
              "\nThis may be due to an error in the slicer or because the source " +
              "\ncode has some unsupported feature, e.g., multiple assignments on " + 
              "\nthe same statement. Please check the highlighted source line on " + 
              "\nthe source window and the temporal context on the sequence diagram " +
              "\nas these may provide some indication as to why this error occurred.\n");
          t.printStackTrace();
        }
        if (slice != null)
        {
          // apply the slice as a filter to the model
          store.sliceFilterAdd(slice.eventSet());
          // now commit the slicing criterion
          if (initial.next() != null)
          {
            transactionLog.setLastUncommittedEvent(initial.next());
          }
          else
          {
            transactionLog.setFinalState();
          }
        }
      }
      finally
      {
        // downgrade lock so that notification code doesn't need to perform additional locking
        transactionLog.readLock().lock();
        transactionLog.writeLock().unlock();
        try
        {
          traceView.notifyVirtualization(true);
        }
        finally
        {
          transactionLog.readLock().unlock();
        }
      }
    }
  }

  /**
   * Exposes the temporal state of the transaction log.
   */
  private class TemporalState implements ITemporalState
  {
    @Override
    public boolean canCommit(final IJiveEvent event)
    {
      // read-locks
      return transactionLog.canCommit(event);
    }

    @Override
    public boolean canReplayCommit()
    {
      // read-locks
      return transactionLog.canReplayCommit();
    }

    @Override
    public boolean canRollback()
    {
      // read-locks
      return transactionLog.canRollback();
    }

    @Override
    public boolean canRollback(final IJiveEvent event)
    {
      // read-locks
      return transactionLog.canRollback(event);
    }

    @Override
    public void commit()
    {
      // read-locks
      transactionLog.commit();
    }

    @Override
    public void consolidateTo(final IJiveEvent event)
    {
      transactionLog.setLastUncommittedEvent(event);
    }

    @Override
    public IJiveEvent event()
    {
      // read-locks
      return transactionLog.getEvent();
    }

    @Override
    public boolean readyToRecord()
    {
      // read-locks
      return transactionLog.isReady();
    }

    @Override
    public void rollback()
    {
      // read-locks
      transactionLog.rollback();
    }

    @Override
    public void setFinalState()
    {
      transactionLog.setFinalState();
    }

    @Override
    public void setInitialState()
    {
      transactionLog.setInitialState();
    }
  }

  /**
   * Exposes a read-only view of the event log.
   */
  private class TraceView implements ITraceView
  {
    private final Set<ITraceViewListener> traceClients;

    private TraceView()
    {
      this.traceClients = new CopyOnWriteArraySet<ITraceViewListener>();
    }

    @Override
    public List<? extends IJiveEvent> events()
    {
      readLock();
      try
      {
        return store.isVirtual() ? sliceView.activeSlice().events() : store.events();
      }
      finally
      {
        readUnlock();
      }
    }

    @Override
    public void register(final ITraceViewListener listener)
    {
      if (listener == null)
      {
        throw new IllegalArgumentException(
            "Cannot add a null trace view listener to the execution model.");
      }
      traceClients.add(listener);
    }

    @Override
    public void unregister(final ITraceViewListener listener)
    {
      if (listener == null)
      {
        throw new IllegalArgumentException(
            "Cannot remove null trace view listener from the execution model.");
      }
      traceClients.remove(listener);
    }

    private void notifyListeners(final List<IJiveEvent> events)
    {
      // if changes were generated, dispatch them to interested subscribers
      for (final ITraceViewListener client : traceClients)
      {
        client.eventsInserted(events);
      }
    }

    private void notifyVirtualization(final boolean isVirtual)
    {
      // if changes were generated, dispatch them to interested subscribers
      for (final ITraceViewListener client : traceClients)
      {
        client.traceVirtualized(isVirtual);
      }
    }
  }
}