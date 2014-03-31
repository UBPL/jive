package edu.buffalo.cse.jive.practql.expression;

import edu.buffalo.cse.jive.practql.expression.atomic.IAtomicExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.literal.ILiteralExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.nary.INAryExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.relational.IRelationalExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.unary.IUnaryExpressionFactory;

// factory for all types of expressions
public interface IExpressionFactory extends IAtomicExpressionFactory, INAryExpressionFactory,
    ILiteralExpressionFactory, IRelationalExpressionFactory, IUnaryExpressionFactory
{
}