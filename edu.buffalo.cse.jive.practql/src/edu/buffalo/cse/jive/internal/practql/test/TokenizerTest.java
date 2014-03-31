package edu.buffalo.cse.jive.internal.practql.test;

import java.util.List;

import edu.buffalo.cse.jive.internal.practql.Factory;
import edu.buffalo.cse.jive.internal.practql.tokenizer.TokenizerFactory;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.parser.ParserException;
import edu.buffalo.cse.jive.practql.schema.IDatabaseSchema;
import edu.buffalo.cse.jive.practql.schema.SchemaException;
import edu.buffalo.cse.jive.practql.tokenizer.IToken;
import edu.buffalo.cse.jive.practql.tokenizer.ITokenizer;
import edu.buffalo.cse.jive.practql.tokenizer.TokenizerException;

class TokenizerTest extends Test
{
  public static void main(final String[] args) throws TokenizerException, ParserException,
      QueryException, SchemaException
  {
    final IDatabaseSchema dbSchema = Factory.INSTANCE.schemaFactory().newDatabaseSchema("db");
    dbSchema.append(Test.getR1());
    dbSchema.append(Test.getIndep());
    TokenizerTest.testExpressions();
    System.out.println("------------------------------------------------------------\n");
    TokenizerTest.testQueries();
    System.out.println("------------------------------------------------------------\n");
  }

  static void testExpressions() throws TokenizerException
  {
    int i = 0;
    final ITokenizer tokenizer = TokenizerFactory.INSTANCE.createTokenizer();
    for (final String exp : Test.EXPs)
    {
      System.out.format("%2d) %s\n", ++i, exp);
      final List<IToken> tokens = tokenizer.tokenize(exp);
      System.out.format("    %s\n\n", tokens);
    }
  }

  static void testQueries() throws TokenizerException
  {
    int i = 0;
    final ITokenizer tokenizer = TokenizerFactory.INSTANCE.createTokenizer();
    for (final String sql : Test.SQLs)
    {
      System.out.format("%2d) %s\n", ++i, sql);
      final List<IToken> tokens = tokenizer.tokenize(sql);
      System.out.format("    %s\n\n", tokens);
    }
  }
}