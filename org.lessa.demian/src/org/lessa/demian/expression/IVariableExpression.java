package org.lessa.demian.expression;

// marker interface for expressions that do not contain subexpressions
public interface IVariableExpression extends IExpression
{
  public String getName();
}
