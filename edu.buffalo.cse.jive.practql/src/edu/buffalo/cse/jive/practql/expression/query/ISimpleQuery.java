package edu.buffalo.cse.jive.practql.expression.query;

import edu.buffalo.cse.jive.practql.schema.ISchemaSignature;

// simple_query ::=
// TK_SELECT projection_list
// TK_FROM from_list
// [TK_WHERE where_clause]
// [TK_GROUP TK_BY group_list]
// [TK_HAVING having_clause]
// [TK_ORDER TK_BY order_list]
public interface ISimpleQuery
{
  public void accept(IQueryExpressionVisitor visitor) throws QueryException;

  // from_list (mapping from tuple variables to relation schemas)
  public IClauseFrom getFrom();

  // group_list (group expressions)
  public IClauseGroupBy getGroupBy();

  // having_clause (boolean expression, may involve aggregate functions)
  public IClauseHaving getHaving();

  // order_list (mapping from expressions to sort directions)
  public IClauseOrderBy getOrderBy();

  // projection_list (projected expressions of this query-- matches the query schema)
  public IClauseSelect getSelect();

  public ISchemaSignature getSignature() throws QueryException;

  // where_clause (boolean expression, may not involve aggregate functions)
  public IClauseWhere getWhere();

  // determines if this query is set compatible with the given argument query
  public boolean isSchemaCompatible(ISimpleQuery query);
}