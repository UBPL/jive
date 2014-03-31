package edu.buffalo.cse.jive.practql.expression.nary;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;

// multiplicands connected by multiplication connectives
public interface IMultiplication extends IHeterogeneousExpression<MultiplicationConnective>
{
  @Override
  public IMultiplication append(MultiplicationConnective connective, IExpression expression)
      throws ExpressionException;
}