package edu.buffalo.cse.jive.practql.expression.atomic;

import edu.buffalo.cse.jive.practql.expression.IExpression;

public interface ICFunctionCallExpression extends IAtomicExpression
{
  public IExpression getArgument1();

  public IExpression getArgument2();

  public CFunctionType getFunctionType();
}