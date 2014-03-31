package edu.buffalo.cse.jive.practql.expression.query;

// marker interface for all query clauses
public interface IQueryClause
{
  public void accept(IQueryExpressionVisitor visitor) throws QueryException;
}