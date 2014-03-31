package org.lessa.demian.internal.expression;

import org.lessa.demian.expression.ExpressionException;
import org.lessa.demian.expression.IExpressionVisitor;
import org.lessa.demian.expression.IVariableExpression;

class VariableExpression extends Expression implements IVariableExpression
{
  private final String name;

  VariableExpression(final String name) throws ExpressionException
  {
    // catch null name
    catchNullVariable(name);
    this.name = name;
  }

  @Override
  public void accept(final IExpressionVisitor visitor) throws ExpressionException
  {
    visitor.visitVariable(this);
  }

  @Override
  public String toString()
  {
    return name;
  }

  private void catchNullVariable(final String name) throws ExpressionException
  {
    if (name == null)
    {
      throw new ExpressionException("Variable cannot be null.");
    }
  }

  @Override
  public String getName()
  {
    return name;
  }
}