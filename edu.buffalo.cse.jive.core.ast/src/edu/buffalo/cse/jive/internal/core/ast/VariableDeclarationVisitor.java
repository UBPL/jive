package edu.buffalo.cse.jive.internal.core.ast;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/**
 * Collects variable declarations from methods, constructors, and type initializers. This visitor
 * does not recurse upon finding type declarations or class creation instantiations, as these are
 * assumed to be processed separately. This visitor is intended to be used by method, constructor,
 * and type initializer nodes.
 */
public final class VariableDeclarationVisitor extends ASTVisitor
{
  private final ResolvedFactory factory;
  private final List<ResolvedVariableDeclaration> catchVariables;
  private final List<ResolvedVariableDeclaration> enhancedForVariables;
  private final List<ResolvedVariableDeclaration> variableFragments;
  private final List<ResolvedVariableDeclaration> variableIndexes;

  public VariableDeclarationVisitor(final ResolvedFactory factory)
  {
    this.factory = factory;
    this.catchVariables = new LinkedList<ResolvedVariableDeclaration>();
    this.enhancedForVariables = new LinkedList<ResolvedVariableDeclaration>();
    this.variableFragments = new LinkedList<ResolvedVariableDeclaration>();
    this.variableIndexes = new LinkedList<ResolvedVariableDeclaration>();
  }

  @Override
  public boolean visit(final CatchClause node)
  {
    addCatchVariable(factory.newVariableDeclaration(node.getException(), node));
    // visit children
    return true;
  }

  @Override
  public boolean visit(final ClassInstanceCreation node)
  {
    // do not recurse
    return false;
  }

  @Override
  public boolean visit(final EnhancedForStatement node)
  {
    addEnhancedForVariable(factory.newVariableDeclaration(node.getParameter(), node));
    // visit children
    return true;
  }

  @Override
  public boolean visit(final EnumDeclaration node)
  {
    // do not recurse
    return false;
  }

  @Override
  public boolean visit(final TypeDeclaration node)
  {
    // do not recurse
    return false;
  }

  @Override
  public boolean visit(final VariableDeclarationExpression node)
  {
    // visit all variable declaration fragments
    processFragments(node.fragments(), node);
    // do not recurse
    return false;
  }

  @Override
  public boolean visit(final VariableDeclarationStatement node)
  {
    // visit all variable declaration fragments
    processFragments(node.fragments(), node);
    // do not recurse
    return false;
  }

  private void addCatchVariable(final ResolvedVariableDeclaration rd)
  {
    catchVariables.add(rd);
    variableIndexes.add(rd);
  }

  private void addEnhancedForVariable(final ResolvedVariableDeclaration rd)
  {
    enhancedForVariables.add(rd);
    variableIndexes.add(rd);
  }

  private void addVariableFragment(final ResolvedVariableDeclaration rd)
  {
    variableFragments.add(rd);
    variableIndexes.add(rd);
  }

  private void processFragments(final List<?> fragments, final ASTNode node)
  {
    // parent of this declaration-- lexical scope of declared variables
    final ASTNode parent = ASTTools.enclosingVariableScope(node);
    // all variables in this declaration
    for (final Object o : fragments)
    {
      // adapter.createVariableNode(rvd.node(), rvd.parentNode(), rvd.lineFrom(), rvd.lineTo());
      addVariableFragment(factory.newVariableDeclaration((VariableDeclarationFragment) o, parent));
    }
  }

  List<ResolvedVariableDeclaration> catchVariables()
  {
    return this.catchVariables;
  }

  List<ResolvedVariableDeclaration> enhancedForVariables()
  {
    return this.enhancedForVariables;
  }

  List<ResolvedVariableDeclaration> getVariables()
  {
    return variableIndexes;
  }

  List<ResolvedVariableDeclaration> variableFragments()
  {
    return this.variableFragments;
  }
}
