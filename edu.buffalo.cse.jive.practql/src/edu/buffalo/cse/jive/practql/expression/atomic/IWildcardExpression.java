package edu.buffalo.cse.jive.practql.expression.atomic;

// reference to a wildcard expression used in the projection list
public interface IWildcardExpression extends IAtomicExpression
{
  public String getRelationVariable();
}