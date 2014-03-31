package edu.buffalo.cse.jive.model;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IEventModel.IEventListener;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IEventModel.ISystemStartEvent;
import edu.buffalo.cse.jive.model.IEventModel.IThreadStartEvent;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;
import edu.buffalo.cse.jive.model.IModel.IValue;
import edu.buffalo.cse.jive.model.factory.IContourFactory;
import edu.buffalo.cse.jive.model.factory.IEventFactory;
import edu.buffalo.cse.jive.model.factory.IQueryFactory;
import edu.buffalo.cse.jive.model.factory.IStaticAnalysisFactory;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory;
import edu.buffalo.cse.jive.model.factory.IValueFactory;

public interface IExecutionModel extends IEventListener
{
  /**
   * Contour factory for creating new contour elements in this execution model.
   */
  public IContourFactory contourFactory();

  /**
   * State view of the execution model at the current temporal context.
   */
  public IContourView contourView();

  public void done();

  /**
   * Event factory for creating new events in this execution model.
   */
  public IEventFactory eventFactory();

  /**
   * Indicates whether this execution model has a snapshot.
   */
  public boolean hasSnapshot();

  /**
   * Event in this execution model associated with the identifier.
   */
  public IJiveEvent lookupEvent(long eventId);

  /**
   * The root start event.
   */
  public ISystemStartEvent lookupRoot();

  /**
   * Thread start event associated with this thread.
   */
  public IThreadStartEvent lookupThread(IThreadValue threadId);

  /**
   * All thread start events.
   */
  public List<IThreadStartEvent> lookupThreads();

  /**
   * Retrieves the top of the stack frame associated with this thread.
   */
  public IMethodCallEvent lookupTop(IThreadValue threadId);

  /**
   * Query factory for creating queries against this execution model.
   */
  public IQueryFactory queryFactory();

  /**
   * Acquires a reentrant model lock for performing atomic operations on this model.
   */
  public void readLock();

  /**
   * Releases a reentrant model lock for performing atomic operations on this model.
   */
  public void readUnlock();

  /**
   * Resets the model correctly so that it can be restarted manually.
   */
  public void reset();

  /**
   * Slicer factory for creating dynamic slices this execution model.
   */
  public ISliceView sliceView();

  /**
   * Static analysis factory for creating nodes in Jive's variant of the program dependence graph.
   */
  public IStaticAnalysisFactory staticAnalysisFactory();

  /**
   * Static model factory for creating static model elements in this execution model.
   */
  public IStaticModelFactory staticModelFactory();

  /**
   * This model's store.
   */
  public IStore store();

  /**
   * Temporal state of the execution.
   */
  public ITemporalState temporalState();

  /**
   * Manually terminates tracing in a consistent manner.
   */
  public void terminate();

  /**
   * View of the execution model as an event set.
   */
  public ITraceView traceView();

  public ITransactionLog transactionLog();

  /**
   * Model factory for creating model elements in this execution model.
   */
  public IValueFactory valueFactory();

  public interface AtomicDelete extends IStateChange
  {
  }

  public interface AtomicInsert extends IStateChange
  {
  }

  public interface AtomicUpdate extends IStateChange
  {
    public IContourMember member();

    public IValue newValue();

    public IValue oldValue();
  }

  /**
   * Contour view of the execution model providing a view of a single state snapshot. This view of
   * the execution model is amenable for rendering as an object diagram.
   */
  public interface IContourView
  {
    /**
     * Determines if the contour exists in the execution model.
     */
    public boolean contains(IContour contour);

    /**
     * All root contour elements in the execution model.
     */
    public List<IContour> lookupRoots();

    /**
     * Visits the contours in the execution model, starting with the root contours and, for each
     * contour found, visits all its children recursively. The traversal of the contour structure is
     * done in a depth-first fashion.
     */
    public void visit(IVisitor<IContour> visitor);
  }

  /**
   * All information produced by a program slice and used for rendering client views.
   */
  public interface IProgramSlice
  {
    /**
     * Static and object contexts relevant to the slice. Used to render the reduced object diagram.
     */
    public Set<IContextContour> contexts();

    /**
     * Events in this slice.
     */
    public List<IJiveEvent> events();

    /**
     * Static, object, and method contour members relevant to the slice. Used to render the reduced
     * object diagram.
     */
    public Set<IContourMember> members();

    /**
     * Method contours relevant to the slice. Used to render the reduced object diagram.
     */
    public Set<IMethodContour> methods();

    public IExecutionModel model();
  }

  public interface ISliceView
  {
    public IProgramSlice activeSlice();

    public void clearSlice();

    /**
     * Computes an inter-procedural object-oriented dynamic slice on the program, starting from the
     * current event (obtained from the temporal view) and the given contour member representing a
     * field or variable. Code executed in all threads is traversed in the slice computation.
     */
    public IProgramSlice computeSlice(IContourMember member);

    /**
     * Computes an inter-procedural object-oriented dynamic slice on the program, starting from the
     * given event. Code executed in all threads is traversed in the slice computation.
     */
    public IProgramSlice computeSlice(long eventId);
  }

  public interface IStateChange
  {
    public IContour contour();
  }

  public interface ITemporalState
  {
    /**
     * Returns true if the model can commit the event with the given identifier.
     */
    public boolean canCommit(IJiveEvent event);

    /**
     * Returns true if the model can replay the effects of a transaction.
     */
    public boolean canReplayCommit();

    /**
     * Returns true if the model can rollback the effects of a transaction.
     */
    public boolean canRollback();

    /**
     * Returns true if the model can replay the effects of the event with the given identifier.
     */
    public boolean canRollback(IJiveEvent event);

    /**
     * Play a step-forward transaction. This only works if there is a transaction available to be
     * committed.
     * <p>
     * This is a synchronous method: when it returns, the stepping backward has completed.
     * 
     * @throws NoSuchElementException
     *           if there is no forward step to be executed.
     */
    public void commit();

    /**
     * The transaction log places the replay cursor exactly on this event. This amounts to setting
     * this event as the last uncommitted event in the log.
     */
    public void consolidateTo(IJiveEvent event);

    /**
     * Returns the event identifier that represents the current temporal state. When in replay mode,
     * this is the identifier of the last event whose effects have *NOT* been applied to the contour
     * model, i.e., the last uncommitted event. In normal mode, this is the identifier of the last
     * committed event.
     */
    public IJiveEvent event();

    /**
     * Indicates if this object is in a state that is ready to record a transaction. The model is
     * ready to record if it is not playing back through history, meaning that there are no future
     * transactions recorded and all previous transactions are committed.
     * 
     * @return true if this is ready to record
     */
    public boolean readyToRecord();

    /**
     * Play a step-backward transaction. This is only possible if there is a transaction to roll
     * back.
     * <p>
     * This is a synchronous method: when it returns, the stepping backward has completed.
     * 
     * @throws NoSuchElementException
     *           if the step backwards cannot be completed because that information is either not
     *           available (not cached).
     * @throws IndexOutOfBoundsException
     *           if the request was to move back from the initial state (state zero).
     */
    public void rollback();

    /**
     * Advances the state of the transaction log to its final state. Equivalent to committing all
     * transactions from the current state.
     */
    public void setFinalState();

    /**
     * Resets the state of the transaction log to its initial state. Equivalent to rolling back all
     * transactions from the current state.
     */
    public void setInitialState();
  }

  /**
   * View of the underlying execution as an event set.
   */
  public interface ITraceView
  {
    /**
     * Read-only view of the underlying event trace as an event set.
     */
    public List<? extends IJiveEvent> events();

    /**
     * Registers a listener interested in trace view notifications.
     */
    public void register(ITraceViewListener listener);

    /**
     * Unregisters a trace view listener.
     */
    public void unregister(ITraceViewListener listener);
  }

  /**
   * Listener for trace view changes.
   */
  public interface ITraceViewListener
  {
    /**
     * Called when a new set of events is added to the model
     */
    public void eventsInserted(List<IJiveEvent> events);

    /**
     * Called when a the trace is set from normal to virtualized or vice-versa.
     */
    public void traceVirtualized(boolean isVirtual);
  }
}