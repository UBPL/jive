package edu.buffalo.cse.jive.model.statics;

import static edu.buffalo.cse.jive.model.IStaticModel.NodeKind.NK_ARRAY;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeKind.NK_CLASS;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeKind.NK_FIELD;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeKind.NK_FILE;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeKind.NK_INTERFACE;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeKind.NK_METHOD;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeKind.NK_PRIMITIVE;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeKind.NK_ROOT;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeKind.NK_VARIABLE;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_FIELD_INITIALIZER;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_TYPE_INITIALIZER;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeOrigin.NO_JIVE;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility.NV_LOCAL;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility.NV_PUBLIC;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentMap;

import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IObjectContour;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IMethodDependenceGraph;
import edu.buffalo.cse.jive.model.contours.ContourFactory;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory;
import edu.buffalo.cse.jive.model.factory.IValueFactory;

public class StaticModelFactory implements IStaticModelFactory
{
  private static final int VAR_INDEX_RESULT = -1;
  private static final int VAR_INDEX_RPDL = -2;
  private static final String VAR_NAME_RESULT = "result";
  private static final String VAR_NAME_RPDL = "return point";
  private static final String SNAPSHOT_RUN_KEY = "run()";
  private static final String SNAPSHOT_RUN_NAME = "run";
  @SuppressWarnings("unchecked")
  private static final Set<NodeModifier> EMPTY_MODIFIERS = Collections.EMPTY_SET;
  private final IExecutionModel model;
  private static ConcurrentMap<IExecutionModel, StaticModelFactory> modelStores = TypeTools
      .newConcurrentHashMap();

  public static StaticModelFactory getDefault(final IExecutionModel model)
  {
    if (!StaticModelFactory.modelStores.containsKey(model))
    {
      StaticModelFactory.modelStores.putIfAbsent(model, new StaticModelFactory(model));
    }
    return StaticModelFactory.modelStores.get(model);
  }

  private StaticModelFactory(final IExecutionModel model)
  {
    this.model = model;
    createKnownTypes();
  }

  @Override
  public IFileNode createFileNode(final String fileName, final int lineFrom, final int lineTo,
      final NodeOrigin origin)
  {
    if (model.store().lookupNode(fileName) == null)
    {
      return new FileNode(fileName, lineFrom, lineTo, origin);
    }
    return (IFileNode) model.store().lookupNode(fileName);
  }

  @Override
  public ITypeNode createThreadNode()
  {
    final ITypeNode threadNode = createTypeNode("Ljava/lang/Thread;", "java.lang.Thread",
        lookupRoot(), -1, -1, NK_CLASS, NodeOrigin.NO_JIVE, Collections.<NodeModifier> emptySet(),
        NV_PUBLIC, lookupObjectType(), Collections.<ITypeNodeRef> emptySet(), valueFactory()
            .createNullValue());
    // thread id
    threadNode.addDataMember("id", -1, -1, lookupTypeNode(KnownType.RT_INT.descriptor()), NO_JIVE,
        Collections.<NodeModifier> emptySet(), NodeVisibility.NV_PRIVATE, valueFactory()
            .createPrimitiveValue("0"));
    // thread name
    threadNode.addDataMember("name", -1, -1, lookupTypeNode(KnownType.OT_STRING.descriptor()),
        NO_JIVE, Collections.<NodeModifier> emptySet(), NodeVisibility.NV_PRIVATE, valueFactory()
            .createNullValue());
    // thread priority
    threadNode.addDataMember("priority", -1, -1, lookupTypeNode(KnownType.RT_INT.descriptor()),
        NO_JIVE, Collections.<NodeModifier> emptySet(), NodeVisibility.NV_PRIVATE, valueFactory()
            .createPrimitiveValue("1"));
    // thread scheduler
    threadNode.addDataMember("scheduler", -1, -1, lookupTypeNode(KnownType.OT_STRING.descriptor()),
        NO_JIVE, Collections.<NodeModifier> emptySet(), NodeVisibility.NV_PRIVATE, valueFactory()
            .createResolvedValue("Normal", ""));
    // thread constructor
    threadNode.addMethodMember("Ljava/lang/Thread;.Thread()", "Thread", -1, -1, lookupVoidType(),
        NO_JIVE, Collections.<NodeModifier> emptySet(), NodeVisibility.NV_PUBLIC,
        Collections.<ITypeNodeRef> emptySet());
    return threadNode;
  }

  @Override
  public ITypeNode createTypeNode(final String key, final String name, final INode parent,
      final int lineFrom, final int lineTo, final NodeKind kind, final NodeOrigin origin,
      final Set<NodeModifier> modifiers, final NodeVisibility visibility,
      final ITypeNodeRef superNode, final Set<ITypeNodeRef> superInterfaceNodes,
      final IValue defaultValue)
  {
    if (model.store().lookupNode(key) == null)
    {
      return new TypeNode(parent, name, lineFrom, lineTo, kind, origin, modifiers, visibility,
          superNode, superInterfaceNodes, defaultValue);
    }
    return (ITypeNode) model.store().lookupNode(key);
  }

  @Override
  public IFileNode lookupFileNode(final String name)
  {
    return model.store().lookupFile(name);
  }

  @Override
  public ILineValue lookupLine(final String fileName, final int lineNo)
  {
    final IFileNode file = lookupFileNode(fileName);
    if (file != null)
    {
      return model.valueFactory().createLine(((FileNode) file).name(), lineNo);
    }
    return model.valueFactory().createUnavailableLine();
  }

  @Override
  public IMethodNode lookupMethodNode(final String key)
  {
    return (IMethodNode) model.store().lookupNode(key);
  }

  @Override
  public IMethodNodeRef lookupMethodRef(final String key)
  {
    // try to retrieve an actual node
    IMethodNodeRef result = lookupMethodNode(key);
    if (result != null)
    {
      return result;
    }
    // try to retrieve a node reference
    result = (IMethodNodeRef) model.store().lookupNodeRef(key);
    if (result != null)
    {
      return result;
    }
    // create and return a new method node reference
    return new MethodNodeRef(key);
  }

  @Override
  public ITypeNode lookupObjectType()
  {
    return lookupTypeNode(KnownType.RT_OBJECT.typeDescriptor);
  }

  @Override
  public IRootNode lookupRoot()
  {
    return model.store().lookupRoot();
  }

  @Override
  public ITypeNode lookupRPDLType()
  {
    return lookupTypeNode(KnownType.RT_RPDL.typeDescriptor);
  }

  @Override
  public IMethodNode lookupSnapshotRunMethod()
  {
    final ITypeNode type = lookupSnapshotType();
    return type.methodMembers().get(0);
  }

  @Override
  public ITypeNode lookupSnapshotType()
  {
    return lookupTypeNode(KnownType.OT_SNAPTSHOT_SERVICE.typeDescriptor);
  }

  @Override
  public ITypeNode lookupTypeNode(final String key)
  {
    return (ITypeNode) model.store().lookupNode(key);
  }

  @Override
  public ITypeNode lookupTypeNodeByName(final String name)
  {
    return lookupTypeNode(typeNameToSignature(name));
  }

  @Override
  public ITypeNodeRef lookupTypeRef(final String key)
  {
    // try to retrieve an actual node
    ITypeNodeRef result = lookupTypeNode(key);
    if (result != null)
    {
      return result;
    }
    // try to retrieve a node reference
    result = (ITypeNodeRef) model.store().lookupNodeRef(key);
    if (result != null)
    {
      return result;
    }
    // create and return a new method node reference
    return new TypeNodeRef(key);
  }

  @Override
  public ITypeNodeRef lookupTypeRefByName(final String name)
  {
    return lookupTypeRef(typeNameToSignature(name));
  }

  @Override
  public ITypeNode lookupVoidType()
  {
    return lookupTypeNode(KnownType.RT_VOID.typeDescriptor);
  }

  @Override
  public IExecutionModel model()
  {
    return model;
  }

  @Override
  public String signatureToTypeName(final String signature)
  {
    String typeName = null;
    // known types
    for (final KnownType type : KnownType.values())
    {
      if (type.descriptor().equals(signature))
      {
        typeName = type.typeName();
        break;
      }
    }
    // array dimensions
    final int arrayDim = signature.lastIndexOf("[") + 1;
    // reference type
    if (signature.indexOf(";") != -1)
    {
      typeName = signature.substring(arrayDim + 1, signature.length() - 1);
    }
    else
    {
      typeName = signature.substring(arrayDim, signature.length());
    }
    typeName = typeName.replace("/", ".").substring(0, typeName.length());
    typeName = typeName.substring(typeName.indexOf("~") + 1);
    for (int i = 1; i <= arrayDim; i++)
    {
      typeName += "[]";
    }
    return typeName;
  }

  @Override
  public String typeNameToSignature(final String name)
  {
    final int index = name.indexOf("[");
    // strip the array part, if necessary
    final String baseType = index == -1 ? name : name.substring(0, index);
    String signature = null;
    // the base type is a known type
    for (final KnownType type : KnownType.values())
    {
      if (type.typeName.equals(baseType))
      {
        signature = type.typeDescriptor;
        break;
      }
    }
    // the base type is reference type
    if (signature == null)
    {
      signature = "L" + baseType.replace(".", "/") + ";";
    }
    // fix array types
    if (index > -1)
    {
      final String suffix = name.substring(index);
      for (int i = 0; i < suffix.length() / 2; i++)
      {
        signature = "[" + signature;
      }
    }
    return signature;
  }

  private IValueFactory valueFactory()
  {
    return model.valueFactory();
  }

  public void createKnownTypes()
  {
    new RootNode();
    // ITypeNode intType = null;
    ITypeNode voidType = null;
    for (final KnownType type : KnownType.values())
    {
      final IValue value;
      if (type == KnownType.RT_RPDL)
      {
        value = valueFactory().createUninitializedValue();
      }
      else if (type.defaultValue() == null)
      {
        value = valueFactory().createNullValue();
      }
      else
      {
        value = valueFactory().createPrimitiveValue(type.defaultValue());
      }
      final ITypeNode typeNode = createTypeNode(type.descriptor(), type.typeName(), lookupRoot(),
          -1, -1, type.kind(), NO_JIVE, StaticModelFactory.EMPTY_MODIFIERS, NV_PUBLIC,
          type.superClass(this), type.superInterfaces(this), value);
      // if (type == KnownType.RT_INT) {
      // intType = typeNode;
      // }
      // else
      if (type == KnownType.RT_VOID)
      {
        voidType = typeNode;
      }
      if (type == KnownType.OT_SNAPTSHOT_SERVICE)
      {
        typeNode.addMethodMember(KnownType.OT_SNAPTSHOT_SERVICE.descriptor() + "."
            + StaticModelFactory.SNAPSHOT_RUN_KEY, StaticModelFactory.SNAPSHOT_RUN_NAME, -1, -1,
            voidType, NO_JIVE, StaticModelFactory.EMPTY_MODIFIERS, NV_PUBLIC,
            Collections.<ITypeNodeRef> emptySet());
      }
      // if (type.kind() == NK_ARRAY) {
      // typeNode.addDataMember("length", -1, -1, intType, NO_JIVE, EMPTY_MODIFIERS, NV_PUBLIC,
      // intType.defaultValue());
      // }
    }
  }

  private final class DataNode extends Node implements IDataNode
  {
    private final IValue defaultValue;
    private final long id;
    private int index;
    private final ITypeNodeRef type;
    private final NodeKind kind;

    private DataNode(final IEnvironmentNode parent, final String name, final ITypeNodeRef type,
        final int index, final int lineFrom, final int lineTo, final NodeOrigin origin,
        final Set<NodeModifier> modifiers, final NodeVisibility visibility, final NodeKind kind,
        final IValue defaultValue)
    {
      super(parent, name, lineFrom, lineTo, origin, modifiers, visibility);
      this.defaultValue = defaultValue;
      this.index = index;
      this.type = type;
      this.kind = kind;
      this.id = model.store().nextNodeId();
    }

    @Override
    public IValue defaultValue()
    {
      return this.defaultValue;
    }

    @Override
    public long id()
    {
      return this.id;
    }

    /**
     * Declaration order of this element with respect to other elements of the same type in its
     * lexical scope.
     * 
     * Local variables inside methods: Local variables (and parameters) declared within a single
     * method are assigned ascending ids in normal code reading order;
     * var1.getVariableId()<var2.getVariableId() means that var1 is declared before var2.
     * 
     * Local variables outside methods: Local variables declared in a type's static initializers (or
     * initializer expressions of static fields) are assigned ascending ids in normal code reading
     * order. Local variables declared in a type's instance initializers (or initializer expressions
     * of non-static fields) are assigned ascending ids in normal code reading order. These ids are
     * useful when checking definite assignment for static initializers (JLS 16.7) and instance
     * initializers (JLS 16.8), respectively.
     * 
     * Fields: Fields declared as members of a type are assigned ascending ids in normal code
     * reading order; field1.getVariableId()<field2.getVariableId() means that field1 is declared
     * before field2.
     */
    @Override
    public int index()
    {
      return this.index;
    }

    @Override
    public NodeKind kind()
    {
      return this.kind;
    }

    @Override
    public IEnvironmentNode parent()
    {
      return (IEnvironmentNode) super.parent();
    }

    @Override
    public ITypeNodeRef type()
    {
      return this.type;
    }

    void setIndex(final int index)
    {
      this.index = index;
    }
  }

  private abstract class EnvironmentNode extends Node implements IEnvironmentNode
  {
    private EnvironmentNode(final INode parent, final String name, final int lineFrom,
        final int lineTo, final NodeOrigin origin, final Set<NodeModifier> modifiers,
        final NodeVisibility visibility)
    {
      super(parent, name, lineFrom, lineTo, origin, modifiers, visibility);
    }

    @Override
    public Map<Integer, IDataNode> dataMembers()
    {
      return model.store().lookupDataMembers(this);
    }

    /**
     * This version requires a kind and uses an explicitly assigned index.
     */
    protected IDataNode addDataMember(final String name, final int lineFrom, final int lineTo,
        final ITypeNodeRef type, final int index, final NodeOrigin origin,
        final Set<NodeModifier> modifiers, final NodeVisibility visibility, final NodeKind kind,
        final IValue defaultValue)
    {
      final IDataNode result = new DataNode(this, name, type, index, lineFrom, lineTo, origin,
          modifiers, visibility, kind, defaultValue);
      dataMembers().put(index, result);
      return result;
    }

    /**
     * This version requires a kind and computes an index based on the existing keys in the map.
     */
    protected IDataNode addDataMember(final String name, final int lineFrom, final int lineTo,
        final ITypeNodeRef type, final NodeOrigin origin, final Set<NodeModifier> modifiers,
        final NodeVisibility visibility, final NodeKind kind, final IValue defaultValue)
    {
      final SortedMap<Integer, IDataNode> dataMembers = (SortedMap<Integer, IDataNode>) dataMembers();
      final int index = dataMembers.isEmpty() ? 0 : dataMembers.lastKey() < 0 ? 0 : dataMembers
          .lastKey() + 1;
      return addDataMember(name, lineFrom, lineTo, type, index, origin, modifiers, visibility,
          kind, defaultValue);
    }
  }

  private final class FileNode extends Node implements IFileNode
  {
    private final long id;

    private FileNode(final String fileName, final int lineFrom, final int lineTo,
        final NodeOrigin origin)
    {
      super(lookupRoot(), fileName, lineFrom, lineTo, origin, StaticModelFactory.EMPTY_MODIFIERS,
          NodeVisibility.NV_PUBLIC);
      this.id = model.store().storeFileNode(this);
    }

    @Override
    public long id()
    {
      return this.id;
    }

    @Override
    public NodeKind kind()
    {
      return NK_FILE;
    }

    @Override
    public IRootNode parent()
    {
      return (IRootNode) super.parent();
    }

    @Override
    public Set<ITypeNode> types()
    {
      return model.store().lookupTypeMembers(this);
    }
  }

  private enum KnownType
  {
    // root types without an ancestor class
    RT_BOOLEAN("boolean", "Z", null, NK_PRIMITIVE, "false", null),
    RT_BYTE("byte", "B", null, NK_PRIMITIVE, "0", null),
    RT_CHAR("char", "C", null, NK_PRIMITIVE, "''", null),
    RT_DOUBLE("double", "D", null, NK_PRIMITIVE, "0.0", null),
    RT_FLOAT("float", "F", null, NK_PRIMITIVE, "0.0", null),
    RT_INT("int", "I", null, NK_PRIMITIVE, "0", null),
    RT_LONG("long", "J", null, NK_PRIMITIVE, "0", null),
    RT_SHORT("short", "S", null, NK_PRIMITIVE, "0", null),
    RT_VOID("void", "V", null, NK_PRIMITIVE, "", null),
    RT_OBJECT("java.lang.Object", null, null, NK_CLASS, null, null),
    RT_SERIALIZABLE("java.io.Serializable", null, null, NK_INTERFACE, null, null),
    RT_COMPARABLE("java.lang.Comparable", null, null, NK_INTERFACE, null, null),
    RT_CHARSEQUENCE("java.lang.CharSequence", null, null, NK_INTERFACE, null, null),
    // synthetic JIVE type
    RT_RPDL("rpdl", "R", null, NK_PRIMITIVE, null, null),
    // object types
    OT_NUMBER("java.lang.Number", null, RT_OBJECT, NK_CLASS, null, KnownType.INT_SER()),
    OT_BOOLEAN("java.lang.Boolean", null, RT_OBJECT, NK_CLASS, null, KnownType.INT_COMP_SER()),
    OT_BYTE("java.lang.Byte", null, OT_NUMBER, NK_CLASS, null, KnownType.INT_COMP()),
    OT_CHAR("java.lang.Character", null, RT_OBJECT, NK_CLASS, null, KnownType.INT_COMP_SER()),
    OT_DOUBLE("java.lang.Double", null, OT_NUMBER, NK_CLASS, null, KnownType.INT_COMP()),
    OT_FLOAT("java.lang.Float", null, OT_NUMBER, NK_CLASS, null, KnownType.INT_COMP()),
    OT_INT("java.lang.Integer", null, OT_NUMBER, NK_CLASS, null, KnownType.INT_COMP()),
    OT_LONG("java.lang.Long", null, OT_NUMBER, NK_CLASS, null, KnownType.INT_COMP()),
    OT_SHORT("java.lang.Short", null, OT_NUMBER, NK_CLASS, null, KnownType.INT_COMP()),
    OT_STRING("java.lang.String", null, RT_OBJECT, NK_CLASS, null, KnownType.INT_COMP_SER_CS()),
    // synthetic JIVE type
    OT_SNAPTSHOT_SERVICE(
        "edu.buffalo.cse.jive.SnapshotService",
        null,
        RT_OBJECT,
        NK_CLASS,
        null,
        null),
    // object array types
    OAT_BOOLEAN("java.lang.Boolean[]", null, RT_OBJECT, NK_ARRAY, null, null),
    OAT_BYTE("java.lang.Byte[]", null, RT_OBJECT, NK_ARRAY, null, null),
    OAT_CHAR("java.lang.Character[]", null, RT_OBJECT, NK_ARRAY, null, null),
    OAT_DOUBLE("java.lang.Double[]", null, RT_OBJECT, NK_ARRAY, null, null),
    OAT_FLOAT("java.lang.Float[]", null, RT_OBJECT, NK_ARRAY, null, null),
    OAT_INT("java.lang.Integer[]", null, RT_OBJECT, NK_ARRAY, null, null),
    OAT_LONG("java.lang.Long[]", null, RT_OBJECT, NK_ARRAY, null, null),
    OAT_NUMBER("java.lang.Number[]", null, RT_OBJECT, NK_ARRAY, null, null),
    OAT_OBJECT("java.lang.Object[]", null, RT_OBJECT, NK_ARRAY, null, null),
    OAT_SHORT("java.lang.Short[]", null, RT_OBJECT, NK_ARRAY, null, null),
    OAT_STRING("java.lang.String[]", null, RT_OBJECT, NK_ARRAY, null, null),
    // primitive array types
    PAT_BOOLEAN("boolean[]", "[Z", RT_OBJECT, NK_ARRAY, null, null),
    PAT_BYTE("byte[]", "[B", RT_OBJECT, NK_ARRAY, null, null),
    PAT_CHAR("char[]", "[C", RT_OBJECT, NK_ARRAY, null, null),
    PAT_DOUBLE("double[]", "[D", RT_OBJECT, NK_ARRAY, null, null),
    PAT_FLOAT("float[]", "[F", RT_OBJECT, NK_ARRAY, null, null),
    PAT_INT("int[]", "[I", RT_OBJECT, NK_ARRAY, null, null),
    PAT_LONG("long[]", "[J", RT_OBJECT, NK_ARRAY, null, null),
    PAT_SHORT("short[]", "[S", RT_OBJECT, NK_ARRAY, null, null);
    private final static KnownType[] INT_COMP()
    {
      return new KnownType[]
      { RT_COMPARABLE };
    }

    private final static KnownType[] INT_COMP_SER()
    {
      return new KnownType[]
      { KnownType.RT_COMPARABLE, KnownType.RT_SERIALIZABLE };
    }

    private final static KnownType[] INT_COMP_SER_CS()
    {
      return new KnownType[]
      { KnownType.RT_COMPARABLE, KnownType.RT_SERIALIZABLE, KnownType.RT_CHARSEQUENCE };
    }

    private final static KnownType[] INT_SER()
    {
      return new KnownType[]
      { RT_SERIALIZABLE };
    }

    private final String defaultValue;
    private final NodeKind kind;
    private final KnownType superClass;
    private final KnownType[] superInterfaces;
    private final String typeDescriptor;
    private final String typeName;

    // TypeName, TypeDescriptor, AncestorClass, DefaultTypeValue
    private KnownType(final String typeName, final String typeDescriptor,
        final KnownType superClass, final NodeKind kind, final String defaultValue,
        final KnownType[] superInterfaces)
    {
      this.typeName = typeName;
      if (typeDescriptor == null)
      {
        final String desc = typeName.replace('.', '/');
        if (kind == NK_ARRAY)
        {
          this.typeDescriptor = String.format("[L%s;", desc.substring(0, typeName.length() - 2));
        }
        else
        {
          this.typeDescriptor = String.format("L%s;", desc);
        }
      }
      else
      {
        this.typeDescriptor = typeDescriptor;
      }
      this.superClass = superClass;
      this.defaultValue = defaultValue;
      this.kind = kind;
      this.superInterfaces = superInterfaces;
    }

    public final String descriptor()
    {
      return this.typeDescriptor;
    }

    @Override
    public String toString()
    {
      return String.format("name: %s, descriptor: %s", typeName, typeDescriptor);
    }

    private final String defaultValue()
    {
      return this.defaultValue;
    }

    private final NodeKind kind()
    {
      return this.kind;
    }

    private final ITypeNode superClass(final StaticModelFactory factory)
    {
      return superClass == null ? null : factory.lookupTypeNode(superClass.typeDescriptor);
    }

    private final Set<ITypeNodeRef> superInterfaces(final StaticModelFactory factory)
    {
      final Set<ITypeNodeRef> result = TypeTools.newHashSet();
      if (superInterfaces != null)
      {
        for (final KnownType implemented : superInterfaces)
        {
          result.add(factory.lookupTypeNode(implemented.descriptor()));
        }
      }
      return result;
    }

    private final String typeName()
    {
      return this.typeName;
    }
  }

  private final class MethodNode extends EnvironmentNode implements IMethodNode
  {
    private final ITypeNodeRef returnType;
    private final int index;
    private final String key;
    private IMethodDependenceGraph mdg;
    private final long id;

    private MethodNode(final String key, final ITypeNode parent, final String name,
        final int lineFrom, final int lineTo, final ITypeNodeRef returnType, final int index,
        final NodeOrigin origin, final Set<NodeModifier> modifiers,
        final NodeVisibility visibility, final Set<ITypeNodeRef> exceptions)
    {
      super(parent, name, lineFrom, lineTo, origin, modifiers, visibility);
      this.key = key;
      this.index = index;
      this.returnType = returnType;
      this.id = model.store().storeMethodNode(this);
      addRPDLNode();
      if (returnType != lookupVoidType())
      {
        addResultNode();
      }
      thrownExceptions().addAll(exceptions);
    }

    @Override
    public IDataNode addDataMember(final IDataNode data)
    {
      final SortedMap<Integer, IDataNode> dataMembers = (SortedMap<Integer, IDataNode>) dataMembers();
      final int index = dataMembers.isEmpty() ? 0 : dataMembers.lastKey() < 0 ? 0 : dataMembers
          .lastKey() + 1;
      ((DataNode) data).setIndex(index);
      dataMembers().put(index, data);
      return data;
    }

    @Override
    public IDataNode addDataMember(final String name, final int lineFrom, final int lineTo,
        final ITypeNodeRef type, final NodeOrigin origin, final Set<NodeModifier> modifiers,
        final NodeVisibility visibility, final IValue defaultValue)
    {
      return addDataMember(name, lineFrom, lineTo, type, origin, modifiers, visibility,
          NK_VARIABLE, model.valueFactory().createUninitializedValue());
    }

    @Override
    public IMethodDependenceGraph getDependenceGraph()
    {
      return this.mdg;
    }

    @Override
    public long id()
    {
      return this.id;
    }

    @Override
    public int index()
    {
      return index;
    }

    @Override
    public String key()
    {
      return key;
    }

    @Override
    public NodeKind kind()
    {
      return NK_METHOD;
    }

    @Override
    public Set<ITypeNode> localTypes()
    {
      return model.store().lookupTypeMembers(this);
    }

    @Override
    public IMethodNode node()
    {
      return this;
    }

    @Override
    public ITypeNode parent()
    {
      return (ITypeNode) super.parent();
    }

    @Override
    public ITypeNodeRef returnType()
    {
      return returnType;
    }

    @Override
    public void setDependenceGraph(final IMethodDependenceGraph mdg)
    {
      this.mdg = mdg;
    }

    @Override
    public Set<ITypeNodeRef> thrownExceptions()
    {
      return model.store().lookupMethodExceptions(this);
    }

    private void addResultNode()
    {
      final Set<NodeModifier> modifiers = TypeTools.newHashSet();
      modifiers.add(NodeModifier.NM_RESULT);
      addDataMember(StaticModelFactory.VAR_NAME_RESULT, lineFrom(), lineTo(), returnType,
          StaticModelFactory.VAR_INDEX_RESULT, NO_JIVE, modifiers, NV_LOCAL, NK_VARIABLE, model
              .valueFactory().createUninitializedValue());
    }

    private void addRPDLNode()
    {
      final Set<NodeModifier> modifiers = TypeTools.newHashSet();
      modifiers.add(NodeModifier.NM_RPDL);
      addDataMember(StaticModelFactory.VAR_NAME_RPDL, lineFrom(), lineTo(), lookupRPDLType(),
          StaticModelFactory.VAR_INDEX_RPDL, NO_JIVE, modifiers, NV_LOCAL, NK_VARIABLE, model
              .valueFactory().createUninitializedValue());
    }
  }

  private final class MethodNodeRef extends NodeRef implements IMethodNodeRef
  {
    private MethodNodeRef(final String key)
    {
      super(key);
    }

    @Override
    public IMethodNode node()
    {
      final IMethodNode node = lookupNode();
      if (node == null)
      {
        return null;
        // throw new IllegalArgumentException(String.format(
        // "The node reference does not point to a valid method node: %s!", key()));
      }
      return node;
    }

    @Override
    protected IMethodNode lookupNode()
    {
      return lookupMethodNode(key());
    }
  }

  private abstract class Node implements INode
  {
    private final int lineFrom;
    private final int lineTo;
    private final Set<NodeModifier> modifiers;
    private final String name;
    private final NodeOrigin origin;
    private final INode parentNode;
    private final NodeVisibility visibility;

    private Node(final INode parentNode, final String name, final int lineFrom, final int lineTo,
        final NodeOrigin origin, final Set<NodeModifier> modifiers, final NodeVisibility visibility)
    {
      this.lineFrom = lineFrom;
      this.lineTo = lineTo;
      this.modifiers = modifiers;
      this.name = name;
      this.origin = origin;
      this.parentNode = parentNode;
      this.visibility = visibility;
    }

    @Override
    public int lineFrom()
    {
      return lineFrom;
    }

    @Override
    public int lineTo()
    {
      return lineTo;
    }

    @Override
    public IExecutionModel model()
    {
      return model;
    }

    @Override
    public Set<NodeModifier> modifiers()
    {
      return this.modifiers;
    }

    @Override
    public String name()
    {
      return this.name;
    }

    @Override
    public NodeOrigin origin()
    {
      return this.origin;
    }

    @Override
    public INode parent()
    {
      return this.parentNode;
    }

    @Override
    public NodeVisibility visibility()
    {
      return this.visibility;
    }
  }

  private abstract class NodeRef implements INodeRef
  {
    private final long id;
    private final String key;

    private NodeRef(final String key)
    {
      if (key == null)
      {
        throw new IllegalArgumentException("Cannnot create a node reference with a null key.");
      }
      this.key = key;
      this.id = model.store().storeNodeRef(this);
    }

    @Override
    public long id()
    {
      return this.id;
    }

    @Override
    public String key()
    {
      return this.key;
    }

    @Override
    public String toString()
    {
      return this.key;
    }

    protected abstract INode lookupNode();
  }

  private final class RootNode extends Node implements IRootNode
  {
    private final long id;

    private RootNode()
    {
      super(null, "ROOT", -1, -1, NO_JIVE, StaticModelFactory.EMPTY_MODIFIERS,
          NodeVisibility.NV_PUBLIC);
      this.id = model.store().storeRootNode(this);
    }

    @Override
    public Set<IFileNode> files()
    {
      return model.store().lookupFiles();
    }

    @Override
    public long id()
    {
      return this.id;
    }

    @Override
    public NodeKind kind()
    {
      return NK_ROOT;
    }

    @Override
    public Set<ITypeNode> types()
    {
      return model.store().lookupNodeTypes(this);
    }
  }

  private final class TypeNode extends EnvironmentNode implements ITypeNode
  {
    private final IValue defaultValue;
    private final NodeKind kind;
    private final ITypeNodeRef superNode;
    private final IContainerNode container;
    private final long id;

    private TypeNode(final INode parent, final String name, final int lineFrom, final int lineTo,
        final NodeKind kind, final NodeOrigin origin, final Set<NodeModifier> modifiers,
        final NodeVisibility visibility, final ITypeNodeRef superNode,
        final Set<ITypeNodeRef> superInterfaceNodes, final IValue defaultValue)
    {
      super(parent, name, lineFrom, lineTo, origin, modifiers, visibility);
      this.kind = kind;
      // member type
      if (parent instanceof ITypeNode)
      {
        ((ITypeNode) parent).typeMembers().add(this);
      }
      // local and anonymous types
      else if (parent instanceof IMethodNode)
      {
        ((IMethodNode) parent).localTypes().add(this);
      }
      // there is no other way to associate a type node to its parent
      else if (!(parent instanceof IContainerNode))
      {
        throw new IllegalArgumentException(String.format(
            "Cannot create a type node for the static model with a parent of type '%s'.",
            parent == null ? "null" : parent.getClass().getName()));
      }
      this.container = resolveContainer(parent);
      // the container must be non-null
      if (this.container == null)
      {
        throw new IllegalArgumentException(
            "Cannot create a type node for the static model with a null container.");
      }
      this.superNode = superNode;
      this.defaultValue = defaultValue;
      this.id = model.store().storeTypeNode(this);
      // put this type under the correct container
      this.container.types().add(this);
      // add all super interfaces
      this.superInterfaces().addAll(superInterfaceNodes);
      // implicit immutable fields for arrays
      if (kind == NK_ARRAY)
      {
        // this array's declared component type
        ITypeNode componentType = lookupTypeNodeByName(name.substring(0, name.length() - 2));
        if (componentType == null)
        {
          componentType = lookupObjectType();
        }
        // this array's template member at position -2
        addDataMember("member", -1, -1, componentType, -2, NO_JIVE,
            StaticModelFactory.EMPTY_MODIFIERS, NV_PUBLIC, NK_FIELD, componentType.defaultValue());
        final ITypeNode intType = lookupTypeNode(KnownType.RT_INT.descriptor());
        // this array's length at position -1
        addDataMember("length", -1, -1, intType, -1, NO_JIVE, StaticModelFactory.EMPTY_MODIFIERS,
            NV_PUBLIC, NK_FIELD, intType.defaultValue());
      }
    }

    @Override
    public IDataNode addDataMember(final String name, final int lineFrom, final int lineTo,
        final ITypeNodeRef type, final NodeOrigin origin, final Set<NodeModifier> modifiers,
        final NodeVisibility visibility, final IValue defaultValue)
    {
      return addDataMember(name, lineFrom, lineTo, type, origin, modifiers, visibility, NK_FIELD,
          defaultValue);
    }

    @Override
    public IMethodNode addMethodMember(final String key, final String name, final int lineFrom,
        final int lineTo, final ITypeNodeRef returnType, final NodeOrigin origin,
        final Set<NodeModifier> modifiers, final NodeVisibility visibility,
        final Set<ITypeNodeRef> exceptions)
    {
      // System.out.println(key);
      if (StaticModelFactory.this.lookupMethodNode(key) == null)
      {
        int index = 0;
        /**
         * Initializers do no synchronize correctly with the Java runtime (i.e., there exists only
         * one <clinit> and one <init> in the program text, consolidated from all initializers in
         * the source). Here, we order the statically collected initializers with negative indices
         * so they do not affect the static-to-dynamic mapping. All other methods are sorted in
         * declaration order.
         * 
         * We have, however, to make sure the consolidate <clinit> maps to the correct index when
         * referenced from JDI.
         */
        final SortedMap<Integer, IMethodNode> methodMembers = (SortedMap<Integer, IMethodNode>) methodMembers();
        if (modifiers.contains(NM_FIELD_INITIALIZER) || modifiers.contains(NM_TYPE_INITIALIZER))
        {
          index = methodMembers.isEmpty() ? -1 : methodMembers.firstKey() - 1;
        }
        else
        {
          index = methodMembers.isEmpty() ? 0 : methodMembers.lastKey() + 1;
        }
        final MethodNode method = new MethodNode(key, this, name, lineFrom, lineTo, returnType,
            index, origin, modifiers, visibility, exceptions);
        addMember(method);
      }
      return StaticModelFactory.this.lookupMethodNode(key);
    }

    @Override
    public IContainerNode container()
    {
      return this.container;
    }

    @Override
    public IObjectContour createArrayContour(final long oid, final int length)
    {
      return ContourFactory.getDefault(model()).createArrayContour(this, oid, length);
    }

    @Override
    public IObjectContour createInstanceContour(final long oid)
    {
      return ContourFactory.getDefault(model()).createInstanceContour(this, oid);
    }

    @Override
    public IContextContour createStaticContour()
    {
      return ContourFactory.getDefault(model()).createStaticContour(this);
    }

    @Override
    public IValue defaultValue()
    {
      return this.defaultValue;
    }

    @Override
    public long id()
    {
      return this.id;
    }

    @Override
    public boolean isPrimitive()
    {
      return kind == NK_PRIMITIVE;
    }

    @Override
    public boolean isString()
    {
      return name().equals(KnownType.OT_STRING.typeName);
    }

    @Override
    public boolean isSuper(final ITypeNode superType)
    {
      if (superType == null)
      {
        return false;
      }
      final Set<ITypeNodeRef> superInterfaceNodes = superInterfaces();
      // the super is an interface
      if (superType.kind() == NK_INTERFACE)
      {
        // it's a parent interface
        for (final ITypeNodeRef intRef : superInterfaceNodes)
        {
          if (intRef.node() == superType)
          {
            return true;
          }
        }
        // it's the super of a parent interface
        for (final ITypeNodeRef intNode : superInterfaceNodes)
        {
          if (intNode.node() != null && intNode.node().isSuper(superType))
          {
            return true;
          }
        }
        // might be the super of the super class
        if (superClass() != null && superClass().node() != null)
        {
          return superClass().node().isSuper(superType);
        }
        return false;
      }
      // the super is a class (or enum)
      else
      {
        // the super class reference is unresolved
        if (superClass() == null || superClass().node() == null)
        {
          return false;
        }
        // it's the super class
        if (superClass() == superType)
        {
          return true;
        }
        // might be the super of the super class
        return superClass().node().isSuper(superType);
      }
    }

    @Override
    public String key()
    {
      return typeNameToSignature(name());
    }

    @Override
    public NodeKind kind()
    {
      return this.kind;
    }

    @Override
    public IDataNode lookupDataMember(final String name)
    {
      for (final IDataNode schema : dataMembers().values())
      {
        // ignore Jive specific fields
        if (schema.index() < 0)
        {
          continue;
        }
        if (schema.name().equals(name))
        {
          return schema;
        }
      }
      return null;
    }

    @Override
    public IContextContour lookupStaticContour()
    {
      return ContourFactory.getDefault(model()).lookupStaticContour(name());
    }

    @Override
    public Map<Integer, IMethodNode> methodMembers()
    {
      return model.store().lookupMethodMembers(this);
    }

    @Override
    public ITypeNode node()
    {
      return this;
    }

    @Override
    public ITypeNodeRef superClass()
    {
      return this.superNode;
    }

    @Override
    public Set<ITypeNodeRef> superInterfaces()
    {
      return model.store().lookupSuperInterfaces(this);
    }

    @Override
    public Set<ITypeNode> typeMembers()
    {
      return model.store().lookupTypeMembers(this);
    }

    private IContainerNode resolveContainer(final INode parentNode)
    {
      if (parentNode instanceof IContainerNode)
      {
        return (IContainerNode) parentNode;
      }
      return resolveContainer(parentNode.parent());
    }

    protected IMethodNode addMember(final MethodNode member)
    {
      final Map<Integer, IMethodNode> methodMembers = methodMembers();
      final IMethodNode method = methodMembers.get(member.index());
      if (method != null)
      {
        throw new IllegalArgumentException(String.format(
            "Cannot add method (%s) to the type at the requested index: index is already in use!",
            method.toString()));
      }
      methodMembers.put(member.index(), member);
      return member;
    }
  }

  private final class TypeNodeRef extends NodeRef implements ITypeNodeRef
  {
    private TypeNodeRef(final String key)
    {
      super(key);
    }

    @Override
    public String name()
    {
      return signatureToTypeName(super.key());
    }

    @Override
    public ITypeNode node()
    {
      return lookupNode();
    }

    @Override
    protected ITypeNode lookupNode()
    {
      return lookupTypeNode(key());
    }
  }
}