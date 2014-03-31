package edu.buffalo.cse.jive.internal.practql.expression.atomic;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.atomic.AggregateType;
import edu.buffalo.cse.jive.practql.expression.atomic.IAggregateExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.IWildcardExpression;
import edu.buffalo.cse.jive.practql.schema.Type;

class AggregateExpression extends AtomicExpression implements IAggregateExpression
{
  private final AggregateType aggregate;
  private final IExpression argument;
  private final boolean isDistinct;
  private final Type type;

  AggregateExpression(final AggregateType aggregate, final IExpression argument,
      final boolean isDistinct) throws ExpressionException
  {
    // catch null aggregate
    if (aggregate == null)
    {
      throw new ExpressionException("Aggregate function cannot be null.");
    }
    // catch null argument
    if (argument == null)
    {
      dieInvalid(argument, null);
    }
    // literal argument
    if (argument.isLiteral())
    {
      dieInvalid(argument, "Aggregate argument cannot be a literal expression.");
    }
    // catch invalid argument types, exception: COUNT(DISTINCT *) and COUNT(*)
    if ((argument instanceof IWildcardExpression) && aggregate != AggregateType.COUNT)
    {
      dieInvalid(argument, null);
    }
    // boolean only supports COUNT
    if (argument.getType() == Type.BOOLEAN && aggregate != AggregateType.COUNT)
    {
      dieInvalid(argument, null);
    }
    // SUM and AVG supported for numeric types
    if ((aggregate == AggregateType.SUM || aggregate == AggregateType.AVG)
        && !argument.getType().isNumeric())
    {
      dieInvalid(argument, null);
    }
    // COUNT: TYPE --> INT
    if (aggregate == AggregateType.COUNT)
    {
      type = Type.INTEGER;
    }
    // *: TP_ENCODED --> TP
    else if (argument.getType() == Type.TP_ENCODED)
    {
      type = Type.TP;
    }
    // *: * --> *
    else
    {
      type = argument.getType();
    }
    this.aggregate = aggregate;
    this.argument = argument;
    this.isDistinct = isDistinct;
  }

  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    visitor.visitAggregate(this, arg);
  }

  @Override
  public AggregateType getAggregateType()
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
    return String.format("%s(%s%s)", aggregate.toString(), isDistinct ? "DISTINCT " : "",
        argument.toString());
  }

  @Override
  public String toStringTyped()
  {
    return String.format("%s(%s%s){%s}", aggregate.toString(), isDistinct ? "DISTINCT " : "",
        argument.toStringTyped(), (type == null ? "ERROR" : type.toString()));
  }

  private void dieInvalid(final IExpression argument, final String message)
      throws ExpressionException
  {
    throw new ExpressionException(String.format(
        "%sError creating aggregate expression '%s' with argument '%s'.", message == null ? ""
            : message, aggregate, argument == null ? "null" : argument.toStringTyped()));
  }
}