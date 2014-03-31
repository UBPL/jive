package edu.buffalo.cse.jive.internal.practql.expression.atomic;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.atomic.IWildcardExpression;
import edu.buffalo.cse.jive.practql.schema.Type;

class WildcardExpression implements IWildcardExpression
{
  private final String relVar;

  WildcardExpression()
  {
    this.relVar = "";
  }

  WildcardExpression(final String relVar) throws ExpressionException
  {
    // catch null relation variable
    if (relVar == null)
    {
      throw new ExpressionException("Relation variable cannot be null.");
    }
    this.relVar = relVar;
  }

  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    throw new ExpressionException(
        "Cannot visit a wildcard expression, they must be expanded instead.");
  }

  @Override
  public String getRelationVariable()
  {
    return this.relVar;
  }

  @Override
  public Type getType()
  {
    return Type.INVALID;
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
    return "*";
  }

  @Override
  public String toStringTyped()
  {
    return "*{}";
  }
}