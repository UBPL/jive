package edu.buffalo.cse.jive.practql.expression.atomic;

import edu.buffalo.cse.jive.practql.expression.IExpression;

// interface for concrete interval constructor expressions
public interface ICIntervalExpression extends IExpression
{
  public IExpression getLeft();

  public IExpression getRight();
}
