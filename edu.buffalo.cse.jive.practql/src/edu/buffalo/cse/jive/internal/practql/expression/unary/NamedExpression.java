package edu.buffalo.cse.jive.internal.practql.expression.unary;

import edu.buffalo.cse.jive.internal.practql.expression.StructuredExpression;
import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.atomic.IFieldExpression;
import edu.buffalo.cse.jive.practql.expression.unary.INamedExpression;
import edu.buffalo.cse.jive.practql.schema.Type;

class NamedExpression extends StructuredExpression implements INamedExpression
{
  private final IExpression expression;
  private final String name;

  NamedExpression(final IExpression expression, final String name) throws ExpressionException
  {
    // catch null name
    if (name == null)
    {
      throw new ExpressionException(String.format(
          "Error creating named expression for '%s': null name.", expression == null ? "null"
              : expression.toStringTyped()));
    }
    // catch null expression
    if (expression == null)
    {
      throw new ExpressionException(String.format(
          "Error creating named expression '%s': null expression.", name));
    }
    this.name = name;
    this.expression = expression;
  }

  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    if (visitor.visitNamed(this, arg))
    {
      expression.accept(visitor, arg);
    }
  }

  // @Override
  // public boolean equals(final Object other) {
  //
  // if (other == this) {
  // return true;
  // }
  // if (!(other instanceof NamedExpression)) {
  // return false;
  // }
  // final NamedExpression ne = (NamedExpression) other;
  // return name.equalsIgnoreCase(ne.getName()) && expression.equals(ne.getExpression());
  // }
  @Override
  public IExpression getExpression()
  {
    return this.expression;
  }

  @Override
  public String getName()
  {
    return this.name;
  }

  @Override
  public Type getType()
  {
    return this.expression.getType();
  }

  @Override
  public int hashCode()
  {
    return 17 * name.toUpperCase().hashCode() + 37 * expression.hashCode();
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
    if (expression instanceof IFieldExpression)
    {
      final IFieldExpression fe = (IFieldExpression) expression;
      if (fe.getFieldReference().getSchema().getName().equals(name))
      {
        return expression.toString();
      }
    }
    return String.format("%s AS %s", expression.toString(), name);
  }

  @Override
  public String toStringTyped()
  {
    if (expression instanceof IFieldExpression)
    {
      final IFieldExpression fe = (IFieldExpression) expression;
      if (fe.getFieldReference().getSchema().getName().equals(name))
      {
        return expression.toStringTyped();
      }
    }
    return String.format("%s AS %s", expression.toStringTyped(), name);
  }
}