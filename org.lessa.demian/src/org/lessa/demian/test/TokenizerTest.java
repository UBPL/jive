package org.lessa.demian.test;

import java.util.List;

import org.lessa.demian.internal.tokenizer.TokenizerFactory;
import org.lessa.demian.parser.ParserException;
import org.lessa.demian.tokenizer.IToken;
import org.lessa.demian.tokenizer.ITokenizer;
import org.lessa.demian.tokenizer.TokenizerException;

class TokenizerTest extends Test
{
  public static void main(final String[] args) throws TokenizerException, ParserException
  {
    System.out.println("------------------------------------------------------------\n");
    TokenizerTest.test(EXPRESSIONS);
    System.out.println("------------------------------------------------------------\n");
  }

  static void test(String[] expressions) throws TokenizerException
  {
    int i = 0;
    final ITokenizer tokenizer = TokenizerFactory.INSTANCE.createTokenizer();
    for (final String exp : expressions)
    {
      System.out.format("%2d) %s\n", ++i, exp);
      final List<IToken> tokens = tokenizer.tokenize(exp);
      System.out.format("    %s\n\n", tokens);
    }
  }
}