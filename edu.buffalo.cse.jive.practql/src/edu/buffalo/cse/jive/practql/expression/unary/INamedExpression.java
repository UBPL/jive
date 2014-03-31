package edu.buffalo.cse.jive.practql.expression.unary;

// (name, expression) pair used for projection lists (SELECT)
public interface INamedExpression extends IUnaryExpression
{
  public String getName();
}