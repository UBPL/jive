package edu.buffalo.cse.jive.internal.practql.expression.literal;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.literal.INullLiteral;
import edu.buffalo.cse.jive.practql.schema.Type;

enum NullValue implements INullLiteral
{
  NULL;
  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    visitor.visitLiteral(this, arg);
  }

  @Override
  public Type getType()
  {
    return Type.NULL;
  }

  @Override
  public INullLiteral getValue()
  {
    return NULL;
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
    return "NULL";
  }

  @Override
  public String toStringTyped()
  {
    return toString() + "{" + getType().toString() + "}";
  }
}