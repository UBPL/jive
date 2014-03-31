package edu.buffalo.cse.jive.practql.expression.atomic;

// reference to a concrete interval field in a translated query expression
public interface ICIntervalFieldExpression extends ITranslatedFieldExpression
{
  public ILeftEndpointFieldExpression getLeft();

  public IRightEndpointFieldExpression getRight();
}