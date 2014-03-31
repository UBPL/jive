package edu.buffalo.cse.jive.internal.practql.compiler;

import edu.buffalo.cse.jive.practql.expression.atomic.IFieldExpression;
import edu.buffalo.cse.jive.practql.expression.query.IClauseGroupBy;
import edu.buffalo.cse.jive.practql.expression.query.IClauseSelect;
import edu.buffalo.cse.jive.practql.expression.query.ISimpleQuery;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.expression.unary.INamedExpression;

/**
 * Removes the DISTINCT modifier and pushes all fields in the projection list to the GROUP BY list.
 * The rewrite is necessary only if the query has the DISTINCT modifier and has some non-aggregated
 * TP_ENCODED expression in the projection list.
 */
public class DistinctRewriter extends AbstractSimpleQueryRewriter
{
  DistinctRewriter(final ISimpleQuery query) throws QueryException
  {
    super(query);
  }

  @Override
  boolean needsRewrite()
  {
    return hasDistinct && !fieldEncoded.isEmpty();
  }

  @Override
  ISimpleQuery rewrite() throws QueryException
  {
    // new select clause
    final IClauseSelect select = factory.queryExpressionFactory().newClauseSelect(false);
    // new group by clause
    final IClauseGroupBy groupBy = factory.queryExpressionFactory().newClauseGroupBy();
    // new simple query
    final ISimpleQuery newQuery = factory.queryExpressionFactory().newSimpleQuery(select,
        query.getFrom(), query.getWhere(), groupBy, query.getHaving(), query.getOrderBy());
    // copy the old select clause without the DISTINCT
    for (int i = 0; i < query.getSelect().size(); i++)
    {
      final INamedExpression expr = query.getSelect().getMember(i);
      select.append(expr.getName(), expr.getExpression());
      if (expr.getExpression() instanceof IFieldExpression)
      {
        groupBy.append(expr.getExpression());
      }
      else
      {
        throw new QueryException(
            "Partition Query Error: only field expressions allowed in the GROUP BY clause.");
      }
    }
    // otherwise, copy the old group by expressions to the new one
    if (query.getGroupBy() != null)
    {
      // copy the old group by clause
      for (int i = 0; i < query.getGroupBy().size(); i++)
      {
        groupBy.append(query.getGroupBy().getMember(i));
      }
    }
    return newQuery;
  }
}
