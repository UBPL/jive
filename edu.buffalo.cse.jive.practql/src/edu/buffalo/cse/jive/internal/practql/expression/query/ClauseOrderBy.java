package edu.buffalo.cse.jive.internal.practql.expression.query;

import java.util.ArrayList;
import java.util.List;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.query.IClauseOrderBy;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.expression.unary.ISortedExpression;
import edu.buffalo.cse.jive.practql.expression.unary.SortDirection;
import edu.buffalo.cse.jive.practql.schema.Type;

class ClauseOrderBy implements IClauseOrderBy
{
  private final List<ISortedExpression> orderBy;

  ClauseOrderBy()
  {
    this.orderBy = new ArrayList<ISortedExpression>();
  }

  @Override
  public void accept(final IQueryExpressionVisitor visitor) throws QueryException
  {
    visitor.visitClauseOrderBy(this, false);
  }

  @Override
  public void append(final SortDirection sortDirection, final IExpression expression)
      throws QueryException
  {
    if (expression.getType() == Type.INVALID)
    {
      throw new QueryException(String.format(QueryExpression.ERR_INVALID_CLAUSE_ORDER_BY, size(),
          expression.toStringTyped()));
    }
    try
    {
      orderBy.add(QueryExpression.ef.newSortedExpression(expression, sortDirection));
    }
    catch (final ExpressionException e)
    {
      throw new QueryException("Error appending sorted expression.", e);
    }
  }

  @Override
  public ISortedExpression getMember(final int index)
  {
    return orderBy.get(index);
  }

  @Override
  public int size()
  {
    return orderBy.size();
  }

  @Override
  public String toString()
  {
    final StringBuffer buffer = new StringBuffer("ORDER BY ");
    for (int i = 0; i < orderBy.size(); i++)
    {
      buffer.append(orderBy.get(i).toString());
      if (i < orderBy.size() - 1)
      {
        buffer.append(", ");
      }
    }
    return buffer.toString();
  }
}