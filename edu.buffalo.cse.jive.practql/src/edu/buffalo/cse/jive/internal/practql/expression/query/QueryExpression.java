package edu.buffalo.cse.jive.internal.practql.expression.query;

import java.util.List;

import edu.buffalo.cse.jive.internal.practql.Factory;
import edu.buffalo.cse.jive.practql.expression.IExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.query.INamedQuery;
import edu.buffalo.cse.jive.practql.expression.query.IQuery;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpression;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.schema.ISchemaFactory;

class QueryExpression implements IQueryExpression
{
  static final IExpressionFactory ef = Factory.INSTANCE.expressionFactory();
  static final String ERR_FROM_FIELD_AMBIGUOUS = "Ambiguous field reference: field '%s' found in multiple relations in the FROM clause.";
  static final String ERR_FROM_FIELD_NOT_FOUND = "Invalid field reference: field '%s' not found in any relation in the FROM clause.";
  static final String ERR_INVALID_CLAUSE_GROUP_BY = "Syntax error. Type checking for the GROUP BY clause failed at position %d. Expression: %s.";
  static final String ERR_INVALID_CLAUSE_HAVING = "Syntax error. Type checking for the HAVING clause failed. Expected a boolean expression but found %s instead. Expression: %s.";
  static final String ERR_INVALID_CLAUSE_ORDER_BY = "Syntax error. Type checking for the ORDER BY clause failed at position %d. Expression: %s.";
  static final String ERR_INVALID_CLAUSE_WHERE = "Syntax error. Type checking for the WHERE clause failed. Expected a boolean expression but found %s instead. Expression: %s.";
  static final String ERR_INVALID_FIELD_TYPE = "Cannot determine the type of field '%s' in the query.";
  static final String ERR_MEMBER_ARITY_MISMATCH = "Member arity mismatch: expected a projection list with %d elements but found %d elements instead.";
  static final String ERR_MEMBER_TYPE_MISMATCH = "Type mismatch: type %s in the query signature is incompatible with type %s in member query %d.";
  static final String ERR_REPEATED_FIELDNAME = "Field name '%s' specified more than once in the SELECT clause.";
  static final String ERR_REPEATED_TABLENAME = "Table name '%s' specified more than once.";
  static final String ERR_UNDEFINED_TUPLEVAR = "Invalid wildcard reference: tuple variable '%s' not defined in the FROM clause.";
  static final ISchemaFactory sf = Factory.INSTANCE.schemaFactory();
  private final List<INamedQuery> ctes;
  private final boolean isRecursive;
  private final IQuery query;

  QueryExpression(final boolean isRecursive, final List<INamedQuery> ctes, final IQuery query)
  {
    this.isRecursive = isRecursive;
    this.ctes = ctes;
    this.query = query;
  }

  @Override
  public void accept(final IQueryExpressionVisitor visitor) throws QueryException
  {
    if (visitor.visitQueryExpression(this, false))
    {
      // visit CTEs
      for (final INamedQuery nq : ctes)
      {
        nq.accept(visitor);
      }
      // visit the actual query
      query.accept(visitor);
      // allow post-processing
      visitor.visitQueryExpression(this, true);
    }
  }

  @Override
  public INamedQuery getMember(final int index)
  {
    return ctes.get(index);
  }

  @Override
  public IQuery getQuery()
  {
    return query;
  }

  @Override
  public boolean isRecursive()
  {
    return isRecursive;
  }

  @Override
  public int size()
  {
    return ctes.size();
  }

  @Override
  public String toString()
  {
    final StringBuffer buffer = new StringBuffer("");
    if (ctes.size() > 0)
    {
      buffer.append("WITH ");
      if (isRecursive)
      {
        buffer.append("RECURSIVE");
      }
      buffer.append("\n");
      for (int i = 0; i < ctes.size(); i++)
      {
        buffer.append(ctes.get(i).toString());
        if (i < ctes.size() - 1)
        {
          buffer.append(", ");
        }
        buffer.append("\n");
      }
    }
    buffer.append(query.toString());
    buffer.append(";");
    return buffer.toString();
  }
}
