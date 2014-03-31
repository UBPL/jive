package org.lessa.demian.test;

import org.lessa.demian.expression.IExpression;
import org.lessa.demian.internal.Factory;
import org.lessa.demian.parser.IParser;
import org.lessa.demian.parser.ParserException;
import org.lessa.demian.tokenizer.TokenizerException;

class ParserTest extends Test
{
  public static void main(final String[] args) throws TokenizerException, ParserException
  {
    System.out.println("------------------------------------------------------------");
    ParserTest.test(EXPRESSIONS);
    System.out.println("------------------------------------------------------------");
  }

  static void test(final String[] expressions) throws ParserException
  {
    int i = 0;
    final IParser parser = Factory.INSTANCE.createParser();
    for (final String exp : expressions)
    {
      System.out.format("%2d) %s\n", ++i, exp);
      final IExpression expression = parser.parse(exp);
      System.out.format("%s\n", expression.toString());
    }
  }
}