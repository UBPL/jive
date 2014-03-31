package edu.buffalo.cse.jive.practql.expression.query;

import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.unary.INamedExpression;
import edu.buffalo.cse.jive.practql.schema.ISchemaSignature;

public interface IClauseSelect extends IQueryClause
{
  public void append(String name, IExpression expression) throws QueryException;

  public INamedExpression getMember(int index);

  public ISchemaSignature getSignature() throws QueryException;

  public boolean isDistinct();

  public INamedExpression resolveAlias(String fieldName) throws QueryException;

  public int size();
}