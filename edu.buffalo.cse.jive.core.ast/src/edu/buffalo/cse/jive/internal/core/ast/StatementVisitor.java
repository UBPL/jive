package edu.buffalo.cse.jive.internal.core.ast;

import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_ASSIGNMENT;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_ASSIGNMENT_ARRAY_CELL;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_BREAK;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_CATCH;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_CLASS_INSTANCE_CREATION;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_CONDITIONAL;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_CONSTRUCTOR_INVOCATION;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_CONTINUE;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_DO;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_ENHANCED_FOR;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_ENUM_CONSTANT_DECLARATION;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_FIELD_DECLARATION;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_FOR;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_IF_THEN;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_IF_THEN_ELSE;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_INITIALIZER;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_METHOD_CALL;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_METHOD_DECLARATION;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_POSTFIX;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_POSTFIX_ARRAY_CELL;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_PREFIX;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_PREFIX_ARRAY_CELL;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_RETURN;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_RETURN_IN_TRY;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_SUPER_CONSTRUCTOR;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_SUPER_METHOD_CALL;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_SWITCH;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_SWITCH_CASE;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_SYNCHRONIZED;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_VARIABLE_DECLARATION;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_WHILE;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import edu.buffalo.cse.jive.lib.OneToMany;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IMethodDependenceGraph;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedData;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedLine;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedNode;
import edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind;

/**
 * Top-level callers should be method declarations and type/field initializers. This visitor will
 * visit the interesting statements in these nodes' bodies.
 * 
 * Statement:
 * 
 * <pre>
 *     AssertStatement                 NOT supported
 *     Block                           supported (transparent: Statement)
 *     BreakStatement                  supported
 *     ConstructorInvocation           supported
 *     ContinueStatement               supported
 *     DoStatement                     supported
 *     EmptyStatement                  transparent
 *     EnhancedForStatement            supported
 *     ExpressionStatement             supported
 *     ForStatement                    supported
 *     IfStatement                     supported
 *     LabeledStatement                supported
 *     ReturnStatement                 supported
 *     SuperConstructorInvocation      supported
 *     SwitchCase                      supported
 *     SwitchStatement                 supported
 *     SynchronizedStatement           supported 
 *     ThrowStatement                  NOT supported
 *     TryStatement                    supported (transparent: Block, CatchClause)
 *     TypeDeclarationStatement        NOT supported
 *     VariableDeclarationStatement    supported
 *     WhileStatement                  supported
 * </pre>
 * 
 * Expression:
 * 
 * <pre>
 *     ArrayAccess                     NOT supported
 *     ArrayCreation                   NOT supported
 *     ArrayInitializer                NOT supported
 *     Assignment                      supported (via ExpressionStatement)
 *     BooleanLiteral                  NOT supported
 *     CastExpression                  NOT supported
 *     CharacterLiteral                NOT supported
 *     ClassInstanceCreation           supported (via ExpressionStatement)
 *     ConditionalExpression           supported (via ExpressionStatement)
 *     FieldAccess                     NOT supported
 *     InfixExpression                 NOT supported
 *     InstanceofExpression            NOT supported
 *     MethodInvocation                supported (via ExpressionStatement)
 *     Name                            NOT supported
 *     NullLiteral                     NOT supported
 *     NumberLiteral                   NOT supported
 *     ParenthesizedExpression         NOT supported
 *     PostfixExpression               supported (via ExpressionStatement)
 *     PrefixExpression                supported (via ExpressionStatement)
 *     StringLiteral                   NOT supported
 *     SuperFieldAccess                NOT supported
 *     SuperMethodInvocation           supported (via ExpressionStatement)
 *     ThisExpression                  NOT supported
 *     TypeLiteral                     NOT supported
 *     VariableDeclarationExpression   supported (via ExpressionStatement)
 * </pre>
 * 
 * Other:
 * 
 * <pre>
 *     CatchClause                     supported
 *     EnumDeclaration                 NOT supported
 *     EnumConstantDeclaration         supported (entry point)
 *     FieldDeclaration                supported (entry point)
 *     Initializer                     supported (entry point)
 *     MethodDeclaration               supported (entry point)
 * </pre>
 */
public final class StatementVisitor extends ASTVisitor
{
  private final IMethodDependenceGraph mdg;
  /**
   * Used internally to propagate break, continue, and return control dependences.
   */
  private final ResolvedFactory factory;
  private final OneToMany<Integer, IResolvedLine> jumpDependences;
  private final Map<IResolvedLine, Integer> jumpDependenceEnds;

  public StatementVisitor(final ResolvedFactory factory)
  {
    // root node associated externally
    this(null, factory);
  }

  private StatementVisitor(final IResolvedLine parent, final ResolvedFactory factory)
  {
    this.factory = factory;
    this.mdg = factory.newMethodDependenceGraph(parent);
    this.jumpDependences = new OneToMany<Integer, IResolvedLine>();
    this.jumpDependenceEnds = new LinkedHashMap<IResolvedLine, Integer>();
  }

  public IMethodDependenceGraph mdg()
  {
    return mdg;
  }

  @Override
  public String toString()
  {
    return mdg.toString();
  }

  /**
   * We do not support assert.
   * 
   * 'assert' Expression [ ':' Expression ] ';'
   */
  @Override
  public boolean visit(final AssertStatement node)
  {
    // do not visit children
    return false;
  }

  /**
   * '{' Statement '}'
   */
  @Override
  public boolean visit(final Block node)
  {
    // visit children
    return true;
  }

  /**
   * 'break' [label]
   */
  @Override
  public boolean visit(final BreakStatement node)
  {
    // check if this break has a loop node parent
    final ASTNode loopNode = ASTTools.enclosingLoopScope(node);
    if (loopNode != null)
    {
      // control node parent
      final ASTNode controlNode = ASTTools.enclosingControlScope(node);
      // either the line after the enclosing control scope, or the line after the break
      final int lineNo = controlNode != null ? lineEnd(controlNode) : lineStart(node);
      // last line of the loop scope
      final int lineEnd = lineEnd(loopNode);
      // new loop dependence
      appendLoopDependence(lineNo, lineEnd,
          createDependence(LK_BREAK, lineStart(node), mdg.parent()));
    }
    else
    {
      // control node parent should be a switch
      final ASTNode switchNode = ASTTools.enclosingSwitchScope(node);
      if (switchNode != null)
      {
        // new break dependence, but no jump dependence for non-loop parents
        createDependence(LK_BREAK, lineStart(node), mdg.parent());
      }
    }
    // do not visit children
    return false;
  }

  /**
   * 'catch(' exception ')' '{' body '}'
   */
  @Override
  public boolean visit(final CatchClause node)
  {
    final int lineNo = lineStart(node);
    // new dependence
    final IResolvedLine rd = createDependence(LK_CATCH, lineNo, mdg.parent());
    // control node parent
    final ASTNode controlNode = ASTTools.enclosingControlScope(node);
    // last line of the control scope
    final int lineEnd = lineEnd(controlNode);
    // the catch dependence must be propagated to all lines in the method's body
    jumpDependences.put(lineEnd, rd);
    // new variable use
    final IResolvedData rv = factory.resolveExpression(node.getException().getName(), node, null,
        false, true);
    // map this definition
    rd.definitions().add(rv);
    // append the control dependence
    rd.uses().add(rv);
    // recursively append dependences
    appendRecursive(lineNo, rd, node.getBody());
    // do not visit children
    return false;
  }

  /**
   * [<type..>] 'this' '(' arguments ')'
   */
  @Override
  public boolean visit(final ConstructorInvocation node)
  {
    final int lineNo = lineStart(node);
    // new dependence
    final IResolvedLine rd = createDependence(LK_CONSTRUCTOR_INVOCATION, lineNo, mdg.parent());
    // append call and data dependences
    processCall(rd, node);
    // do not visit children
    return false;
  }

  /**
   * 'continue' [label]
   */
  @Override
  public boolean visit(final ContinueStatement node)
  {
    // loop node parent
    final ASTNode loopNode = ASTTools.enclosingLoopScope(node);
    if (loopNode != null)
    {
      // line of the loop
      final int lineNo = lineStart(loopNode);
      // last line of the loop scope
      final int lineEnd = lineEnd(loopNode);
      // new loop dependence
      appendLoopDependence(lineNo, lineEnd,
          createDependence(LK_CONTINUE, lineStart(node), mdg.parent()));
      // do not visit children
      return false;
    }
    return true;
  }

  /**
   * 'do {' body '} while (' expression ')'
   */
  @Override
  public boolean visit(final DoStatement node)
  {
    // the DO node is a NO-OP, but useful to synchronize in the dynamic analysis
    createDependence(LineKind.LK_DO_NOOP, lineStart(node), mdg.parent());
    // the control node is the WHILE part, not the DO
    visitControlNode(LK_DO, lineEnd(node), node.getExpression(), node.getBody());
    // do not visit children
    return false;
  }

  /**
   * 'for(' parameter ':' expression ') {' body '}'
   */
  @Override
  public boolean visit(final EnhancedForStatement node)
  {
    final int lineNo = lineStart(node);
    // new dependence
    final IResolvedLine rd = createDependence(LK_ENHANCED_FOR, lineNo, mdg.parent());
    // add this definition
    rd.definitions().add(
        factory.resolveExpression(node.getParameter().getName(), node, null, false, true));
    // collect uses for this control predicate
    appendDependences(rd, node.getExpression(), null, false);
    // recursively append dependences
    appendRecursive(lineNo, rd, node.getBody());
    // do not visit children
    return false;
  }

  @Override
  public boolean visit(final EnumConstantDeclaration node)
  {
    // new dependence
    final IResolvedLine rd = createDependence(LK_ENUM_CONSTANT_DECLARATION, lineStart(node),
        mdg.parent());
    // new resolved variable
    final IResolvedData rv = factory.newResolvedVariable(node, ASTTools.enclosingScope(node), null,
        true, false, true);
    // map the enum constant definition
    rd.definitions().add(rv);
    // append call and data dependences
    processCall(rd, node);
    // // map the enum constant constructor binding
    // final IMethodBinding mb = node.resolveConstructorBinding();
    // final IResolvedCall rc = factory.newResolvedCall(node, mb, lineStart(node), null, false,
    // false);
    // rd.uses().add(rc);
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
   * The only expressions explicitly processed here are those used as statements. They are not
   * processed via the normal visitation path, but rather explicitly as part of an expression
   * statement.
   * 
   * StatementExpression
   * 
   * TODO: reduce the cyclomatic complexity.
   */
  @Override
  public boolean visit(final ExpressionStatement node)
  {
    if (node.getExpression() instanceof Assignment)
    {
      if (((Assignment) node.getExpression()).getLeftHandSide() instanceof ArrayAccess)
      {
        visitAssignArrayCell((Assignment) node.getExpression());
      }
      else
      {
        visitAssign((Assignment) node.getExpression());
      }
    }
    else if (node.getExpression() instanceof ClassInstanceCreation)
    {
      visitExpression((ClassInstanceCreation) node.getExpression());
    }
    else if (node.getExpression() instanceof ConditionalExpression)
    {
      visitExpression((ConditionalExpression) node.getExpression());
    }
    else if (node.getExpression() instanceof MethodInvocation)
    {
      visitExpression((MethodInvocation) node.getExpression());
    }
    else if (node.getExpression() instanceof PostfixExpression)
    {
      if (((PostfixExpression) node.getExpression()).getOperand() instanceof ArrayAccess)
      {
        visitPostfixArrayCell((PostfixExpression) node.getExpression());
      }
      else
      {
        visitPostfix((PostfixExpression) node.getExpression());
      }
    }
    else if (node.getExpression() instanceof PrefixExpression)
    {
      if (((PrefixExpression) node.getExpression()).getOperand() instanceof ArrayAccess)
      {
        visitPrefixArrayCell((PrefixExpression) node.getExpression());
      }
      else
      {
        visitPrefix((PrefixExpression) node.getExpression());
      }
    }
    else if (node.getExpression() instanceof SuperMethodInvocation)
    {
      // uses an expression visitor for the arguments
      visitExpression((SuperMethodInvocation) node.getExpression());
    }
    else if (node.getExpression() instanceof VariableDeclarationExpression)
    {
      // uses an expression visitor for the initializer
      visitExpression((VariableDeclarationExpression) node.getExpression());
    }
    // do not visit children
    return false;
  }

  /**
   * Entry point.
   */
  @Override
  public boolean visit(final FieldDeclaration node)
  {
    // new dependence
    final IResolvedLine rd = createDependence(LK_FIELD_DECLARATION, lineStart(node), mdg.parent());
    // visit all variable declaration fragments
    processFragments(rd, node.fragments());
    // do not visit children
    return false;
  }

  /**
   * 'for(' initializers ';' expression ';' updaters ') {' body '}'
   */
  @Override
  public boolean visit(final ForStatement node)
  {
    final int lineNo = lineStart(node);
    // new dependence
    final IResolvedLine rd = createDependence(LK_FOR, lineNo, mdg.parent());
    // // recursively append initializer, control, and updater dependences
    // appendRecursive(lineNo, rd, node);
    /**
     * Recursively append initializers. The list should consist of either a list of so called
     * statement expressions (JLS2, 14.8), or a single VariableDeclarationExpression. Otherwise, the
     * for statement would have no Java source equivalent.
     */
    for (final Object o : node.initializers())
    {
      if (o instanceof VariableDeclarationExpression)
      {
        // visit all variable declaration fragments
        processFragments(rd, ((VariableDeclarationExpression) o).fragments());
      }
      else if (o instanceof ExpressionStatement)
      {
        visit((ExpressionStatement) o);
      }
    }
    // append control dependences
    appendDependences(rd, node.getExpression(), null, false);
    // recursively append updated dependences
    for (final Object o : node.updaters())
    {
      /**
       * The list should consist of so called statement expressions. Otherwise, the for statement
       * would have no Java source equivalent.
       */
      if (o instanceof ExpressionStatement)
      {
        visit((ExpressionStatement) o);
      }
      else if (o instanceof Expression)
      {
        appendDependences(rd, (Expression) o, null, false);
      }
    }
    // recursively append body dependences
    appendRecursive(lineNo, rd, node.getBody());
    // do not visit children
    return false;
  }

  /**
   * 'if(' expression ') {' thenStatement '}' ['else {' elseStatement '}']
   */
  @Override
  public boolean visit(final IfStatement node)
  {
    final int lineNo = lineStart(node);
    // dependence id
    final LineKind kind = node.getElseStatement() == null ? LK_IF_THEN : LK_IF_THEN_ELSE;
    // new dependence
    final IResolvedLine rd = createDependence(kind, lineNo, mdg.parent());
    // append control dependences
    appendDependences(rd, node.getExpression(), null, false);
    // recursively append "then" dependences
    appendRecursive(lineNo, rd, node.getThenStatement());
    // recursively append "else" dependences
    if (node.getElseStatement() != null)
    {
      appendRecursive(lineNo, rd, node.getElseStatement());
    }
    // do not visit children
    return false;
  }

  /**
   * Type or instance initializer (entry point).
   * 
   * ['static'] '{' statement '}'
   */
  @Override
  public boolean visit(final Initializer node)
  {
    // initializers are processed only at the top level
    if (mdg.parent() != null)
    {
      return false;
    }
    final int lineNo = lineStart(node);
    // new dependence
    createDependence(LK_INITIALIZER, lineNo, mdg.parent());
    // recursively append "body" dependences
    if (node.getBody() != null)
    {
      appendRecursive(lineNo, mdg.parent(), node.getBody());
      processJumpDependences();
    }
    // visit children
    return false;
  }

  /**
   * Labeled statement.
   * 
   * identifier ':' statement
   */
  @Override
  public boolean visit(final LabeledStatement node)
  {
    // visit children
    return true;
  }

  /**
   * Entry point.
   */
  @Override
  public boolean visit(final MethodDeclaration node)
  {
    // methods are processed only at the top level
    if (mdg.parent() != null)
    {
      return false;
    }
    final int lineNo = lineStart(node.getName());
    // new dependence
    final IResolvedLine rd = createDependence(LK_METHOD_DECLARATION, lineNo, mdg.parent());
    // formal-ins
    for (final Object o : node.parameters())
    {
      // new definition
      final IResolvedData rv = factory.resolveExpression(((SingleVariableDeclaration) o).getName(),
          node, null, false, false);
      // formal in
      rd.definitions().add(rv);
    }
    // recursively append "body" dependences
    if (node.getBody() != null)
    {
      appendRecursive(lineNo, mdg.parent(), node.getBody());
      processJumpDependences();
    }
    // visit children
    return false;
  }

  /**
   * 'return' [expression]
   */
  @Override
  public boolean visit(final ReturnStatement node)
  {
    final int lineNo = lineStart(node);
    // control node parent
    final ASTNode controlNode = ASTTools.enclosingControlScope(node);
    // last line of the control scope
    final int lineEnd = lineEnd(controlNode);
    // determine if this is contained in a try--finally block.
    final boolean isTryFinally = ASTTools.enclosingTryFinallyScope(node) != null;
    // new return dependence
    final IResolvedLine rd = createDependence(isTryFinally ? LK_RETURN_IN_TRY : LK_RETURN, lineNo,
        mdg.parent());
    // propagate the return dependences starting from the line end
    jumpDependences.put(lineEnd, rd);
    // actual-out
    if (node.getExpression() != null)
    {
      // append call and data dependences
      appendDependences(rd, node.getExpression(), null, false);
    }
    // do not visit children
    return false;
  }

  /**
   * [expression '.'] [<type..>] 'super' '(' arguments ')'
   */
  @Override
  public boolean visit(final SuperConstructorInvocation node)
  {
    final int lineNo = lineStart(node);
    // new dependence
    final IResolvedLine rd = createDependence(LK_SUPER_CONSTRUCTOR, lineNo, mdg.parent());
    // append call and data dependences
    processCall(rd, node);
    // do not visit children
    return false;
  }

  /**
   * SwitchStatement: 'switch' '(' expression ')' '{' SwitchCase | statement '}'
   * 
   * SwitchCase: 'case' expression ':' | 'default' ':'
   */
  @Override
  public boolean visit(final SwitchStatement node)
  {
    final int lineNo = lineStart(node);
    // new dependence
    final IResolvedLine rl = createDependence(LK_SWITCH, lineNo, mdg.parent());
    // append control dependences
    appendDependences(rl, node.getExpression(), null, false);
    // the next switch case statement
    IResolvedLine scl = null;
    // recursively append dependences
    for (final Object o : node.statements())
    {
      // the uses of a case statement accumulate as we drill down each case
      if (o instanceof SwitchCase)
      {
        final SwitchCase caseNode = (SwitchCase) o;
        // if we were in statement mode, then it's time to create a case line
        scl = createDependence(LK_SWITCH_CASE, lineStart(caseNode), scl == null ? rl : scl);
        // default case has no additional expressions to append
        if (caseNode.getExpression() != null)
        {
          // the resolved case expression as a use
          final IResolvedData rd = factory.resolveExpression(caseNode.getExpression(), node, null,
              false, false);
          // collect uses
          scl.uses().add(rd);
        }
        // do not append recursive-- statements are appended recursively under the respective case
        continue;
      }
      appendRecursive(lineNo, scl, (ASTNode) o);
    }
    // do not visit children
    return false;
  }

  /**
   * 'synchronized' '(' Expression ')' Block
   */
  @Override
  public boolean visit(final SynchronizedStatement node)
  {
    visitControlNode(LK_SYNCHRONIZED, lineStart(node), node.getExpression(), node.getBody());
    // do not visit children
    return false;
  }

  /**
   * An exception throw does not introduce a control or a data dependence. Perhaps there's something
   * that can be done here to improve processing at run-time.
   * 
   * 'throw' Expression ';'
   */
  @Override
  public boolean visit(final ThrowStatement node)
  {
    // do not visit children
    return false;
  }

  /**
   * 'try' Block [ '{' CatchClause '}' ] [ 'finally' Block ]
   */
  @Override
  public boolean visit(final TryStatement node)
  {
    // visit children
    return true;
  }

  @Override
  public boolean visit(final TypeDeclarationStatement node)
  {
    // do not visit children
    return false;
  }

  @Override
  public boolean visit(final VariableDeclarationStatement node)
  {
    // new dependence
    final IResolvedLine rd = createDependence(LK_VARIABLE_DECLARATION, lineStart(node),
        mdg.parent());
    // visit all variable declaration fragments
    processFragments(rd, node.fragments());
    // do not visit children
    return false;
  }

  /**
   * 'while (' expression ') {' body '}
   */
  @Override
  public boolean visit(final WhileStatement node)
  {
    visitControlNode(LK_WHILE, lineStart(node), node.getExpression(), node.getBody());
    // do not visit children
    return false;
  }

  private void appendDependences(final IResolvedLine rd, final ASTNode node,
      final IResolvedNode qualifierOf, final boolean isLHS)
  {
    // collect all variable references
    final ExpressionVisitor visitor = new ExpressionVisitor(factory, isLHS, qualifierOf);
    // recursively visit the expression
    node.accept(visitor);
    // collect uses
    rd.uses().addAll(visitor.uses());
    // update the line
    if (visitor.hasConditional())
    {
      rd.setHasConditional();
    }
  }

  /**
   * A loop dependence propagates to all dependences in a line range.
   */
  private void appendLoopDependence(final int lineNo, final int lineEnd, final IResolvedLine rd)
  {
    jumpDependences.put(lineNo, rd);
    jumpDependenceEnds.put(rd, lineEnd);
  }

  /**
   * Visits the node recursively using the resolved line as its parent. This allows correct chaining
   * of control dependences. The resolved line must represent a control dependence, not a call or
   * data dependence.
   */
  private void appendRecursive(final int lineNo, final IResolvedLine control, final ASTNode node)
  {
    // visit the node dependent on this control dependence
    final StatementVisitor visitor = new StatementVisitor(control, factory);
    node.accept(visitor);
    // merge dependences
    for (final Integer key : visitor.mdg.dependenceMap().keySet())
    {
      if (mdg.dependenceMap().containsKey(key))
      {
        (mdg.dependenceMap().get(key)).merge(visitor.mdg.dependenceMap().get(key));
      }
      else
      {
        mdg.dependenceMap().put(key, visitor.mdg.dependenceMap().get(key));
      }
    }
    // merge propagated dependences
    for (final Integer key : visitor.jumpDependences.lookupKeys())
    {
      // if no dependences exist for the key, import from the visitor
      if (!jumpDependences.containsKey(key))
      {
        jumpDependences.putAll(key, visitor.jumpDependences.lookupChildren(key));
      }
      // otherwise, for each dependence merge each dependence of the visitor
      else
      {
        jumpDependences.putAll(key, visitor.jumpDependences.lookupChildren(key));
      }
    }
    // there should be no conflict
    jumpDependenceEnds.putAll(visitor.jumpDependenceEnds);
  }

  private IResolvedLine createDependence(final LineKind kind, final int lineNo,
      final IResolvedLine parent)
  {
    // new resolved dependence
    final IResolvedLine rd = factory.newResolvedDependence(kind, lineNo, parent);
    // merge the dependence
    mergeDependence(lineNo, rd);
    // the defined dependence
    return rd;
  }

  private Expression getQualifier(final Expression exp)
  {
    if (exp instanceof FieldAccess)
    {
      return ((FieldAccess) exp).getExpression();
    }
    if (exp instanceof SuperFieldAccess)
    {
      return ((SuperFieldAccess) exp).getQualifier();
    }
    if (exp instanceof MethodInvocation)
    {
      return ((MethodInvocation) exp).getExpression();
    }
    if (exp instanceof QualifiedName)
    {
      return ((QualifiedName) exp).getQualifier();
    }
    return null;
  }

  private boolean isNamedDataMember(final Expression exp)
  {
    if (exp instanceof FieldAccess)
    {
      // always has a name
      return true;
    }
    if (exp instanceof SuperFieldAccess)
    {
      // always has a name
      return true;
    }
    if (exp instanceof ArrayAccess)
    {
      // true only if the array expression is a named data member
      return isNamedDataMember(((ArrayAccess) exp).getArray());
    }
    if (exp instanceof QualifiedName)
    {
      // is a name
      return true;
    }
    if (exp instanceof SimpleName)
    {
      // is a name
      return true;
    }
    // does not have a name
    return false;
  }

  /**
   * An expression like "foo.this" can only be represented as a this expression (ThisExpression)
   * containing a simple name. "this" is a keyword, and therefore invalid as an identifier.
   * 
   * An expression like "this.foo" can only be represented as a field access expression
   * (FieldAccess) containing a this expression and a simple name. Again, this is because "this" is
   * a keyword, and therefore invalid as an identifier.
   * 
   * An expression with "super" can only be represented as a super field access expression
   * (SuperFieldAccess). "super" is a also keyword, and therefore invalid as an identifier.
   * 
   * An expression like "foo.bar" can be represented either as a qualified name (QualifiedName) or
   * as a field access expression (FieldAccess) containing simple names. Either is acceptable, and
   * there is no way to choose between them without information about what the names resolve to
   * (ASTParser may return either).
   * 
   * Other expressions ending in an identifier, such as "foo().bar" can only be represented as field
   * access expressions (FieldAccess).
   */
  private Expression lhsDef(final Expression lhs)
  {
    if (lhs instanceof FieldAccess)
    {
      return ((FieldAccess) lhs).getName();
    }
    if (lhs instanceof SuperFieldAccess)
    {
      return ((SuperFieldAccess) lhs).getName();
    }
    if (lhs instanceof QualifiedName)
    {
      return ((QualifiedName) lhs).getName();
    }
    if (lhs instanceof SimpleName)
    {
      return lhs;
    }
    // multi-dimensional array access
    if (lhs instanceof ArrayAccess)
    {
      return lhsDef(((ArrayAccess) lhs).getArray());
    }
    throw new IllegalStateException("Unexpected LHS expression node: " + lhs.getClass().getName()
        + " --> " + lhs.toString());
  }

  private int lineEnd(final ASTNode node)
  {
    return ASTTools.lineNumber(node, node.getStartPosition() + node.getLength() - 1);
  }

  private int lineStart(final ASTNode node)
  {
    return ASTTools.lineNumber(node);
  }

  private void mergeDependence(final int lineNo, final IResolvedLine rd)
  {
    if (mdg.dependenceMap().get(lineNo) == null)
    {
      mdg.dependenceMap().put(lineNo, rd);
    }
    else
    {
      (mdg.dependenceMap().get(lineNo)).merge(rd);
    }
  }

  private void processCall(final IResolvedLine rd, final ASTNode call)
  {
    // collect all variable references
    final ExpressionVisitor visitor = new ExpressionVisitor(factory, false, null);
    // recursively visit the method call
    call.accept(visitor);
    // collect uses
    rd.uses().addAll(visitor.uses());
    // update the line
    if (visitor.hasConditional())
    {
      rd.setHasConditional();
    }
  }

  private void processFragments(final IResolvedLine rd, final List<?> fragments)
  {
    // all variables in this declaration
    for (final Object o : fragments)
    {
      final VariableDeclarationFragment fragment = (VariableDeclarationFragment) o;
      // new resolved variable
      final IResolvedData rv = factory.newResolvedVariable(fragment,
          ASTTools.enclosingScope(fragment), null, true, false, fragment.getInitializer() != null);
      // map this definition
      rd.definitions().add(rv);
      // process only if this variable is initialized
      if (fragment.getInitializer() != null)
      {
        // collect data uses for this definition
        appendDependences(rd, fragment.getInitializer(), null, false);
      }
    }
  }

  private void processJumpDependences()
  {
    // process jump dependences
    if (jumpDependences.isEmpty() || mdg.dependenceMap().isEmpty())
    {
      return;
    }
    // all target lines of jumps (breaks, continues, and returns)
    final List<Integer> jumpsIndex = new LinkedList<Integer>(jumpDependences.lookupKeys());
    Collections.sort(jumpsIndex);
    // source lines in the mdg
    final List<Integer> dependencesIndex = new LinkedList<Integer>(mdg.dependenceMap().keySet());
    Collections.sort(dependencesIndex);
    int i = 0;
    int j = 0;
    while (i < jumpsIndex.size() && j < dependencesIndex.size())
    {
      // find the next dependence affected by the i-th jump
      while (j < dependencesIndex.size()
          && (jumpsIndex.get(i) >= dependencesIndex.get(j) || mdg.dependenceMap().get(
              dependencesIndex.get(j)) == null))
      {
        j++;
      }
      // no dependences found
      if (j >= dependencesIndex.size())
      {
        break;
      }
      // current jump dependences to propagate
      final Set<IResolvedLine> jumps = jumpDependences.lookupChildren(jumpsIndex.get(i));
      for (final IResolvedLine jump : jumps)
      {
        final Integer lineFrom = jump.lineNumber();
        final Integer lineTo = jumpDependenceEnds.get(jump);
        // propagate to each k-th dependence, k >= j
        for (int k = j; k < dependencesIndex.size(); k++)
        {
          // propagate only if the current line is within the proper range
          if (lineTo == null || lineTo >= dependencesIndex.get(k))
          {
            final IResolvedLine target = mdg.dependenceMap().get(dependencesIndex.get(k));
            if (target != jump && target.lineNumber() > lineFrom)
            {
              target.jumpDependences().add(jump);
            }
          }
        }
      }
      // move to the next jump dependence
      i++;
      // DO NOT move to the next dependence, since the current one may be affected by the new i
    }
    // do not propagate upstream
    jumpDependences.clear();
  }

  /**
   * lhs op rhs
   */
  private void visitAssign(final Assignment node)
  {
    final int lineNo = lineStart(node);
    // new dependence
    final IResolvedLine rd = createDependence(LK_ASSIGNMENT, lineNo, mdg.parent());
    // separate the variable definition from any additional expressions
    final Expression lhs = node.getLeftHandSide();
    // field access, super field access, or name
    final Expression def = lhsDef(lhs);
    // new variable
    final IResolvedData rv = factory.resolveExpression(def, node, null, true, true);
    // map this definition
    rd.definitions().add(rv);
    // process the qualifying expressions of the def as left hand side uses
    final Expression qualifier = getQualifier(lhs);
    if (qualifier != null)
    {
      appendDependences(rd, qualifier, rv, true);
    }
    // append RHS data dependences
    appendDependences(rd, node.getRightHandSide(), null, false);
    // append implicit data dependence
    if (node.getOperator() != Assignment.Operator.ASSIGN)
    {
      rd.uses().add(rv);
    }
  }

  private void visitAssignArrayCell(final Assignment node)
  {
    final int lineNo = lineStart(node);
    // new dependence
    final IResolvedLine rd = createDependence(LK_ASSIGNMENT_ARRAY_CELL, lineNo, mdg.parent());
    // separate the variable definition from any additional expressions
    final ArrayAccess array = (ArrayAccess) node.getLeftHandSide();
    // if the array is a field access, super field access, or name, the array is referenced by name
    final Expression def = isNamedDataMember(array) ? lhsDef(array.getArray()) : null;
    // new variable only if the def expression is not null
    final IResolvedData rv = def == null ? null : factory.resolveExpression(def, node, null, true,
        true);
    /**
     * map this definition-- if the array cell is the result of an expression, there is no named
     * variable to add as the def, e.g.:
     * 
     * foo()[1] = 10;
     */
    if (def != null)
    {
      rd.definitions().add(rv);
    }
    // the array was referenced by name, so process its qualifier
    if (rv != null)
    {
      // process the qualifying expressions of the def as left hand side uses
      final Expression qualifier = getQualifier(array.getArray());
      if (qualifier != null)
      {
        appendDependences(rd, qualifier, rv, true);
      }
    }
    // the array was not referenced by name, so the array expression should be processed
    else
    {
      appendDependences(rd, array.getArray(), null, true);
    }
    // process the array index-- fake it as LHS
    appendDependences(rd, array.getIndex(), null, false);
    // append RHS data dependences
    appendDependences(rd, node.getRightHandSide(), null, false);
    // append implicit data dependence
    if (node.getOperator() != Assignment.Operator.ASSIGN)
    {
      rd.uses().add(rv);
    }
  }

  private void visitControlNode(final LineKind kind, final int lineNo, final Expression control,
      final Statement body)
  {
    // new dependence
    final IResolvedLine rd = createDependence(kind, lineNo, mdg.parent());
    // append control dependences
    appendDependences(rd, control, null, false);
    // recursively append dependences
    appendRecursive(lineNo, rd, body);
  }

  /**
   * [expression '.'] 'new' [<type..>] type( arguments ) anonymousClassDeclaration
   */
  private void visitExpression(final ClassInstanceCreation node)
  {
    final int lineNo = lineStart(node);
    // new dependence
    final IResolvedLine rd = createDependence(LK_CLASS_INSTANCE_CREATION, lineNo, mdg.parent());
    // append call and data dependences
    processCall(rd, node);
  }

  /**
   * expression '?' thenStatement ':' elseStatement ';'
   */
  private void visitExpression(final ConditionalExpression node)
  {
    final int lineNo = lineStart(node);
    // new dependence
    final IResolvedLine rd = createDependence(LK_CONDITIONAL, lineNo, mdg.parent());
    // append control dependences
    appendDependences(rd, node.getExpression(), null, false);
    // recursively append "then" dependences
    appendRecursive(lineNo, rd, node.getThenExpression());
    // recursively append "else" dependences
    appendRecursive(lineNo, rd, node.getElseExpression());
  }

  /**
   * [expression] '.' [<type..>] method '(' arguments ')'
   */
  private void visitExpression(final MethodInvocation node)
  {
    final int lineNo = lineStart(node);
    // new dependence
    final IResolvedLine rd = createDependence(LK_METHOD_CALL, lineNo, mdg.parent());
    // append call and data dependences
    processCall(rd, node);
  }

  /**
   * [class '.'] 'super' '.' [<type..>] method '(' arguments ')'
   */
  private void visitExpression(final SuperMethodInvocation node)
  {
    final int lineNo = lineStart(node);
    // new dependence
    final IResolvedLine rd = createDependence(LK_SUPER_METHOD_CALL, lineNo, mdg.parent());
    // append call and data dependences
    processCall(rd, node);
  }

  private void visitExpression(final VariableDeclarationExpression node)
  {
    // new dependence
    final IResolvedLine rd = createDependence(LK_VARIABLE_DECLARATION, lineStart(node),
        mdg.parent());
    // visit all variable declaration fragments
    processFragments(rd, node.fragments());
  }

  /**
   * expression PostfixOperator
   */
  private void visitPostfix(final PostfixExpression node)
  {
    visitPreOrPostfix(node, node.getOperand(), LK_POSTFIX);
  }

  /**
   * expression PostfixOperator
   */
  private void visitPostfixArrayCell(final PostfixExpression node)
  {
    visitPreOrPostfixArrayCell(node, (ArrayAccess) node.getOperand(), LK_POSTFIX_ARRAY_CELL);
  }

  /**
   * expression PrefixOperator
   */
  private void visitPrefix(final PrefixExpression node)
  {
    visitPreOrPostfix(node, node.getOperand(), LK_PREFIX);
  }

  /**
   * expression PrefixOperator
   */
  private void visitPrefixArrayCell(final PrefixExpression node)
  {
    visitPreOrPostfixArrayCell(node, (ArrayAccess) node.getOperand(), LK_PREFIX_ARRAY_CELL);
  }

  /**
   * Standard handling of prefix and postfix expression on non-array cell expressions.
   */
  private void visitPreOrPostfix(final Expression node, final Expression lhs, final LineKind kind)
  {
    final int lineNo = lineStart(node);
    // new dependence
    final IResolvedLine rd = factory.newResolvedDependence(kind, lineNo, mdg.parent());
    // field access, super field access, or name
    final Expression def = lhsDef(lhs);
    // new variable
    final IResolvedData rv = factory.resolveExpression(def, node, null, false, true);
    // map this definition
    rd.definitions().add(rv);
    // process the qualifying expressions of the postfix expression as left hand side uses
    final Expression qualifier = getQualifier(lhs);
    if (qualifier != null)
    {
      appendDependences(rd, qualifier, rv, true);
    }
    // implicit use in the variable definition
    rd.uses().add(rv);
    // append the dependence
    mergeDependence(lineNo, rd);
  }

  /**
   * Standard handling of prefix and postfix expression on array cell expressions.
   */
  private void visitPreOrPostfixArrayCell(final Expression node, final ArrayAccess array,
      final LineKind kind)
  {
    final int lineNo = lineStart(node);
    // new dependence
    final IResolvedLine rd = factory.newResolvedDependence(kind, lineNo, mdg.parent());
    // if the array is a field access, super field access, or name, the array is referenced by name
    final Expression def = isNamedDataMember(array) ? lhsDef(array.getArray()) : null;
    // new variable only if the def expression is not null
    final IResolvedData rv = def == null ? null : factory.resolveExpression(def, node, null, true,
        true);
    /**
     * map this definition-- if the array cell is the result of an expression, there is no named
     * variable to add as the def, e.g.:
     * 
     * foo()[1]++;
     * 
     * ++foo()[1];
     */
    if (def != null)
    {
      rd.definitions().add(rv);
      // implicit use in the variable definition
      rd.uses().add(rv);
    }
    // the array was referenced by name, so process its qualifier
    if (rv != null)
    {
      // process the qualifying expressions of the def as left hand side uses
      final Expression qualifier = getQualifier(array.getArray());
      if (qualifier != null)
      {
        appendDependences(rd, qualifier, rv, true);
      }
    }
    // the array was not referenced by name, so the array expression should be processed
    else
    {
      appendDependences(rd, array.getArray(), null, true);
    }
    // process the array index
    appendDependences(rd, array.getIndex(), null, true);
    // append the dependence
    mergeDependence(lineNo, rd);
  }
}