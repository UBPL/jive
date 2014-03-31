package edu.buffalo.cse.jive.internal.core.ast;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;

import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedCall;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedData;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedNode;

/**
 * Statement:
 * 
 * <pre>
 *     AssertStatement                 NOT supported
 *     Block                           NOT supported
 *     BreakStatement                  NOT supported
 *     ConstructorInvocation           supported (entry point)
 *     ContinueStatement               NOT supported
 *     DoStatement                     NOT supported
 *     EmptyStatement                  transparent
 *     EnhancedForStatement            NOT supported
 *     ExpressionStatement             NOT supported
 *     ForStatement                    NOT supported
 *     IfStatement                     NOT supported
 *     LabeledStatement                NOT supported
 *     ReturnStatement                 NOT supported
 *     SuperConstructorInvocation      supported (entry point)
 *     SwitchCase                      NOT supported
 *     SwitchStatement                 NOT supported
 *     SynchronizedStatement           NOT supported 
 *     ThrowStatement                  NOT supported
 *     TryStatement                    NOT supported
 *     TypeDeclarationStatement        NOT supported
 *     VariableDeclarationStatement    NOT supported
 *     WhileStatement                  NOT supported
 * </pre>
 * 
 * Expression:
 * 
 * <pre>
 *     ArrayAccess                     NOT supported
 *     ArrayCreation                   NOT supported
 *     ArrayInitializer                NOT supported
 *     Assignment                      NOT supported
 *     BooleanLiteral                  NOT supported
 *     CastExpression                  NOT supported
 *     CharacterLiteral                NOT supported
 *     ClassInstanceCreation           supported (entry point)
 *     ConditionalExpression           NOT supported
 *     FieldAccess                     supported
 *     InfixExpression                 NOT supported
 *     InstanceofExpression            NOT supported
 *     MethodInvocation                supported (entry point)
 *     Name                            supported (simple)
 *     NullLiteral                     NOT supported
 *     NumberLiteral                   NOT supported
 *     ParenthesizedExpression         supported
 *     PostfixExpression               NOT supported
 *     PrefixExpression                NOT supported
 *     StringLiteral                   NOT supported
 *     SuperFieldAccess                supported
 *     SuperMethodInvocation           supported (entry point)
 *     ThisExpression                  supported
 *     TypeLiteral                     NOT supported
 *     VariableDeclarationExpression   NOT supported
 * </pre>
 * 
 * Other:
 * 
 * <pre>
 *     CatchClause                     NOT supported
 *     EnumDeclaration                 NOT supported
 *     FieldDeclaration                NOT supported
 *     Initializer                     NOT supported
 *     MethodDeclaration               NOT supported
 * </pre>
 */
final class ExpressionVisitor extends ASTVisitor
{
  private final ResolvedFactory factory;
  /**
   * Determines if this visitor is currently visiting an actual argument position.
   */
  private final boolean isActual;
  /**
   * Determines if this visitor is currently visiting an expression of the left hand side.
   */
  private final boolean isLHS;
  private final IResolvedNode qualifierOf;
  private boolean hasConditional;
  /**
   * Ordered list of variables, fields, and method calls used in the expression.
   */
  private final List<IResolvedNode> uses;

  private ExpressionVisitor(final ResolvedFactory factory, final boolean isLHS,
      final boolean isActual, final IResolvedNode qualifierOf)
  {
    this.factory = factory;
    this.hasConditional = false;
    this.isActual = isActual;
    this.isLHS = isLHS;
    this.qualifierOf = qualifierOf;
    this.uses = new LinkedList<IResolvedNode>();
  }

  ExpressionVisitor(final ResolvedFactory factory, final boolean isLHS,
      final IResolvedNode qualifierOf)
  {
    this(factory, isLHS, false, qualifierOf);
  }

  /**
   * [expression '.'] 'new' [<type..>] type( arguments ) anonymousClassDeclaration
   */
  @Override
  public boolean visit(final ClassInstanceCreation node)
  {
    // method binding for this creation
    final IMethodBinding method = node.resolveConstructorBinding().getMethodDeclaration();
    // create the resolved call
    final IResolvedCall rcall = createResolvedCall(node, node.getType().getStartPosition(), method);
    // process the caller expression
    processRecursive(node.getExpression(), rcall);
    // process the arguments
    if (rcall != null)
    {
      // process the actuals of this instance creation
      processActualParameters(node, node.getType().getStartPosition(), rcall, node.arguments());
      // append the call after all its arguments
      uses.add(rcall);
    }
    // do not visit children
    return false;
  }

  /**
   * expression '?' thenStatement ':' elseStatement ';'
   */
  @Override
  public boolean visit(final ConditionalExpression node)
  {
    // this expression involves a conditional expression
    hasConditional = true;
    // visit the children
    return true;
  }

  /**
   * [<type..>] this( arguments )
   */
  @Override
  public boolean visit(final ConstructorInvocation node)
  {
    // method binding for this creation
    final IMethodBinding method = node.resolveConstructorBinding().getMethodDeclaration();
    // create the resolved call
    final IResolvedCall rcall = createResolvedCall(node, node.getStartPosition(), method);
    // process the arguments
    if (rcall != null)
    {
      // actual-ins
      processActualParameters(node, node.getStartPosition(), rcall, node.arguments());
      // append the call after all its arguments
      uses.add(rcall);
    }
    // do not visit children
    return false;
  }

  @Override
  public boolean visit(final EnumConstantDeclaration node)
  {
    // method binding for this creation (with *formal* argument types)
    final IMethodBinding method = node.resolveConstructorBinding().getMethodDeclaration();
    // create the resolved call
    final IResolvedCall rcall = createResolvedCall(node, node.getName().getStartPosition(), method);
    // process the arguments
    if (rcall != null)
    {
      // actual-ins
      processActualParameters(node, node.getName().getStartPosition(), rcall, node.arguments());
      // append the call after all its arguments
      uses.add(rcall);
    }
    // do not visit children
    return false;
  }

  @Override
  public boolean visit(final EnumDeclaration node)
  {
    // do not visit children
    return false;
  }

  /**
   * [expression '.'] name
   */
  @Override
  public boolean visit(final FieldAccess node)
  {
    // collect this reference
    final IResolvedNode rnode = processNode(node);
    // process the qualifier expression
    processRecursive(node.getExpression(), rnode);
    // collect variable reference
    if (rnode != null)
    {
      uses.add(rnode);
    }
    // visit children
    return false;
  }

  @Override
  public boolean visit(final MethodDeclaration node)
  {
    // do not visit children
    return false;
  }

  @Override
  public boolean visit(final MethodInvocation node)
  {
    // method binding for this creation (with *formal* argument types)
    final IMethodBinding method = node.resolveMethodBinding().getMethodDeclaration();
    // create the resolved call
    final IResolvedCall rcall = createResolvedCall(node, node.getName().getStartPosition(), method);
    // process the caller expression
    processRecursive(node.getExpression(), rcall);
    // process the arguments
    if (rcall != null)
    {
      // actual-ins
      processActualParameters(node, node.getName().getStartPosition(), rcall, node.arguments());
      // append the call after all its arguments
      uses.add(rcall);
    }
    // do not visit children
    return false;
  }

  @Override
  public boolean visit(final SimpleName node)
  {
    // collect this reference
    final IResolvedNode rnode = processNode(node);
    // collect variable reference
    if (rnode != null)
    {
      uses.add(rnode);
    }
    // visit children
    return false;
  }

  @Override
  public boolean visit(final SuperConstructorInvocation node)
  {
    // method binding for this creation
    final IMethodBinding method = node.resolveConstructorBinding().getMethodDeclaration();
    // create the resolved call
    final IResolvedCall rcall = createResolvedCall(node, node.getStartPosition(), method);
    // process the arguments
    if (rcall != null)
    {
      // actual-ins
      processActualParameters(node, node.getStartPosition(), rcall, node.arguments());
      // append the call after all its arguments
      uses.add(rcall);
    }
    // do not visit children
    return false;
  }

  /**
   * [ ClassName '.' ] 'super' '.' Identifier
   */
  @Override
  public boolean visit(final SuperFieldAccess node)
  {
    // collect this reference
    final IResolvedNode rnode = processNode(node.getName());
    // collect variable reference
    if (rnode != null)
    {
      uses.add(rnode);
    }
    // visit children
    return false;
  }

  @Override
  public boolean visit(final SuperMethodInvocation node)
  {
    // method binding for this creation
    final IMethodBinding method = node.resolveMethodBinding().getMethodDeclaration();
    // create the resolved call
    final IResolvedCall rcall = createResolvedCall(node, node.getName().getStartPosition(), method);
    // process the arguments
    if (rcall != null)
    {
      // actual-ins
      processActualParameters(node, node.getName().getStartPosition(), rcall, node.arguments());
      // append the call after all its arguments
      uses.add(rcall);
    }
    // visit children
    return false;
  }

  /**
   * [ClassName '.'] this
   */
  @Override
  public boolean visit(final ThisExpression node)
  {
    ASTNode enclosing = null;
    if (node.getQualifier() != null)
    {
      enclosing = ASTTools.enclosingTypeScope(node, node.getQualifier());
    }
    else
    {
      enclosing = ASTTools.enclosingTypeScope(node);
    }
    if (enclosing instanceof AbstractTypeDeclaration)
    {
      uses.add(factory.newResolvedThisVariable(node, (AbstractTypeDeclaration) enclosing,
          qualifierOf, isLHS, isActual));
    }
    else
    {
      uses.add(factory.newResolvedThisVariable(node, (AnonymousClassDeclaration) enclosing,
          qualifierOf, isLHS, isActual));
    }
    // do not visit children
    return false;
  }

  @Override
  public boolean visit(final TypeDeclarationStatement node)
  {
    // do not visit children
    return false;
  }

  private IResolvedCall createResolvedCall(final ASTNode node, final int startPosition,
      final IMethodBinding method)
  {
    if (method == null)
    {
      return null;
    }
    return factory.newResolvedCall(node, method, startPosition, qualifierOf, isLHS, isActual);
  }

  private void processActualParameters(final ASTNode node, final int startPosition,
      final IResolvedCall rcall, final List<?> actuals)
  {
    // all variables in this declaration
    for (int i = 0; i < actuals.size(); i++)
    {
      final Expression expression = (Expression) actuals.get(i);
      // visitor that collects variable references in the actual expressions
      final ExpressionVisitor visitor = new ExpressionVisitor(factory, isLHS, true, null);
      // visit the expression
      expression.accept(visitor);
      // collect the call dependences in the expression of this actual
      rcall.append(visitor.uses());
      // append the resolved nodes to the list
      uses.addAll(visitor.uses);
    }
  }

  private IResolvedNode processNode(final ASTNode node)
  {
    IResolvedData rv = null;
    if (node instanceof Name)
    {
      final IBinding binding = ((Name) node).resolveBinding();
      if (binding instanceof IVariableBinding)
      {
        rv = factory.newResolvedVariable(node, ASTTools.enclosingScope(node), qualifierOf, isLHS,
            isActual, false);
      }
    }
    else if (node instanceof FieldAccess)
    {
      rv = factory.newResolvedVariable(node, ASTTools.enclosingScope(node), qualifierOf, isLHS,
          isActual, false);
    }
    else if (node instanceof SuperFieldAccess)
    {
      rv = factory.newResolvedVariable(node, ASTTools.enclosingScope(node), qualifierOf, isLHS,
          isActual, false);
    }
    // return the resolved node
    return rv;
  }

  private void processRecursive(final Expression expression, final IResolvedNode rnode)
  {
    if (expression != null)
    {
      final ExpressionVisitor visitor = new ExpressionVisitor(factory, isLHS, isActual, rnode);
      expression.accept(visitor);
      uses.addAll(visitor.uses);
    }
  }

  boolean hasConditional()
  {
    return hasConditional;
  }

  /**
   * Ordered list of variables, fields, and method calls used in the expression.
   */
  List<IResolvedNode> uses()
  {
    return this.uses;
  }
}
