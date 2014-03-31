package edu.buffalo.cse.jive.internal.practql.expression.atomic;

import java.util.List;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.AggregateType;
import edu.buffalo.cse.jive.practql.expression.atomic.CAggregateType;
import edu.buffalo.cse.jive.practql.expression.atomic.CFunctionType;
import edu.buffalo.cse.jive.practql.expression.atomic.FunctionType;
import edu.buffalo.cse.jive.practql.expression.atomic.IAtomicExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.atomic.ICIntervalFieldExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.IFieldExpression;
import edu.buffalo.cse.jive.practql.schema.IFieldReference;

public enum AtomicExpressionFactory implements IAtomicExpressionFactory
{
  INSTANCE;
  @Override
  public IExpression newAggregate(final AggregateType aggregate, final IExpression argument,
      final boolean isDistinct) throws ExpressionException
  {
    return new AggregateExpression(aggregate, argument, isDistinct);
  }

  @Override
  public IExpression newCAggregate(final CAggregateType aggregate, final IExpression argument,
      final boolean isDistinct) throws ExpressionException
  {
    return new CAggregateExpression(aggregate, argument, isDistinct);
  }

  @Override
  public IExpression newCFunctionCall(final CFunctionType functionType, final IExpression arg1,
      final IExpression arg2) throws ExpressionException
  {
    return new CFunctionCallExpression(functionType, arg1, arg2);
  }

  @Override
  public IExpression newCIntervalExpression(final IExpression lowerBound,
      final IExpression upperBound) throws ExpressionException
  {
    return new CIntervalExpression(lowerBound, upperBound);
  }

  @Override
  public ICIntervalFieldExpression newCIntervalFieldExpression(final IFieldReference fieldReference)
      throws ExpressionException
  {
    return new CIntervalFieldExpression(fieldReference);
  }

  @Override
  public IFieldExpression newFieldExpression(final IFieldReference fieldReference)
      throws ExpressionException
  {
    return new FieldExpression(fieldReference);
  }

  @Override
  public IExpression newFunctionCall(final FunctionType functionType, final List<IExpression> args)
      throws ExpressionException
  {
    return new FunctionCallExpression(functionType, args);
  }

  @Override
  public IExpression newWildcardExpression()
  {
    return new WildcardExpression();
  }

  @Override
  public IExpression newWildcardExpression(final String relVar) throws ExpressionException
  {
    return new WildcardExpression(relVar);
  }
}