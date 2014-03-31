package org.lessa.demian.internal.expression;

import java.util.List;

import org.lessa.demian.expression.ExpressionException;
import org.lessa.demian.expression.IExpression;
import org.lessa.demian.expression.IExpressionVisitor;
import org.lessa.demian.expression.FunctionType;
import org.lessa.demian.expression.IFunctionExpression;
import org.lessa.demian.expression.IVariableExpression;

class FunctionExpression extends Expression implements IFunctionExpression
{
  private final List<IExpression> arguments;
  private final FunctionType functionType;

  FunctionExpression(final FunctionType function, final List<IExpression> args)
      throws ExpressionException
  {
    // catch null function
    catchNullFunction(function);
    // catch null arguments
    catchNullArguments(args);
    // catch null argument
    catchNullArgument(function, args);
    // numeric binary functions
    if (function == FunctionType.FT_ADD || function == FunctionType.FT_DIV
        || function == FunctionType.FT_MULT || function == FunctionType.FT_SUB)
    {
      catchInvalidArgumentList(function, args, 2);
    }
    // ternary functions with polymorphic types
    else
    {
      catchInvalidArgumentList(function, args, 3);
      catchInvalidVariable(args.get(0));
    }
    this.arguments = args;
    this.functionType = function;
  }

  @Override
  public void accept(final IExpressionVisitor visitor) throws ExpressionException
  {
    switch (functionType)
    {
      case FT_ADD:
        visitor.visitAdd(arguments.get(0), arguments.get(1));
        break;
      case FT_DIV:
        visitor.visitDiv(arguments.get(0), arguments.get(1));
        break;
      case FT_LET:
        visitor.visitLet((IVariableExpression) arguments.get(0), arguments.get(1), arguments.get(2));
        break;
      case FT_MULT:
        visitor.visitMult(arguments.get(0), arguments.get(1));
        break;
      case FT_SUB:
        visitor.visitSub(arguments.get(0), arguments.get(1));
        break;
    }
  }

  @Override
  public List<IExpression> getArguments()
  {
    return this.arguments;
  }

  @Override
  public FunctionType getFunctionType()
  {
    return this.functionType;
  }

  @Override
  public String toString()
  {
    final StringBuffer result = new StringBuffer(functionType.toString());
    result.append("(");
    for (int i = 0; i < arguments.size(); i++)
    {
      if (i > 0)
      {
        result.append(", ");
      }
      result.append(arguments.get(i).toString());
    }
    result.append(")");
    return result.toString();
  }

  private void catchInvalidVariable(final IExpression arg) throws ExpressionException
  {
    if (!(arg instanceof IVariableExpression))
    {
      throw new ExpressionException(String.format("Invalid variable expression '%s'.",
          arg.toString()));
    }
  }

  private void catchInvalidArgumentList(final FunctionType function, final List<IExpression> args,
      final int argsSize) throws ExpressionException
  {
    if (args.size() != argsSize)
    {
      throw new ExpressionException(String.format(
          "Invalid argument list to '%s': expecting %d arguments but found %d arguments instead.",
          function.toString(), argsSize, args.size()));
    }
  }

  private void catchNullArgument(final FunctionType function, final List<IExpression> args)
      throws ExpressionException
  {
    for (final IExpression arg : args)
    {
      if (arg == null)
      {
        throw new ExpressionException(String.format(
            "Invalid argument to function call '%s': null argument.", function.toString()));
      }
    }
  }

  private void catchNullArguments(final List<IExpression> args) throws ExpressionException
  {
    if (args == null)
    {
      throw new ExpressionException("Argument list cannot be null.");
    }
  }

  private void catchNullFunction(final FunctionType function) throws ExpressionException
  {
    if (function == null)
    {
      throw new ExpressionException("Function cannot be null.");
    }
  }
}