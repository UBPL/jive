package edu.buffalo.cse.jive.internal.practql.expression.literal;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.literal.IBooleanLiteral;
import edu.buffalo.cse.jive.practql.schema.Type;

enum BooleanValue implements IBooleanLiteral
{
  FALSE(false),
  TRUE(true);
  static IBooleanLiteral getValue(final boolean value)
  {
    if (value)
    {
      return TRUE;
    }
    else
    {
      return FALSE;
    }
  }

  private final boolean value;

  private BooleanValue(final boolean value)
  {
    this.value = value;
  }

  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    visitor.visitLiteral(this, arg);
  }

  @Override
  public Type getType()
  {
    return Type.BOOLEAN;
  }

  @Override
  public Boolean getValue()
  {
    return value;
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