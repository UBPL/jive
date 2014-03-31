package edu.buffalo.cse.jive.internal.practql.expression.unary;

import edu.buffalo.cse.jive.internal.practql.TypeUtils;
import edu.buffalo.cse.jive.internal.practql.expression.StructuredExpression;
import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.unary.INegatedExpression;
import edu.buffalo.cse.jive.practql.schema.Type;

class NegatedExpression extends StructuredExpression implements INegatedExpression
{
  private final IExpression expression;
  private Type type = null;

  NegatedExpression(final IExpression expression) throws ExpressionException
  {
    // catch null expression
    if (expression == null)
    {
      throw new ExpressionException("Error creating negated expression: null expression.");
    }
    // we can proceed
    this.expression = expression;
    this.type = TypeUtils.initForNegation(expression.getType());
  }

  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    if (visitor.visitNegated(this, arg))
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
  public Type getType()
  {
    return this.type;
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
    return "(NOT " + expression.toString() + ")";
  }

  @Override
  public String toStringTyped()
  {
    return "(NOT " + expression.toStringTyped() + ")" + "{" + getType().toString() + "}";
  }
}