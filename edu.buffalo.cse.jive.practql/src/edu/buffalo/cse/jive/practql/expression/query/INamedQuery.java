package edu.buffalo.cse.jive.practql.expression.query;

import edu.buffalo.cse.jive.practql.schema.IRelationSchema;

// with_expression_list ::= TK_WITH named_query (TK_COMMA with_expression)*
// named_query ::= identifier TK_LPAREN field_list TK_RPAREN TK_AS TK_LPAREN query TK_RPAREN
// field_list ::= identifier (TK_COMMA identifier)*
public interface INamedQuery extends IQuery
{
  // name and signature of this query
  public IRelationSchema getSchema();

  // the FROM clause of some simple query references this name query's name
  public boolean isRecursive();
}