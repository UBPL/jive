package edu.buffalo.cse.jive.internal.practql.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.buffalo.cse.jive.internal.practql.Factory;
import edu.buffalo.cse.jive.practql.IQueryCompiler;
import edu.buffalo.cse.jive.practql.expression.query.IClauseFrom;
import edu.buffalo.cse.jive.practql.expression.query.IClauseGroupBy;
import edu.buffalo.cse.jive.practql.expression.query.IClauseHaving;
import edu.buffalo.cse.jive.practql.expression.query.IClauseOrderBy;
import edu.buffalo.cse.jive.practql.expression.query.IClauseSelect;
import edu.buffalo.cse.jive.practql.expression.query.IClauseWhere;
import edu.buffalo.cse.jive.practql.expression.query.INamedQuery;
import edu.buffalo.cse.jive.practql.expression.query.IQuery;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpression;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.query.ISimpleQuery;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.schema.IDatabaseSchema;

class QueryCompilerVisitor implements IQueryExpressionVisitor, IQueryCompiler
{
  private IQuery currentQuery;
  private final Factory factory;
  private final List<INamedQuery> helperNamedQueries = new ArrayList<INamedQuery>();
  private final List<INamedQuery> namedQueries = new ArrayList<INamedQuery>();
  private IQueryExpression queryExpression;
  private final List<ISimpleQuery> simpleQueries = new ArrayList<ISimpleQuery>();
  private final IDatabaseSchema schema;
  private String recursiveQuery;

  QueryCompilerVisitor(final IDatabaseSchema schema)
  {
    this.factory = Factory.INSTANCE;
    this.schema = schema;
  }

  @Override
  public IQueryExpression compile(final IQueryExpression expression) throws QueryException
  {
    expression.accept(this);
    return queryExpression();
  }

  @Override
  public IQueryExpression queryExpression()
  {
    return queryExpression;
  }

  @Override
  public String queryString()
  {
    return recursiveQuery == null ? queryExpression.toString() : recursiveQuery;
  }

  @Override
  public boolean visitClauseFrom(final IClauseFrom from, final boolean isAfter)
      throws QueryException
  {
    return false;
  }

  @Override
  public boolean visitClauseGroupBy(final IClauseGroupBy groupBy, final boolean isAfter)
      throws QueryException
  {
    return false;
  }

  @Override
  public boolean visitClauseHaving(final IClauseHaving having, final boolean isAfter)
      throws QueryException
  {
    return false;
  }

  @Override
  public boolean visitClauseOrderBy(final IClauseOrderBy orderBy, final boolean isAfter)
      throws QueryException
  {
    return false;
  }

  @Override
  public boolean visitClauseSelect(final IClauseSelect select, final boolean isAfter)
      throws QueryException
  {
    return false;
  }

  @Override
  public boolean visitClauseWhere(final IClauseWhere where, final boolean isAfter)
      throws QueryException
  {
    return false;
  }

  @Override
  public boolean visitCTE(final INamedQuery query, final boolean isAfter) throws QueryException
  {
    if (isAfter)
    {
      // helper named queries are referenced by the queries in the body of the current query
      namedQueries.addAll(helperNamedQueries);
      namedQueries.add(factory.queryExpressionFactory().newNamedQuery(query.getSchema(),
          currentQuery));
      // ready for the next CTE or query
      currentQuery = null;
      helperNamedQueries.clear();
    }
    return true;
  }

  @Override
  public boolean visitQuery(final IQuery query, final boolean isAfter) throws QueryException
  {
    if (isAfter)
    {
      currentQuery = factory.queryExpressionFactory().newQuery(simpleQueries.get(0));
      for (int i = 1; i < simpleQueries.size(); i++)
      {
        currentQuery.append(query.getConnective(i - 1), simpleQueries.get(i));
      }
      // set/bag operations may require normalization
      final SetAndBagRewriter rewriter = new SetAndBagRewriter(currentQuery);
      if (rewriter.needsRewrite())
      {
        currentQuery = rewriter.rewrite();
        helperNamedQueries.addAll(rewriter.helperQueries());
      }
      // ready for the next query
      simpleQueries.clear();
    }
    return true;
  }

  @Override
  public boolean visitQueryExpression(final IQueryExpression expression, final boolean isAfter)
      throws QueryException
  {
    if (!isAfter)
    {
      currentQuery = null;
      queryExpression = null;
      recursiveQuery = null;
      namedQueries.clear();
      helperNamedQueries.clear();
      simpleQueries.clear();
      if (expression.isRecursive())
      {
        final RecursiveQueryCompiler rqc = new RecursiveQueryCompiler(schema,
            expression.getMember(0));
        final CompiledRecursiveQueryParts recursiveParts = rqc.compile(expression.getMember(0));
        recursiveQuery = recursiveParts.compiledString();
        // query as an expression
        final IQueryExpression newExpression = factory.queryExpressionFactory().newQueryExpression(
            false, Collections.<INamedQuery> emptyList(), expression.getQuery());
        // compile the query
        final QueryCompilerVisitor qcv = new QueryCompilerVisitor(schema);
        newExpression.accept(qcv);
        // the compiled query expression may include CTEs
        recursiveQuery += qcv.queryExpression();
        return false;
      }
    }
    else
    {
      namedQueries.addAll(helperNamedQueries);
      queryExpression = factory.queryExpressionFactory().newQueryExpression(
          expression.isRecursive(), namedQueries, currentQuery);
      helperNamedQueries.clear();
    }
    return true;
  }

  @Override
  public boolean visitQueryMember(final ISimpleQuery rawQuery, final boolean isAfter)
      throws QueryException
  {
    if (!isAfter)
    {
      ISimpleQuery query = rawQuery;
      // 1. REWRITE DISTINCT AS GROUP BY
      final DistinctRewriter dr = new DistinctRewriter(query);
      if (dr.needsRewrite())
      {
        query = dr.rewrite();
      }
      // 2. REWRITE AGGREGATION AND GROUP BY
      final GroupByRewriter gbr = new GroupByRewriter(query);
      if (gbr.needsRewrite())
      {
        query = gbr.rewrite();
        // add all helper queries created during the transformation
        helperNamedQueries.addAll(gbr.helperQueries());
      }
      // translate the simple query
      final SimpleQueryCompiler sqc = new SimpleQueryCompiler();
      simpleQueries.add(sqc.compile(query));
    }
    return false;
  }
}