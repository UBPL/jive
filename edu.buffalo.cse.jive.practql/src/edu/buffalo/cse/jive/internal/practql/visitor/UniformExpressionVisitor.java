package edu.buffalo.cse.jive.internal.practql.visitor;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.atomic.IAggregateExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.IFieldExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.IFunctionCallExpression;
import edu.buffalo.cse.jive.practql.expression.literal.ILiteral;
import edu.buffalo.cse.jive.practql.expression.nary.IAddition;
import edu.buffalo.cse.jive.practql.expression.nary.IConjunction;
import edu.buffalo.cse.jive.practql.expression.nary.IDisjunction;
import edu.buffalo.cse.jive.practql.expression.nary.IMultiplication;
import edu.buffalo.cse.jive.practql.expression.relational.IRelationalExpression;
import edu.buffalo.cse.jive.practql.expression.unary.INamedExpression;
import edu.buffalo.cse.jive.practql.expression.unary.INegatedExpression;
import edu.buffalo.cse.jive.practql.expression.unary.INegativeExpression;
import edu.buffalo.cse.jive.practql.expression.unary.ISortedExpression;

/**
 * Abstract visitor that performs the same action for all methods.
 */
public abstract class UniformExpressionVisitor implements IExpressionVisitor
{
  @Override
  public boolean visitAddition(final IAddition expression, final Object arg)
      throws ExpressionException
  {
    return visit(expression, arg);
  }

  @Override
  public boolean visitAggregate(final IAggregateExpression expression, final Object arg)
      throws ExpressionException
  {
    return visit(expression, arg);
  }

  @Override
  public boolean visitConjunction(final IConjunction expression, final Object arg)
      throws ExpressionException
  {
    return visit(expression, arg);
  }

  @Override
  public boolean visitDisjunction(final IDisjunction expression, final Object arg)
      throws ExpressionException
  {
    return visit(expression, arg);
  }

  @Override
  public boolean visitField(final IFieldExpression expression, final Object arg)
      throws ExpressionException
  {
    return visit(expression, arg);
  }

  @Override
  public boolean visitFunctionCall(final IFunctionCallExpression expression, final Object arg)
      throws ExpressionException
  {
    return visit(expression, arg);
  }

  @Override
  public boolean visitLiteral(final ILiteral expression, final Object arg)
      throws ExpressionException
  {
    return visit(expression, arg);
  }

  @Override
  public boolean visitMultiplication(final IMultiplication expression, final Object arg)
      throws ExpressionException
  {
    return visit(expression, arg);
  }

  @Override
  public boolean visitNamed(final INamedExpression expression, final Object arg)
      throws ExpressionException
  {
    return visit(expression, arg);
  }

  @Override
  public boolean visitNegated(final INegatedExpression expression, final Object arg)
      throws ExpressionException
  {
    return visit(expression, arg);
  }

  @Override
  public boolean visitNegative(final INegativeExpression expression, final Object arg)
      throws ExpressionException
  {
    return visit(expression, arg);
  }

  @Override
  public boolean visitRelational(final IRelationalExpression expression, final Object arg)
      throws ExpressionException
  {
    return visit(expression, arg);
  }

  @Override
  public boolean visitSorted(final ISortedExpression expression, final Object arg)
      throws ExpressionException
  {
    return visit(expression, arg);
  }

  protected abstract boolean visit(final IExpression expression, final Object arg)
      throws ExpressionException;
}