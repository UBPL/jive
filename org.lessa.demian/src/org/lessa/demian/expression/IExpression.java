package org.lessa.demian.expression;

public interface IExpression
{
  // visits the expression
  public void accept(IExpressionVisitor visitor) throws ExpressionException;
}