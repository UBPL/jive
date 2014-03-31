package edu.buffalo.cse.jive.practql.expression.atomic;

import java.util.List;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.schema.IFieldReference;

// factory for atomic expressions
public interface IAtomicExpressionFactory
{
  public IExpression newAggregate(AggregateType aggregate, IExpression argument, boolean isDistinct)
      throws ExpressionException;

  public IExpression newCAggregate(CAggregateType aggregateType, IExpression carg, boolean distinct)
      throws ExpressionException;

  public IExpression newCFunctionCall(CFunctionType functionType, IExpression arg1, IExpression arg2)
      throws ExpressionException;

  public IExpression newCIntervalExpression(IExpression lowerBound, IExpression upperBound)
      throws ExpressionException;

  public ICIntervalFieldExpression newCIntervalFieldExpression(IFieldReference fieldReference)
      throws ExpressionException;

  public IFieldExpression newFieldExpression(IFieldReference fieldReference)
      throws ExpressionException;

  public IExpression newFunctionCall(FunctionType functionType, List<IExpression> args)
      throws ExpressionException;

  public IExpression newWildcardExpression();

  public IExpression newWildcardExpression(String prefix) throws ExpressionException;
}