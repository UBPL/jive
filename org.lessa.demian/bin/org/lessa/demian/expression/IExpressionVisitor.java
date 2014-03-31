package org.lessa.demian.expression;

// visitor for expressions
public interface IExpressionVisitor
{
  public void visitInteger(IIntegerLiteral literal);

  public void visitVariable(IVariableExpression var) throws ExpressionException;

  public void visitSub(IExpression lhs, IExpression rhs) throws ExpressionException;

  public void visitMult(IExpression lhs, IExpression rhs) throws ExpressionException;

  public void visitLet(IVariableExpression var, IExpression rhs, IExpression exp)
      throws ExpressionException;

  public void visitDiv(IExpression lhs, IExpression rhs) throws ExpressionException;

  public void visitAdd(IExpression lhs, IExpression rhs) throws ExpressionException;
}