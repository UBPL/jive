package edu.buffalo.cse.jive.internal.practql.expression.literal;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.literal.IStringLiteral;
import edu.buffalo.cse.jive.practql.schema.Type;

class StringValue implements IStringLiteral
{
  private final String value;

  StringValue(final String value) throws ExpressionException
  {
    // catch null value
    if (value == null)
    {
      throw new ExpressionException("String value cannot be null.");
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
    return (other instanceof StringValue)
        && ((StringValue) other).getValue().equalsIgnoreCase(getValue());
  }

  @Override
  public Type getType()
  {
    return Type.STRING;
  }

  @Override
  public String getValue()
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
    return String.valueOf("'" + escape() + "'");
  }

  @Override
  public String toStringTyped()
  {
    return toString() + "{" + getType().toString() + "}";
  }

  private String escape()
  {
    final StringBuffer result = new StringBuffer("");
    for (int i = 0; i < value.length(); i++)
    {
      result.append(value.charAt(i));
      if (value.charAt(i) == '\'')
      {
        result.append(value.charAt(i));
      }
    }
    return result.toString();
  }
}