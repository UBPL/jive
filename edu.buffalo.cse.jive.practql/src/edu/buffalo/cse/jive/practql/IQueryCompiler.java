package edu.buffalo.cse.jive.practql;

import edu.buffalo.cse.jive.practql.expression.query.IQueryExpression;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;

public interface IQueryCompiler
{
  public IQueryExpression compile(IQueryExpression expression) throws QueryException;

  public IQueryExpression queryExpression();

  public String queryString();
}
