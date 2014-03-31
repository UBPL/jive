package org.lessa.demian.parser;

import org.lessa.demian.expression.IExpression;

public interface IParser
{
  /**
   * Parses a query string assuming all fields/relations referenced in the query exist. This amounts
   * to checking whether the SQL is syntactically valid.
   * <p>
   * Parses a query string, checking that all fields/relations referenced in the query belong to
   * their respective schemas, that recursive queries belong to a supported class of recursive
   * queries, and that, attribute independence is observed by the temporal queries.
   * 
   * @param source
   *          string containing a single SQL query to validate
   * @param schema
   *          optional schema containing the relations against which the SQL query is validated
   * @return a QueryExpression object containing a representation of the input query
   * @throws ParserException
   *           if the input string does not represent a valid SQL query
   */
  public IExpression parse(final String source) throws ParserException;
}