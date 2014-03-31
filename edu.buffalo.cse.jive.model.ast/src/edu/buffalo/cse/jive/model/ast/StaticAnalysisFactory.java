package edu.buffalo.cse.jive.model.ast;

import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_CONSTRUCTOR;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_FIELD_INITIALIZER;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_STATIC;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_SYNTHETIC;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_TYPE_INITIALIZER;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeOrigin.NO_AST;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility.NV_PACKAGE;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.buffalo.cse.jive.lib.StringTools;
import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IStaticModel.IDataNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNodeRef;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNodeRef;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.model.factory.IStaticAnalysisFactory;

public class StaticAnalysisFactory implements IStaticAnalysisFactory
{
  private final IExecutionModel model;

  public StaticAnalysisFactory(final IExecutionModel model)
  {
    this.model = model;
  }

  @Override
  public boolean bridges(final String bridgeKey, final String targetKey)
  {
    if (bridgeKey.equals(targetKey))
    {
      return true;
    }
    final String bridge = bridgeKey.substring(bridgeKey.lastIndexOf('.') + 1);
    final String target = targetKey.substring(targetKey.lastIndexOf('.') + 1);
    if (!bridge.equals(target))
    {
      return false;
    }
    final ITypeNode bridgeType = model.staticModelFactory().lookupTypeNode(
        bridgeKey.substring(0, bridgeKey.lastIndexOf('.')));
    final ITypeNode targetType = model.staticModelFactory().lookupTypeNode(
        targetKey.substring(0, targetKey.lastIndexOf('.')));
    return bridgeType.isSuper(targetType);
  }

  @Override
  public IExecutionModel model()
  {
    return model;
  }

  @Override
  public IMethodDependenceGraph newMethodDependenceGraph(final IResolvedLine parent)
  {
    return new MethodDependenceGraph(parent);
  }

  @Override
  public IResolvedCall newResolvedCall(final IMethodNodeRef ref, final int index,
      final IResolvedNode qualifierOf, final boolean isLHS, final boolean isActual)
  {
    return new ResolvedCall(ref, index, qualifierOf, isLHS, isActual);
  }

  @Override
  public IResolvedData newResolvedData(final IDataNode data, final int index,
      final IResolvedNode qualifierOf, final boolean isLHS, final boolean isActual,
      final boolean isDef)
  {
    return new ResolvedDataNode(data, index, qualifierOf, isLHS, isActual, isDef);
  }

  @Override
  public IResolvedData newResolvedData(final String typeKey, final String fieldName,
      final int index, final IResolvedNode qualifierOf, final boolean isLHS,
      final boolean isActual, final boolean isDef)
  {
    return new ResolvedLazyFieldNode(model.staticModelFactory().lookupTypeRef(typeKey), fieldName,
        index, qualifierOf, isLHS, isActual, isDef);
  }

  @Override
  public IResolvedLine newResolvedLine(final LineKind kind, final int lineNo,
      final IResolvedLine parent)
  {
    return new ResolvedLine(kind, lineNo, parent);
  }

  @Override
  public IResolvedThis newResolvedThis(final ITypeNode type, final int index,
      final IResolvedNode qualifierOf, final boolean isLHS, final boolean isActual)
  {
    return new ResolvedThisNode(type, index, qualifierOf, isLHS, isActual);
  }

  /**
   * Polymorphism is resolved here-- static analysis collects a node as a more general type/method
   * but dynamic analysis resolves to the actual type/method. For run-time type, try it first; then,
   * try the super class and all super interfaces; if it fails, try it recursively for the super
   * class and each super interface; the check consists in computing the run-time signature and
   * comparing against the method reference's signature; perhaps use "equals" to compare the keys
   * transparently somehow?
   * 
   * For example, a Comparable instance may resolve at run-time to an instance of MyCommparable. At
   * resolution time, the call node may fail to resolve the method node because the static type
   * (Comparable) is not in the AST. At run-time, the call will have the concrete signature of
   * MyObject.compareTo(), which will not resolve correctly to this method reference.
   */
  @Override
  public boolean overrides(final ITypeNode type, final IMethodNode method, final IResolvedCall node)
  {
    // no node
    if (node == null)
    {
      return false;
    }
    // no call
    if (node.call() == null)
    {
      return false;
    }
    // the input method's key
    final String thisMethodKey = method.key();
    // the unresolved reference
    final IMethodNodeRef ref = node.call();
    // reference points to a resolved method
    if (ref.node() != null)
    {
      final String refMethodKey = ref.node().key();
      if (overrides(method.key(), refMethodKey))
      {
        return true;
      }
      return methodSignaturesMatch(thisMethodKey, refMethodKey)
          && type.isSuper(ref.node().parent());
    }
    /**
     * Overrides uses the method's key, which points to the implementing (i.e., possibly inherited)
     * method. Some times, this will not suffice since an abstract base class may not implement an
     * interface but implement one of the interface methods. Then, a subclass inheriting from this
     * base class does not need to reimplement the method, it does so by inheritance. In these
     * cases, we have to look at the actual run-time type of the method's invoking context, as
     * opposed to the type which introduced the method.
     */
    final String refMethodKey = ref.key();
    if (overrides(thisMethodKey, refMethodKey))
    {
      return true;
    }
    final String refTypeKey = typeKeyFromMethodKey(refMethodKey);
    final ITypeNode refType = model.staticModelFactory().lookupTypeNode(refTypeKey);
    return methodSignaturesMatch(thisMethodKey, refMethodKey) && type.isSuper(refType);
  }

  /**
   * TODO: reduce the cyclomatic complexity.
   */
  @Override
  public void postProcessMDGs(final ITypeNode type)
  {
    final List<IMethodNode> clinits = TypeTools.newArrayList();
    final List<IMethodNode> inits = TypeTools.newArrayList();
    final List<IMethodNode> constructors = TypeTools.newArrayList();
    int lineFromClinit = -1;
    int lineToClinit = -1;
    int lineFromInit = -1;
    int lineToInit = -1;
    final Map<Integer, IMethodNode> methodMembers = type.methodMembers();
    for (final IMethodNode method : methodMembers.values())
    {
      if (method.modifiers().contains(NM_FIELD_INITIALIZER)
          || method.modifiers().contains(NM_TYPE_INITIALIZER))
      {
        // all <clinit@...> lines must be included on the synthetic <clinit> method node
        if (method.modifiers().contains(NM_STATIC))
        {
          clinits.add(method);
          // update line from
          if (lineFromClinit == -1)
          {
            lineFromClinit = method.lineFrom();
          }
          else if (lineFromClinit > method.lineFrom() && method.lineFrom() != -1)
          {
            lineFromClinit = method.lineFrom();
          }
          // update line to
          if (lineToClinit == -1)
          {
            lineToClinit = method.lineTo();
          }
          else if (lineToClinit < method.lineTo() && method.lineTo() != -1)
          {
            lineToClinit = method.lineTo();
          }
        }
        // all <init@...> lines must be included on every constructor
        else
        {
          inits.add(method);
          // update line from
          if (lineFromInit == -1)
          {
            lineFromInit = method.lineFrom();
          }
          else if (lineFromInit > method.lineFrom() && method.lineFrom() != -1)
          {
            lineFromInit = method.lineFrom();
          }
          // update line to
          if (lineToInit == -1)
          {
            lineToInit = method.lineTo();
          }
          else if (lineToInit < method.lineTo() && method.lineTo() != -1)
          {
            lineToInit = method.lineTo();
          }
        }
      }
      // every constructor must contain all <init@...> lines; if no constructors exist, creates a
      // synthetic default constructor node and adds all <init@...> lines to it
      else if (method.modifiers().contains(NM_CONSTRUCTOR))
      {
        constructors.add(method);
      }
    }
    final Set<ITypeNodeRef> exceptions = Collections.<ITypeNodeRef> emptySet();
    // create a synthetic <clinit> method if static type initializers exist or the type is an enum
    if (!clinits.isEmpty() || type.kind() == NodeKind.NK_ENUM)
    {
      final Set<NodeModifier> modifiers = TypeTools.newHashSet();
      modifiers.add(NM_STATIC);
      modifiers.add(NM_SYNTHETIC);
      modifiers.add(NM_TYPE_INITIALIZER);
      // Create the new synthetic type initializer.
      final IMethodNode newClinit = type.addMethodMember("L" + type.name().replace(".", "/")
          + ";.<clinit>()", "<clinit>", type.lineFrom(), lineToClinit, model.staticModelFactory()
          .lookupVoidType(), NO_AST, modifiers, NV_PACKAGE, exceptions);
      // create a method declaration for <clinit> starting at the type declaration
      final IResolvedLine clinitDecl = newResolvedLine(LineKind.LK_METHOD_DECLARATION,
          type.lineFrom(), null);
      // create a method dependence graph starting at the type declaration
      final IMethodDependenceGraph mdg = model.staticAnalysisFactory().newMethodDependenceGraph(
          null);
      // add the method declaration itself to the mdg
      mdg.dependenceMap().put(clinitDecl.lineNumber(), clinitDecl);
      // process all class initializers
      for (final IMethodNode clinit : clinits)
      {
        // get the mdg of the current <clinit>
        final IMethodDependenceGraph clinitMDG = clinit.getDependenceGraph();
        // add all dependent lines of the existing <clinit@...> to the consolidated mdg
        for (final Integer line : clinitMDG.dependenceMap().keySet())
        {
          final IResolvedLine l = clinitMDG.dependenceMap().get(line);
          mdg.dependenceMap().put(line, l);
        }
        // add all *real* data members to the new clinit
        for (final IDataNode data : clinit.dataMembers().values())
        {
          if (data.index() >= 0)
          {
            newClinit.addDataMember(data);
          }
        }
        // remove <clinit@...> from the method members
        methodMembers.remove(clinit.index());
      }
      // record the new mdg
      if (newClinit != null && mdg != null)
      {
        newClinit.setDependenceGraph(mdg);
      }
    }
    // consolidated (possibly synthetic) constructor containing all instance initializers
    if (!inits.isEmpty())
    {
      final Set<NodeModifier> modifiers = TypeTools.newHashSet();
      modifiers.add(NM_CONSTRUCTOR);
      if (constructors.isEmpty())
      {
        modifiers.add(NM_SYNTHETIC);
      }
      IMethodNode newInit = null;
      IMethodDependenceGraph mdg = null;
      for (final IMethodNode init : inits)
      {
        final IMethodDependenceGraph initMDG = init.getDependenceGraph();
        if (mdg == null)
        {
          if (constructors.isEmpty())
          {
            // if this class is non-static and the parent of the class is a class,
            // this is a synthetic method that takes in the parent class as an argument
            final String args = (!type.modifiers().contains(NodeModifier.NM_STATIC) && type
                .parent().kind() == NodeKind.NK_CLASS) ? model.staticModelFactory()
                .typeNameToSignature(type.parent().name()) : "";
            final String key = "L" + type.name().replace(".", "/") + ";."
                + constructorName(type.name()) + "(" + args + ")";
            newInit = type.addMethodMember(key, constructorName(type.name()), lineFromInit,
                lineToInit, init.returnType(), NO_AST, modifiers, init.visibility(), exceptions);
          }
          mdg = model.staticAnalysisFactory().newMethodDependenceGraph(initMDG.parent());
        }
        // add all dependent lines of the existing <init@...> to the consolidated mdg
        for (final Integer line : initMDG.dependenceMap().keySet())
        {
          mdg.dependenceMap().put(line, initMDG.dependenceMap().get(line));
        }
        // remove <init@...> from the method members
        methodMembers.remove(init.index());
      }
      // append the lines of the new mdg to the synthetic constructor
      if (newInit != null && mdg != null)
      {
        newInit.setDependenceGraph(mdg);
      }
      else
      {
        for (final IMethodNode constructor : constructors)
        {
          // add all dependent lines of the consolidated mdg to the constructor
          for (final Integer line : mdg.dependenceMap().keySet())
          {
            constructor.getDependenceGraph().dependenceMap()
                .put(line, mdg.dependenceMap().get(line));
          }
        }
      }
    }
    // default (synthetic) constructor
    else if (constructors.isEmpty())
    {
      final Set<NodeModifier> modifiers = TypeTools.newHashSet();
      modifiers.add(NM_CONSTRUCTOR);
      if (constructors.isEmpty())
      {
        modifiers.add(NM_SYNTHETIC);
      }
      // if this class is non-static and the parent of the class is a class,
      // this is a synthetic method that takes in the parent class as an argument
      final String args = (!type.modifiers().contains(NodeModifier.NM_STATIC) && type.parent()
          .kind() == NodeKind.NK_CLASS) ? model.staticModelFactory().typeNameToSignature(
          type.parent().name()) : "";
      final String key = "L" + type.name().replace(".", "/") + ";." + constructorName(type.name())
          + "(" + args + ")";
      type.addMethodMember(key, constructorName(type.name()), type.lineFrom(), type.lineFrom(),
          model.staticModelFactory().lookupVoidType(), NO_AST, modifiers, NV_PACKAGE, exceptions);
    }
  }

  private String constructorName(final String typeName)
  {
    String lastName = typeName;
    if (typeName.indexOf("$") != -1)
    {
      lastName = typeName.substring(typeName.lastIndexOf("$") + 1);
      try
      {
        Integer.parseInt(lastName);
        lastName = "<init>";
      }
      catch (final NumberFormatException nfe)
      {
        // not a number, so last name is an actual name
      }
    }
    else if (typeName.indexOf(".") != -1)
    {
      lastName = typeName.substring(typeName.lastIndexOf(".") + 1);
    }
    return lastName;
  }

  private boolean isSuperType(final String thisTypeKey, final String thatTypeKey)
  {
    final ITypeNode thisType = model.staticModelFactory().lookupTypeNode(thisTypeKey);
    final ITypeNode thatType = model.staticModelFactory().lookupTypeNode(thatTypeKey);
    return thisType != null && thatType != null && thisType.isSuper(thatType);
  }

  private boolean methodSignaturesMatch(final String thisMethodKey, final String thatMethodKey)
  {
    if (thisMethodKey.equals(thatMethodKey))
    {
      return true;
    }
    final String thisMethodSignature = thisMethodKey.substring(thisMethodKey.lastIndexOf('.') + 1);
    final String thatMethodSignature = thatMethodKey.substring(thatMethodKey.lastIndexOf('.') + 1);
    return thisMethodSignature.equals(thatMethodSignature);
  }

  /**
   * Checks whether method signatures match and whether the declaring type of one method (this) is a
   * descendant of the declaring type of the other method (that).
   */
  private boolean overrides(final String thisMethodKey, final String thatMethodKey)
  {
    if (!methodSignaturesMatch(thisMethodKey, thatMethodKey))
    {
      return false;
    }
    final String declaringTypeKey = typeKeyFromMethodKey(thisMethodKey);
    final String thatTypeKey = typeKeyFromMethodKey(thatMethodKey);
    return isSuperType(declaringTypeKey, thatTypeKey);
  }

  private String typeKeyFromMethodKey(final String methodKey)
  {
    return methodKey == null ? "" : methodKey.substring(0, methodKey.lastIndexOf('.'));
  }

  private abstract class AbstractResolvedCall extends AbstractResolvedNode implements IResolvedCall
  {
    private final List<List<IResolvedNode>> uses;

    AbstractResolvedCall(final int index, final IResolvedNode qualifierOf, final boolean isLHS,
        final boolean isActual)
    {
      super(index, qualifierOf, isLHS, isActual);
      this.uses = TypeTools.newArrayList();
    }

    @Override
    public void append(final List<IResolvedNode> uses)
    {
      this.uses.add(uses);
    }

    @Override
    public int size()
    {
      return uses.size();
    }

    @Override
    public String toString()
    {
      return StringTools.resolvedCallToString(this);
    }

    @Override
    public List<IResolvedNode> uses(final int index)
    {
      return uses.get(index);
    }
  }

  /**
   * An abstract resolved node defines equality and hash code based on its index. This means that
   * nodes uniquely identify a reference within a file. Resolved nodes from different files should
   * never be processed together-- i.e., all nodes representing a dependence should belong to the
   * same file.
   */
  private abstract class AbstractResolvedNode implements IResolvedNode
  {
    private final long id;
    private final Integer index;
    private final boolean isActual;
    private final boolean isLHS;
    private final IResolvedNode qualifierOf;

    AbstractResolvedNode(final int index, final IResolvedNode qualifierOf, final boolean isLHS,
        final boolean isActual)
    {
      this.index = index;
      this.isActual = isActual;
      this.isLHS = isLHS;
      this.qualifierOf = qualifierOf;
      this.id = model.store().storeResolvedNode(this);
    }

    @Override
    public int compareTo(final IResolvedNode other)
    {
      return index.compareTo(((AbstractResolvedNode) other).index);
    }

    @Override
    public boolean equals(final Object other)
    {
      return other instanceof AbstractResolvedNode && ((AbstractResolvedNode) other).id == id;
    }

    @Override
    public int hashCode()
    {
      return (int) id;
    }

    @Override
    public long id()
    {
      return this.id;
    }

    @Override
    public boolean isActual()
    {
      return isActual;
    }

    @Override
    public boolean isLHS()
    {
      return isLHS;
    }

    @Override
    public IResolvedNode qualifierOf()
    {
      return qualifierOf;
    }

    @Override
    public int sourceIndex()
    {
      return index;
    }
  }

  private final class MethodDependenceGraph implements IMethodDependenceGraph
  {
    private final Map<Integer, IResolvedLine> dependenceMap;
    private final ResolvedLine parent;
    private boolean checkedSystemExit;
    private boolean hasSystemExit;

    private MethodDependenceGraph(final IResolvedLine parent)
    {
      this.parent = (ResolvedLine) parent;
      this.dependenceMap = TypeTools.newLinkedHashMap();
      this.checkedSystemExit = false;
      this.hasSystemExit = false;
    }

    @Override
    public Map<Integer, IResolvedLine> dependenceMap()
    {
      return dependenceMap;
    }

    @Override
    public boolean hasSystemExit()
    {
      if (checkedSystemExit)
      {
        return hasSystemExit;
      }
      // avoid cycles in the resolution
      checkedSystemExit = true;
      // check each method line
      for (final IResolvedLine node : dependenceMap.values())
      {
        if (((ResolvedLine) node).hasSystemExit())
        {
          hasSystemExit = true;
          break;
        }
      }
      // check parent
      if (!hasSystemExit)
      {
        hasSystemExit = parent != null && parent.hasSystemExit();
      }
      // final result
      return hasSystemExit;
    }

    @Override
    public IResolvedLine parent()
    {
      return parent;
    }
  }

  private final class ResolvedCall extends AbstractResolvedCall
  {
    /**
     * Reference to a method declared within the source. This should be replaced with a IMethodNode
     * of the static model.
     */
    private final IMethodNodeRef ref;

    ResolvedCall(final IMethodNodeRef ref, final int index, final IResolvedNode qualifierOf,
        final boolean isLHS, final boolean isActual)
    {
      super(index, qualifierOf, isLHS, isActual);
      this.ref = ref;
    }

    @Override
    public IMethodNodeRef call()
    {
      return ref;
    }

    @Override
    public String methodName()
    {
      // resolved reference
      if (ref instanceof IMethodNode)
      {
        return ((IMethodNode) ref).name();
      }
      // lookup the resolved reference
      final IMethodNode node = model.staticModelFactory().lookupMethodNode(ref.key());
      // lookup the resolved reference
      return node == null ? ref.key().substring(0, ref.key().length() - 1) : node.name();
    }

    boolean isSystemExit()
    {
      return ref.key().equals("Ljava/lang/System;.exit(I)");
    }
  }

  /**
   * Try to make {@code hashCode} and {@code equals} consistent across {@code IResolvedVariable}
   * instances.
   */
  private final class ResolvedDataNode extends AbstractResolvedNode implements IResolvedData
  {
    private final IDataNode dataNode;
    private final boolean isDef;

    private ResolvedDataNode(final IDataNode dataNode, final int index,
        final IResolvedNode qualifierOf, final boolean isLHS, final boolean isActual,
        final boolean isDef)
    {
      super(index, qualifierOf, isLHS, isActual);
      if (dataNode == null)
      {
        throw new IllegalArgumentException("Invalid data node for a resolved data node.");
      }
      this.dataNode = dataNode;
      this.isDef = isDef;
    }

    @Override
    public IDataNode data()
    {
      return this.dataNode;
    }

    @Override
    public boolean isDef()
    {
      return this.isDef;
    }

    @Override
    public String name()
    {
      return dataNode.name();
    }

    @Override
    public String toString()
    {
      return StringTools.resolvedDataToString(this);
    }

    @Override
    public ITypeNodeRef type()
    {
      return dataNode.type();
    }
  }

  private final class ResolvedLazyFieldNode extends AbstractResolvedNode implements
      IResolvedLazyData
  {
    // determines whether this reference is a definition
    private final boolean isDef;
    // field name, for display and look up purposes
    private final String name;
    // declaring context
    private final ITypeNodeRef type;

    public ResolvedLazyFieldNode(final ITypeNodeRef type, final String name, final int index,
        final IResolvedNode qualifierOf, final boolean isLHS, final boolean isActual,
        final boolean isDef)
    {
      super(index, qualifierOf, isLHS, isActual);
      this.type = type;
      this.name = name;
      this.isDef = isDef;
    }

    @Override
    public IDataNode data()
    {
      final ITypeNode node = type.node();
      if (node == null)
      {
        return null;
      }
      // the field can be resolved by name within the type
      for (final IDataNode data : node.dataMembers().values())
      {
        if (data.name().equals(name))
        {
          return data;
        }
      }
      return null;
    }

    @Override
    public boolean isDef()
    {
      return this.isDef;
    }

    @Override
    public String name()
    {
      return this.name;
    }

    @Override
    public String toString()
    {
      return StringTools.resolvedLazyFieldToString(this);
    }

    @Override
    public ITypeNodeRef type()
    {
      return this.type;
    }
  }

  private final class ResolvedLine implements IResolvedLine
  {
    private boolean hasConditional;
    private final IResolvedLine parent;
    private final List<IResolvedNode> uses;
    private final List<IResolvedData> definitions;
    private final List<IResolvedLine> propagatedDependences;
    private final LineKind kind;
    private final int lineNo;

    ResolvedLine(final LineKind kind, final int lineNo, final IResolvedLine parent)
    {
      this.kind = kind;
      // this.hasAssignment = kind.isAssignment();
      this.lineNo = lineNo;
      this.parent = parent;
      this.uses = TypeTools.newArrayList();
      this.definitions = TypeTools.newArrayList();
      this.propagatedDependences = TypeTools.newArrayList();
    }

    @Override
    public List<IResolvedData> definitions()
    {
      return this.definitions;
    }

    @Override
    public boolean hasConditional()
    {
      return this.hasConditional;
    }

    @Override
    public boolean isControl()
    {
      return kind == LineKind.LK_CATCH || kind == LineKind.LK_CONDITIONAL || kind == LineKind.LK_DO
          || kind == LineKind.LK_ENHANCED_FOR || kind == LineKind.LK_FOR
          || kind == LineKind.LK_IF_THEN || kind == LineKind.LK_IF_THEN_ELSE
          || kind == LineKind.LK_SWITCH || kind == LineKind.LK_WHILE;
    }

    @Override
    public boolean isLoopControl()
    {
      return kind == LineKind.LK_DO || kind == LineKind.LK_ENHANCED_FOR || kind == LineKind.LK_FOR
          || kind == LineKind.LK_WHILE;
    }

    @Override
    public List<IResolvedLine> jumpDependences()
    {
      return this.propagatedDependences;
    }

    @Override
    public LineKind kind()
    {
      return this.kind;
    }

    @Override
    public int lineNumber()
    {
      return this.lineNo;
    }

    @Override
    public void merge(final IResolvedLine source)
    {
      uses.addAll(source.uses());
      definitions.addAll(source.definitions());
      // this line is a direct assignment or is merged with a line that has an assignment
      // hasAssignment = hasAssignment || source.hasAssignment();
    }

    @Override
    public IResolvedLine parent()
    {
      return this.parent;
    }

    @Override
    public void setHasConditional()
    {
      this.hasConditional = true;
    }

    @Override
    public String toString()
    {
      return StringTools.resolvedLineToString(this);
    }

    @Override
    public List<IResolvedNode> uses()
    {
      return this.uses;
    }

    boolean hasSystemExit()
    {
      // pass one-- check actual call nodes for the system exit flag
      for (final IResolvedNode node : uses)
      {
        if (node instanceof IResolvedCall && ((ResolvedCall) node).isSystemExit())
        {
          return true;
        }
      }
      // pass two-- traverse other MDGs recursively looking for the system exit flag
      for (final IResolvedNode node : uses)
      {
        if (!(node instanceof IResolvedCall))
        {
          continue;
        }
        final ResolvedCall rcall = (ResolvedCall) node;
        if (rcall.call().node() != null)
        {
          final IMethodDependenceGraph callee = ((ResolvedCall) node).call().node()
              .getDependenceGraph();
          if (callee != null && ((MethodDependenceGraph) callee).hasSystemExit())
          {
            return true;
          }
        }
      }
      return false;
    }
  }

  /**
   * Try to make {@code hashCode} and {@code equals} consistent across {@code IResolvedVariable}
   * instances.
   */
  private final class ResolvedThisNode extends AbstractResolvedNode implements IResolvedThis
  {
    private final ITypeNode typeNode;

    private ResolvedThisNode(final ITypeNode typeNode, final int index,
        final IResolvedNode qualifierOf, final boolean isLHS, final boolean isActual)
    {
      super(index, qualifierOf, isLHS, isActual);
      if (typeNode == null)
      {
        throw new IllegalArgumentException("Invalid type node for a resolved 'this' node.");
      }
      this.typeNode = typeNode;
    }

    @Override
    public String toString()
    {
      return StringTools.resolvedThisToString(this);
    }

    @Override
    public ITypeNode type()
    {
      return this.typeNode;
    }
  }
}
