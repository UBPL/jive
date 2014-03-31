package edu.buffalo.cse.jive.practql.expression.relational;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;

// factory for relational expressions
public interface IRelationalExpressionFactory
{
  public IExpression newRelationalExpression(IExpression lhs, RelationalConnective rc,
      IExpression rhs) throws ExpressionException;
}