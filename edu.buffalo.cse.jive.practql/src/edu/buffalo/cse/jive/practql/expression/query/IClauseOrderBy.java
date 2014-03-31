package edu.buffalo.cse.jive.practql.expression.query;

import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.unary.ISortedExpression;
import edu.buffalo.cse.jive.practql.expression.unary.SortDirection;

public interface IClauseOrderBy extends IQueryClause
{
  public void append(SortDirection sortDirection, IExpression expression) throws QueryException;

  public ISortedExpression getMember(int index);

  public int size();
}