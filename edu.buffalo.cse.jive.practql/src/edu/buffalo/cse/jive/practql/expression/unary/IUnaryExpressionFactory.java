package edu.buffalo.cse.jive.practql.expression.unary;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;

// factory for unary expressions
public interface IUnaryExpressionFactory
{
  public INamedExpression newAlias(INamedExpression expression) throws ExpressionException;

  public INamedExpression newNamedExpression(IExpression expression, String name)
      throws ExpressionException;

  public IExpression newNegatedExpression(IExpression expression) throws ExpressionException;

  public IExpression newNegativeExpression(IExpression expression) throws ExpressionException;

  public ISortedExpression newSortedExpression(IExpression expression, SortDirection sortDirection)
      throws ExpressionException;
}