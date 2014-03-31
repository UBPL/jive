package edu.buffalo.cse.jive.internal.practql.expression.query;

import edu.buffalo.cse.jive.internal.practql.TypeUtils;
import edu.buffalo.cse.jive.practql.expression.query.IClauseFrom;
import edu.buffalo.cse.jive.practql.expression.query.IClauseGroupBy;
import edu.buffalo.cse.jive.practql.expression.query.IClauseHaving;
import edu.buffalo.cse.jive.practql.expression.query.IClauseOrderBy;
import edu.buffalo.cse.jive.practql.expression.query.IClauseSelect;
import edu.buffalo.cse.jive.practql.expression.query.IClauseWhere;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.query.ISimpleQuery;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.expression.unary.INamedExpression;
import edu.buffalo.cse.jive.practql.schema.ISchemaSignature;

class SimpleQuery implements ISimpleQuery
{
  private final IClauseFrom from;
  private final IClauseGroupBy groupBy;
  private final IClauseHaving having;
  private final IClauseOrderBy orderBy;
  private final IClauseSelect select;
  private final IClauseWhere where;

  SimpleQuery(final IClauseSelect select, final IClauseFrom from, final IClauseWhere where,
      final IClauseGroupBy groupBy, final IClauseHaving having, final IClauseOrderBy orderBy)
  {
    this.select = select;
    this.from = from;
    this.where = where;
    this.groupBy = groupBy;
    this.having = having;
    this.orderBy = orderBy;
  }

  @Override
  public void accept(final IQueryExpressionVisitor visitor) throws QueryException
  {
    if (visitor.visitQueryMember(this, false))
    {
      select.accept(visitor);
      from.accept(visitor);
      if (where != null)
      {
        where.accept(visitor);
      }
      if (groupBy != null)
      {
        groupBy.accept(visitor);
      }
      if (having != null)
      {
        having.accept(visitor);
      }
      if (orderBy != null)
      {
        orderBy.accept(visitor);
      }
      // allow post-processing
      visitor.visitQueryMember(this, true);
    }
  }

  @Override
  public IClauseFrom getFrom()
  {
    return from;
  }

  @Override
  public IClauseGroupBy getGroupBy()
  {
    return groupBy;
  }

  @Override
  public IClauseHaving getHaving()
  {
    return having;
  }

  @Override
  public IClauseOrderBy getOrderBy()
  {
    return orderBy;
  }

  @Override
  public IClauseSelect getSelect()
  {
    return select;
  }

  @Override
  public ISchemaSignature getSignature() throws QueryException
  {
    return select.getSignature();
  }

  @Override
  public IClauseWhere getWhere()
  {
    return where;
  }

  @Override
  public boolean isSchemaCompatible(final ISimpleQuery query)
  {
    if (query.getSelect().size() != select.size())
    {
      return false;
    }
    for (int i = 0; i < select.size(); i++)
    {
      final INamedExpression ne1 = select.getMember(i);
      final INamedExpression ne2 = query.getSelect().getMember(i);
      if (!TypeUtils.isSchemaCompatible(ne1.getType(), ne2.getType()))
      {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString()
  {
    final StringBuffer buffer = new StringBuffer(select.toString());
    buffer.append(" " + from.toString());
    buffer.append(where == null ? "" : " " + where.toString());
    buffer.append(groupBy == null ? "" : " " + groupBy.toString());
    buffer.append(having == null ? "" : " " + having.toString());
    buffer.append(orderBy == null ? "" : " " + orderBy.toString());
    return buffer.toString();
  }
}