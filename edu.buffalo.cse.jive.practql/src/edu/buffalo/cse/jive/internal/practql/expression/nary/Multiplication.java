package edu.buffalo.cse.jive.internal.practql.expression.nary;

import java.util.ArrayList;
import java.util.List;

import edu.buffalo.cse.jive.internal.practql.TypeUtils;
import edu.buffalo.cse.jive.internal.practql.expression.StructuredExpression;
import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.nary.IMultiplication;
import edu.buffalo.cse.jive.practql.expression.nary.MultiplicationConnective;
import edu.buffalo.cse.jive.practql.schema.Type;

class Multiplication extends StructuredExpression implements IMultiplication
{
  private final List<MultiplicationConnective> connectives = new ArrayList<MultiplicationConnective>();
  private boolean isAggregate;
  private boolean isLiteral;
  private final List<IExpression> members = new ArrayList<IExpression>();
  private Type type = null;

  Multiplication(final IExpression expression) throws ExpressionException
  {
    // catch null expression
    if (expression == null)
    {
      dieInvalid(expression, null);
    }
    final Type t = TypeUtils.initMultiplication(expression.getType());
    // catch invalid type
    if (t == Type.INVALID)
    {
      dieInvalid(expression, null);
    }
    // we can proceed
    this.type = t;
    this.members.add(expression);
    this.isLiteral = expression.isLiteral();
    this.isAggregate = expression.isAggregate();
  }

  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    if (visitor.visitMultiplication(this, arg))
    {
      for (int i = 0; i < members.size(); i++)
      {
        members.get(i).accept(visitor, arg);
      }
    }
  }

  @Override
  public IMultiplication append(final MultiplicationConnective connective,
      final IExpression expression) throws ExpressionException
  {
    // catch null connective
    if (connective == null)
    {
      dieInvalid(expression, "Multiplication connective cannot be null. ");
    }
    // catch null expression
    if (expression == null)
    {
      dieInvalid(expression, null);
    }
    final Type t = TypeUtils.inferForMultiplication(type, expression.getType());
    // catch invalid type
    if (t == Type.INVALID)
    {
      dieInvalid(expression, null);
    }
    // we can proceed
    this.type = t;
    this.members.add(expression);
    this.connectives.add(connective);
    this.isLiteral = isLiteral && expression.isLiteral();
    this.isAggregate = isAggregate || expression.isAggregate();
    return this;
  }

  @Override
  public MultiplicationConnective getConnective(final int index)
  {
    return connectives.get(index);
  }

  @Override
  public IExpression getMember(final int index)
  {
    return members.get(index);
  }

  @Override
  public Type getType()
  {
    return this.type;
  }

  @Override
  public boolean isAggregate()
  {
    return this.isAggregate;
  }

  @Override
  public boolean isLiteral()
  {
    return this.isLiteral;
  }

  @Override
  public int size()
  {
    return members.size();
  }

  @Override
  public String toString()
  {
    final StringBuffer result = new StringBuffer(size() > 0 ? members.get(0).toString() : "");
    for (int i = 1; i < members.size(); i++)
    {
      result.append(" " + connectives.get(i - 1).toString());
      result.append(" " + members.get(i).toString());
    }
    final String value = "(" + result.toString() + ")";
    return value;
  }

  @Override
  public String toStringTyped()
  {
    final StringBuffer result = new StringBuffer(size() > 0 ? members.get(0).toStringTyped() : "");
    for (int i = 1; i < members.size(); i++)
    {
      result.append(" " + connectives.get(i - 1).toString());
      result.append(" " + members.get(i).toStringTyped());
    }
    return "(" + result.toString() + ")" + "{" + (type == null ? "" : type.toString()) + "}";
  }

  private void dieInvalid(final IExpression expression, final String message)
      throws ExpressionException
  {
    throw new ExpressionException(String.format(
        "Error appending multiplicand '%s' to expression '%s'. %s", expression == null ? "null"
            : expression.toStringTyped(), toStringTyped(), message == null ? "" : message));
  }
}