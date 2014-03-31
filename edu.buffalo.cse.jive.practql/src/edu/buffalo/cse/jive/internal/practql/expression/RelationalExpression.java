package edu.buffalo.cse.jive.internal.practql.expression;

import edu.buffalo.cse.jive.internal.practql.TypeUtils;
import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.atomic.IAtomicExpression;
import edu.buffalo.cse.jive.practql.expression.relational.IRelationalExpression;
import edu.buffalo.cse.jive.practql.expression.relational.RelationalConnective;
import edu.buffalo.cse.jive.practql.schema.Type;

class RelationalExpression extends StructuredExpression implements IRelationalExpression
{
  private final RelationalConnective connective;
  private final IExpression lhs;
  private final IExpression rhs;
  private Type type = null;

  RelationalExpression(final IExpression lhs, final RelationalConnective rc, final IExpression rhs)
      throws ExpressionException
  {
    // catch null expression
    catchNullLHS(lhs);
    // catch null expression
    catchNullRHS(rhs);
    // catch null relational operator
    catchNullRelConnective(rc);
    // resolve the type for the relational expression 'lhs rc rhs'
    final Type t = TypeUtils.initForRelationalExpression(lhs.getType(), rc, rhs.getType());
    // catch invalid relational expression
    catchInvalidType(lhs, rc, rhs, t);
    // validate temporal relational expressions
    if (lhs.getType().isStrictTemporal() || rhs.getType().isStrictTemporal())
    {
      // at least one of the expressions must be an atomic expression
      catchNoAtomicExpression(lhs, rc, rhs);
      // if lhs is not temporal
      if (!lhs.getType().isStrictTemporal())
      {
        // rhs must be atomic: c <relop> t
        catchNoAtomicRHS(rhs, lhs);
        // lhs must be an atomic literal: c <relop> t
        catchNoAtomicLiteralLHS(rhs, lhs);
      }
      // if rhs is not temporal
      if (!rhs.getType().isStrictTemporal())
      {
        // lhs must be atomic: t <relop> c
        catchNoAtomicLHS(rhs, lhs);
        // rhs must be an atomic literal: t <relop> c
        catchNoAtomicLiteralRHS(rhs, lhs);
      }
    }
    // we can proceed
    this.type = t;
    this.lhs = lhs;
    this.rhs = rhs;
    this.connective = rc;
  }

  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    if (visitor.visitRelational(this, arg))
    {
      lhs.accept(visitor, arg);
      rhs.accept(visitor, arg);
    }
  }

  @Override
  public RelationalConnective getConnective()
  {
    return this.connective;
  }

  @Override
  public IExpression getLHS()
  {
    return this.lhs;
  }

  @Override
  public IExpression getRHS()
  {
    return this.rhs;
  }

  @Override
  public Type getType()
  {
    return this.type;
  }

  @Override
  public boolean isAggregate()
  {
    return lhs.isAggregate() || rhs.isAggregate();
  }

  @Override
  public boolean isLiteral()
  {
    return lhs.isLiteral() && rhs.isLiteral();
  }

  @Override
  public String toString()
  {
    return String.format("(%s %s %s)", lhs.toString(), connective.toString(), rhs.toString());
  }

  @Override
  public String toStringTyped()
  {
    return String.format("(%s %s %s)", lhs.toStringTyped(), connective.toString(),
        rhs.toStringTyped())
        + "{" + getType().toString() + "}";
  }

  private void catchInvalidType(final IExpression lhs, final RelationalConnective rc,
      final IExpression rhs, final Type t) throws ExpressionException
  {
    if (t == Type.INVALID)
    {
      throw new ExpressionException(String.format(
          "Error creating relational expression: invalid expression '%s %s %s'.",
          lhs.toStringTyped(), rc.toString(), rhs.toStringTyped()));
    }
  }

  private void catchNoAtomicExpression(final IExpression lhs, final RelationalConnective rc,
      final IExpression rhs) throws ExpressionException
  {
    if (!(lhs instanceof IAtomicExpression) && !(rhs instanceof IAtomicExpression))
    {
      throw new ExpressionException(
          String
              .format(
                  "Error creating temporal relational expression: both lhs and rhs expressions are non-atomic '%s %s %s'.",
                  lhs.toStringTyped(), rc.toString(), rhs.toStringTyped()));
    }
  }

  private void catchNoAtomicLHS(final IExpression rhs, final IExpression lhs)
      throws ExpressionException
  {
    if (!(lhs instanceof IAtomicExpression))
    {
      throw new ExpressionException(
          String
              .format(
                  "Error creating temporal relational expression: non-atomic expression '%s' with integer literal '%s'.",
                  lhs.toStringTyped(), rhs.toStringTyped()));
    }
  }

  private void catchNoAtomicLiteralLHS(final IExpression rhs, final IExpression lhs)
      throws ExpressionException
  {
    if (!lhs.isLiteral() || !(lhs instanceof IAtomicExpression))
    {
      throw new ExpressionException(
          String
              .format(
                  "Error creating temporal relational expression: integer expression '%s' is either non-literal or non-atomic.",
                  lhs.toStringTyped()));
    }
  }

  private void catchNoAtomicLiteralRHS(final IExpression rhs, final IExpression lhs)
      throws ExpressionException
  {
    if (!rhs.isLiteral() || !(rhs instanceof IAtomicExpression))
    {
      throw new ExpressionException(
          String
              .format(
                  "Error creating temporal relational expression: integer expression '%s' is either non-literal or non-atomic.",
                  rhs.toStringTyped()));
    }
  }

  private void catchNoAtomicRHS(final IExpression rhs, final IExpression lhs)
      throws ExpressionException
  {
    if (!(rhs instanceof IAtomicExpression))
    {
      throw new ExpressionException(
          String
              .format(
                  "Error creating temporal relational expression: non-atomic expression '%s' with integer literal '%s'.",
                  rhs.toStringTyped(), lhs.toStringTyped()));
    }
  }

  private void catchNullLHS(final IExpression lhs) throws ExpressionException
  {
    if (lhs == null)
    {
      throw new ExpressionException(
          "Error creating relational expression: null left hand side expression.");
    }
  }

  private void catchNullRelConnective(final RelationalConnective rc) throws ExpressionException
  {
    if (rc == null)
    {
      throw new ExpressionException(
          "Error creating relational expression: null relational operator.");
    }
  }

  private void catchNullRHS(final IExpression rhs) throws ExpressionException
  {
    if (rhs == null)
    {
      throw new ExpressionException(
          "Error creating relational expression: null right hand side expression.");
    }
  }
}