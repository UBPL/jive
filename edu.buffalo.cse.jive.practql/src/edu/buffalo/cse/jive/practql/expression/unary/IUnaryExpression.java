package edu.buffalo.cse.jive.practql.expression.unary;

import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IStructuredExpression;

// structured expression that encapsulates and possibly adorns a single expression
public interface IUnaryExpression extends IStructuredExpression
{
  public IExpression getExpression();
}