package edu.buffalo.cse.jive.internal.practql.expression.atomic;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.atomic.ICIntervalExpression;
import edu.buffalo.cse.jive.practql.schema.Type;

class CIntervalExpression implements ICIntervalExpression
{
  private final IExpression left;
  private final IExpression right;

  CIntervalExpression(final IExpression lowerBound, final IExpression upperBound)
      throws ExpressionException
  {
    this.left = lowerBound;
    this.right = upperBound;
  }

  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    // concrete types are not visited
  }

  @Override
  public IExpression getLeft()
  {
    return this.left;
  }

  @Override
  public IExpression getRight()
  {
    return this.right;
  }

  @Override
  public Type getType()
  {
    return Type.CINTERVAL;
  }

  @Override
  public boolean isAggregate()
  {
    return false;
  }

  @Override
  public boolean isLiteral()
  {
    return false;
  }

  @Override
  public String toString()
  {
    return "(" + left.toString() + ", " + right.toString() + ")::cinterval";
  }

  @Override
  public String toStringTyped()
  {
    return toString() + "{cinterval}";
  }
}