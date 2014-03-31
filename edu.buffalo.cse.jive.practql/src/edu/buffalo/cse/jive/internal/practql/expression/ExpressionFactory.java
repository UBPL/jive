package edu.buffalo.cse.jive.internal.practql.expression;

import java.util.List;

import edu.buffalo.cse.jive.internal.practql.expression.atomic.AtomicExpressionFactory;
import edu.buffalo.cse.jive.internal.practql.expression.literal.LiteralFactory;
import edu.buffalo.cse.jive.internal.practql.expression.nary.NAryExpressionFactory;
import edu.buffalo.cse.jive.internal.practql.expression.unary.UnaryExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.atomic.AggregateType;
import edu.buffalo.cse.jive.practql.expression.atomic.CAggregateType;
import edu.buffalo.cse.jive.practql.expression.atomic.CFunctionType;
import edu.buffalo.cse.jive.practql.expression.atomic.FunctionType;
import edu.buffalo.cse.jive.practql.expression.atomic.IAtomicExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.atomic.ICIntervalFieldExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.IFieldExpression;
import edu.buffalo.cse.jive.practql.expression.literal.ILiteral;
import edu.buffalo.cse.jive.practql.expression.literal.ILiteralExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.nary.IAddition;
import edu.buffalo.cse.jive.practql.expression.nary.IConjunction;
import edu.buffalo.cse.jive.practql.expression.nary.IDisjunction;
import edu.buffalo.cse.jive.practql.expression.nary.IMultiplication;
import edu.buffalo.cse.jive.practql.expression.nary.INAryExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.relational.RelationalConnective;
import edu.buffalo.cse.jive.practql.expression.unary.INamedExpression;
import edu.buffalo.cse.jive.practql.expression.unary.ISortedExpression;
import edu.buffalo.cse.jive.practql.expression.unary.IUnaryExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.unary.SortDirection;
import edu.buffalo.cse.jive.practql.schema.IFieldReference;

public enum ExpressionFactory implements IExpressionFactory
{
  INSTANCE;
  private final IAtomicExpressionFactory aef = AtomicExpressionFactory.INSTANCE;
  private final INAryExpressionFactory cef = NAryExpressionFactory.INSTANCE;
  private final ILiteralExpressionFactory lef = LiteralFactory.INSTANCE;
  private final IUnaryExpressionFactory uef = UnaryExpressionFactory.INSTANCE;

  @Override
  public IAddition newAddition(final IExpression expression) throws ExpressionException
  {
    return cef.newAddition(expression);
  }

  @Override
  public IExpression newAggregate(final AggregateType aggregate, final IExpression argument,
      final boolean isDistinct) throws ExpressionException
  {
    return aef.newAggregate(aggregate, argument, isDistinct);
  }

  @Override
  public INamedExpression newAlias(final INamedExpression expression) throws ExpressionException
  {
    return uef.newAlias(expression);
  }

  @Override
  public ILiteral newBoolean(final boolean value)
  {
    return lef.newBoolean(value);
  }

  @Override
  public IExpression newCAggregate(final CAggregateType aggregate, final IExpression argument,
      final boolean isDistinct) throws ExpressionException
  {
    return aef.newCAggregate(aggregate, argument, isDistinct);
  }

  @Override
  public IExpression newCFunctionCall(final CFunctionType functionType, final IExpression arg1,
      final IExpression arg2) throws ExpressionException
  {
    return aef.newCFunctionCall(functionType, arg1, arg2);
  }

  @Override
  public IExpression newCIntervalExpression(final IExpression lowerBound,
      final IExpression upperBound) throws ExpressionException
  {
    return aef.newCIntervalExpression(lowerBound, upperBound);
  }

  @Override
  public ICIntervalFieldExpression newCIntervalFieldExpression(final IFieldReference fieldReference)
      throws ExpressionException
  {
    return aef.newCIntervalFieldExpression(fieldReference);
  }

  @Override
  public IConjunction newConjunction(final IExpression expression) throws ExpressionException
  {
    return cef.newConjunction(expression);
  }

  @Override
  public ILiteral newDecimal(final Double value) throws ExpressionException
  {
    return lef.newDecimal(value);
  }

  @Override
  public IDisjunction newDisjunction(final IExpression expression) throws ExpressionException
  {
    return cef.newDisjunction(expression);
  }

  @Override
  public IFieldExpression newFieldExpression(final IFieldReference fieldReference)
      throws ExpressionException
  {
    return aef.newFieldExpression(fieldReference);
  }

  @Override
  public IExpression newFunctionCall(final FunctionType functionType, final List<IExpression> args)
      throws ExpressionException
  {
    return aef.newFunctionCall(functionType, args);
  }

  @Override
  public ILiteral newInteger(final Integer value) throws ExpressionException
  {
    return lef.newInteger(value);
  }

  @Override
  public IMultiplication newMultiplication(final IExpression expression) throws ExpressionException
  {
    return cef.newMultiplication(expression);
  }

  @Override
  public INamedExpression newNamedExpression(final IExpression expression, final String name)
      throws ExpressionException
  {
    return uef.newNamedExpression(expression, name);
  }

  @Override
  public IExpression newNegatedExpression(final IExpression expression) throws ExpressionException
  {
    return uef.newNegatedExpression(expression);
  }

  @Override
  public IExpression newNegativeExpression(final IExpression expression) throws ExpressionException
  {
    return uef.newNegativeExpression(expression);
  }

  @Override
  public ILiteral newNull()
  {
    return lef.newNull();
  }

  @Override
  public IExpression newRelationalExpression(final IExpression lhs, final RelationalConnective rc,
      final IExpression rhs) throws ExpressionException
  {
    return new RelationalExpression(lhs, rc, rhs);
  }

  @Override
  public ISortedExpression newSortedExpression(final IExpression expression,
      final SortDirection sortDirection) throws ExpressionException
  {
    return uef.newSortedExpression(expression, sortDirection);
  }

  @Override
  public ILiteral newString(final String value) throws ExpressionException
  {
    return lef.newString(value);
  }

  @Override
  public IExpression newWildcardExpression()
  {
    return aef.newWildcardExpression();
  }

  @Override
  public IExpression newWildcardExpression(final String prefix) throws ExpressionException
  {
    return aef.newWildcardExpression(prefix);
  }
}