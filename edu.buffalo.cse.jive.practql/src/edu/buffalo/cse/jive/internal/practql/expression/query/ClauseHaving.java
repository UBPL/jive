package edu.buffalo.cse.jive.internal.practql.expression.query;

import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.query.IClauseHaving;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.schema.Type;

class ClauseHaving implements IClauseHaving
{
  private final IExpression havingClause;

  ClauseHaving(final IExpression expression) throws QueryException
  {
    if (expression.getType() != Type.BOOLEAN)
    {
      throw new QueryException(String.format(QueryExpression.ERR_INVALID_CLAUSE_HAVING,
          expression.getType(), expression.toStringTyped()));
    }
    this.havingClause = expression;
  }

  @Override
  public void accept(final IQueryExpressionVisitor visitor) throws QueryException
  {
    visitor.visitClauseHaving(this, false);
  }

  @Override
  public IExpression getExpression()
  {
    return havingClause;
  }

  @Override
  public String toString()
  {
    return String.format("HAVING %s", havingClause.toString());
  }
}