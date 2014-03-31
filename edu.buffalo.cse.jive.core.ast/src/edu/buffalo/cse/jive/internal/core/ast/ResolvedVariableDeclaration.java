package edu.buffalo.cse.jive.internal.core.ast;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Simple class that encapsulates AST variable information for further processing by the factory
 * adapter responsible for creating IDataNode instances for the static model.
 */
final class ResolvedVariableDeclaration
{
  private final ASTNode node;
  private final ASTNode parentNode;
  private final int lineFrom;
  private final int lineTo;

  ResolvedVariableDeclaration(final ASTNode node, final ASTNode parentNode)
  {
    this(node, parentNode, ASTTools.lineNumber(node, node.getStartPosition()), ASTTools.lineNumber(
        parentNode, parentNode.getStartPosition() + parentNode.getLength() - 1));
  }

  ResolvedVariableDeclaration(final ASTNode node, final ASTNode parentNode, final int lineFrom,
      final int lineTo)
  {
    this.node = node;
    this.parentNode = parentNode;
    this.lineFrom = lineFrom;
    this.lineTo = lineTo;
  }

  int lineFrom()
  {
    return this.lineFrom;
  }

  int lineTo()
  {
    return this.lineTo;
  }

  ASTNode node()
  {
    return this.node;
  }

  ASTNode parentNode()
  {
    return this.parentNode;
  }
}