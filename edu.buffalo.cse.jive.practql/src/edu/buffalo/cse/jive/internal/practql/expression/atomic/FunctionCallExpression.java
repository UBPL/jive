package edu.buffalo.cse.jive.internal.practql.expression.atomic;

import java.util.List;

import edu.buffalo.cse.jive.internal.practql.TypeUtils;
import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.atomic.FunctionType;
import edu.buffalo.cse.jive.practql.expression.atomic.IFunctionCallExpression;
import edu.buffalo.cse.jive.practql.expression.relational.RelationalConnective;
import edu.buffalo.cse.jive.practql.schema.Type;

class FunctionCallExpression extends AtomicExpression implements IFunctionCallExpression
{
  private final List<IExpression> arguments;
  private final FunctionType functionType;
  private final boolean isLiteral;
  private final Type type;

  FunctionCallExpression(final FunctionType function, final List<IExpression> args)
      throws ExpressionException
  {
    // catch null function
    catchNullFunction(function);
    // catch null arguments
    catchNullArguments(args);
    // catch null argument
    catchNullArgument(function, args);
    // numeric monadic functions
    if (function == FunctionType.FT_ABS || function == FunctionType.FT_FLOOR
        || function == FunctionType.FT_CEIL)
    {
      // monadic
      catchInvalidArgumentList(function, args);
      // non-numeric argument
      catchInvalidArgumentType(function, args);
      // type is the same as argument: INTEGER or DECIMAL
      type = args.get(0).getType();
    }
    // variadic functions with polymorphic types
    else
    {
      Type t = Type.NULL;
      for (int i = 0; i < args.size(); i++)
      {
        // first non-NULL argument
        // widen: NULL --> t
        if (t == Type.NULL)
        {
          t = args.get(0).getType();
        }
        // all other non-NULL arguments
        else if (args.get(i).getType() != Type.NULL)
        {
          final Type argType = args.get(i).getType();
          final Type t2 = TypeUtils.initForRelationalExpression(t, RelationalConnective.RC_EQ,
              argType);
          catchArgumentIncompatibility(function, t, t2, args.get(i));
          // types are equality-comparable
          // widen: INTEGER --> DECIMAL
          if (t == Type.INTEGER && argType == Type.DECIMAL)
          {
            t = argType;
          }
          // widen: INTEGER --> TP | TP_ENCODED
          // widen: TP --> TP | TP_ENCODED
          else if ((t == Type.INTEGER || t == Type.TP) && argType.isTemporal())
          {
            t = argType;
          }
        }
      }
      type = t;
    }
    this.arguments = args;
    this.functionType = function;
    boolean literal = true;
    for (int i = 0; i < args.size(); i++)
    {
      literal = literal && args.get(i).isLiteral();
    }
    isLiteral = literal;
  }

  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    visitor.visitFunctionCall(this, arg);
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
  public Type getType()
  {
    return this.type;
  }

  @Override
  public boolean isAggregate()
  {
    return false;
  }

  @Override
  public boolean isLiteral()
  {
    return isLiteral;
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

  @Override
  public String toStringTyped()
  {
    final StringBuffer result = new StringBuffer(functionType.toString());
    result.append("(");
    for (int i = 0; i < arguments.size(); i++)
    {
      if (i > 0)
      {
        result.append(", ");
      }
      result.append(arguments.get(i).toStringTyped());
    }
    result.append(")");
    return result.toString() + "{" + getType().toString() + "}";
  }

  private void catchArgumentIncompatibility(final FunctionType function, final Type t,
      final Type t2, final IExpression arg) throws ExpressionException
  {
    if (t2 == Type.INVALID)
    {
      throw new ExpressionException(
          String
              .format(
                  "Invalid argument list to '%s'. Expecting an argument compatibly with '%s' at position %d but found '%s' instead.",
                  function.toString(), t.toString(), arg.toStringTyped()));
    }
  }

  private void catchInvalidArgumentList(final FunctionType function, final List<IExpression> args)
      throws ExpressionException
  {
    if (args.size() > 1 || args.size() == 0)
    {
      throw new ExpressionException(
          String
              .format(
                  "Invalid argument list to '%s': expecting a single argument but found %d arguments instead.",
                  function.toString(), args.size()));
    }
  }

  private void catchInvalidArgumentType(final FunctionType function, final List<IExpression> args)
      throws ExpressionException
  {
    if (!args.get(0).getType().isNumeric())
    {
      throw new ExpressionException(String.format(
          "Invalid argument to '%s'. Expecting a numeric type but found '%s' instead.",
          function.toString(), args.get(0).toStringTyped()));
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