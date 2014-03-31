package edu.buffalo.cse.jive.internal.practql.compiler;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.CAggregateType;
import edu.buffalo.cse.jive.practql.expression.atomic.CFunctionType;
import edu.buffalo.cse.jive.practql.expression.atomic.IAggregateExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.IFieldExpression;
import edu.buffalo.cse.jive.practql.expression.nary.IConjunction;
import edu.buffalo.cse.jive.practql.expression.query.IClauseFrom;
import edu.buffalo.cse.jive.practql.expression.query.IClauseGroupBy;
import edu.buffalo.cse.jive.practql.expression.query.IClauseSelect;
import edu.buffalo.cse.jive.practql.expression.query.INamedQuery;
import edu.buffalo.cse.jive.practql.expression.query.ISimpleQuery;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.expression.relational.RelationalConnective;
import edu.buffalo.cse.jive.practql.expression.unary.INamedExpression;
import edu.buffalo.cse.jive.practql.schema.IFieldSchema;
import edu.buffalo.cse.jive.practql.schema.IRelationReference;
import edu.buffalo.cse.jive.practql.schema.ISchemaSignature;

class GroupByRewriter extends AbstractSimpleQueryRewriter
{
  GroupByRewriter(final ISimpleQuery query) throws QueryException
  {
    super(query);
  }

  /**
   * Projects all scalar fields in the SELECT clause (including expressions used as arguments to
   * AGGREGATE functions) and all expressions in the GROUP BY list of the object's query. Uses the
   * same FROM and WHERE clauses and drops the GROUP BY clause.
   * 
   * Returns the *translated* based query.
   */
  private INamedQuery createBaseQuery() throws QueryException
  {
    // create base SPJ query w/o AGGREGATES or GROUP BY
    final String rel = CompilerUtils.newRelationName();
    // reference select for the family of projection queries
    final IClauseSelect relSelect = factory.queryExpressionFactory().newClauseSelect(false);
    // non-encoded scalar fields in the original projection and group by lists
    for (final int i : fieldOther.keySet())
    {
      final INamedExpression fe = fieldOther.get(i);
      relSelect.append(fe.getName(), fe.getExpression());
    }
    // non-encoded scalar expressions in the original group by list only
    for (final int i : groupOther.keySet())
    {
      final IExpression ge = groupOther.get(i);
      relSelect.append(CompilerUtils.newFieldName(), ge);
    }
    // encoded scalar fields in the original projection and group by lists
    for (final int i : fieldEncoded.keySet())
    {
      final INamedExpression fe = fieldEncoded.get(i);
      relSelect.append(fe.getName(), fe.getExpression());
    }
    // encoded scalar expressions in the original group by list only
    for (final int i : groupEncoded.keySet())
    {
      final IExpression ge = groupEncoded.get(i);
      relSelect.append(CompilerUtils.newProjectionName(), ge);
    }
    // non-encoded scalar expressions in aggregates of the original projection list
    for (final int i : fieldAggregateOther.keySet())
    {
      final INamedExpression fe = fieldAggregateOther.get(i);
      final IAggregateExpression agg = (IAggregateExpression) fe.getExpression();
      relSelect.append(CompilerUtils.newFieldName(), agg.getArgument());
    }
    // encoded scalar expressions in aggregates of the original projection list
    for (final int i : fieldAggregateEncoded.keySet())
    {
      final INamedExpression fe = fieldAggregateEncoded.get(i);
      final IAggregateExpression agg = (IAggregateExpression) fe.getExpression();
      relSelect.append(CompilerUtils.newFieldName(), agg.getArgument());
    }
    // create the simple query
    final ISimpleQuery relQuery = factory.queryExpressionFactory().newSimpleQuery(relSelect,
        query.getFrom(), query.getWhere(), null, null, null);
    // compile the simple query
    final SimpleQueryCompiler sqc = new SimpleQueryCompiler();
    // create and return the compiled named query
    return createNamedQuery(rel, sqc.compile(relQuery));
  }

  /**
   * The projected query now has the unfolded years and the scalar groups. We join this query with
   * the base query on the scalar group fields and compute the aggregates of the original query.
   */
  private ISimpleQuery createGroupByQuery(final INamedQuery prjQuery) throws QueryException
  {
    // from clause references the base and the projection query
    final IClauseFrom qFrom = factory.queryExpressionFactory().newClauseFrom();
    qFrom.append(prjQuery.getSchema().getName(), prjQuery.getSchema());
    // select clause
    final IClauseSelect qSelect = factory.queryExpressionFactory().newClauseSelect(false);
    // group by clause
    final IClauseGroupBy qGroupBy = factory.queryExpressionFactory().newClauseGroupBy();
    // field references are created using references to queries
    final IRelationReference relPrj = qFrom.getMember(0);
    // field references are created using signatures of the queries
    final ISchemaSignature sigPrj = prjQuery.getSignature();
    final int group1 = fieldOther.size();
    final int group2 = group1 + groupOther.size();
    final int group3 = group2 + fieldEncoded.size();
    final int group4 = group3 + groupEncoded.size();
    final int group5 = group4 + fieldAggregateOther.size();
    final int group6 = group5 + fieldAggregateEncoded.size();
    // process every field in the projection query
    for (int i = 0; i < group6; i++)
    {
      final IFieldSchema fs = sigPrj.getFieldSchema(i);
      // expression encapsulating the field reference
      final IFieldExpression fe1 = CompilerUtils.createFieldExpression(relPrj, fs);
      // non-aggregated fields
      if (i < group4)
      {
        final IFieldExpression fe2 = CompilerUtils.createFieldExpression(relPrj, fs);
        // project out fields appearing only in the group by list of the original query
        if (i < group1 || (i >= group2 && i < group3))
        {
          qSelect.append(fs.getName(), fe1);
        }
        qGroupBy.append(fe2);
      }
      // compute aggregates on remaining fields
      else
      {
        // the "delta" here are the fields that appear ONLY in the GROUP BY of the original query
        final INamedExpression ne = query.getSelect().getMember(
            i - groupEncoded.size() - groupOther.size());
        IExpression agg;
        try
        {
          agg = factory.expressionFactory().newAggregate(
              ((IAggregateExpression) ne.getExpression()).getAggregateType(), fe1, false);
        }
        catch (final ExpressionException e)
        {
          throw new QueryException("Error creating aggregate field expression for group by query.",
              e);
        }
        // use the name of the original field in the input query
        qSelect.append(ne.getName(), agg);
      }
    }
    // query is concrete since it was generated from compiled queries
    return factory.queryExpressionFactory().newSimpleQuery(qSelect, qFrom, null,
        qGroupBy.size() == 0 ? null : qGroupBy, null, null);
  }

  private INamedQuery createPartitionQuery(final INamedQuery baseQuery) throws QueryException
  {
    // new name for the partition
    final String par = CompilerUtils.newPartitionName();
    // from clause references only the base query
    final IClauseFrom qparFrom = factory.queryExpressionFactory().newClauseFrom();
    qparFrom.append(baseQuery.getSchema().getName(), baseQuery.getSchema());
    // select clause references all non-aggregate fields of the base query
    final IClauseSelect qparSelect = factory.queryExpressionFactory().newClauseSelect(false);
    final IClauseGroupBy qparGroupBy = factory.queryExpressionFactory().newClauseGroupBy();
    // field references are created using a reference to the base query
    final IRelationReference relRef = qparFrom.getMember(0);
    // field references are created using the base query's signature
    final ISchemaSignature baseSignature = baseQuery.getSignature();
    // order of the fields in the base query
    final int group1 = fieldOther.size();
    final int group2 = group1 + groupOther.size();
    final int group3 = group2 + fieldEncoded.size();
    final int group4 = group3 + groupEncoded.size();
    // the partition query ignores aggregate expressions
    for (int i = 0; i < group4; i++)
    {
      final IFieldSchema fs = baseSignature.getFieldSchema(i);
      // expression encapsulating the field reference
      final IFieldExpression fe1 = CompilerUtils.createFieldExpression(relRef, fs);
      // the first two groups of fields appear in both the SELECT and GROUP BY lists
      if (i < group2)
      {
        final IFieldExpression fe2 = CompilerUtils.createFieldExpression(relRef, fs);
        /**
         * We must project fields that appear ONLY in the GROUP BY of the base query since these
         * open the possibility of duplicate tuples in the final query. If we group by these fields
         * here and do not project, this means that aggregates will be computed over larger groups
         * in the final query.
         */
        qparSelect.append(fs.getName(), fe1);
        qparGroupBy.append(fe2);
      }
      // compute partition aggregates on remaining fields
      else
      {
        IExpression agg;
        try
        {
          agg = factory.expressionFactory().newCAggregate(CAggregateType.CPARTITION, fe1, false);
        }
        catch (final ExpressionException e)
        {
          throw new QueryException(
              "Error creating aggregate field expression for partition query.", e);
        }
        qparSelect.append("P" + fs.getName(), agg);
      }
    }
    // partition query is concrete since it was generated from the compiled base query
    final ISimpleQuery qpar = factory.queryExpressionFactory().newSimpleQuery(qparSelect, qparFrom,
        null, qparGroupBy.size() == 0 ? null : qparGroupBy, null, null);
    // create the respective named query
    return createNamedQuery(par, qpar);
  }

  /**
   * Outputs a join of the reference query and the partition query on the grouping attributes. The
   * number of tuples, as compared to the number of tuples in the reference query, should only
   * increase by virtue of the projection.
   */
  private INamedQuery createProjectionQuery(final INamedQuery refQuery, final INamedQuery parQuery,
      final int projectionId) throws QueryException
  {
    // new name for the projection
    final String prj = CompilerUtils.newProjectionName();
    // from clause references the reference and the partition query
    final IClauseFrom qprjFrom = factory.queryExpressionFactory().newClauseFrom();
    qprjFrom.append(refQuery.getSchema().getName(), refQuery.getSchema());
    qprjFrom.append(parQuery.getSchema().getName(), parQuery.getSchema());
    // where clause
    IConjunction join = null;
    // select clause references all fields of the reference query
    final IClauseSelect qprjSelect = factory.queryExpressionFactory().newClauseSelect(false);
    // field references are created using references to queries
    final IRelationReference relRef = qprjFrom.getMember(0);
    final IRelationReference relPar = qprjFrom.getMember(1);
    // field references are created using signatures of the queries
    final ISchemaSignature sigRef = refQuery.getSignature();
    final ISchemaSignature sigPar = parQuery.getSignature();
    final int group1 = fieldOther.size();
    final int group2 = group1 + groupOther.size();
    final int group3 = group2 + fieldEncoded.size();
    final int group4 = group3 + groupEncoded.size();
    final int group5 = group4 + fieldAggregateOther.size();
    final int group6 = group5 + fieldAggregateEncoded.size();
    // at most one field per field in the reference query
    for (int i = 0; i < group6; i++)
    {
      final IFieldSchema fs = sigRef.getFieldSchema(i);
      // expression encapsulating the field reference
      final IFieldExpression fe1 = CompilerUtils.createFieldExpression(relRef, fs);
      if (i - group2 == projectionId)
      {
        final IFieldSchema fsp = sigPar.getFieldSchema(i);
        // expression encapsulating the field reference
        final IFieldExpression fep = CompilerUtils.createFieldExpression(relPar, fsp);
        // expression encapsulating the call to the projection function
        final IExpression fce;
        try
        {
          fce = factory.expressionFactory().newCFunctionCall(CFunctionType.FT_PROJECT, fe1, fep);
        }
        catch (final ExpressionException e)
        {
          throw new QueryException(
              "Error creating projection call expression for projection query.", e);
        }
        qprjSelect.append(fs.getName(), fce);
      }
      else
      {
        qprjSelect.append(fs.getName(), fe1);
        // the first two field groups are joined with the respective partition fields
        if (i < group2)
        {
          final IFieldSchema fsp = sigPar.getFieldSchema(i);
          // expression encapsulating the field reference
          final IFieldExpression fep = CompilerUtils.createFieldExpression(relPar, fsp);
          final IExpression eq;
          try
          {
            eq = factory.expressionFactory().newRelationalExpression(fe1,
                RelationalConnective.RC_EQ, fep);
          }
          catch (final ExpressionException e)
          {
            throw new QueryException("Error creating relational expression for projection query.",
                e);
          }
          try
          {
            if (join == null)
            {
              join = factory.expressionFactory().newConjunction(eq);
            }
            else
            {
              join.append(eq);
            }
          }
          catch (final ExpressionException e)
          {
            throw new QueryException("Error creating join expression for projection query.", e);
          }
        }
      }
    }
    // projection query is concrete since it was generated from compiled queries
    final ISimpleQuery qprj = factory.queryExpressionFactory().newSimpleQuery(qprjSelect, qprjFrom,
        join == null ? null : factory.queryExpressionFactory().newClauseWhere(join), null, null,
        null);
    // create the respective named query
    return createNamedQuery(prj, qprj);
  }

  // true only if the group by has some TP_ENCODED expression
  @Override
  boolean needsRewrite()
  {
    return query.getGroupBy() != null && (!groupEncoded.isEmpty() || !fieldEncoded.isEmpty());
  }

  /**
   * Compiling GROUP BY (with support for AGGREGATES).
   * 
   * <pre>
   * SELECT S, E, A FROM F WHERE W GROUP BY S', E' ORDER BY O
   * 
   * S : non-encoded expressions
   * E : encoded expressions
   * A : aggregate expressions, i.e., AGG1(exp1), ..., AGGn(expn)
   * S': non-encoded expressions, contains S
   * E': encoded expressions, contains E
   * 
   * we also define 
   * 
   * AS: non-encoded expressions associated with the argument expressions of A
   * AE: encoded expressions associated with the argument expressions of A
   * </pre>
   * 
   * The transformation below is performed only is E' is non-empty:
   * 
   * <pre>
   *   BASE : SELECT S' U E' U AS U AE FROM F WHERE W
   *   PART : SELECT S', {AGG_PARTITION(E'i) AS PEi}i FROM BASE GROUP BY S'
   *   PRJi : SELECT S', E1, ..., Ei-1, PROJECT(Ei, PEi) AS Ei, ..., Ek, AS, AE 
   *          FROM PRJi-1, PART WHERE PRJi-1.S' = PART.S'
   *  FINAL : SELECT S, E, A FROM PRJk GROUP BY S', E' ORDER BY O
   * </pre>
   * 
   * Each named query in the above is created as a helper CTE in compiled form and added to
   * helperNamedQueries.
   * 
   */
  @Override
  ISimpleQuery rewrite() throws QueryException
  {
    final INamedQuery baseQuery = createBaseQuery();
    // System.err.println(baseQuery.toString());
    final INamedQuery partitionQuery = createPartitionQuery(baseQuery);
    // System.err.println(partitionQuery.toString());
    INamedQuery prev = baseQuery;
    INamedQuery curr = null;
    // partitions must be projected one at a time
    final int prjCount = fieldEncoded.size() + groupEncoded.size();
    for (int i = 0; i < prjCount; i++)
    {
      // each projection joins with a previous projection or the base query
      curr = createProjectionQuery(prev, partitionQuery, i);
      // System.err.println(curr.toString());
      prev = curr;
    }
    /**
     * The compiled query joins the base and projection queries on their group by fields and
     * computes the original aggregates of the input query.
     */
    final ISimpleQuery compiled = createGroupByQuery(curr);
    // System.err.println(compiled.toString());
    return compiled;
  }
}
