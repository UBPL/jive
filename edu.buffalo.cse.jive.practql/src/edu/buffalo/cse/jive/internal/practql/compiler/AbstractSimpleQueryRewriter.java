package edu.buffalo.cse.jive.internal.practql.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.buffalo.cse.jive.internal.practql.Factory;
import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.IAggregateExpression;
import edu.buffalo.cse.jive.practql.expression.query.INamedQuery;
import edu.buffalo.cse.jive.practql.expression.query.ISimpleQuery;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.expression.unary.INamedExpression;
import edu.buffalo.cse.jive.practql.schema.Type;

abstract class AbstractSimpleQueryRewriter
{
  protected final Factory factory;
  protected final Map<Integer, INamedExpression> fieldAggregateEncoded;
  protected final Map<Integer, INamedExpression> fieldAggregateOther;
  protected final Map<Integer, INamedExpression> fieldEncoded;
  protected final Map<Integer, INamedExpression> fieldOther;
  protected final Map<Integer, IExpression> groupEncoded;
  protected final Map<Integer, IExpression> groupOther;
  protected final boolean hasDistinct;
  protected final List<INamedQuery> helperNamedQueries;
  protected final ISimpleQuery query;

  AbstractSimpleQueryRewriter(final ISimpleQuery query) throws QueryException
  {
    this.fieldAggregateEncoded = new HashMap<Integer, INamedExpression>();
    this.fieldAggregateOther = new HashMap<Integer, INamedExpression>();
    this.fieldEncoded = new HashMap<Integer, INamedExpression>();
    this.fieldOther = new HashMap<Integer, INamedExpression>();
    this.groupEncoded = new HashMap<Integer, IExpression>();
    this.groupOther = new HashMap<Integer, IExpression>();
    this.hasDistinct = query.getSelect().isDistinct();
    this.helperNamedQueries = new ArrayList<INamedQuery>();
    this.query = query;
    try
    {
      buildInfo();
    }
    catch (final ExpressionException e)
    {
      throw new QueryException("Error while constructing QueryInfo.", e);
    }
    this.factory = Factory.INSTANCE;
  }

  protected void buildInfo() throws ExpressionException
  {
    // analyze and partition the projection list
    for (int i = 0; i < query.getSelect().size(); i++)
    {
      final Integer index = i;
      final INamedExpression ne = query.getSelect().getMember(i);
      if (ne.isAggregate())
      {
        final IAggregateExpression agg = (IAggregateExpression) ne.getExpression();
        if (agg.getArgument().getType() == Type.TP_ENCODED)
        {
          fieldAggregateEncoded.put(index, ne);
        }
        else
        {
          fieldAggregateOther.put(index, ne);
        }
      }
      else
      {
        if (ne.getType() == Type.TP_ENCODED)
        {
          fieldEncoded.put(index, ne);
        }
        else
        {
          fieldOther.put(index, ne);
        }
      }
    }
    // analyze and partition the group by list
    if (query.getGroupBy() != null)
    {
      for (int i = 0; i < query.getGroupBy().size(); i++)
      {
        final Integer index = i;
        final IExpression ge = query.getGroupBy().getMember(i);
        // don't duplicate fields in the projection list
        if (ge.getType() == Type.TP_ENCODED || ge.getType() == Type.CINTERVAL)
        {
          if (!isProjected(ge, fieldEncoded))
          {
            groupEncoded.put(index, ge);
          }
        }
        else if (!isProjected(ge, fieldOther))
        {
          groupOther.put(index, ge);
        }
      }
    }
  }

  protected INamedQuery createNamedQuery(final String name, final ISimpleQuery query)
      throws QueryException
  {
    // create the named query
    final INamedQuery result = CompilerUtils.createNamedQuery(name, query);
    // register the named query
    helperNamedQueries.add(result);
    // return the named query
    return result;
  }

  protected boolean isProjected(final IExpression exp, final Map<Integer, INamedExpression> fields)
  {
    // make sure this group by expression is not one of the projected fields
    for (final int j : fields.keySet())
    {
      final INamedExpression ne = fields.get(j);
      if (ne.getExpression().toString().equals(exp.toString()))
      {
        return true;
      }
    }
    return false;
  }

  protected ISimpleQuery query()
  {
    return this.query;
  }

  List<INamedQuery> helperQueries()
  {
    return this.helperNamedQueries;
  }

  abstract boolean needsRewrite();

  abstract ISimpleQuery rewrite() throws QueryException;
}
