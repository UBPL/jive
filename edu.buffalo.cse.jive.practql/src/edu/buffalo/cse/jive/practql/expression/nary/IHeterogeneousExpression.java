package edu.buffalo.cse.jive.practql.expression.nary;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;

// n-ary expression in which members are connected by possibly distinct connectives
public interface IHeterogeneousExpression<C> extends INAryExpression
{
  public IHeterogeneousExpression<C> append(C connective, IExpression expression)
      throws ExpressionException;

  public C getConnective(int index);
}