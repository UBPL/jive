package edu.buffalo.cse.jive.internal.practql.expression.literal;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.literal.IDecimalLiteral;
import edu.buffalo.cse.jive.practql.schema.Type;

class DecimalValue implements IDecimalLiteral
{
  private final Double value;

  public DecimalValue(final Double value) throws ExpressionException
  {
    // catch null value
    if (value == null)
    {
      throw new ExpressionException("Decimal value cannot be null.");
    }
    this.value = value;
  }

  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    visitor.visitLiteral(this, arg);
  }

  @Override
  public boolean equals(final Object other)
  {
    return (other instanceof DecimalValue) && ((DecimalValue) other).getValue().equals(getValue());
  }

  @Override
  public Type getType()
  {
    return Type.DECIMAL;
  }

  @Override
  public Double getValue()
  {
    return this.value;
  }

  @Override
  public boolean isAggregate()
  {
    return false;
  }

  @Override
  public boolean isLiteral()
  {
    return true;
  }

  @Override
  public String toString()
  {
    return String.valueOf(getValue());
  }

  @Override
  public String toStringTyped()
  {
    return toString() + "{" + getType().toString() + "}";
  }
}