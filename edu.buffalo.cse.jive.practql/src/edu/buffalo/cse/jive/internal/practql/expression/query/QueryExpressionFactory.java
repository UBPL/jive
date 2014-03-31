package edu.buffalo.cse.jive.internal.practql.expression.query;

import java.util.List;

import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.query.IClauseFrom;
import edu.buffalo.cse.jive.practql.expression.query.IClauseGroupBy;
import edu.buffalo.cse.jive.practql.expression.query.IClauseHaving;
import edu.buffalo.cse.jive.practql.expression.query.IClauseOrderBy;
import edu.buffalo.cse.jive.practql.expression.query.IClauseSelect;
import edu.buffalo.cse.jive.practql.expression.query.IClauseWhere;
import edu.buffalo.cse.jive.practql.expression.query.INamedQuery;
import edu.buffalo.cse.jive.practql.expression.query.IQuery;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpression;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.query.ISimpleQuery;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.schema.IRelationSchema;

public enum QueryExpressionFactory implements IQueryExpressionFactory
{
  INSTANCE;
  @Override
  public IClauseFrom newClauseFrom()
  {
    return new ClauseFrom();
  }

  @Override
  public IClauseGroupBy newClauseGroupBy()
  {
    return new ClauseGroupBy();
  }

  @Override
  public IClauseHaving newClauseHaving(final IExpression expression) throws QueryException
  {
    return new ClauseHaving(expression);
  }

  @Override
  public IClauseOrderBy newClauseOrderBy()
  {
    return new ClauseOrderBy();
  }

  @Override
  public IClauseSelect newClauseSelect(final boolean isDistinct)
  {
    return new ClauseSelect(isDistinct);
  }

  @Override
  public IClauseWhere newClauseWhere(final IExpression expression) throws QueryException
  {
    return new ClauseWhere(expression);
  }

  @Override
  public INamedQuery newNamedQuery(final IRelationSchema schema, final IQuery query)
  {
    return new NamedQuery(schema, query);
  }

  @Override
  public IQuery newQuery(final ISimpleQuery headQuery) throws QueryException
  {
    return new Query(headQuery);
  }

  @Override
  public IQueryExpression newQueryExpression(final boolean isRecursive,
      final List<INamedQuery> ctes, final IQuery query)
  {
    return new QueryExpression(isRecursive, ctes, query);
  }

  @Override
  public ISimpleQuery newSimpleQuery(final IClauseSelect select, final IClauseFrom from,
      final IClauseWhere where, final IClauseGroupBy groupBy, final IClauseHaving having,
      final IClauseOrderBy orderBy)
  {
    return new SimpleQuery(select, from, where, groupBy, having, orderBy);
  }
}