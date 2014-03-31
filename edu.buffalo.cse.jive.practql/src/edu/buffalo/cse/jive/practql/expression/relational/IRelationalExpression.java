package edu.buffalo.cse.jive.practql.expression.relational;

import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IStructuredExpression;

// relational expression used to represent (lhs relop rhs)
public interface IRelationalExpression extends IStructuredExpression
{
  public RelationalConnective getConnective();

  public IExpression getLHS();

  public IExpression getRHS();
}