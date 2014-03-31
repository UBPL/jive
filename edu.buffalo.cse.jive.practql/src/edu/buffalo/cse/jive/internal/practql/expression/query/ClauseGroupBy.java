package edu.buffalo.cse.jive.internal.practql.expression.query;

import java.util.ArrayList;
import java.util.List;

import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.query.IClauseGroupBy;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.schema.Type;

class ClauseGroupBy implements IClauseGroupBy
{
  private final List<IExpression> groupBy;

  ClauseGroupBy()
  {
    this.groupBy = new ArrayList<IExpression>();
  }

  @Override
  public void accept(final IQueryExpressionVisitor visitor) throws QueryException
  {
    visitor.visitClauseGroupBy(this, false);
  }

  @Override
  public void append(final IExpression expression) throws QueryException
  {
    if (expression.getType() == Type.INVALID)
    {
      throw new QueryException(String.format(QueryExpression.ERR_INVALID_CLAUSE_GROUP_BY, size(),
          expression.toStringTyped()));
    }
    groupBy.add(expression);
  }

  @Override
  public IExpression getMember(final int index)
  {
    return groupBy.get(index);
  }

  @Override
  public int size()
  {
    return groupBy.size();
  }

  @Override
  public String toString()
  {
    final StringBuffer buffer = new StringBuffer("GROUP BY ");
    for (int i = 0; i < groupBy.size(); i++)
    {
      buffer.append(groupBy.get(i).toString());
      if (i < groupBy.size() - 1)
      {
        buffer.append(", ");
      }
    }
    return buffer.toString();
  }
}