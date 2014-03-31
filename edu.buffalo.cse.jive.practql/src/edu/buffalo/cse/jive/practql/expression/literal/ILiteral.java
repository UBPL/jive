package edu.buffalo.cse.jive.practql.expression.literal;

import edu.buffalo.cse.jive.practql.expression.atomic.IAtomicExpression;

// marker interface for literal values (interpreted as themselves)
public interface ILiteral extends IAtomicExpression
{
  public Object getValue();
}