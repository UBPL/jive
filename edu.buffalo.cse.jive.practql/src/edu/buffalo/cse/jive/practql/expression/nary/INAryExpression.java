package edu.buffalo.cse.jive.practql.expression.nary;

import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IStructuredExpression;

// structured expression that encapsulates one or more expressions
public interface INAryExpression extends IStructuredExpression
{
  public IExpression getMember(int index);

  public int size();
}