package org.lessa.demian;

import java.util.HashMap;

import org.lessa.demian.expression.ExpressionException;
import org.lessa.demian.expression.IExpression;
import org.lessa.demian.expression.IExpressionVisitor;
import org.lessa.demian.expression.IIntegerLiteral;
import org.lessa.demian.expression.IVariableExpression;

public class ExpressionEvaluator implements IExpressionVisitor
{
  private Integer result;
  private HashMap<IVariableExpression, Integer> varTable;

  public ExpressionEvaluator()
  {
    this.result = null;
    this.varTable = new HashMap<IVariableExpression, Integer>();
  }

  public Integer evaluate(IExpression expression) throws ExpressionException
  {
    expression.accept(this);
    return result;
  }

  @Override
  public void visitInteger(IIntegerLiteral literal)
  {
    result = literal.getValue();
  }

  @Override
  public void visitVariable(IVariableExpression var) throws ExpressionException
  {
    result = varTable.get(var);
    if (result == null)
    {
      throw new ExpressionException(String.format("Undefined variable '%s'.\n", var.toString()));
    }
  }

  @Override
  public void visitSub(IExpression lhs, IExpression rhs) throws ExpressionException
  {
    result = evaluate(lhs) - evaluate(rhs);
  }

  @Override
  public void visitMult(IExpression lhs, IExpression rhs) throws ExpressionException
  {
    result = evaluate(lhs) * evaluate(rhs);
  }

  @Override
  public void visitLet(IVariableExpression var, IExpression rhs, IExpression exp)
      throws ExpressionException
  {
    varTable.put(var, evaluate(rhs));
    result = evaluate(exp);
  }

  @Override
  public void visitDiv(IExpression lhs, IExpression rhs) throws ExpressionException
  {
    result = evaluate(lhs) / evaluate(rhs);
  }

  @Override
  public void visitAdd(IExpression lhs, IExpression rhs) throws ExpressionException
  {
    result = evaluate(lhs) + evaluate(rhs);
  }
}
