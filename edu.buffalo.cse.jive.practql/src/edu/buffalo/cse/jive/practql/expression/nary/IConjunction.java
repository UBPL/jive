package edu.buffalo.cse.jive.practql.expression.nary;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;

// conjuncts connected by AND connectives
public interface IConjunction extends IUniformExpression
{
  @Override
  public IConjunction append(IExpression expression) throws ExpressionException;
}