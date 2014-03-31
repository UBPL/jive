package edu.buffalo.cse.jive.practql.expression.query;

import edu.buffalo.cse.jive.practql.expression.IExpression;

public interface IClauseHaving extends IQueryClause
{
  public IExpression getExpression();
}