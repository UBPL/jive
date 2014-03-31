package edu.buffalo.cse.jive.internal.practql.expression.atomic;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.atomic.ICIntervalFieldExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.ILeftEndpointFieldExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.IRightEndpointFieldExpression;
import edu.buffalo.cse.jive.practql.schema.IFieldReference;
import edu.buffalo.cse.jive.practql.schema.Type;

class CIntervalFieldExpression extends TranslatedFieldExpression implements
    ICIntervalFieldExpression
{
  private final LeftEndpointFieldExpression left;
  private final RightEndpointFieldExpression right;

  CIntervalFieldExpression(final IFieldReference fieldReference) throws ExpressionException
  {
    super(fieldReference);
    this.left = new LeftEndpointFieldExpression(fieldReference);
    this.right = new RightEndpointFieldExpression(fieldReference);
  }

  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    // concrete types are not visited
  }

  @Override
  public ILeftEndpointFieldExpression getLeft()
  {
    return this.left;
  }

  @Override
  public IRightEndpointFieldExpression getRight()
  {
    return this.right;
  }

  private class LeftEndpointFieldExpression extends TranslatedFieldExpression implements
      ILeftEndpointFieldExpression
  {
    LeftEndpointFieldExpression(final IFieldReference fieldReference) throws ExpressionException
    {
      super(fieldReference);
    }

    @Override
    public Type getType()
    {
      return Type.TP;
    }

    @Override
    public String toString()
    {
      return "get_left(" + super.toString() + ")";
    }

    @Override
    public String toStringTyped()
    {
      return toString() + "{tp}";
    }
  }

  private class RightEndpointFieldExpression extends TranslatedFieldExpression implements
      IRightEndpointFieldExpression
  {
    RightEndpointFieldExpression(final IFieldReference fieldReference) throws ExpressionException
    {
      super(fieldReference);
    }

    @Override
    public Type getType()
    {
      return Type.TP;
    }

    @Override
    public String toString()
    {
      return "get_right(" + super.toString() + ")";
    }

    @Override
    public String toStringTyped()
    {
      return toString() + "{tp}";
    }
  }
}