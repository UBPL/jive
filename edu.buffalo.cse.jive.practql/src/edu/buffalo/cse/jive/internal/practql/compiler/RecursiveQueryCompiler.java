package edu.buffalo.cse.jive.internal.practql.compiler;

import static edu.buffalo.cse.jive.practql.expression.query.QueryConnective.QC_BAG_DIFFERENCE;
import static edu.buffalo.cse.jive.practql.expression.query.QueryConnective.QC_SET_DIFFERENCE;

import java.util.Collections;

import edu.buffalo.cse.jive.internal.practql.Factory;
import edu.buffalo.cse.jive.practql.expression.atomic.IFieldExpression;
import edu.buffalo.cse.jive.practql.expression.query.IClauseFrom;
import edu.buffalo.cse.jive.practql.expression.query.IClauseSelect;
import edu.buffalo.cse.jive.practql.expression.query.INamedQuery;
import edu.buffalo.cse.jive.practql.expression.query.IQuery;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpression;
import edu.buffalo.cse.jive.practql.expression.query.ISimpleQuery;
import edu.buffalo.cse.jive.practql.expression.query.QueryConnective;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.parser.IParser;
import edu.buffalo.cse.jive.practql.parser.ParserException;
import edu.buffalo.cse.jive.practql.schema.IDatabaseSchema;
import edu.buffalo.cse.jive.practql.schema.IFieldSchema;
import edu.buffalo.cse.jive.practql.schema.IRelationReference;
import edu.buffalo.cse.jive.practql.schema.IRelationSchema;
import edu.buffalo.cse.jive.practql.schema.SchemaException;
import edu.buffalo.cse.jive.practql.schema.Type;

/**
 * If some FROM clause reference this query's name, the query is recursive. Therefore, it must be
 * treated differently by the compiler. Recursive queries are expected to have a particular form,
 * which is ASSUMED by the compiler. This introduces no difficulties because the form in which
 * recursive queries must be given is easily obtained by a preliminary rewriting phase. In
 * particular, recursive queries are expected to have the following form:
 * 
 * <pre>
 *   NAME(X, T) AS (
 *     base_case UOP recursive_case [EOP except_case]
 *   )
 * 
 *   The base_case query is a simple query and has the form
 *   
 *     SELECT X1, T1 FROM R1, ..., Rm WHERE W
 *  
 *   where NAME != Rj for all j. The recursive_case query is a simple query and has the form
 *  
 *     SELECT X2, T2 FROM S1, ..., Sn WHERE X
 *  
 *   where NAME == Rj for exactly one j. And the except_case is an optional simple query and
 *   has the form
 *  
 *     SELECT X3, T3 FROM U1, ..., Up WHERE Y
 *  
 *   where NAME != Rj for all j. 
 *  
 *   Further, X is a set of non-encoded temporal attributes, T is a set of encoded temporal 
 *   attributes, UOP is a union operator, and EOP is a difference operator. Set operations,
 *   aggregates, grouping, and other complex operations can be factored out from the simple 
 *   queries using CTEs.
 * </pre>
 */
class RecursiveQueryCompiler
{
  private final Factory factory;
  private final SimpleQueryCompiler compiler;
  private final IRelationSchema __TNP;
  private final IDatabaseSchema schema;

  RecursiveQueryCompiler(final IDatabaseSchema schema, final INamedQuery query)
      throws QueryException
  {
    this.schema = schema;
    this.factory = Factory.INSTANCE;
    this.compiler = new SimpleQueryCompiler();
    IRelationSchema TNP;
    try
    {
      TNP = factory.schemaFactory()
          .newRelationSchema(CompilerUtils.newTNPName(), query.getSchema());
      final IFieldSchema fs = factory.schemaFactory().newFieldSchema("__round", Type.INTEGER);
      TNP.getSignature().append(fs);
    }
    catch (final SchemaException e)
    {
      throw new QueryException("Error creating relation schema for __TNP.", e);
    }
    __TNP = TNP;
  }

  private IQueryExpression createBaseExpression(final ISimpleQuery diffQuery,
      final INamedQuery query) throws QueryException
  {
    final boolean isSet = query.getConnective(0).isSet();
    final QueryConnective EOP = isSet ? QC_SET_DIFFERENCE : QC_BAG_DIFFERENCE;
    // base query: TP^0 - except_case
    final IQuery TP0 = factory.queryExpressionFactory().newQuery(query.getMember(0));
    TP0.append(EOP, diffQuery);
    // container expression
    final IQueryExpression expression = factory.queryExpressionFactory().newQueryExpression(false,
        Collections.<INamedQuery> emptyList(), TP0);
    // compile the query to perform a normalized difference
    final QueryCompilerVisitor qcv = new QueryCompilerVisitor(schema);
    expression.accept(qcv);
    // the normalized query expression will include CTEs
    return qcv.queryExpression();
  }

  private ISimpleQuery createDiffQuery(final INamedQuery query) throws QueryException
  {
    final IClauseFrom from = factory.queryExpressionFactory().newClauseFrom();
    from.append(__TNP.getName(), __TNP);
    final IRelationReference diffRel = from.getMember(0);
    final IClauseSelect select = factory.queryExpressionFactory().newClauseSelect(false);
    for (int i = 0; i < __TNP.getSignature().size() - 1; i++)
    {
      final IFieldSchema fs = __TNP.getSignature().getFieldSchema(i);
      final IFieldExpression fe1 = CompilerUtils.createFieldExpression(diffRel, fs);
      select.append(__TNP.getSignature().getFieldSchema(i).getName(), fe1);
    }
    // simple query: TP^(r-1) U execpt_case(D)
    return factory.queryExpressionFactory().newSimpleQuery(select, from, null, null, null, null);
  }

  private IQueryExpression createRecursiveExpression(final ISimpleQuery diffQuery,
      final INamedQuery query) throws QueryException
  {
    final boolean isSet = query.getConnective(0).isSet();
    final QueryConnective EOP = isSet ? QC_SET_DIFFERENCE : QC_BAG_DIFFERENCE;
    // recursive query: DELTA^r = TNP^r - (TNP^(r-1) U compile(execpt_case)(D))
    final ISimpleQuery recursiveCase = rewriteRecursiveCase(query);
    final IQuery TPr = factory.queryExpressionFactory().newQuery(recursiveCase);
    TPr.append(EOP, diffQuery);
    // container expression
    final IQueryExpression expression = factory.queryExpressionFactory().newQueryExpression(false,
        Collections.<INamedQuery> emptyList(), TPr);
    // compile the query to perform a normalized difference
    final QueryCompilerVisitor qcv = new QueryCompilerVisitor(schema);
    expression.accept(qcv);
    // the normalized query expression will include CTEs
    return qcv.queryExpression();
  }

  /**
   * The strategy used here to rewrite the recursive case uses the parser to produce a modified
   * query from a properly built query string. This is a simpler approach than rebuilding the query
   * from scratch by composing query and expression visitors and recursively modifying all
   * references to the named query and its fields in the SELECT, FROM, and WHERE clauses.
   */
  private ISimpleQuery rewriteRecursiveCase(final INamedQuery query) throws QueryException
  {
    // temporarily append __TNP to the schema
    try
    {
      schema.append(__TNP);
    }
    catch (final SchemaException e)
    {
      throw new QueryException("Error registering temporary relations within schema.", e);
    }
    final ISimpleQuery recursive = query.getMember(1);
    // new FROM clause
    final IClauseFrom from = factory.queryExpressionFactory().newClauseFrom();
    // copy original FROM clause
    for (int i = 0; i < recursive.getFrom().size(); i++)
    {
      final IRelationReference rel = recursive.getFrom().getMember(i);
      from.append(rel.getVariable(), rel.getSchema());
    }
    // build a new FROM clause replacing the recursive reference with a reference to __TNP
    final StringBuffer newFrom = new StringBuffer("FROM ");
    final StringBuffer newWhere = new StringBuffer("");
    for (int i = 0; i < from.size(); i++)
    {
      final String fromName = from.getMember(i).getSchema().getName();
      final String relName;
      final String relVar = from.getMember(i).getVariable();
      if (fromName.equalsIgnoreCase(query.getSchema().getName()))
      {
        relName = __TNP.getName();
        final String conjunct = String.format("%s.__round >= 0", relVar);
        newWhere.append(recursive.getWhere() == null ? String.format("WHERE %s", conjunct) : String
            .format("WHERE %s AND (%s)", conjunct, recursive.getWhere().getExpression()));
      }
      else
      {
        relName = fromName;
      }
      newFrom.append(String.format("%s AS %s", relName, relVar));
      if (i < from.size() - 1)
      {
        newFrom.append(", ");
      }
    }
    // rewritten SPJ query string
    final String queryString = String.format("%s %s %s;", recursive.getSelect(), newFrom, newWhere);
    // parse the rewritten query string
    final IParser parser = Factory.INSTANCE.createParser();
    IQueryExpression rewrittenQuery;
    try
    {
      rewrittenQuery = parser.parse(queryString, schema);
    }
    catch (final ParserException e)
    {
      throw new QueryException("Error parsing the rewritten query string for the recursive case.",
          e);
    }
    // remove __TNP from the schema
    schema.remove(__TNP);
    // simple query is the first query component in the query expression
    return rewrittenQuery.getQuery().getMember(0);
  }

  /**
   * Performs some basic checks on the query to make sure it conforms to the recursive queries
   * supported by the compiler.
   */
  private void validateQuery(final INamedQuery query) throws QueryException
  {
    for (int i = 0; i < query.size(); i++)
    {
      final ISimpleQuery q = query.getMember(i);
      if (q.getGroupBy() != null)
      {
        throw new QueryException(
            "GROUP BY clause not supported in query members of recursive queries.");
      }
      for (int j = 0; j < q.getSelect().size(); j++)
      {
        if (q.getSelect().getMember(j).isAggregate())
        {
          throw new QueryException(
              "Aggregate functions not supported in the projection list of query members of recursive queries.");
        }
      }
    }
    if (query.size() == 1)
    {
      throw new QueryException("Recursive queries require a base case query.");
    }
    if (query.size() > 3)
    {
      throw new QueryException(
          "Recursive queries support at most three query components-- a base case query, a recursive case query, and an optional except case.");
    }
    if (query.getConnective(0) != QueryConnective.QC_BAG_UNION
        && query.getConnective(0) != QueryConnective.QC_SET_UNION)
    {
      throw new QueryException(
          "Expected a UNION operator connecting the base case query and the recursive case query.");
    }
    if (query.size() == 3)
    {
      if (query.getConnective(1) != QueryConnective.QC_BAG_DIFFERENCE
          && query.getConnective(1) != QueryConnective.QC_SET_DIFFERENCE)
      {
        throw new QueryException(
            "Expected an EXCEPT operator connecting the recursive case query and the except case query.");
      }
      if (query.getConnective(0).isSet() != query.getConnective(1).isSet())
      {
        throw new QueryException(
            "UNION and EXCEPT operators must both have the same semantics-- SET or BAG.");
      }
    }
  }

  CompiledRecursiveQueryParts compile(final INamedQuery query) throws QueryException
  {
    validateQuery(query);
    // difference query: TP^(r-1) U execpt_case
    final ISimpleQuery diffQuery = createDiffQuery(query);
    // compiled base query expression: TNP^0 = compile(base_case - execpt_case)(D)
    final IQueryExpression TNP0_expression = createBaseExpression(diffQuery, query);
    // create a named expression for the base case
    IRelationSchema TNP0_schema;
    try
    {
      TNP0_schema = factory.schemaFactory().newRelationSchema(CompilerUtils.newBaseQueryName(),
          TNP0_expression.getQuery().getSignature());
    }
    catch (final SchemaException e)
    {
      throw new QueryException(
          "Error creating relation schema for the named query of the base case.", e);
    }
    // query used to evaluate TNP^0 - QE(D)
    final INamedQuery TNP0 = factory.queryExpressionFactory().newNamedQuery(TNP0_schema,
        TNP0_expression.getQuery());
    // compiled recursive query expression: DELTA^r = compile(recursive_case - execpt_case)(D)
    final IQueryExpression DELTAr_expression = createRecursiveExpression(diffQuery, query);
    // create a named expression for the recursive case
    IRelationSchema TNPr_schema;
    try
    {
      TNPr_schema = factory.schemaFactory().newRelationSchema(
          CompilerUtils.newRecursiveQueryName(), DELTAr_expression.getQuery().getSignature());
    }
    catch (final SchemaException e)
    {
      throw new QueryException(
          "Error creating relation schema for the named query of the recursive case.", e);
    }
    // query used to evaluate DELTA^r = TNP(TNP^(r-1)) - (TNP^(r-1) U QE(D))
    final INamedQuery DELTAr = factory.queryExpressionFactory().newNamedQuery(TNPr_schema,
        DELTAr_expression.getQuery());
    final INamedQuery QE;
    if (query.size() == 3)
    {
      QE = CompilerUtils.createNamedQuery(CompilerUtils.newExceptQueryName(),
          compiler.compile(query.getMember(2)));
    }
    else
    {
      QE = null;
    }
    return new CompiledRecursiveQueryParts(__TNP, query, TNP0, TNP0_expression, DELTAr,
        DELTAr_expression, QE);
  }
}
