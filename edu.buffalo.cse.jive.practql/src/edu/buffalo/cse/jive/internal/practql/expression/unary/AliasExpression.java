package edu.buffalo.cse.jive.internal.practql.expression.unary;

import edu.buffalo.cse.jive.internal.practql.expression.StructuredExpression;
import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.unary.INamedExpression;
import edu.buffalo.cse.jive.practql.schema.Type;

class AliasExpression extends StructuredExpression implements INamedExpression
{
  private final INamedExpression expression;

  AliasExpression(final INamedExpression expression) throws ExpressionException
  {
    // catch null expression
    if (expression == null)
    {
      throw new ExpressionException("Error creating alias expression: null named expression.");
    }
    // we can proceed
    this.expression = expression;
  }

  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    visitor.visitNamed(this, arg);
  }

  @Override
  public IExpression getExpression()
  {
    return this.expression;
  }

  @Override
  public String getName()
  {
    return expression.getName();
  }

  @Override
  public Type getType()
  {
    return expression.getType();
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
    return String.format("%s", getExpression().toString());
  }

  @Override
  public String toStringTyped()
  {
    return toString() + "{" + getType().toString() + "}";
  }
}