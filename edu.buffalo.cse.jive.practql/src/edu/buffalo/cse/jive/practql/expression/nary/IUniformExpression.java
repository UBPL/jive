package edu.buffalo.cse.jive.practql.expression.nary;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;

// n-ary expression in which members are connected by a fixed connective
public interface IUniformExpression extends INAryExpression
{
  public IUniformExpression append(IExpression expression) throws ExpressionException;
}