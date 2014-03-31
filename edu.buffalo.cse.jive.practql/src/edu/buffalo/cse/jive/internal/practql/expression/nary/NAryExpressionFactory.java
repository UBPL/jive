package edu.buffalo.cse.jive.internal.practql.expression.nary;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.nary.IAddition;
import edu.buffalo.cse.jive.practql.expression.nary.IConjunction;
import edu.buffalo.cse.jive.practql.expression.nary.IDisjunction;
import edu.buffalo.cse.jive.practql.expression.nary.IMultiplication;
import edu.buffalo.cse.jive.practql.expression.nary.INAryExpressionFactory;

public enum NAryExpressionFactory implements INAryExpressionFactory
{
  INSTANCE;
  @Override
  public IAddition newAddition(final IExpression expression) throws ExpressionException
  {
    return new Addition(expression);
  }

  @Override
  public IConjunction newConjunction(final IExpression expression) throws ExpressionException
  {
    return new Conjunction(expression);
  }

  @Override
  public IDisjunction newDisjunction(final IExpression expression) throws ExpressionException
  {
    return new Disjunction(expression);
  }

  @Override
  public IMultiplication newMultiplication(final IExpression expression) throws ExpressionException
  {
    return new Multiplication(expression);
  }
}