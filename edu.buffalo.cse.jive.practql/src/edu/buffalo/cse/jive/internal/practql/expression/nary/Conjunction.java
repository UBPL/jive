package edu.buffalo.cse.jive.internal.practql.expression.nary;

import java.util.ArrayList;
import java.util.List;

import edu.buffalo.cse.jive.internal.practql.TypeUtils;
import edu.buffalo.cse.jive.internal.practql.expression.StructuredExpression;
import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.nary.IConjunction;
import edu.buffalo.cse.jive.practql.schema.Type;

class Conjunction extends StructuredExpression implements IConjunction
{
  private boolean isLiteral;
  private final List<IExpression> members = new ArrayList<IExpression>();
  private Type type = null;

  Conjunction(final IExpression expression) throws ExpressionException
  {
    // catch null expression
    if (expression == null)
    {
      dieInvalid(expression);
    }
    final Type t = TypeUtils.initFormula(expression.getType());
    // catch invalid type
    if (t == Type.INVALID)
    {
      dieInvalid(expression);
    }
    // we can proceed
    this.type = t;
    this.members.add(expression);
    this.isLiteral = expression.isLiteral();
  }

  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    if (visitor.visitConjunction(this, arg))
    {
      for (int i = 0; i < members.size(); i++)
      {
        members.get(i).accept(visitor, arg);
      }
    }
  }

  @Override
  public IConjunction append(final IExpression expression) throws ExpressionException
  {
    // catch null expression
    if (expression == null)
    {
      dieInvalid(expression);
    }
    final Type t = TypeUtils.inferForConjunction(type, expression.getType());
    // catch invalid type
    if (t == Type.INVALID)
    {
      dieInvalid(expression);
    }
    // we can proceed
    this.type = t;
    this.members.add(expression);
    this.isLiteral = isLiteral && expression.isLiteral();
    return this;
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
    return false;
  }

  @Override
  public boolean isLiteral()
  {
    return isLiteral;
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
      result.append(" AND " + members.get(i).toString());
    }
    return "(" + result.toString() + ")";
  }

  @Override
  public String toStringTyped()
  {
    final StringBuffer result = new StringBuffer(size() > 0 ? members.get(0).toStringTyped() : "");
    for (int i = 1; i < members.size(); i++)
    {
      result.append(" AND " + members.get(i).toStringTyped());
    }
    return "(" + result.toString() + ")" + "{" + (type == null ? "" : type.toString()) + "}";
  }

  private void dieInvalid(final IExpression expression) throws ExpressionException
  {
    throw new ExpressionException(String.format(
        "Error appending conjunct '%s' to expression '%s'.", expression == null ? "null"
            : expression.toStringTyped(), toStringTyped()));
  }
}