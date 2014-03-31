package edu.buffalo.cse.jive.practql.expression.query;

import java.util.List;

import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.schema.IRelationSchema;

// factory for query expressions
public interface IQueryExpressionFactory
{
  public IClauseFrom newClauseFrom();

  public IClauseGroupBy newClauseGroupBy();

  public IClauseHaving newClauseHaving(IExpression expression) throws QueryException;

  public IClauseOrderBy newClauseOrderBy();

  public IClauseSelect newClauseSelect(boolean isDistinct);

  public IClauseWhere newClauseWhere(IExpression expression) throws QueryException;

  public INamedQuery newNamedQuery(IRelationSchema schema, IQuery query);

  public IQuery newQuery(ISimpleQuery headQuery) throws QueryException;

  public IQueryExpression newQueryExpression(boolean isRecursive, List<INamedQuery> ctes,
      IQuery query);

  public ISimpleQuery newSimpleQuery(IClauseSelect select, IClauseFrom from, IClauseWhere where,
      IClauseGroupBy groupBy, IClauseHaving having, IClauseOrderBy orderBy);
}