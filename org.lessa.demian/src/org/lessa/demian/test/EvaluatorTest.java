package org.lessa.demian.test;

import org.lessa.demian.ExpressionEvaluator;
import org.lessa.demian.expression.ExpressionException;
import org.lessa.demian.expression.IExpression;
import org.lessa.demian.internal.Factory;
import org.lessa.demian.parser.IParser;
import org.lessa.demian.parser.ParserException;
import org.lessa.demian.tokenizer.TokenizerException;

class EvaluatorTest extends Test
{
  public static void main(final String[] args) throws TokenizerException, ParserException,
      ExpressionException
  {
    System.out.println("------------------------------------------------------------\n");
    EvaluatorTest.test(EXPRESSIONS);
    System.out.println("------------------------------------------------------------\n");
  }

  static void test(final String[] expressions) throws ParserException, ExpressionException
  {
    int i = 0;
    final IParser parser = Factory.INSTANCE.createParser();
    for (final String exp : expressions)
    {
      System.out.format("%2d) %s\n", ++i, exp);
      final IExpression expression = parser.parse(exp);
      ExpressionEvaluator ee = new ExpressionEvaluator();
      Integer result = ee.evaluate(expression);
      System.out.format("%s = %d\n", expression.toString(), result);
    }
  }
}