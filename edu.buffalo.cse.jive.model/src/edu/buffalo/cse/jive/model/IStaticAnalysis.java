package edu.buffalo.cse.jive.model;

import java.util.List;
import java.util.Map;

import edu.buffalo.cse.jive.model.IStaticModel.IDataNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNodeRef;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNodeRef;

/**
 * A tree-based static representation of the program, partially constructed from the program's AST
 * and partially from the debugger API.
 */
public interface IStaticAnalysis extends IModel
{
  public interface IMethodDependenceGraph
  {
    /**
     * Relationship method (TODO: push to store).
     */
    public Map<Integer, IResolvedLine> dependenceMap();

    /**
     * Data method.
     */
    public boolean hasSystemExit();

    /**
     * Data method.
     */
    public IResolvedLine parent();
  }

  /**
   * A resolved call represents a method invocation in the source. These are not only limited to
   * conventional method calls and can also represent contructor invocation, super constructor
   * invocation, super method invocation, class initializers, field initializers, etc. Each argument
   * of the method call may be an expression involving any number of data and call dependences.
   */
  public interface IResolvedCall extends IResolvedNode
  {
    /**
     * Business action method (TODO: push to store?).
     */
    public void append(List<IResolvedNode> uses);

    /**
     * Data method.
     */
    public IMethodNodeRef call();

    /**
     * Data method.
     */
    public String methodName();

    /**
     * Data method.
     */
    public int size();

    /**
     * Lookup method (TODO: push to store?).
     */
    public List<IResolvedNode> uses(int index);
  }

  /**
   * A resolved data represents a reference to a local variable or field in the source.
   */
  public interface IResolvedData extends IResolvedNode
  {
    /**
     * Data method.
     */
    public IDataNode data();

    /**
     * Data method.
     */
    public boolean isDef();

    /**
     * Lookup method (TODO: push to store).
     */
    public String name();

    /**
     * Lookup method (TODO: push to store).
     */
    public ITypeNodeRef type();
  }

  /**
   * A resolved data represents a reference to a local variable or field in the source.
   */
  public interface IResolvedLazyData extends IResolvedData
  {
    /**
     * Data method.
     */
    @Override
    public IDataNode data();

    /**
     * Data method.
     */
    @Override
    public boolean isDef();

    /**
     * Data method.
     */
    @Override
    public String name();

    /**
     * Data method.
     */
    @Override
    public ITypeNodeRef type();
  }

  /**
   * A resolved line consists of definitions, call dependences, control dependences, and data
   * dependences. Typically, a source line either defines a new value for a variable of field, or it
   * controls the flow of execution.
   */
  public interface IResolvedLine
  {
    /**
     * Lookup method (TODO: push to store).
     * 
     * Data elements defined on this line.
     */
    public List<IResolvedData> definitions();

    /**
     * Data method.
     * 
     * Indicates if this line has one or more conditional expressions.
     */
    public boolean hasConditional();

    /**
     * Query method (TODO: push to store?).
     * 
     * Determines whether this line is a control statement.
     */
    public boolean isControl();

    /**
     * Query method (TODO: push to store?).
     * 
     * Determines whether this line is a loop control statement.
     */
    public boolean isLoopControl();

    /**
     * Lookup method (TODO: push to store).
     * 
     * Non-structured control-flow lines on which this line depends (e.g., break, continue, return).
     */
    public List<IResolvedLine> jumpDependences();

    /**
     * Data method.
     * 
     * Kind of this dependence-- allows fine-grained control of the analysis.
     */
    public LineKind kind();

    /**
     * Data method.
     */
    public int lineNumber();

    /**
     * Business action method (TODO: push to store).
     */
    public void merge(IResolvedLine source);

    /**
     * Data method.
     * 
     * Parent node of this line-- either a resolved line representing a control-flow parent, or a
     * resolved method corresponding to the method (or initializer) containing this line.
     */
    public IResolvedLine parent();

    /**
     * Business action method (TODO: push to store).
     * 
     * Used by the visitor, flags the line as having a conditional expression.
     */
    public void setHasConditional();

    /**
     * Lookup method (TODO: push to store).
     * 
     * Ordered list of variables, fields, and method calls used in this line.
     */
    public List<IResolvedNode> uses();
  }

  /**
   * A resolved node represents a source element that is executable (i.e., method call) or part of
   * an executable element (i.e., variable or field reference). This includes method declarations,
   * lines within method bodies, variable and field references within source lines, and method calls
   * within source lines.
   */
  public interface IResolvedNode extends Comparable<IResolvedNode>
  {
    /**
     * Data method.
     */
    public long id();

    /**
     * Data method.
     * 
     * True if this node appears in an actual parameter expression of a method call.
     */
    public boolean isActual();

    /**
     * Data method.
     * 
     * True if this node appears on the left hand side of an assignment expression.
     */
    public boolean isLHS();

    /**
     * Data method.
     * 
     * The source node to which this node acts as a qualifier, if any. For example,
     * 
     * <pre>
     * method().field = 10;
     * </pre>
     * 
     * The node representing the call to {@code method} should have a non-null {@code qualifierOf}
     * member representing the field {@code field}. In this particular case, both {@code method} and
     * {@code field} also have their left hand side flag set. Further, {@code field} is a def but
     * {@code method} is not.
     */
    public IResolvedNode qualifierOf();

    /**
     * Data method.
     * 
     * Position within the source file in which this node appears.
     */
    public int sourceIndex();
  }

  /**
   * A resolved this reference is a reference to the "this" keyword in an expression.
   */
  public interface IResolvedThis extends IResolvedNode
  {
    /**
     * Data method.
     */
    public ITypeNode type();
  }

  public enum LineKind
  {
    LK_ASSIGNMENT("assignment"),
    LK_ASSIGNMENT_ARRAY_CELL("assignment to array cell"),
    LK_BREAK("break"),
    LK_CATCH("catch"),
    LK_CLASS_INSTANCE_CREATION("new class instance"),
    LK_CONDITIONAL("conditional"),
    LK_CONSTRUCTOR_INVOCATION("this"),
    LK_CONTINUE("continue"),
    LK_DO("do-while"),
    LK_DO_NOOP("do-noop"),
    LK_ENHANCED_FOR("efor"),
    LK_ENUM_CONSTANT_DECLARATION("enum constant declaration"),
    LK_FIELD_DECLARATION("field declaration"),
    LK_FOR("for"),
    LK_IF_THEN("if-then"),
    LK_IF_THEN_ELSE("if-then-else"),
    LK_INITIALIZER("initializer"),
    LK_METHOD_CALL("call"),
    LK_METHOD_DECLARATION("method declaration"),
    LK_POSTFIX("postfix expression"),
    LK_POSTFIX_ARRAY_CELL("postfix expression to array cell"),
    LK_PREFIX("prefix expression"),
    LK_PREFIX_ARRAY_CELL("prefix expression to array cell"),
    LK_RETURN("return"),
    LK_RETURN_IN_TRY("return in try"),
    LK_SUPER_CONSTRUCTOR("super"),
    LK_SUPER_METHOD_CALL("super call"),
    LK_SWITCH("switch"),
    LK_SWITCH_CASE("switch-case"),
    LK_SYNCHRONIZED("synchronized"),
    LK_THROW("throw"),
    LK_VARIABLE_DECLARATION("variable declaration"),
    LK_WHILE("while");
    private final String kind;

    private LineKind(final String kind)
    {
      this.kind = kind;
    }

    /**
     * Kinds that directly assign to an array cell.
     */
    public boolean isArrayCellAssignment()
    {
      return (this == LK_ASSIGNMENT_ARRAY_CELL) || (this == LK_POSTFIX_ARRAY_CELL)
          || (this == LK_PREFIX_ARRAY_CELL);
    }

    /**
     * Kinds that directly assign to a variable or field.
     */
    public boolean isAssignment()
    {
      return (this == LK_ASSIGNMENT) || (this == LK_ASSIGNMENT_ARRAY_CELL) || (this == LK_CATCH)
          || (this == LK_ENHANCED_FOR) || (this == LK_ENUM_CONSTANT_DECLARATION)
          || (this == LK_FIELD_DECLARATION) || (this == LK_FOR) || (this == LK_POSTFIX)
          || (this == LK_POSTFIX_ARRAY_CELL) || (this == LK_PREFIX)
          || (this == LK_PREFIX_ARRAY_CELL) || (this == LK_RETURN)
          || (this == LK_VARIABLE_DECLARATION);
    }

    public boolean isReturn()
    {
      return this == LK_RETURN || this == LK_RETURN_IN_TRY;
    }

    @Override
    public String toString()
    {
      return kind;
    }
  }
}