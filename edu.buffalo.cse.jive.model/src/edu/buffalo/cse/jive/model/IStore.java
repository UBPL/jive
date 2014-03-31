package edu.buffalo.cse.jive.model;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IModel.IFileValue;
import edu.buffalo.cse.jive.model.IModel.ILineValue;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;
import edu.buffalo.cse.jive.model.IModel.IValue;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedNode;
import edu.buffalo.cse.jive.model.IStaticModel.IDataNode;
import edu.buffalo.cse.jive.model.IStaticModel.IFileNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.INode;
import edu.buffalo.cse.jive.model.IStaticModel.INodeRef;
import edu.buffalo.cse.jive.model.IStaticModel.IRootNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNodeRef;

public interface IStore
{
  // ------------------------------------------------------------------------
  // Data retrieval
  // ------------------------------------------------------------------------
  public Collection<? extends IFileValue> getFiles();

  public Collection<? extends ILineValue> getLines();

  public Collection<? extends IThreadValue> getThreads();

  public Collection<IValue> getValues();

  // ------------------------------------------------------------------------
  // Indexing store elements
  // ------------------------------------------------------------------------
  public void indexInstanceContour(IContour contour, long oid);

  public void indexStaticContour(IContour contour);

  public void indexValue(Object key, IValue value);

  // ------------------------------------------------------------------------
  // Data queries
  // ------------------------------------------------------------------------
  public boolean isGarbageCollected(long oid, long eventId);

  /**
   * Returns true if there currently is a (sliced) view on top of the model/store.
   */
  public boolean isVirtual();

  public Map<Integer, IDataNode> lookupDataMembers(INode node);

  public IJiveEvent lookupDestroyEvent(long objectId);

  public IFileNode lookupFile(String name);

  public Set<IFileNode> lookupFiles();

  public IFileValue lookupFileValue(String fileName);

  public IContour lookupInstanceContour(String typeName, long oid);

  public ILineValue lookupLineValue(String fileName, int lineNumber);

  public Set<ITypeNodeRef> lookupMethodExceptions(IMethodNode node);

  public Map<Integer, IMethodNode> lookupMethodMembers(ITypeNode node);

  /**
   * Returns the next event that is visible in the model. This method observes whether a view is
   * currently in place.
   */
  public IJiveEvent lookupNextEvent(final IJiveEvent event);

  public INode lookupNode(String fileName);

  public INodeRef lookupNodeRef(String key);

  public Collection<? extends INodeRef> lookupNodeRefs();

  public Set<ITypeNode> lookupNodeTypes(INode node);

  /**
   * Returns the prior event that is visible in the model. This method observes whether a view is
   * currently in place.
   */
  public IJiveEvent lookupPriorEvent(final IJiveEvent event);

  /**
   * Returns the event matching the identifier in the model. This method does not consider whether a
   * view is currently in place and uses the actual event identifier as a key. This means that an
   * event not currently visible under the view may be returned.
   */
  public IJiveEvent lookupRawEvent(long eventId);

  public Collection<? extends IResolvedNode> lookupResolvedNodes();

  public IRootNode lookupRoot();

  public IContour lookupStaticContour(String typeName);

  public Set<ITypeNodeRef> lookupSuperInterfaces(ITypeNode node);

  public IThreadValue lookupThread(Long uniqueId);

  public Set<ITypeNode> lookupTypeMembers(INode node);

  public IValue lookupValue(Object object);

  // ------------------------------------------------------------------------
  // Key generation helpers
  // ------------------------------------------------------------------------
  public long nextCount(Object object);

  public long nextNodeId();

  // ------------------------------------------------------------------------
  // Storing model elements
  // ------------------------------------------------------------------------
  public long storeFileNode(IFileNode node);

  public void storeFileValue(String fileName, IFileValue fileValue);

  public void storeLine(String fileName, ILineValue value);

  public long storeMethodNode(IMethodNode node);

  public long storeNodeRef(INodeRef nodeRef);

  public long storeResolvedNode(IResolvedNode node);

  public long storeRootNode(IRootNode node);

  public void storeThread(Long uniqueId, IThreadValue value);

  public long storeTypeNode(ITypeNode node);

  public long storeValue(IValue value);
}