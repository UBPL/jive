package edu.buffalo.cse.jive.internal.practql.expression.unary;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.unary.INamedExpression;
import edu.buffalo.cse.jive.practql.expression.unary.ISortedExpression;
import edu.buffalo.cse.jive.practql.expression.unary.IUnaryExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.unary.SortDirection;

public enum UnaryExpressionFactory implements IUnaryExpressionFactory
{
  INSTANCE;
  @Override
  public INamedExpression newAlias(final INamedExpression expression) throws ExpressionException
  {
    return new AliasExpression(expression);
  }

  @Override
  public INamedExpression newNamedExpression(final IExpression expression, final String name)
      throws ExpressionException
  {
    return new NamedExpression(expression, name);
  }

  @Override
  public IExpression newNegatedExpression(final IExpression expression) throws ExpressionException
  {
    return new NegatedExpression(expression);
  }

  @Override
  public IExpression newNegativeExpression(final IExpression expression) throws ExpressionException
  {
    return new NegativeExpression(expression);
  }

  @Override
  public ISortedExpression newSortedExpression(final IExpression expression,
      final SortDirection sortDirection) throws ExpressionException
  {
    return new SortedExpression(expression, sortDirection);
  }
}