package edu.buffalo.cse.jive.practql.expression.atomic;

import edu.buffalo.cse.jive.practql.schema.IFieldReference;

// reference to a field used in an query expression
public interface IFieldExpression extends IAtomicExpression
{
  public IFieldReference getFieldReference();
}