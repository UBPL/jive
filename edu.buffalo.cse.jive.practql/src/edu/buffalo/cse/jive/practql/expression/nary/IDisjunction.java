package edu.buffalo.cse.jive.practql.expression.nary;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;

// disjuncts connected by OR connectives
public interface IDisjunction extends IUniformExpression
{
  @Override
  public IDisjunction append(IExpression expression) throws ExpressionException;
}