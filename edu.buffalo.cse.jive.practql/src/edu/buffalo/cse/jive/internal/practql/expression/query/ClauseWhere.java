package edu.buffalo.cse.jive.internal.practql.expression.query;

import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.query.IClauseWhere;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.schema.Type;

class ClauseWhere implements IClauseWhere
{
  private final IExpression whereClause;

  ClauseWhere(final IExpression expression) throws QueryException
  {
    if (expression.getType() != Type.BOOLEAN)
    {
      throw new QueryException(String.format(QueryExpression.ERR_INVALID_CLAUSE_WHERE,
          expression.getType(), expression.toStringTyped()));
    }
    whereClause = expression;
  }

  @Override
  public void accept(final IQueryExpressionVisitor visitor) throws QueryException
  {
    visitor.visitClauseWhere(this, false);
  }

  @Override
  public IExpression getExpression()
  {
    return whereClause;
  }

  @Override
  public String toString()
  {
    return String.format("WHERE %s", whereClause.toString());
  }
}