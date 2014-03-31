package edu.buffalo.cse.jive.practql.expression.query;

import edu.buffalo.cse.jive.practql.expression.IExpression;

public interface IClauseGroupBy extends IQueryClause
{
  public void append(IExpression expression) throws QueryException;

  public IExpression getMember(int index);

  public int size();
}