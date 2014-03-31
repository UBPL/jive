package edu.buffalo.cse.jive.practql.expression.unary;

// (sort direction, expression) pair used for sort lists (ORDER BY)
public interface ISortedExpression extends IUnaryExpression
{
  public SortDirection getSortDirection();
}