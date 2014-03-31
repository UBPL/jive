package edu.buffalo.cse.jive.practql.expression.literal;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;

// factory for literal expressions
public interface ILiteralExpressionFactory
{
  public ILiteral newBoolean(boolean value);

  public ILiteral newDecimal(Double value) throws ExpressionException;

  public ILiteral newInteger(Integer value) throws ExpressionException;

  public ILiteral newNull();

  public ILiteral newString(String value) throws ExpressionException;
}