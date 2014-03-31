package org.lessa.demian.internal.expression;

import org.lessa.demian.expression.ExpressionException;
import org.lessa.demian.expression.IExpressionVisitor;
import org.lessa.demian.expression.IIntegerLiteral;

class IntegerValue implements IIntegerLiteral
{
  private final Integer value;

  IntegerValue(final Integer value) throws ExpressionException
  {
    // catch null value
    if (value == null)
    {
      throw new ExpressionException("Integer value cannot be null.");
    }
    this.value = value;
  }

  @Override
  public void accept(final IExpressionVisitor visitor) throws ExpressionException
  {
    visitor.visitInteger(this);
  }

  @Override
  public boolean equals(final Object other)
  {
    return (other instanceof IntegerValue) && ((IntegerValue) other).getValue().equals(getValue());
  }

  @Override
  public Integer getValue()
  {
    return this.value;
  }

  @Override
  public String toString()
  {
    return String.valueOf(getValue());
  }
}