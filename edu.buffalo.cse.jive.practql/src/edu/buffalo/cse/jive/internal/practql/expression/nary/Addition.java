package edu.buffalo.cse.jive.internal.practql.expression.nary;

import java.util.ArrayList;
import java.util.List;

import edu.buffalo.cse.jive.internal.practql.TypeUtils;
import edu.buffalo.cse.jive.internal.practql.expression.StructuredExpression;
import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.atomic.IAtomicExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.ITranslatedFieldExpression;
import edu.buffalo.cse.jive.practql.expression.literal.ILiteral;
import edu.buffalo.cse.jive.practql.expression.nary.AdditionConnective;
import edu.buffalo.cse.jive.practql.expression.nary.IAddition;
import edu.buffalo.cse.jive.practql.expression.unary.INegativeExpression;
import edu.buffalo.cse.jive.practql.schema.Type;

class Addition extends StructuredExpression implements IAddition
{
  private static final String ERR_TEMPORAL = "Supported temporal arithmetic expressions are of the form 't', 'c', 't + c', or 'c + t', where 't' is a temporal field and 'c' is a non-negative constant. ";
  private final List<AdditionConnective> connectives = new ArrayList<AdditionConnective>();
  private boolean isAggregate;
  private boolean isLiteral;
  private final List<IExpression> members = new ArrayList<IExpression>();
  private Type type = null;

  Addition(final IExpression expression) throws ExpressionException
  {
    // catch null expression
    if (expression == null)
    {
      dieInvalid(expression, null);
    }
    final Type t = TypeUtils.initAddition(expression.getType());
    // catch invalid type
    if (t == Type.INVALID)
    {
      dieInvalid(expression, null);
    }
    // the temporal expression must be an atomic expression
    if (t == Type.TP_ENCODED && !(expression instanceof IAtomicExpression))
    {
      dieInvalid(expression, Addition.ERR_TEMPORAL);
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
    if (visitor.visitAddition(this, arg))
    {
      for (int i = 0; i < members.size(); i++)
      {
        members.get(i).accept(visitor, arg);
      }
    }
  }

  @Override
  public IAddition append(final AdditionConnective connective, final IExpression expression)
      throws ExpressionException
  {
    // catch null connective
    catchNullConnective(connective, expression);
    // catch null expression
    catchNullExpression(expression);
    // infer the type of the operation 'type connective expression.type'
    final Type t = TypeUtils.inferForAddtion(type, expression.getType(), connective);
    // catch invalid type
    if (t == Type.INVALID)
    {
      catchInvalidTemporalExpression(expression);
      dieInvalid(expression, null);
    }
    // check temporal expressions
    if (type.isStrictTemporal() || expression.getType().isStrictTemporal())
    {
      // temporal expressions have size at most 2
      catchInvalidExpressionSize(expression);
      // one of the temporal expressions must be a literal (there are no temporal literals!)
      catchNoTemporalLiteral(expression);
      // expressions must be atomic
      catchNoAtomicExpression(expression);
      // expressions must be non-negative
      catchNonNegativeExpression(expression);
      // expression must be a translated temporal type or a literal
      catchInvalidSubtraction(expression, connective);
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
  public AdditionConnective getConnective(final int index)
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

  private void catchInvalidExpressionSize(final IExpression expression) throws ExpressionException
  {
    if (members.size() == 2)
    {
      dieInvalid(expression, Addition.ERR_TEMPORAL);
    }
  }

  private void catchInvalidSubtraction(final IExpression expression,
      final AdditionConnective connective) throws ExpressionException
  {
    if (connective == AdditionConnective.AC_MINUS && members.get(0).getType() == Type.TP)
    {
      if (expression.getType() != Type.TP && !(expression instanceof ILiteral))
      {
        dieInvalid(expression, Addition.ERR_TEMPORAL);
      }
    }
  }

  private void catchInvalidTemporalExpression(final IExpression expression)
      throws ExpressionException
  {
    if (type.isStrictTemporal())
    {
      dieInvalid(expression, Addition.ERR_TEMPORAL);
    }
  }

  private void catchNoAtomicExpression(final IExpression expression) throws ExpressionException
  {
    if (!(members.get(0) instanceof IAtomicExpression)
        || !(expression instanceof IAtomicExpression))
    {
      if (!(members.get(0) instanceof ITranslatedFieldExpression)
          && !(expression instanceof ITranslatedFieldExpression))
      {
        dieInvalid(expression, Addition.ERR_TEMPORAL);
      }
    }
  }

  private void catchNonNegativeExpression(final IExpression expression) throws ExpressionException
  {
    if (members.get(0) instanceof INegativeExpression || expression instanceof INegativeExpression)
    {
      dieInvalid(expression, Addition.ERR_TEMPORAL);
    }
  }

  private void catchNoTemporalLiteral(final IExpression expression) throws ExpressionException
  {
    if (!isLiteral() && !expression.isLiteral() && getType() != Type.TP
        && expression.getType() != Type.TP)
    {
      dieInvalid(expression, Addition.ERR_TEMPORAL);
    }
  }

  private void catchNullConnective(final AdditionConnective connective, final IExpression expression)
      throws ExpressionException
  {
    if (connective == null)
    {
      dieInvalid(expression, "Addition connective cannot be null.");
    }
  }

  private void catchNullExpression(final IExpression expression) throws ExpressionException
  {
    if (expression == null)
    {
      dieInvalid(expression, Addition.ERR_TEMPORAL);
    }
  }

  private void dieInvalid(final IExpression expression, final String message)
      throws ExpressionException
  {
    throw new ExpressionException(String.format(
        "Error appending summand '%s' to expression '%s'. %s", expression == null ? "null"
            : expression.toStringTyped(), toStringTyped(), message == null ? "" : message));
  }
}