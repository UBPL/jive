package edu.buffalo.cse.jive.internal.practql.expression.unary;

import edu.buffalo.cse.jive.internal.practql.expression.StructuredExpression;
import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.unary.ISortedExpression;
import edu.buffalo.cse.jive.practql.expression.unary.SortDirection;
import edu.buffalo.cse.jive.practql.schema.Type;

class SortedExpression extends StructuredExpression implements ISortedExpression
{
  private final IExpression expression;
  private final SortDirection sortDirection;

  SortedExpression(final IExpression expression, final SortDirection sortDirection)
      throws ExpressionException
  {
    // catch null expression
    if (expression == null)
    {
      throw new ExpressionException("Error creating sorted expression: null expression.");
    }
    // catch null direction
    if (sortDirection == null)
    {
      throw new ExpressionException("Error creating sorted expression: null sort direction.");
    }
    this.sortDirection = sortDirection;
    this.expression = expression;
  }

  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    if (visitor.visitSorted(this, arg))
    {
      expression.accept(visitor, arg);
    }
  }

  @Override
  public IExpression getExpression()
  {
    return this.expression;
  }

  @Override
  public SortDirection getSortDirection()
  {
    return this.sortDirection;
  }

  @Override
  public Type getType()
  {
    return this.expression.getType();
  }

  @Override
  public boolean isAggregate()
  {
    return expression.isAggregate();
  }

  @Override
  public boolean isLiteral()
  {
    return expression.isLiteral();
  }

  @Override
  public String toString()
  {
    return String.format("%s %s", expression.toString(), sortDirection.toString());
  }

  @Override
  public String toStringTyped()
  {
    return String.format("%s %s", expression.toStringTyped(), sortDirection.toString()) + "{"
        + getType().toString() + "}";
  }
}