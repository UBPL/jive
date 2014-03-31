package edu.buffalo.cse.jive.internal.practql.compiler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.buffalo.cse.jive.internal.practql.Factory;
import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.atomic.CAggregateType;
import edu.buffalo.cse.jive.practql.expression.atomic.CFunctionType;
import edu.buffalo.cse.jive.practql.expression.atomic.IFieldExpression;
import edu.buffalo.cse.jive.practql.expression.nary.IConjunction;
import edu.buffalo.cse.jive.practql.expression.query.IClauseFrom;
import edu.buffalo.cse.jive.practql.expression.query.IClauseGroupBy;
import edu.buffalo.cse.jive.practql.expression.query.IClauseSelect;
import edu.buffalo.cse.jive.practql.expression.query.INamedQuery;
import edu.buffalo.cse.jive.practql.expression.query.IQuery;
import edu.buffalo.cse.jive.practql.expression.query.ISimpleQuery;
import edu.buffalo.cse.jive.practql.expression.query.QueryConnective;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.expression.relational.RelationalConnective;
import edu.buffalo.cse.jive.practql.expression.unary.INamedExpression;
import edu.buffalo.cse.jive.practql.schema.IFieldSchema;
import edu.buffalo.cse.jive.practql.schema.IRelationReference;
import edu.buffalo.cse.jive.practql.schema.ISchemaFactory;
import edu.buffalo.cse.jive.practql.schema.ISchemaSignature;
import edu.buffalo.cse.jive.practql.schema.Type;

/**
 * Rewriter for queries of the form: Q1 setop [ALL] Q2 setop [ALL] ... setop [ALL] Qn. Requires that
 * all simple queries be compiled. If the query contains more than one member and the its signature
 * contains interval fields, it must be rewritten for set/bag operations.
 * 
 * TODO: partition and projection methods are almost identical to those in GroupByRewriter. Check if
 * it is a good idea to generalize them so that a single code base/logic is maintained.
 */
class SetAndBagRewriter
{
  protected final IExpressionFactory ef = Factory.INSTANCE.expressionFactory();
  protected final ISchemaFactory sf = Factory.INSTANCE.schemaFactory();
  protected final Set<Integer> fieldEncoded;
  protected final Set<Integer> fieldOther;
  protected final List<INamedQuery> finalProjections;
  protected final List<INamedQuery> helperNamedQueries;
  protected final List<INamedQuery> unionQueryMembers;
  protected final IQuery query;
  private final Factory factory;

  SetAndBagRewriter(final IQuery query) throws QueryException
  {
    this.factory = Factory.INSTANCE;
    this.fieldEncoded = new HashSet<Integer>();
    this.fieldOther = new HashSet<Integer>();
    this.finalProjections = new ArrayList<INamedQuery>();
    this.helperNamedQueries = new ArrayList<INamedQuery>();
    this.query = query;
    // partition the projection list
    for (int i = 0; i < query.getSignature().size(); i++)
    {
      final Integer index = i;
      final IFieldSchema fs = query.getSignature().getFieldSchema(i);
      if (fs.getType() == Type.CINTERVAL)
      {
        fieldEncoded.add(index);
      }
      else
      {
        fieldOther.add(index);
      }
    }
    this.unionQueryMembers = new ArrayList<INamedQuery>();
  }

  private IQuery createNormalizedQuery() throws QueryException
  {
    IQuery result = null;
    for (int i = 0; i < query.size(); i++)
    {
      final INamedQuery SNQi = finalProjections.get(i);
      final ISimpleQuery q = createSPQuery(SNQi);
      if (result == null)
      {
        result = factory.queryExpressionFactory().newQuery(q);
      }
      else
      {
        result.append(query.getConnective(i - 1), q);
      }
    }
    return result;
  }

  private INamedQuery createPartitionQuery(final INamedQuery baseQuery) throws QueryException
  {
    // new name for the partition
    final String par = CompilerUtils.newPartitionName();
    // from clause references only the base query
    final IClauseFrom from = factory.queryExpressionFactory().newClauseFrom();
    from.append(baseQuery.getSchema().getName(), baseQuery.getSchema());
    // select clause references all non-aggregate fields of the base query
    final IClauseSelect select = factory.queryExpressionFactory().newClauseSelect(false);
    final IClauseGroupBy groupBy = factory.queryExpressionFactory().newClauseGroupBy();
    // field references are created using a reference to the base query
    final IRelationReference relRef = from.getMember(0);
    // field references are created using the base query's signature
    final ISchemaSignature baseSignature = baseQuery.getSignature();
    // order of the fields in the base query
    final int group1 = fieldOther.size();
    final int group2 = group1 + fieldEncoded.size();
    // the partition query ignores aggregate expressions
    for (int i = 0; i < group2; i++)
    {
      final IFieldSchema fs = baseSignature.getFieldSchema(i);
      // expression encapsulating the field reference
      final IFieldExpression fe1 = CompilerUtils.createFieldExpression(relRef, fs);
      // the first group of fields appears in both the SELECT and GROUP BY lists
      if (i < group1)
      {
        final IFieldExpression fe2 = CompilerUtils.createFieldExpression(relRef, fs);
        /**
         * We must project fields that appear ONLY in the GROUP BY of the base query since these
         * open the possibility of duplicate tuples in the final query. If we group by these fields
         * here and do not project, this means that aggregates will be computed over larger groups
         * in the final query.
         */
        select.append(fs.getName(), fe1);
        groupBy.append(fe2);
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
        select.append("P" + fs.getName(), agg);
      }
    }
    // partition query is concrete since it was generated from the compiled base query
    final ISimpleQuery qpar = factory.queryExpressionFactory().newSimpleQuery(select, from, null,
        groupBy.size() == 0 ? null : groupBy, null, null);
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
    final int group2 = group1 + fieldEncoded.size();
    // at most one field per field in the reference query
    for (int i = 0; i < group2; i++)
    {
      final IFieldSchema fs = sigRef.getFieldSchema(i);
      // expression encapsulating the field reference
      final IFieldExpression fe1 = CompilerUtils.createFieldExpression(relRef, fs);
      if (i - group1 == projectionId)
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
        // the fields in the first group are joined with the respective partition fields
        if (i < group1)
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

  private void createProjections(final INamedQuery partition) throws QueryException
  {
    // partitions must be projected one at a time
    final int prjCount = fieldEncoded.size();
    // for each union query member, project all the encoded fields
    for (final INamedQuery SQNi : unionQueryMembers)
    {
      INamedQuery prev = SQNi;
      INamedQuery curr = null;
      for (int i = 0; i < prjCount; i++)
      {
        // each projection joins with a previous projection or the base query
        curr = createProjectionQuery(prev, partition, i);
        // System.err.println(curr.toString());
        prev = curr;
      }
      // each final projection corresponds to a normalized SQi
      finalProjections.add(curr);
    }
  }

  // WITH Foo(S) AS ( ... ) --> SELECT S FROM Foo
  private ISimpleQuery createSPQuery(final INamedQuery namedQuery) throws QueryException
  {
    // FROM clause for the SP query
    final IClauseFrom from = factory.queryExpressionFactory().newClauseFrom();
    from.append(namedQuery.getSchema().getName(), namedQuery.getSchema());
    // field references are created using a reference to the input query
    final IRelationReference relRef = from.getMember(0);
    // SELECT clause for the SP query
    final IClauseSelect select = factory.queryExpressionFactory().newClauseSelect(false);
    // project all fields in the named query
    for (int i = 0; i < namedQuery.getSignature().size(); i++)
    {
      final IFieldSchema fs = namedQuery.getSignature().getFieldSchema(i);
      // expression encapsulating the field reference
      final IFieldExpression fe1 = CompilerUtils.createFieldExpression(relRef, fs);
      // project the field
      select.append(fs.getName(), fe1);
    }
    // return the query
    return factory.queryExpressionFactory().newSimpleQuery(select, from, null, null, null, null);
  }

  private INamedQuery createUnionQuery() throws QueryException
  {
    INamedQuery union = null;
    for (int i = 0; i < query.size(); i++)
    {
      final INamedQuery SNQi = unionQueryMembers.get(i);
      final ISimpleQuery q = createSPQuery(SNQi);
      if (union == null)
      {
        union = createNamedQuery(CompilerUtils.newUnionName(), q);
      }
      else
      {
        union.append(QueryConnective.QC_SET_UNION, q);
      }
    }
    return union;
  }

  // re-order fields so they appear non-encoded first, encoded last
  private void createUnionQueryMembers() throws QueryException
  {
    for (int i = 0; i < query.size(); i++)
    {
      final ISimpleQuery source = query.getMember(i);
      // SELECT clause for the query
      final IClauseSelect select = factory.queryExpressionFactory().newClauseSelect(false);
      // non-encoded scalar fields in the original projection and group by lists
      for (final int j : fieldOther)
      {
        final INamedExpression fe = source.getSelect().getMember(j);
        select.append(fe.getName(), fe.getExpression());
      }
      // encoded scalar fields in the original projection and group by lists
      for (final int j : fieldEncoded)
      {
        final INamedExpression fe = source.getSelect().getMember(j);
        select.append(fe.getName(), fe.getExpression());
      }
      // source query with the re-ordered projection
      final ISimpleQuery SQi = factory.queryExpressionFactory().newSimpleQuery(select,
          source.getFrom(), source.getWhere(), source.getGroupBy(), null, null);
      // named query
      final INamedQuery SNQi = createNamedQuery(CompilerUtils.newRelationName(), SQi);
      // append to the union
      unionQueryMembers.add(SNQi);
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

  List<INamedQuery> helperQueries()
  {
    return this.helperNamedQueries;
  }

  boolean needsRewrite()
  {
    return query.size() > 1 && fieldEncoded.size() > 0;
  }

  IQuery query()
  {
    return this.query;
  }

  /**
   * Compiling set/bag operations.
   * 
   * <pre>
   * SELECT S, E FROM SQ1 WHERE W1 GROUP BY G1
   * SETOP [ALL]
   * ...
   * SELECT S, E FROM SQp WHERE Wp GROUP BY Gp
   * 
   * S : non-encoded expressions
   * E : encoded expressions, ||E|| = k
   * </pre>
   * 
   * The transformation below is performed only if E is non-empty:
   * 
   * <pre>
   *   SNQi : SELECT S, E FROM SQi WHERE Wi GROUP BY Gi
   *    UNI : SELECT S, E FROM SNQi UNION ... UNION SELECT S, E FROM SNQp
   *   PART : SELECT S, {AGG_PARTITION(Ei) AS PEi}i FROM UNI GROUP BY S
   * PRJi,1 : SELECT S, PROJECT(PE1) AS E1, ..., PEk 
   *          FROM SNQi, PART WHERE SNQi.S = PART.S
   * PRJi,j : SELECT S, E1, PROJECT(PEj) AS Ej, ..., PEk 
   *          FROM PRJi,j-1, PART WHERE PRJi,j-1.S = PART.S
   *  FINAL : SELECT S, E FROM PRJ1,k
   *          SETOP [ALL]
   *          ...
   *          SELECT S, E FROM PRJp,k
   * </pre>
   * 
   * Each named query in the above is created as a helper CTE in compiled form and added to
   * helperNamedQueries.
   * 
   */
  IQuery rewrite() throws QueryException
  {
    // 1. create each of the SNQi queries and add them to the union query member map
    createUnionQueryMembers();
    // 2. create union query over which the partitions will be constructed
    final INamedQuery union = createUnionQuery();
    // 3. create the partition query
    final INamedQuery partition = createPartitionQuery(union);
    // 4. for each union query member (P) and encoded field (K), create a projection query (PxK)
    createProjections(partition);
    /**
     * 5. final query replaces each query member in Query with an SP query over the respective last
     * partition query
     */
    return createNormalizedQuery();
  }
}
