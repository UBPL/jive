package edu.buffalo.cse.jive.practql.expression.nary;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;

// factory for connected expressions
public interface INAryExpressionFactory
{
  public IAddition newAddition(IExpression expression) throws ExpressionException;

  public IConjunction newConjunction(IExpression expression) throws ExpressionException;

  public IDisjunction newDisjunction(IExpression expression) throws ExpressionException;

  public IMultiplication newMultiplication(IExpression expression) throws ExpressionException;
}