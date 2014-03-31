package calculator;

import org.lessa.demian.ExpressionEvaluator;
import org.lessa.demian.expression.ExpressionException;
import org.lessa.demian.internal.Factory;
import org.lessa.demian.parser.ParserException;
import org.lessa.demian.tokenizer.TokenizerException;

public class Main
{
  public static void main(final String[] args) throws TokenizerException, ParserException,
      ExpressionException
  {
    if (args.length != 1)
    {
      System.err.println(String.format("Error: one argument expected but %d found.", args.length));
      System.exit(-1);
    }
    System.out.println(
        new ExpressionEvaluator()
          .evaluate(
              Factory.INSTANCE
                .createParser()
                  .parse("let(a, let(b, 10, add(b, b)), let(b, 20, add(a, b))")));
  }
}
