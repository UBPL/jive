package edu.buffalo.cse.jive.model;

import java.util.Map;
import java.util.Set;

import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IObjectContour;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IMethodDependenceGraph;

/**
 * A tree-based static representation of the program, partially constructed from the program's AST
 * and partially from the debugger API.
 */
public interface IStaticModel extends IModel
{
  public interface IContainerNode extends INode
  {
    /**
     * Lookup method (TODO: push to store).
     * 
     * All top-level types known declared in this file.
     */
    public Set<ITypeNode> types();
  }

  public interface IDataNode extends INode
  {
    /**
     * Data method.
     */
    public IValue defaultValue();

    /**
     * Data method.
     */
    public int index();

    /**
     * Data method.
     */
    @Override
    public IEnvironmentNode parent();

    /**
     * Data method.
     * 
     * This node's type. For fields, it is the declared type, for methods it is the return type, and
     * for enum constants it is the enum type.
     */
    public ITypeNodeRef type();
  }

  public interface IEnvironmentNode extends INode
  {
    /**
     * Business action method (TODO: push to store).
     */
    public IDataNode addDataMember(String name, int lineFrom, int lineTo, ITypeNodeRef typeNode,
        NodeOrigin origin, Set<NodeModifier> modifiers, NodeVisibility visibility,
        IValue defaultValue);

    /**
     * Lookup method (TODO: push to store).
     * 
     * Data member methods declared within the this environment.
     * 
     * @return a possibly empty map with all data members declared in this environment
     */
    public Map<Integer, IDataNode> dataMembers();
  }

  public interface IFileNode extends IContainerNode
  {
    /**
     * Data method.
     */
    @Override
    public IRootNode parent();
  }

  /**
   * A method node represents a method declaration or initializer within the source. It defines any
   * number of types and variables (some of which may be arguments), and may throw any number of
   * exceptions. As a resolved node, a method consists of a collection of resolved lines, one per
   * source code line in the method's body. A strong assumption is made: the source code is properly
   * formatted such that each line consists of exactly one statement.
   */
  public interface IMethodNode extends IEnvironmentNode, IMethodNodeRef
  {
    /**
     * Business action method (TODO: push to store).
     */
    public IDataNode addDataMember(IDataNode data);

    /**
     * Lookup method (TODO: push to store).
     */
    public IMethodDependenceGraph getDependenceGraph();

    /**
     * Data method.
     */
    public int index();

    /**
     * Lookup method (TODO: push to store).
     * 
     * List of type nodes corresponding to local and anonymous types in this method.
     */
    public Set<ITypeNode> localTypes();

    /**
     * Data method.
     */
    @Override
    public ITypeNode parent();

    /**
     * Data method.
     */
    public ITypeNodeRef returnType();

    /**
     * Business action method (TODO: push to store).
     */
    public void setDependenceGraph(IMethodDependenceGraph mdg);

    /**
     * Lookup method (TODO: push to store).
     * 
     * List of type nodes corresponding to the exceptions possibly thrown by this method.
     */
    public Set<ITypeNodeRef> thrownExceptions();
  }

  public interface IMethodNodeRef extends INodeRef
  {
    /**
     * Data method.
     */
    @Override
    public IMethodNode node();
  }

  public interface INode extends IModel
  {
    /**
     * Data method.
     * 
     * Unique identifier of this node across the tree.
     */
    public long id();

    /**
     * Data method.
     * 
     * This node's kind.
     */
    public NodeKind kind();

    /**
     * Data method.
     * 
     * Line at which this node's associated code starts within the source. In the case of local
     * variables, this determines the start of the lexical scope within which the variable is
     * visible.
     */
    public int lineFrom();

    /**
     * Data method.
     * 
     * Line at which this node's associated code ends within the source. In the case of local
     * variables, this determines the end of the lexical scope within which the variable is visible.
     */
    public int lineTo();

    /**
     * Data method.
     * 
     * Modifiers associated with this node.
     */
    public Set<NodeModifier> modifiers();

    /**
     * Data method.
     * 
     * Name of this node, not necessarily unique across the tree.
     */
    public String name();

    /**
     * Data method.
     * 
     * Indicates whether this is a node created from the AST, JDI, or inferred from out-of-model.
     */
    public NodeOrigin origin();

    /**
     * Data method.
     * 
     * This node's lexical parent. For a type node, this is either the file, a type node, or a
     * method node. For a method or field node, this is always a type node. For a variable node,
     * this is always a method node.
     */
    public INode parent();

    /**
     * Data method.
     * 
     * This node's parent.
     */
    public NodeVisibility visibility();
  }

  /**
   * Lazy reference to INode values, to be used during dynamic resolution in order to avoid problems
   * with type loading times cyclic type references. Also used in the static analysis since method
   * calls and variable names in the source are essentially references to particular data and method
   * nodes, with additional information about the location of the reference.
   */
  public interface INodeRef
  {
    /**
     * Data method.
     */
    public long id();

    /**
     * Data method.
     */
    public String key();

    /**
     * Lookup method (TODO: push to store).
     */
    public INode node();
  }

  public interface IRootNode extends IContainerNode
  {
    /**
     * Lookup method (TODO: push to store).
     * 
     * All top-level file nodes known to this root.
     */
    public Set<IFileNode> files();
  }

  /**
   * Top-Level type: named type not enclosed by a type declaration.
   * 
   * Nested type: all types that are not top-level types.
   * 
   * Member type: named nested type declared within a type declaration.
   * 
   * Inner type: non-static nested type declared within a type declaration, method/constructor body,
   * or as part of an expression or statement. Some member types are inner types but not all inner
   * types are member types.
   * 
   * Local type: inner type declared within a method/constructor body.
   * 
   * Anonymous class: inner type declared and instantiated within a an expression or statement.
   */
  public interface ITypeNode extends IEnvironmentNode, ITypeNodeRef
  {
    public IMethodNode addMethodMember(String key, String name, int lineFrom, int lineTo,
        ITypeNodeRef typeNode, NodeOrigin origin, Set<NodeModifier> modifiers,
        NodeVisibility visibility, Set<ITypeNodeRef> exceptions);

    /**
     * Data method.
     * 
     * The source container of a type is either the root node or a file node.
     * 
     * @return non-null node representing the file within which the type is declared or the root
     *         node if the file node is unknown or is unavailable.
     */
    public IContainerNode container();

    /**
     * Business action method (TODO: push to store).
     * 
     * Creates an element of the dynamic model using this node's non-static field information.
     */
    public IObjectContour createArrayContour(long oid, int length);

    /**
     * Business action method (TODO: push to store).
     * 
     * Creates an element of the dynamic model using this node's non-static field information.
     */
    public IObjectContour createInstanceContour(long oid);

    /**
     * Business action method (TODO: push to store).
     * 
     * Creates an element of the dynamic model using this node's static field information.
     */
    public IContextContour createStaticContour();

    /**
     * Data method.
     * 
     * Default value of the type associated with this node.
     * 
     * @return a string encoding the default value of this type
     */
    public IValue defaultValue();

    /**
     * Query method (TODO: push to store).
     * 
     * One of the Java primitive types.
     */
    public boolean isPrimitive();

    /**
     * Query method (TODO: push to store).
     */
    public boolean isString();

    /**
     * Query method (TODO: push to store).
     * 
     * Determines whether the given input type is a super type of the current type.
     */
    public boolean isSuper(ITypeNode superType);

    /**
     * Lookup method (TODO: push to store).
     * 
     * Looks up a declared field by name.
     */
    public IDataNode lookupDataMember(String name);

    /**
     * Lookup method (TODO: push to store).
     */
    public IContextContour lookupStaticContour();

    /**
     * Lookup method (TODO: push to store).
     * 
     * Member methods declared within the body of this type.
     * 
     * @return a possibly empty list with all member methods declared by this type
     */
    public Map<Integer, IMethodNode> methodMembers();

    /**
     * Data method.
     * 
     * @return super class reference
     */
    public ITypeNodeRef superClass();

    /**
     * Lookup method (TODO: push to store).
     * 
     * @return a possibly empty list with all interfaces directly implemented by this type
     */
    public Set<ITypeNodeRef> superInterfaces();

    /**
     * Lookup method (TODO: push to store).
     * 
     * Member types declared within the body of this type.
     * 
     * @return a possibly empty list with all member types declared by this type
     */
    public Set<ITypeNode> typeMembers();
  }

  public interface ITypeNodeRef extends INodeRef
  {
    /**
     * Data method.
     */
    public String name();

    /**
     * Lookup method (TODO: push to store).
     */
    @Override
    public ITypeNode node();
  }

  // kind of the element described by the node
  public enum NodeKind
  {
    NK_ARRAY,
    NK_CLASS,
    NK_ENUM,
    NK_FIELD,
    NK_FILE,
    NK_INTERFACE,
    NK_METHOD,
    NK_PRIMITIVE,
    NK_ROOT,
    NK_VARIABLE;
  }

  // modifiers of the element described by the node
  public enum NodeModifier
  {
    // classes, methods
    NM_ABSTRACT,
    // classes
    NM_ANONYMOUS,
    // variables
    NM_ARGUMENT,
    // methods
    NM_BRIDGE,
    // variables
    NM_CATCH_VARIABLE,
    // fields
    NM_COMPILE_TIME_FINAL,
    // methods
    NM_CONSTRUCTOR,
    // fields
    NM_ENUM_CONSTANT,
    // methods
    NM_FIELD_INITIALIZER,
    // classes, methods, fields, variables
    NM_FINAL,
    // classes
    NM_LOCAL,
    // methods
    NM_NATIVE,
    // JIVE variable
    NM_RESULT,
    // JIVE variable
    NM_RPDL,
    // classes, methods, fields
    NM_STATIC,
    // methods, blocks
    NM_SYNCHRONIZED,
    // methods, fields
    NM_SYNTHETIC,
    // fields
    NM_TRANSIENT,
    // methods
    NM_TYPE_INITIALIZER,
    // fields
    NM_VOLATILE, ;
    public static String toString(final Set<NodeModifier> modifiers)
    {
      final StringBuffer buffer = new StringBuffer("");
      buffer.append("{");
      int i = 0;
      for (final NodeModifier mod : modifiers)
      {
        buffer.append(mod.ordinal() + 1);
        i++;
        if (i < modifiers.size())
        {
          buffer.append(",");
        }
      }
      buffer.append("}");
      return buffer.toString();
    }
  }

  // creation origin of the element described by the node
  public enum NodeOrigin
  {
    // obtained from AST
    NO_AST,
    // obtained from JDI
    NO_JDI,
    // generated by Jive
    NO_JIVE;
  }

  // visibility scope of the element described by the node
  public enum NodeVisibility
  {
    NV_LOCAL('$'),
    NV_PACKAGE('~'),
    NV_PRIVATE('-'),
    NV_PROTECTED('#'),
    NV_PUBLIC('+');
    private final char marker;

    private NodeVisibility(final char marker)
    {
      this.marker = marker;
    }

    public String description()
    {
      return name().toLowerCase().substring(3);
    }

    public char marker()
    {
      return this.marker;
    }
  }
}