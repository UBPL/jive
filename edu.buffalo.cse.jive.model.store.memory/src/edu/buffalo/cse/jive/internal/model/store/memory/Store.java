package edu.buffalo.cse.jive.internal.model.store.memory;

import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import edu.buffalo.cse.jive.lib.IndexedOneToIndexedMany;
import edu.buffalo.cse.jive.lib.OneToIndexedMany;
import edu.buffalo.cse.jive.lib.OneToMany;
import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.model.contours.ContourFactory.IMutableContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IEventModel.IAssignEvent;
import edu.buffalo.cse.jive.model.IEventModel.IDestroyObjectEvent;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.ITransaction;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IExecutionModel.AtomicDelete;
import edu.buffalo.cse.jive.model.IExecutionModel.AtomicInsert;
import edu.buffalo.cse.jive.model.IExecutionModel.AtomicUpdate;
import edu.buffalo.cse.jive.model.IExecutionModel.IProgramSlice;
import edu.buffalo.cse.jive.model.IExecutionModel.IStateChange;
import edu.buffalo.cse.jive.model.IModel;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedNode;
import edu.buffalo.cse.jive.model.IStaticModel.IDataNode;
import edu.buffalo.cse.jive.model.IStaticModel.IFileNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.INode;
import edu.buffalo.cse.jive.model.IStaticModel.INodeRef;
import edu.buffalo.cse.jive.model.IStaticModel.IRootNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNodeRef;
import edu.buffalo.cse.jive.model.IStore;
import edu.buffalo.cse.jive.model.events.EventFactory.AssignEvent;
import edu.buffalo.cse.jive.model.events.EventFactory.JiveEvent;

class Store implements IModel, IStore
{
  private static final long COUNTER_START = 0;
  private static final String PREFIX_CONTOUR_INSTANCE = "/contour/i/";
  private static final String PREFIX_CONTOUR_STATIC = "/contour/s/";
  private static final String PREFIX_COUNTER = "/counter/";
  private static final String PREFIX_OID = "/oid/";
  private static final int SZ_MEDIUM = 1024;
  private static final int SZ_NODEREFS = 512;
  private static final int SZ_SMALL = 32;
  private static final int SZ_VALUES = 2048;
  /**
   * In {@code contourChildToParent}, null keys are not allowed since we do not allow null contours
   * in the model. Each child contour maps to a non-null parent; multiple children may map to the
   * same parent.
   * <p>
   * In {@code contourParentToChildren}, null keys map to root contours, which relieves the class
   * from creating an additional structure to maintain root contours separately. Each parent maps to
   * a non-null list of children. The list is initially non-empty, but may become empty as contour
   * elements are removed from the model.
   */
  private final Map<Long, IDestroyObjectEvent> oidToGC;
  private final Map<IContour, IContour> contourChildToParent;
  private final Map<IContour, List<IContour>> contourParentToChildren;
  private final ConcurrentMap<Object, IContour> contourStore;
  private final ConcurrentMap<Object, AtomicLong> counters;
  private final List<JiveEvent> eventStore;
  private final IndexedOneToIndexedMany<String, IFileValue, Integer, ILineValue> fileValueStore;
  // last event that was associated with a transaction in the model-- not modified by the view
  private IJiveEvent lastTransactionEvent;
  private final OneToMany<IMethodNode, ITypeNodeRef> methodToExceptions;
  private final ExecutionModel model;
  private final ConcurrentMap<String, INodeRef> noderefStore;
  private final IndexedOneToIndexedMany<String, INode, String, INode> nodeToChildNodes;
  private final OneToIndexedMany<INode, Integer, IDataNode> nodeToMemberData;
  private final OneToMany<INode, ITypeNode> nodeToTypes;
  private final Map<Long, IResolvedNode> resolvedNodes;
  private IRootNode root;
  private final Map<String, IFileNode> rootFiles;
  // currently active view on the model, if any
  private BitSet slicedView;
  private final ConcurrentMap<Long, IThreadValue> threadStore;
  // number of transactions in the log
  private int transactionCount;
  // reference to the event containing the last rolled back transaction (null if none)
  private IJiveEvent transactionReplayCursor = null;
  private final OneToIndexedMany<ITypeNode, Integer, IMethodNode> typeToMemberMethods;
  private final OneToMany<ITypeNode, ITypeNodeRef> typeToSuperInterfaces;
  private final ConcurrentMap<Object, IValue> valueIndex;
  private final ConcurrentMap<Long, IValue> valueStore;

  Store(final ExecutionModel model)
  {
    this.model = model;
    this.contourChildToParent = TypeTools.newHashMap(Store.SZ_MEDIUM);
    this.contourParentToChildren = TypeTools.newHashMap(Store.SZ_MEDIUM);
    this.contourStore = TypeTools.newConcurrentHashMap(Store.SZ_MEDIUM);
    this.counters = TypeTools.newConcurrentHashMap(Store.SZ_SMALL);
    this.eventStore = TypeTools.newArrayList(); // new PagedArrayList<JiveEvent>();
    this.fileValueStore = new IndexedOneToIndexedMany<String, IFileValue, Integer, ILineValue>();
    this.lastTransactionEvent = null;
    this.methodToExceptions = new OneToMany<IMethodNode, ITypeNodeRef>();
    this.noderefStore = TypeTools.newConcurrentHashMap(Store.SZ_NODEREFS);
    this.nodeToMemberData = new OneToIndexedMany<INode, Integer, IDataNode>(true);
    this.nodeToChildNodes = new IndexedOneToIndexedMany<String, INode, String, INode>();
    this.nodeToTypes = new OneToMany<INode, ITypeNode>();
    this.oidToGC = TypeTools.newHashMap(Store.SZ_MEDIUM);
    this.resolvedNodes = TypeTools.newHashMap();
    this.root = null;
    this.rootFiles = TypeTools.newLinkedHashMap();
    this.slicedView = null;
    this.threadStore = TypeTools.newConcurrentHashMap(Store.SZ_SMALL);
    this.transactionCount = 0;
    this.transactionReplayCursor = null;
    this.typeToMemberMethods = new OneToIndexedMany<ITypeNode, Integer, IMethodNode>(true);
    this.typeToSuperInterfaces = new OneToMany<ITypeNode, ITypeNodeRef>();
    this.valueIndex = TypeTools.newConcurrentHashMap(Store.SZ_VALUES);
    this.valueStore = TypeTools.newConcurrentHashMap(Store.SZ_VALUES);
  }

  @Override
  public Collection<? extends IFileValue> getFiles()
  {
    return fileValueStore.lookupKeys();
  }

  @Override
  public IJiveEvent lookupDestroyEvent(final long oid)
  {
    return oidToGC.get(oid);
  }

  @Override
  public IJiveEvent lookupRawEvent(final long eventId)
  {
    return eventStore.get((int) eventId - 1);
  }

  /**
   * Determines if the object with the given identifier had been garbage collected at the execution
   * state corresponding to the given eventId.
   */
  public boolean isGarbageCollected(final long oid, final long eventId)
  {
    final IJiveEvent destroy = oidToGC.get(oid);
    return destroy != null && destroy.eventId() > 0 && eventId > destroy.eventId();
  }

  @Override
  public Collection<? extends ILineValue> getLines()
  {
    return fileValueStore.allValues();
  }

  @Override
  public Collection<? extends INodeRef> lookupNodeRefs()
  {
    return noderefStore.values();
  }

  @Override
  public Collection<? extends IResolvedNode> lookupResolvedNodes()
  {
    return resolvedNodes.values();
  }

  @Override
  public IRootNode lookupRoot()
  {
    return root;
  }

  @Override
  public IExecutionModel model()
  {
    return model;
  }

  @Override
  public Collection<? extends IThreadValue> getThreads()
  {
    return threadStore.values();
  }

  @Override
  public Collection<IValue> getValues()
  {
    return valueStore.values();
  }

  /**
   * Thread-safe verification if an element has children in the model. This method is an accessor.
   * 
   * @throws IllegalArgumentException
   *           when element is null
   * 
   * @requires non-null parent
   */
  private boolean contourHasChildren(final IContour parent)
  {
    // parent must be non-null
    if (parent == null)
    {
      throw new IllegalArgumentException("Cannot retrieve child list of a null element.");
    }
    final List<IContour> model = contourParentToChildren.get(parent);
    // short circuit
    if (slicedView == null || model == null || model.size() == 0)
    {
      return false;
    }
    // if some child is contained in the slice view
    for (final IContour contour : model)
    {
      if (slice().contexts().contains(contour) || slice().methods().contains(contour))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Thread-safe verification if an element has a parent in the model. This method is an accessor.
   * 
   * @throws IllegalArgumentException
   *           when element is null
   * 
   * @requires non-null element via {@code getChildren(Contour)}
   */
  private boolean contourHasParent(final IContour element)
  {
    return contourIsRoot(element) || model.store().contourParent(element) != null;
  }

  /**
   * Verifies if the element is a root node in the model.
   * 
   * @throws IllegalArgumentException
   *           when element is null
   * 
   * @requires non-null element
   */
  private boolean contourIsRoot(final IContour element)
  {
    if (element == null)
    {
      throw new IllegalArgumentException("Cannot check if a null element is a root node.");
    }
    final List<IContour> roots = contourRoots();
    return roots != null ? roots.contains(element) : false;
  }

  private AtomicLong getCounter(final Object key)
  {
    if (counters.get(key) == null)
    {
      counters.putIfAbsent(key, new AtomicLong(Store.COUNTER_START));
    }
    return counters.get(key);
  }

  private String instanceContourKey(final String typeName, final long oid)
  {
    return Store.PREFIX_CONTOUR_INSTANCE + typeName + Store.PREFIX_OID + oid;
  }

  /**
   * Returns the event identifier of the last event in the view, or -1 if not in virtual mode.
   */
  private int lastVirtualId()
  {
    return slicedView == null ? -1 : slicedView.length() - 1;
  }

  private IProgramSlice slice()
  {
    return model.sliceView().activeSlice();
  }

  private String staticContourKey(final String key)
  {
    return Store.PREFIX_CONTOUR_STATIC + key;
  }

  private void transactionCommit(final ITransaction transaction)
  {
    // apply all changes in this transaction to the model, in the order in which they occurred
    for (int i = 0; i < transaction.changes().size(); i++)
    {
      final IStateChange change = transaction.changes().get(i);
      if (change instanceof AtomicInsert)
      {
        final AtomicInsert added = (AtomicInsert) change;
        transactionInsertElement(added.contour());
      }
      else if (change instanceof AtomicDelete)
      {
        final AtomicDelete removed = (AtomicDelete) change;
        transactionDeleteElement(removed.contour());
      }
      else if (change instanceof AtomicUpdate)
      {
        final AtomicUpdate updated = (AtomicUpdate) change;
        transactionUpdateElement(updated.contour(), updated.member(), updated.newValue());
      }
    }
    // update the cursor only if in "replay" mode
    if (transactionReplayCursor != null)
    {
      // if we committed the last transaction, we are back in normal mode
      if (transactionReplayCursor == lastTransactionEvent())
      {
        transactionReplayCursor = null;
      }
      else
      {
        transactionReplayCursor = lookupNextEvent(transactionReplayCursor);
      }
    }
  }

  /**
   * Thread-safe deletion of a {@code Contour} element. This method is a mutator.
   * 
   * @return the parent containing the removed element
   * 
   * @requires <ol>
   *           <li>non-null element</li>
   *           <li>element exists in the model</li>
   *           <li>element has a parent in the model</li>
   *           <li>element has an empty child list</li>
   *           </ol>
   * 
   * @ensures <ol>
   *          <li>the key of element is no longer in elementMap</li>
   *          <li>parentToChildren no longer maps element to a child list</li>
   *          <li>childToParent no longer maps element to a parent</li>
   *          </ol>
   */
  private IContour transactionDeleteElement(final IContour element)
  {
    // element must be non-null
    if (element == null)
    {
      throw new IllegalArgumentException("Cannot delete: element is null.");
    }
    // element must have a parent
    if (!contourHasParent(element))
    {
      throw new IllegalArgumentException(
          "Cannot delete: element appears to be orphaned in the model. (" + element.toString()
              + ")");
    }
    // element must not have children
    if (contourHasChildren(element))
    {
      throw new IllegalArgumentException("Cannot delete: element has children in the model. ("
          + element.toString() + ")");
    }
    // remove the element from the model
    return model.store().contourDelete(element);
  }

  /**
   * Thread-safe insertion of a {@code Contour} under a given parent. The first inserted element
   * must be a root element, that is, it must have a null parent. In general, an element is added
   * only if the parent is a root element (null) or it already appears in the relationship. This
   * method is a mutator.
   * 
   * @requires <ol>
   *           <li>non-null element</li>
   *           <li>element does not yet exist in the model</li>
   *           <li>null parent or non-null parent that exists in the model</li>
   *           </ol>
   * 
   * @ensures <ol>
   *          <li>parent references a non-empty list in parentToChildren containing element</li>
   *          <li>element references parent in childToParent</li>
   *          <li>the key of element references element in elementMap</li>
   *          </ol>
   */
  private void transactionInsertElement(final IContour element)
  {
    if (element == null)
    {
      throw new IllegalArgumentException("Cannot insert: element is null.");
    }
    // parent has been pushed to the model
    final IContour parent = element.parent();
    if (parent != null && !contourHasParent(parent))
    {
      throw new IllegalArgumentException(
          "Cannot insert: a non-null parent must already exist in the model. ("
              + element.toString() + ")");
    }
    model.store().contourInsert(parent, element);
  }

  /**
   * Thread-safe (almost) update of the value of a variable instance. Assumption: all variable write
   * operations go through this code, and all code that depends on variable reads relies on the
   * events fired by the model to update their local copies (if any).
   * <p>
   * Obviously (and unfortunately), correct usage according to the assumptions above cannot be
   * guaranteed in general since contours are exposed by this model and thus, a reference to any
   * model part may be retrieved and manipulated independently from this code.
   * <p>
   * A definitive solution requires pushing the {@code ContourForest} to a lower layer of the
   * framework, perhaps closer to the {@code ContourFactoryImpl}, and sharing the reentrant lock
   * with the variable instances.
   * <p>
   * This method is a mutator.
   */
  private IValue transactionUpdateElement(final IContour element, final IContourMember member,
      final IValue newValue)
  {
    // element must be non-null
    if (element == null)
    {
      throw new IllegalArgumentException("Cannot update: element is null.");
    }
    // member must be non-null
    if (member == null)
    {
      throw new IllegalArgumentException("Cannot update: variable is null.");
    }
    // newValue must be non-null
    if (newValue == null)
    {
      throw new IllegalArgumentException("Cannot update: value is null.");
    }
    if (!contourInModel(element))
    {
      throw new IllegalArgumentException("Cannot update: element is not in the model. ("
          + element.toString() + ")");
    }
    // the variable must be a mutable instance
    if (!(member instanceof IMutableContourMember))
    {
      throw new IllegalArgumentException("Cannot update: variable instance is immutable.");
    }
    return ((IMutableContourMember) member).setValue(newValue);
  }

  List<IContour> contourChildren(final IContour parent)
  {
    final List<IContour> model = contourParentToChildren.get(parent);
    if (slicedView == null || model == null)
    {
      return model;
    }
    // create the set to hold the contours
    final List<IContour> filtered = TypeTools.newArrayList();
    for (final IContour contour : model)
    {
      if (slice().contexts().contains(contour) || slice().methods().contains(contour))
      {
        filtered.add(contour);
      }
    }
    return filtered;
  }

  IContour contourDelete(final IContour contour)
  {
    // remove the element from its parent's child list
    if (!contourParentToChildren.get(contourParent(contour)).remove(contour))
    {
      throw new IllegalArgumentException(
          "Cannot delete: element not found among its parent's children.");
    }
    // element has no child left, remove it from the parent list
    contourParentToChildren.remove(contour);
    // element is no longer a child of its parent
    return contourChildToParent.remove(contour);
  }

  /**
   * Returns true if this element is currently either a child or a parent in the contour model. This
   * method is an accessor.This method considers whether a view is currently in place.
   */
  boolean contourInModel(final IContour contour)
  {
    final boolean inStore = contour != null
        && (contourChildToParent.containsKey(contour) || contourParentToChildren
            .containsKey(contour));
    if (slicedView == null)
    {
      return inStore;
    }
    return inStore && (slice().contexts().contains(contour) || slice().methods().contains(contour));
  }

  void contourInsert(final IContour parent, final IContour element)
  {
    // children is null if this is the first child of this parent
    List<IContour> children = contourParentToChildren.get(parent);
    if (children == null)
    {
      children = TypeTools.newArrayList();
      contourParentToChildren.put(parent, children);
    }
    children.add(element);
    contourChildToParent.put(element, parent);
  }

  IContour contourParent(final IContour child)
  {
    // must be always in the model
    return contourChildToParent.get(child);
  }

  /**
   * Returns the root contours in the model. This method considers whether a view is currently in
   * place.
   */
  List<IContour> contourRoots()
  {
    return contourChildren(null);
  }

  /**
   * Return the current counter value for the key.
   */
  long currentCount(final Object key)
  {
    return getCounter(key).get();
  }

  /**
   * Returns the events in the model. This method considers whether a view is currently in place.
   */
  List<? extends IJiveEvent> events()
  {
    if (slicedView == null)
    {
      return eventStore;
    }
    // create the list to hold the events
    final List<IJiveEvent> result = TypeTools.newArrayList(slicedView.cardinality());
    // add only those in the view
    for (int eventId = slicedView.nextSetBit(0); eventId >= 0; eventId = slicedView
        .nextSetBit(eventId + 1))
    {
      result.add(eventStore.get(eventId - 1));
    }
    // return the newly created view
    return result;
  }

  @Override
  public void indexInstanceContour(final IContour contour, final long oid)
  {
    // index by string identifier
    // contourStore.putIfAbsent(instanceContourKey(contour.signature()), contour);
    contourStore.putIfAbsent(contour.id(), contour);
    // index by name and oid
    contourStore.putIfAbsent(instanceContourKey(contour.schema().name(), oid), contour);
    // debug trace
    // System.err.println(contour.id());
  }

  @Override
  public void indexStaticContour(final IContour contour)
  {
    // index by name
    contourStore.putIfAbsent(staticContourKey(contour.schema().name()), contour);
    // debug trace
    // System.err.println(contour.id());
  }

  @Override
  public void indexValue(final Object key, final IValue value)
  {
    valueIndex.putIfAbsent(key, value);
  }

  @Override
  public boolean isVirtual()
  {
    return slicedView != null;
  }

  /**
   * Returns the last transaction in the model. This method observes whether a view is currently in
   * place.
   */
  ITransaction lastTransaction()
  {
    final IJiveEvent event = lastTransactionEvent();
    return event == null ? null : event.transaction();
  }

  /**
   * Returns the last event having a transaction in the model. This method observes whether a view
   * is currently in place.
   */
  IJiveEvent lastTransactionEvent()
  {
    if (slicedView == null)
    {
      return lastTransactionEvent;
    }
    return eventStore.get(lastVirtualId() - 1);
  }

  IContour lookupContour(final long contourId)
  {
    return contourStore.get(contourId);
  }

  @Override
  public Map<Integer, IDataNode> lookupDataMembers(final INode node)
  {
    return nodeToMemberData.lookupChildren(node);
  }

  /**
   * Returns the event matching the identifier in the model. This method considers whether a view is
   * currently in place.
   */
  IJiveEvent lookupEvent(final long eventId)
  {
    if (slicedView == null)
    {
      return eventId > eventStore.size() || eventId <= 0 ? null : eventStore.get((int) eventId - 1);
    }
    // number of bits set seen so far
    int seenCount = 0;
    // set the virtual identifier for the events in the view
    for (int index = slicedView.nextSetBit(0); index >= 0; index = slicedView.nextSetBit(index + 1))
    {
      seenCount++;
      if (seenCount == eventId)
      {
        // the eventId was seen at this index
        return eventStore.get(index - 1);
      }
    }
    // this should NEVER happen
    return null;
  }

  @Override
  public IFileNode lookupFile(final String fileName)
  {
    return rootFiles.get(fileName);
  }

  @Override
  public Set<IFileNode> lookupFiles()
  {
    final Set<IFileNode> result = TypeTools.newHashSet();
    result.addAll(rootFiles.values());
    return result;
  }

  @Override
  public IFileValue lookupFileValue(final String fileName)
  {
    return fileValueStore.lookupKey(fileName.replace('\\', '/'));
  }

  @Override
  public IContour lookupInstanceContour(final String typeName, final long oid)
  {
    if (typeName == null)
    {
      return null;
    }
    return contourStore.get(instanceContourKey(typeName, oid));
  }

  /**
   * Returns the last event that is visible in the model. This method observes whether a view is
   * currently in place.
   */
  IJiveEvent lookupLastEvent()
  {
    if (slicedView == null)
    {
      return eventStore.get((int) currentCount(IJiveEvent.class) - 1 - 1);
    }
    return eventStore.get(lastVirtualId() - 1);
  }

  @Override
  public ILineValue lookupLineValue(final String fileName, final int lineNumber)
  {
    return fileValueStore.lookupValue(fileName, lineNumber);
  }

  @Override
  public Set<ITypeNodeRef> lookupMethodExceptions(final IMethodNode node)
  {
    return methodToExceptions.lookupChildren(node);
  }

  @Override
  public Map<Integer, IMethodNode> lookupMethodMembers(final ITypeNode node)
  {
    return typeToMemberMethods.lookupChildren(node);
  }

  @Override
  public IJiveEvent lookupNextEvent(final IJiveEvent event)
  {
    return lookupEvent(event.eventId() + 1);
  }

  @Override
  public INode lookupNode(final String key)
  {
    return key == null ? null : nodeToChildNodes.lookupKey(key);
    // nodeIndex.get(key);
  }

  @Override
  public INodeRef lookupNodeRef(final String key)
  {
    return key == null ? null : noderefStore.get(key);
  }

  @Override
  public Set<ITypeNode> lookupNodeTypes(final INode node)
  {
    return nodeToTypes.lookupChildren(node);
  }

  @Override
  public IJiveEvent lookupPriorEvent(final IJiveEvent event)
  {
    return lookupEvent(event.eventId() - 1);
  }

  @Override
  public IContour lookupStaticContour(final String typeName)
  {
    if (typeName == null)
    {
      return null;
    }
    return contourStore.get(staticContourKey(typeName));
  }

  @Override
  public Set<ITypeNodeRef> lookupSuperInterfaces(final ITypeNode node)
  {
    return typeToSuperInterfaces.lookupChildren(node);
  }

  @Override
  public IThreadValue lookupThread(final Long key)
  {
    return threadStore.get(key);
  }

  @Override
  public Set<ITypeNode> lookupTypeMembers(final INode node)
  {
    return nodeToTypes.lookupChildren(node);
  }

  @Override
  public IValue lookupValue(final Object key)
  {
    return valueIndex.get(key);
  }

  long nextCount()
  {
    return nextCount(Store.PREFIX_COUNTER);
  }

  /**
   * Return the next counter value for the key.
   */
  @Override
  public long nextCount(final Object key)
  {
    return getCounter(key).incrementAndGet();
  }

  // Collection<? extends INode> nodes() {
  //
  // return null;
  // }
  @Override
  public long nextNodeId()
  {
    return nextCount(INode.class);
  }

  void reset()
  {
    contourChildToParent.clear();
    contourParentToChildren.clear();
    contourStore.clear();
    counters.clear();
    eventStore.clear();
    fileValueStore.clear();
    lastTransactionEvent = null;
    methodToExceptions.clear();
    noderefStore.clear();
    nodeToChildNodes.clear();
    nodeToMemberData.clear();
    nodeToTypes.clear();
    resolvedNodes.clear();
    root = null;
    rootFiles.clear();
    slicedView = null;
    threadStore.clear();
    transactionCount = 0;
    transactionReplayCursor = null;
    typeToMemberMethods.clear();
    typeToSuperInterfaces.clear();
    valueIndex.clear();
    valueStore.clear();
  }

  /**
   * Creates a virtualId for every event in the view, where the virtual identifier is the position
   * of the event in the view (1-based). This method updates the transactional state of the view.
   */
  void sliceFilterAdd(final BitSet view)
  {
    // only one view is supported at any given time
    if (slicedView != null)
    {
      sliceFilterRemove();
    }
    // the model becomes virtual at this point (virtualization relies on this being set)
    this.slicedView = view;
    // clone for a destructive traversal
    final BitSet copy = (BitSet) view.clone();
    // the first virtual identifier
    int virtualId = copy.cardinality();
    /**
     * Set the virtual identifier for the events in the view by traversing a copy of the view in
     * reverse order. Unfortunately, BitSet does not provide reverse order traversal, so we must
     * carry the traversal on a copy since the process is destructive, i.e., modifies the state of
     * the bit set.
     */
    while (!copy.isEmpty())
    {
      final long eventId = copy.length() - 1;
      // the highest bit set in the view
      final JiveEvent event = eventStore.get((int) eventId - 1);
      // little sanity check
      assert event.transaction() != null : "All events in a view must have an associated transaction.";
      // virtualize the event
      event.setVirtualId(virtualId);
      // decrease the virtual identifier
      virtualId--;
      // clear the last bit
      copy.clear((int) eventId);
    }
  }

  /**
   * Clears the virtualId of every event in the view. This method updates the transactional state of
   * the view.
   */
  void sliceFilterRemove()
  {
    if (slicedView == null)
    {
      return;
    }
    final BitSet view = slicedView;
    // the model is no longer virtual at this point
    slicedView = null;
    // reset the virtual identifier for the events in the view
    for (int eventId = view.nextSetBit(0); eventId >= 0; eventId = view.nextSetBit(eventId + 1))
    {
      final JiveEvent event = eventStore.get(eventId - 1);
      event.resetVirtualId();
      if (event instanceof AssignEvent)
      {
        ((AssignEvent) event).setLastAssignment(null);
        // System.err.println("LAST_ASSIGN_OLD_CLEARED[" + event + "]");
      }
    }
    /**
     * The caller code must have set the transaction log to its initial state. Any contours left in
     * these maps are residual contours from the non-filtered model, which were "invisible" but now
     * must be removed.
     */
    contourChildToParent.clear();
    contourParentToChildren.clear();
  }

  void storeEvent(final JiveEvent event)
  {
    event.setEventId(nextCount(IJiveEvent.class));
    eventStore.add(event);
    if (event instanceof IDestroyObjectEvent)
    {
      oidToGC.put(((IDestroyObjectEvent) event).destroyedContour().oid(),
          (IDestroyObjectEvent) event);
    }
  }

  @Override
  public long storeFileNode(final IFileNode node)
  {
    // the new node's identifier
    final long result = nextNodeId();
    // create the file node entry
    rootFiles.put(node.name(), node);
    // create an entry for the file's types
    nodeToTypes.putKey(node);
    // return this node's identifier
    return result;
  }

  @Override
  public void storeFileValue(final String fileName, final IFileValue file)
  {
    fileValueStore.putKey(fileName, file);
  }

  @Override
  public void storeLine(final String fileName, final ILineValue line)
  {
    fileValueStore.put(fileName, line.lineNumber(), line);
  }

  @Override
  public long storeMethodNode(final IMethodNode node)
  {
    // remove this method's entry from the node reference index
    final INodeRef ref = noderefStore.remove(node.key());
    // the new node's identifier
    final long result = ref != null ? ref.id() : nextNodeId();
    // create the method node entry
    nodeToChildNodes.putKey(node.key(), node);
    // map this method node as a child of its parent type
    nodeToChildNodes.put(node.parent().key(), node.key(), node);
    // create an entry for the methods's data members (variables)
    nodeToMemberData.putKey(node);
    // create an entry for the method's local types
    nodeToTypes.putKey(node);
    // create an entry for the method's thrown exceptions
    methodToExceptions.putKey(node);
    // return this node's identifier
    return result;
  }

  @Override
  public long storeNodeRef(final INodeRef nodeRef)
  {
    // create a new identifier for this reference-- by using the same counter as the nodes, we
    // guarantee that node identifiers and node references have a disjoint set of identifiers
    final long result = nextNodeId();
    // make sure this reference is indexed
    noderefStore.putIfAbsent(nodeRef.key(), nodeRef);
    // return the reference identifier
    return result;
  }

  @Override
  public long storeResolvedNode(final IResolvedNode node)
  {
    // the new node's identifier
    final long result = nextCount(IResolvedNode.class);
    resolvedNodes.put(result, node);
    return result;
  }

  @Override
  public long storeRootNode(final IRootNode node)
  {
    // the new node's identifier
    final long result = nextNodeId();
    // keep a reference to the root node
    root = node;
    // create an entry for the root's types
    nodeToTypes.putKey(node);
    // return this node's identifier
    return result;
  }

  @Override
  public void storeThread(final Long uniqueId, final IThreadValue value)
  {
    threadStore.putIfAbsent(uniqueId, value);
  }

  /**
   * TODO: push to the event. Transactions are computed on a per-need basis. Here, they should be
   * computed, committed, then discarded
   */
  void storeTransaction(final IJiveEvent event, final List<IStateChange> changes)
  {
    // associate a new, uncommitted transaction with the event
    ((JiveEvent) event).setTransaction(new Transaction(event, changes));
    // commit the transaction
    transactionCommit(event.transaction());
    // remember the transaction count
    transactionCount++;
    // remember the last committed transaction
    lastTransactionEvent = event;
  }

  @Override
  public long storeTypeNode(final ITypeNode node)
  {
    // remove this method's entry from the node reference index
    final INodeRef ref = noderefStore.remove(node.key());
    // the new node's identifier
    final long result = ref != null ? ref.id() : nextNodeId();
    // map this type node as a child of its parent (root or file)
    nodeToChildNodes.put(node.parent().name(), node.key(), node);
    // map this type node as a child of its parent (root or file)
    nodeToChildNodes.putKey(node.key(), node);
    // create an entry for the type's data members (fields)
    nodeToMemberData.putKey(node);
    // create an entry for the type's member types
    nodeToTypes.putKey(node);
    // create an entry for the type's member methods
    typeToMemberMethods.putKey(node);
    // create an entry for the type's implementing super interfaces
    typeToSuperInterfaces.putKey(node);
    // return this node's identifier
    return result;
  }

  @Override
  public long storeValue(final IValue value)
  {
    final long id = nextCount(IValue.class);
    valueStore.put(id, value);
    return id;
  }

  void transactionCommitReplay()
  {
    // retrieve an uncommitted (normal mode) or rolled back transaction (replay mode) to commit
    transactionCommit(transactionReplayCursor.transaction());
  }

  /**
   * Size of the transaction log. This method observes whether a view is currently in place.
   */
  int transactionCount()
  {
    if (slicedView == null)
    {
      return this.transactionCount;
    }
    return slicedView.cardinality();
  }

  IJiveEvent transactionLogCursor()
  {
    return transactionReplayCursor;
  }

  IJiveEvent transactionLogRollback()
  {
    // update the replay cursor
    if (transactionReplayCursor == null)
    {
      transactionReplayCursor = lastTransactionEvent();
    }
    else if (transactionReplayCursor != model.lookupRoot())
    {
      transactionReplayCursor = lookupPriorEvent(transactionReplayCursor);
    }
    return transactionReplayCursor;
  }

  void transactionRollback()
  {
    IJiveEvent replayEvent = transactionReplayCursor;
    // update the replay cursor
    if (replayEvent == null)
    {
      replayEvent = lastTransactionEvent();
    }
    else if (replayEvent != model.lookupRoot())
    {
      replayEvent = replayEvent.prior();
    }
    final ITransaction transaction = replayEvent.transaction();
    // revert all changes in this transaction, in the reverse order in which they occurred
    for (int i = transaction.changes().size() - 1; i >= 0; i--)
    {
      final IStateChange change = transaction.changes().get(i);
      if (change instanceof AtomicInsert)
      {
        final AtomicInsert added = (AtomicInsert) change;
        transactionDeleteElement(added.contour());
      }
      else if (change instanceof AtomicDelete)
      {
        final AtomicDelete removed = (AtomicDelete) change;
        transactionInsertElement(removed.contour());
      }
      else if (change instanceof AtomicUpdate)
      {
        final AtomicUpdate updated = (AtomicUpdate) change;
        final IValue oldValue;
        if (replayEvent instanceof IAssignEvent
            && ((IAssignEvent) replayEvent).getLastValue() != null)
        {
          oldValue = ((IAssignEvent) replayEvent).getLastValue();
          // System.err.println("LAST_ASSIGN_OLD_VALUE[" + oldValue + ", " + replayEvent + "]");
        }
        else
        {
          oldValue = updated.oldValue();
        }
        transactionUpdateElement(updated.contour(), updated.member(), oldValue);
      }
    }
    // mark the transaction as uncommitted
    transactionReplayCursor = replayEvent;
  }

  /**
   * Given the input event, the transaction log places the replay cursor exactly on this event. This
   * amounts to setting this event as the last uncommitted event in the log.
   */
  void transactionSetLastUncommittedEvent(final IJiveEvent event)
  {
    if (transactionReplayCursor == null || !transactionReplayCursor.isVisible()
        || transactionReplayCursor.eventId() > event.eventId())
    {
      // done when replayEvent.eventId() == event.eventId(); thus, event is the last uncommitted
      do
      {
        // guarantees at least one rollback
        transactionRollback();
      } while (transactionReplayCursor != null
          && transactionReplayCursor.eventId() > event.eventId());
    }
    else
    {
      // done when replayEvent.eventId() == event.eventId(); thus, event is the last uncommitted
      while (transactionReplayCursor != null && transactionReplayCursor.eventId() < event.eventId())
      {
        transactionCommitReplay();
      }
    }
    // post-condition: replayEvent == event
  }

  /**
   * Standard implementation of a transaction-- a container for a list of atomic changes associated
   * with an event. A transaction has one of two states-- committed or uncommitted.
   * 
   * TODO: push the transactions to the event model. Instead of making the transactions part of the
   * stored model, make them transient and compute them on a per-need basis.
   */
  private final static class Transaction implements ITransaction
  {
    private final List<IStateChange> changes;
    private final IJiveEvent parent;

    @SuppressWarnings("unchecked")
    private Transaction(final IJiveEvent parent, final List<IStateChange> changes)
    {
      // this.isCommitted = false;
      this.changes = (List<IStateChange>) (changes == null ? Collections.emptyList() : changes);
      this.parent = parent;
    }

    // TODO: push to the event
    @Override
    public List<IStateChange> changes()
    {
      return changes;
    }

    // TODO: push to the event
    @Override
    public boolean isCommitted()
    {
      final Store store = ((ExecutionModel) parent.model()).store();
      if (store.transactionReplayCursor != null)
      {
        return parent.eventId() < store.transactionReplayCursor.eventId();
      }
      return parent.eventId() <= store.lastTransactionEvent().eventId();
    }

    @Override
    public String toString()
    {
      final StringBuffer sb = new StringBuffer("Transaction: {\n");
      for (final IStateChange change : changes())
      {
        sb.append(change + "\n");
      }
      sb.append("}");
      return sb.toString();
    }
  }
}