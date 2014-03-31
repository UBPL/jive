package edu.buffalo.cse.jive.practql.expression.query;

// visitor for query expressions
public interface IQueryExpressionVisitor
{
  public boolean visitClauseFrom(IClauseFrom from, boolean isAfter) throws QueryException;

  public boolean visitClauseGroupBy(IClauseGroupBy groupBy, boolean isAfter) throws QueryException;

  public boolean visitClauseHaving(IClauseHaving having, boolean isAfter) throws QueryException;

  public boolean visitClauseOrderBy(IClauseOrderBy orderBy, boolean isAfter) throws QueryException;

  public boolean visitClauseSelect(IClauseSelect select, boolean isAfter) throws QueryException;

  public boolean visitClauseWhere(IClauseWhere where, boolean isAfter) throws QueryException;

  public boolean visitCTE(INamedQuery query, boolean isAfter) throws QueryException;

  public boolean visitQuery(IQuery query, boolean isAfter) throws QueryException;

  public boolean visitQueryExpression(IQueryExpression expression, boolean isAfter)
      throws QueryException;

  public boolean visitQueryMember(ISimpleQuery query, boolean isAfter) throws QueryException;
}