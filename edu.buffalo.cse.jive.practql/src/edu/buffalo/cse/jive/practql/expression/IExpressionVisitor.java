package edu.buffalo.cse.jive.practql.expression;

import edu.buffalo.cse.jive.practql.expression.atomic.IAggregateExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.IFieldExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.IFunctionCallExpression;
import edu.buffalo.cse.jive.practql.expression.literal.ILiteral;
import edu.buffalo.cse.jive.practql.expression.nary.IAddition;
import edu.buffalo.cse.jive.practql.expression.nary.IConjunction;
import edu.buffalo.cse.jive.practql.expression.nary.IDisjunction;
import edu.buffalo.cse.jive.practql.expression.nary.IMultiplication;
import edu.buffalo.cse.jive.practql.expression.relational.IRelationalExpression;
import edu.buffalo.cse.jive.practql.expression.unary.INamedExpression;
import edu.buffalo.cse.jive.practql.expression.unary.INegatedExpression;
import edu.buffalo.cse.jive.practql.expression.unary.INegativeExpression;
import edu.buffalo.cse.jive.practql.expression.unary.ISortedExpression;

// visitor for expressions
public interface IExpressionVisitor
{
  public boolean visitAddition(IAddition expression, Object arg) throws ExpressionException;

  public boolean visitAggregate(IAggregateExpression expression, Object arg)
      throws ExpressionException;

  public boolean visitConjunction(IConjunction expression, Object arg) throws ExpressionException;

  public boolean visitDisjunction(IDisjunction expression, Object arg) throws ExpressionException;

  public boolean visitField(IFieldExpression expression, Object arg) throws ExpressionException;

  public boolean visitFunctionCall(IFunctionCallExpression expression, Object arg)
      throws ExpressionException;

  public boolean visitLiteral(ILiteral expression, Object arg) throws ExpressionException;

  public boolean visitMultiplication(IMultiplication expression, Object arg)
      throws ExpressionException;

  public boolean visitNamed(INamedExpression expression, Object arg) throws ExpressionException;

  public boolean visitNegated(INegatedExpression expression, Object arg) throws ExpressionException;

  public boolean visitNegative(INegativeExpression expression, Object arg)
      throws ExpressionException;

  public boolean visitRelational(IRelationalExpression expression, Object arg)
      throws ExpressionException;

  public boolean visitSorted(ISortedExpression expression, Object arg) throws ExpressionException;
}