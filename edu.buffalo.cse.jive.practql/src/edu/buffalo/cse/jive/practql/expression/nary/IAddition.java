package edu.buffalo.cse.jive.practql.expression.nary;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;

// summands connected by addition connectives
public interface IAddition extends IHeterogeneousExpression<AdditionConnective>
{
  @Override
  public IAddition append(AdditionConnective connective, IExpression expression)
      throws ExpressionException;
}