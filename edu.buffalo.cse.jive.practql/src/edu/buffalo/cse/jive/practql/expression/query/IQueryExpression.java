package edu.buffalo.cse.jive.practql.expression.query;

// QueryExpression consists of a possibly empty common table expression followed by a query
// query_expression ::= [with_expression_list] query TK_SEMICOLON
public interface IQueryExpression
{
  public void accept(IQueryExpressionVisitor visitor) throws QueryException;

  // with_expression_list
  public INamedQuery getMember(int index);

  // query
  public IQuery getQuery();

  // TK_RECURSIVE
  public boolean isRecursive();

  // number of CTEs in this query expression
  public int size();
}