package org.lessa.demian.expression;

import java.util.List;

// factory for all types of expressions
public interface IExpressionFactory
{
  public ILiteral newInteger(Integer value) throws ExpressionException;

  public IVariableExpression newVariable(String name) throws ExpressionException;

  public IFunctionExpression newFunction(FunctionType function, List<IExpression> args)
      throws ExpressionException;
}