package edu.buffalo.cse.jive.practql.expression.atomic;

import java.util.List;

import edu.buffalo.cse.jive.practql.expression.IExpression;

public interface IFunctionCallExpression extends IAtomicExpression
{
  public List<IExpression> getArguments();

  public FunctionType getFunctionType();
}