package org.lessa.demian.expression;

import java.util.List;

// marker interface for expressions that do not contain subexpressions
public interface IFunctionExpression extends IExpression
{
  public List<IExpression> getArguments();

  public FunctionType getFunctionType();
}
