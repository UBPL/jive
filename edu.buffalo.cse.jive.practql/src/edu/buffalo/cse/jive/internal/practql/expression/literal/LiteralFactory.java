package edu.buffalo.cse.jive.internal.practql.expression.literal;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.literal.ILiteral;
import edu.buffalo.cse.jive.practql.expression.literal.ILiteralExpressionFactory;

public enum LiteralFactory implements ILiteralExpressionFactory
{
  INSTANCE;
  @Override
  public ILiteral newBoolean(final boolean value)
  {
    return BooleanValue.getValue(value);
  }

  @Override
  public ILiteral newDecimal(final Double value) throws ExpressionException
  {
    return new DecimalValue(value);
  }

  @Override
  public ILiteral newInteger(final Integer value) throws ExpressionException
  {
    return new IntegerValue(value);
  }

  @Override
  public ILiteral newNull()
  {
    return NullValue.NULL;
  }

  @Override
  public ILiteral newString(final String value) throws ExpressionException
  {
    return new StringValue(value);
  }
}