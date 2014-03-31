package edu.buffalo.cse.jive.practql.expression;

import edu.buffalo.cse.jive.practql.schema.Type;

public interface IExpression
{
  // visits the expression
  public void accept(IExpressionVisitor visitor, Object arg) throws ExpressionException;

  // resolved type of this expression
  public Type getType();

  // an expression consisting of one or more aggregates
  public boolean isAggregate();

  // an expression consisting only of literals
  public boolean isLiteral();

  public String toStringTyped();
}