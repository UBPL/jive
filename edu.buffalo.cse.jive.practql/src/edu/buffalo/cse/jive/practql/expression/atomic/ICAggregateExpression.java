package edu.buffalo.cse.jive.practql.expression.atomic;

import edu.buffalo.cse.jive.practql.expression.IExpression;

// a concrete aggregate computed over an expression
public interface ICAggregateExpression extends IAtomicExpression
{
  public CAggregateType getAggregateType();

  public IExpression getArgument();

  public boolean isDistinct();
}