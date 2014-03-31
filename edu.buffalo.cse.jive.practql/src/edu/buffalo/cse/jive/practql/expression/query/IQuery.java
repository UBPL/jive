package edu.buffalo.cse.jive.practql.expression.query;

import edu.buffalo.cse.jive.practql.schema.ISchemaSignature;

// query ::= simple_query (set_op [TK_ALL] simple_query)*
public interface IQuery
{
  public void accept(IQueryExpressionVisitor visitor) throws QueryException;

  public void append(QueryConnective connective, ISimpleQuery newMember) throws QueryException;

  // set_op [TK_ALL]
  public QueryConnective getConnective(int index);

  // simple_query
  public ISimpleQuery getMember(int index);

  // consolidated signature of the query
  public ISchemaSignature getSignature();

  // checks whether the schema is resolved and throws an exception if any problem is found
  public void schemaResolved() throws QueryException;

  // number of members in the query (one more than the number of connectives)
  public int size();
}