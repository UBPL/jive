package org.lessa.demian.internal.expression;

import java.util.List;

import org.lessa.demian.expression.ExpressionException;
import org.lessa.demian.expression.FunctionType;
import org.lessa.demian.expression.IExpression;
import org.lessa.demian.expression.IExpressionFactory;
import org.lessa.demian.expression.IFunctionExpression;
import org.lessa.demian.expression.ILiteral;
import org.lessa.demian.expression.IVariableExpression;

public enum ExpressionFactory implements IExpressionFactory
{
  INSTANCE;
  public IFunctionExpression newFunction(final FunctionType functionType,
      final List<IExpression> args) throws ExpressionException
  {
    return new FunctionExpression(functionType, args);
  }

  @Override
  public ILiteral newInteger(final Integer value) throws ExpressionException
  {
    return new IntegerValue(value);
  }

  @Override
  public IVariableExpression newVariable(final String name) throws ExpressionException
  {
    return new VariableExpression(name);
  }
}