package org.lessa.demian.expression;

// marker interface for literal values (interpreted as themselves)
public interface ILiteral extends IExpression
{
  public Object getValue();
}