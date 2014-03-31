package edu.buffalo.cse.jive.model.contours;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentMap;

import edu.buffalo.cse.jive.lib.StringTools;
import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IStaticModel.IDataNode;
import edu.buffalo.cse.jive.model.IStaticModel.IEnvironmentNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.model.factory.IContourFactory;

/**
 * Separates the construction of contour elements from their storage, indexing, and look ups.
 * Storage implementation can be modified without affecting client code. Further, the way in which
 * keys formed and used to look up contours is left as a local implementation decision.
 */
public class ContourFactory implements IContourFactory
{
  private static ConcurrentMap<IExecutionModel, ContourFactory> factories = TypeTools
      .newConcurrentHashMap();

  public static ContourFactory getDefault(final IExecutionModel model)
  {
    if (!ContourFactory.factories.containsKey(model))
    {
      ContourFactory.factories.putIfAbsent(model, new ContourFactory(model));
    }
    return ContourFactory.factories.get(model);
  }

  private final IExecutionModel model;

  private ContourFactory(final IExecutionModel model)
  {
    this.model = model;
  }

  public IObjectContour createArrayContour(final ITypeNode concreteNode, final long oid,
      final int length)
  {
    // cannot create a contour with a null schema
    if (concreteNode == null)
    {
      return null;
    }
    // contour already exists
    final InstanceContour result = lookupInstanceContour(concreteNode.name(), oid);
    if (result != null)
    {
      return result;
    }
    // the superclass of an array type node *must* be Object
    final InstanceContour superContour = new InstanceContour(concreteNode.superClass().node(),
        null, oid);
    // concrete array contour
    final InstanceContour arrayContour = new ArrayContour(concreteNode.node(), superContour,
        length, oid);
    // all contours are virtual, except the last (never gets a child instance assigned)
    superContour.setChildInstance(arrayContour);
    // index the contours
    model.store().indexInstanceContour(superContour, oid);
    model.store().indexInstanceContour(arrayContour, oid);
    // return the concrete contour
    return arrayContour;
  }

  public IObjectContour createInstanceContour(final ITypeNode concreteNode, final long oid)
  {
    // cannot create a contour with a null schema
    if (concreteNode == null)
    {
      return null;
    }
    // contour already exists
    final InstanceContour result = lookupInstanceContour(concreteNode.name(), oid);
    if (result != null)
    {
      return result;
    }
    // schema stack to create all instance contours
    final Stack<ITypeNode> stack = new Stack<ITypeNode>();
    ITypeNode type = concreteNode;
    while (type != null)
    {
      stack.push(type.node());
      type = type.superClass() == null ? null : type.superClass().node();
    }
    // unfold the stack to create the instance contours
    InstanceContour parent = null;
    while (!stack.isEmpty())
    {
      type = stack.pop();
      final InstanceContour contour = new InstanceContour(type, parent, oid);
      // all contours are virtual, except the last (never gets a child instance assigned)
      if (parent != null)
      {
        parent.setChildInstance(contour);
      }
      // index the contour
      model.store().indexInstanceContour(contour, oid);
      // process the parent
      parent = contour;
    }
    // return the concrete contour
    return parent;
  }

  public IContextContour createStaticContour(final ITypeNode node)
  {
    // cannot create a contour with a null schema
    if (node == null)
    {
      return null;
    }
    // contour already exists
    StaticContour result = lookupStaticContour(node.name());
    if (result != null)
    {
      return result;
    }
    // the super class node reference must be resolved at this point
    final StaticContour superClass = node.superClass() == null ? null
        : (StaticContour) createStaticContour(node.superClass().node());
    result = new StaticContour(node, superClass);
    // index the contour
    model.store().indexStaticContour(result);
    return result;
  }

  @Override
  public InstanceContour lookupInstanceContour(final String typeName, final long oid)
  {
    return (InstanceContour) model.store().lookupInstanceContour(typeName, oid);
  }

  @Override
  public StaticContour lookupStaticContour(final String typeName)
  {
    return (StaticContour) model.store().lookupStaticContour(typeName);
  }

  @Override
  public IExecutionModel model()
  {
    return model;
  }

  @Override
  public IObjectContour retrieveInstanceContour(final String typeName, final long oid)
  {
    final IObjectContour contour = lookupInstanceContour(typeName, oid);
    if (contour == null)
    {
      throw new IllegalArgumentException(String.format(
          "No contour identifier for object id: %s (type: %s)!", oid, typeName));
    }
    return contour;
  }

  @Override
  public IContextContour retrieveStaticContour(final String typeName)
  {
    final IContextContour contour = lookupStaticContour(typeName);
    if (contour == null)
    {
      throw new IllegalArgumentException(String.format("No contour identifier for type: %s!",
          typeName));
    }
    return contour;
  }

  @SuppressWarnings("unused")
  private IExecutionModel executionModel()
  {
    return this.model;
  }

  private IMethodContour newMethodContour(final IMethodNode schema, final IThreadValue threadId,
      final IContextContour parent)
  {
    return new MethodContour(schema, threadId, parent);
  }

  /**
   * The default context contour represents a static contour.
   */
  public abstract class ContextContour extends AbstractContour implements IContextContour
  {
    private final List<IMethodCallEvent> initiators;

    private ContextContour(final ITypeNode schema, final IContextContour parent,
        final boolean isStatic)
    {
      super(schema, parent, isStatic);
      this.initiators = TypeTools.newArrayList();
    }

    public void addInitiator(final IMethodCallEvent initiator)
    {
      this.initiators.add(initiator);
    }

    /**
     * The method schema must be known at the time this method is called.
     */
    @Override
    public IMethodContour createMethodContour(final IMethodNode schema, final IThreadValue threadId)
    {
      return newMethodContour(schema, threadId, this);
    }

    @Override
    public boolean isStatic()
    {
      return true;
    }

    @Override
    public boolean isVirtual()
    {
      return false;
    }

    @Override
    public List<IMethodCallEvent> nestedInitiators()
    {
      if (!model.store().isVirtual())
      {
        return initiators;
      }
      final List<IMethodCallEvent> virtualInitiators = TypeTools.newArrayList();
      for (final IMethodCallEvent initiator : initiators)
      {
        if (initiator.isVisible())
        {
          virtualInitiators.add(initiator);
        }
      }
      return virtualInitiators;
    }

    @Override
    public IContextContour parent()
    {
      return (IContextContour) super.parent();
    }

    @Override
    public ITypeNode schema()
    {
      return (ITypeNode) super.schema();
    }

    @Override
    public IContourTokens tokenize()
    {
      return new ContourTokenizer(this);
    }
  }

  /**
   * Marks a contour member implementation that supports the changing of its value. This interface
   * should only be used by the underlying implementation engine, not at a higher level (e.g. GUI);
   * hence its inclusion in this package.
   */
  public interface IMutableContourMember extends IContourMember
  {
    /**
     * * Change the value of this contour member.
     * 
     * @param value
     *          new value
     * @return old value
     */
    public IValue setValue(IValue value);
  }

  private final class ArrayContour extends InstanceContour
  {
    private ArrayContour(final ITypeNode schema, final InstanceContour parent, final int length,
        final long oid)
    {
      super(schema, parent, oid);
      final Map<Integer, IContourMember> members = internalMembers();
      // the template member data node is always at position -2
      final IDataNode node = schema.dataMembers().get(-2);
      for (int i = 0; i < length; i++)
      {
        members.put(i, new ArrayMember(node, i));
      }
    }
  }

  private final class ArrayMember extends ContourMember
  {
    private final int index;

    private ArrayMember(final IDataNode schema, final int index)
    {
      super(schema);
      this.index = index;
    }

    @Override
    public String name()
    {
      return String.valueOf(index);
    }
  }

  private class ContourMember implements IMutableContourMember
  {
    protected IValue value = null;
    private final IDataNode schema;

    private ContourMember(final IDataNode schema)
    {
      this.schema = schema;
      this.value = schema.defaultValue();
    }

    @Override
    public String name()
    {
      return schema.name();
    }

    @Override
    public IDataNode schema()
    {
      return this.schema;
    }

    @Override
    public IValue setValue(final IValue value)
    {
      final IValue old = this.value;
      this.value = value;
      return old;
    }

    @Override
    public String toString()
    {
      return StringTools.contourMemberToString(this);
    }

    @Override
    public IValue value()
    {
      if (value == null)
      {
        value = schema().defaultValue();
      }
      return value;
    }
  }

  /**
   * Utility class used to break a {@code Contour} into its components.
   */
  private final class ContourTokenizer implements IContourTokens
  {
    private String callNumber = "";
    private String className = "";
    private String instanceNumber = "";
    private String methodName = "";

    private ContourTokenizer(final IContour contour)
    {
      tokenize(contour);
    }

    @Override
    public String callNumber()
    {
      return this.callNumber;
    }

    @Override
    public String className()
    {
      return this.className;
    }

    @Override
    public String instanceNumber()
    {
      return this.instanceNumber;
    }

    @Override
    public String methodName()
    {
      return this.methodName;
    }

    /**
     * Breaks the supplied {@code ContourID} into its various components.
     */
    private void tokenize(final IContour contour)
    {
      if (contour == null)
      {
        return;
      }
      final StringTokenizer tokenizer = new StringTokenizer(contour.signature(), ":#", true);
      className = tokenizer.nextToken();
      if (tokenizer.hasMoreTokens())
      {
        String delimiter = tokenizer.nextToken();
        if (delimiter.equals(":"))
        {
          instanceNumber = tokenizer.nextToken();
          if (tokenizer.hasMoreTokens())
          {
            delimiter = tokenizer.nextToken();
          }
          else
          { // ID for instance contour
            return;
          }
        }
        // ID for method contour
        if (delimiter.equals("#"))
        {
          methodName = tokenizer.nextToken();
          callNumber = tokenizer.nextToken();
        }
      }
      else
      { // ID for static contour
        return;
      }
    }
  }

  private class InstanceContour extends ContextContour implements IObjectContour
  {
    private InstanceContour childInstance;
    private final long oid;

    private InstanceContour(final ITypeNode schema, final InstanceContour parent, final long oid)
    {
      super(schema, parent, false);
      this.childInstance = null;
      this.oid = oid;
    }

    @Override
    public IContextContour concreteContour()
    {
      if (childInstance == null)
      {
        return this;
      }
      return childInstance.concreteContour();
    }

    // @Override
    // public boolean isGarbageCollected(final long eventId)
    // {
    // return model.store().isGarbageCollected(this.oid, eventId);
    // }
    @Override
    public boolean isStatic()
    {
      return false;
    }

    @Override
    public boolean isVirtual()
    {
      return childInstance != null;
    }

    @Override
    public ContourKind kind()
    {
      return childInstance == null ? ContourKind.CK_INSTANCE : ContourKind.CK_INSTANCE_VIRTUAL;
    }

    @Override
    public long oid()
    {
      return this.oid;
    }

    @Override
    public String signature()
    {
      return String.format("%s:%d", schema().name(), ordinalId());
    }

    private void setChildInstance(final InstanceContour childInstance)
    {
      this.childInstance = childInstance;
    }
  }

  private final class MethodContour extends AbstractContour implements IMethodContour
  {
    private final IThreadValue thread;

    private MethodContour(final IMethodNode schema, final IThreadValue thread,
        final IContextContour parent)
    {
      super(schema, parent);
      this.thread = thread;
      assert this.thread != null : "MethodContour was provided with a null thread!";
    }

    @Override
    public ContourKind kind()
    {
      return ContourKind.CK_METHOD;
    }

    /**
     * Returns a {@code ContourMember} at the given index position, if one exists, or null
     * otherwise.
     */
    @Override
    public IContourMember lookupMember(final int index)
    {
      return super.members.get(index);
    }

    /**
     * Returns a {@code ContourMember} with the name and line number, of one exists, or null.
     */
    @Override
    public IContourMember lookupMember(final String varName, final int lineNumber)
    {
      for (final IContourMember cm : super.members.values())
      {
        if (!cm.schema().name().equals(varName))
        {
          continue;
        }
        if (cm.schema().lineFrom() <= lineNumber && lineNumber <= cm.schema().lineTo())
        {
          return cm;
        }
      }
      return null;
    }

    @Override
    public IContourMember lookupResultMember()
    {
      for (final IContourMember member : members())
      {
        if (member.schema().modifiers().contains(NodeModifier.NM_RESULT))
        {
          return member;
        }
      }
      return null;
    }

    @Override
    public IContourMember lookupRPDLMember()
    {
      for (final IContourMember member : members())
      {
        if (member.schema().modifiers().contains(NodeModifier.NM_RPDL))
        {
          return member;
        }
      }
      return null;
    }

    @Override
    public IContextContour parent()
    {
      return (IContextContour) super.parent();
    }

    @Override
    public IMethodNode schema()
    {
      return (IMethodNode) super.schema();
    }

    @Override
    public String signature()
    {
      return String.format("%s#%s:%d", parent().signature(), schema().name(), ordinalId());
    }

    @Override
    public IThreadValue thread()
    {
      return this.thread;
    }

    @Override
    public IContourTokens tokenize()
    {
      return new ContourTokenizer(this);
    }
  }

  private final class StaticContour extends ContextContour
  {
    private StaticContour(final ITypeNode schema, final StaticContour parent)
    {
      super(schema, parent, true);
    }

    @Override
    public IContextContour concreteContour()
    {
      return this;
    }

    @Override
    public ContourKind kind()
    {
      return ContourKind.CK_STATIC;
    }

    @Override
    public String signature()
    {
      return schema().name();
    }
  }

  abstract class AbstractContour implements IContour
  {
    private final IEnvironmentNode environment;
    private final long id;
    private final Map<Integer, IContourMember> members;
    private final long ordinalId;
    private final IContour parent;

    /**
     * Used by method environment contours.
     */
    private AbstractContour(final IEnvironmentNode environment, final IContour parent)
    {
      this.environment = environment;
      this.id = model.store().nextCount(IContour.class);
      this.ordinalId = model.store().nextCount(environment);
      this.parent = parent;
      this.members = createVariableMembers();
    }

    /**
     * Used by static and instance environment contours.
     */
    private AbstractContour(final IEnvironmentNode environment, final IContour parent,
        final boolean isStatic)
    {
      this.environment = environment;
      this.id = model.store().nextCount(IContour.class);
      this.ordinalId = model.store().nextCount(isStatic ? IEnvironmentNode.class : environment);
      this.parent = parent;
      this.members = createFieldMembers(isStatic);
    }

    /**
     * List this contour's children. This operation is thread-safe.
     * 
     * @return immutable, non-null list of children
     */
    @Override
    public List<IContour> children()
    {
      return model.transactionLog().getChildren(this);
    }

    @Override
    public boolean equals(final Object other)
    {
      return (other instanceof AbstractContour) && id == ((AbstractContour) other).id;
    }

    @Override
    public long id()
    {
      return this.id;
    }

    @Override
    public IContourMember lookupMember(final IDataNode schema)
    {
      for (final IContourMember instance : members())
      {
        if (instance.schema().equals(schema))
        {
          return instance;
        }
      }
      // could be an inherited field
      if (parent != null)
      {
        return parent.lookupMember(schema);
      }
      return null;
    }

    @Override
    public IContourMember lookupMember(final int index)
    {
      return members.get(index);
    }

    @Override
    public IContourMember lookupMember(final String name)
    {
      for (final IContourMember instance : members())
      {
        if (instance.schema().name().equals(name))
        {
          return instance;
        }
      }
      // could be an inherited field
      if (parent != null)
      {
        return parent.lookupMember(name);
      }
      return null;
    }

    @Override
    public Collection<IContourMember> members()
    {
      final Collection<IContourMember> result = this.members.values();
      if (!model.store().isVirtual())
      {
        return result;
      }
      final List<IContourMember> filtered = TypeTools.newArrayList();
      for (final IContourMember member : result)
      {
        if (model.sliceView().activeSlice().members().contains(member))
        {
          filtered.add(member);
        }
        // add arrays (for now...) as we can't determine which indices are relevant
        else if (this instanceof ArrayContour)
        {
          filtered.add(member);
        }
        // always add the RPDL
        else if (this instanceof IMethodContour
            && member.schema().modifiers().contains(NodeModifier.NM_RPDL))
        {
          filtered.add(member);
        }
      }
      return filtered;
    }

    @Override
    public IExecutionModel model()
    {
      return model;
    }

    @Override
    public long ordinalId()
    {
      return this.ordinalId;
    }

    /**
     * Return this contour's parent.
     * 
     * @return contour's parent or null if it is a root contour
     */
    @Override
    public IContour parent()
    {
      return parent;
    }

    @Override
    public IEnvironmentNode schema()
    {
      return this.environment;
    }

    @Override
    public String toString()
    {
      return StringTools.contourToString(this);
    }

    private Map<Integer, IContourMember> createFieldMembers(final boolean isStatic)
    {
      final Map<Integer, IContourMember> members = TypeTools.newLinkedHashMap();
      if (this instanceof ArrayContour)
      {
        return members;
      }
      for (final int id : environment.dataMembers().keySet())
      {
        final IDataNode node = environment.dataMembers().get(id);
        if (isStatic == node.modifiers().contains(NodeModifier.NM_STATIC))
        {
          members.put(node.index(), new ContourMember(node));
        }
      }
      return members;
    }

    private Map<Integer, IContourMember> createVariableMembers()
    {
      final Map<Integer, IContourMember> members = TypeTools.newLinkedHashMap();
      IDataNode rpdl = null;
      IDataNode result = null;
      // all data nodes
      for (final int id : environment.dataMembers().keySet())
      {
        final IDataNode node = environment.dataMembers().get(id);
        if (node.modifiers().contains(NodeModifier.NM_RPDL))
        {
          rpdl = node;
        }
        else if (node.modifiers().contains(NodeModifier.NM_RESULT))
        {
          result = node;
        }
        else
        {
          members.put(node.index(), new ContourMember(node));
        }
      }
      // result node
      if (result != null)
      {
        members.put(result.index(), new ContourMember(result));
      }
      // rpdl node
      if (rpdl != null)
      {
        members.put(rpdl.index(), new ContourMember(rpdl));
      }
      return members;
    }

    protected final long contourId()
    {
      return id;
    }

    protected Map<Integer, IContourMember> internalMembers()
    {
      return this.members;
    }
  }
}