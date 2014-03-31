package edu.buffalo.cse.jive.internal.practql.expression.atomic;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.atomic.IFieldExpression;
import edu.buffalo.cse.jive.practql.schema.IFieldReference;
import edu.buffalo.cse.jive.practql.schema.Type;

class FieldExpression extends AtomicExpression implements IFieldExpression
{
  private final IFieldReference fieldReference;

  FieldExpression(final IFieldReference fieldReference) throws ExpressionException
  {
    // catch null field reference
    if (fieldReference == null)
    {
      throw new ExpressionException("Error creating field expression: null field reference.");
    }
    // catch invalid type in field reference
    if (fieldReference.getSchema().getType() == Type.INVALID
        || fieldReference.getSchema().getType() == Type.NULL)
    {
      throw new ExpressionException(String.format(
          "Error creating field expression: '%s' field reference type.", fieldReference.getSchema()
              .getType()));
    }
    this.fieldReference = fieldReference;
  }

  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    visitor.visitField(this, arg);
  }

  @Override
  public IFieldReference getFieldReference()
  {
    return this.fieldReference;
  }

  @Override
  public Type getType()
  {
    return this.fieldReference.getSchema().getType();
  }

  @Override
  public boolean isAggregate()
  {
    return false;
  }

  @Override
  public boolean isLiteral()
  {
    return false;
  }

  @Override
  public String toString()
  {
    return fieldReference.toString();
  }

  @Override
  public String toStringTyped()
  {
    return toString() + "{" + getType().toString() + "}";
  }
}