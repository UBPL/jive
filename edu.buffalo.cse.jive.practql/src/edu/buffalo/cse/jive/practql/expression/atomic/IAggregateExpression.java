package edu.buffalo.cse.jive.practql.expression.atomic;

import edu.buffalo.cse.jive.practql.expression.IExpression;

// an aggregate computed over an expression
public interface IAggregateExpression extends IAtomicExpression
{
  public AggregateType getAggregateType();

  public IExpression getArgument();

  public boolean isDistinct();
}