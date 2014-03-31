package edu.buffalo.cse.jive.internal.practql.expression.atomic;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.atomic.CFunctionType;
import edu.buffalo.cse.jive.practql.expression.atomic.ICFunctionCallExpression;
import edu.buffalo.cse.jive.practql.schema.Type;

class CFunctionCallExpression extends AtomicExpression implements ICFunctionCallExpression
{
  private final IExpression arg1;
  private final IExpression arg2;
  private final CFunctionType functionType;

  CFunctionCallExpression(final CFunctionType function, final IExpression arg1,
      final IExpression arg2) throws ExpressionException
  {
    this.arg1 = arg1;
    this.arg2 = arg2;
    this.functionType = function;
  }

  @Override
  public void accept(final IExpressionVisitor visitor, final Object arg) throws ExpressionException
  {
    // concrete types are not visited
  }

  @Override
  public IExpression getArgument1()
  {
    return this.arg1;
  }

  @Override
  public IExpression getArgument2()
  {
    return this.arg2;
  }

  @Override
  public CFunctionType getFunctionType()
  {
    return this.functionType;
  }

  @Override
  public Type getType()
  {
    return this.functionType.returnType();
  }

  @Override
  public boolean isAggregate()
  {
    return false;
  }

  @Override
  public boolean isLiteral()
  {
    return false;
  }

  @Override
  public String toString()
  {
    final StringBuffer result = new StringBuffer(functionType.toString());
    result.append("(");
    result.append(arg1.toString());
    result.append(", ");
    result.append(arg2.toString());
    result.append(")");
    return result.toString();
  }

  @Override
  public String toStringTyped()
  {
    return toString() + "{" + getType().toString() + "}";
  }
}