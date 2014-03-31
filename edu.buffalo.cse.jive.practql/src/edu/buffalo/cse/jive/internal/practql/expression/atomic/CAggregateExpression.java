package edu.buffalo.cse.jive.internal.practql.expression.atomic;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.atomic.CAggregateType;
import edu.buffalo.cse.jive.practql.expression.atomic.ICAggregateExpression;
import edu.buffalo.cse.jive.practql.schema.Type;

class CAggregateExpression extends AtomicExpression implements ICAggregateExpression
{
  private final CAggregateType aggregate;
  private final IExpression argument;
  private final boolean isDistinct;
  private final Type type;

  CAggregateExpression(final CAggregateType aggregate, final IExpression argument,
      final boolean isDistinct) throws ExpressionException
  {
    // distinct is not supported
    if (isDistinct)
    {
      dieInvalid(argument, null);
    }
    // COUNT returns the number of time points
    if (aggregate == CAggregateType.CCOUNT)
    {
      type = Type.INTEGER;
    }
    // MIN and MAX return a time point
    else if (aggregate == CAggregateType.CPARTITION)
    {
      type = Type.CINTERVAL_ARRAY;
    }
    else
    {
      type = Type.TP;
    }
    this.aggregate = aggregate;
    this.argument = argument;
    this.isDistinct = isDistinct;
  }

  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    // concrete types are not visited
  }

  @Override
  public CAggregateType getAggregateType()
  {
    return aggregate;
  }

  @Override
  public IExpression getArgument()
  {
    return argument;
  }

  @Override
  public Type getType()
  {
    return type;
  }

  @Override
  public boolean isAggregate()
  {
    return true;
  }

  @Override
  public boolean isDistinct()
  {
    return isDistinct;
  }

  @Override
  public boolean isLiteral()
  {
    return false;
  }

  @Override
  public String toString()
  {
    return String.format("%s(%s)", aggregate.toString(), argument.toString());
  }

  @Override
  public String toStringTyped()
  {
    return String.format("%s(%s){%s}", aggregate.toString(), argument.toStringTyped(),
        (type == null ? "ERROR" : type.toString()));
  }

  private void dieInvalid(final IExpression argument, final String message)
      throws ExpressionException
  {
    throw new ExpressionException(String.format(
        "%sError creating aggregate expression '%s' with argument '%s'.", message == null ? ""
            : message, aggregate, argument == null ? "null" : argument.toStringTyped()));
  }
}