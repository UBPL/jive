package edu.buffalo.cse.jive.internal.practql.test;

import edu.buffalo.cse.jive.internal.practql.Factory;
import edu.buffalo.cse.jive.practql.IQueryCompiler;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpression;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.parser.IParser;
import edu.buffalo.cse.jive.practql.parser.ParserException;
import edu.buffalo.cse.jive.practql.schema.IDatabaseSchema;
import edu.buffalo.cse.jive.practql.schema.SchemaException;
import edu.buffalo.cse.jive.practql.tokenizer.TokenizerException;

class RecursiveTest extends Test
{
  public static void main(final String[] args) throws TokenizerException, ParserException,
      QueryException, SchemaException
  {
    final IDatabaseSchema dbSchema = Factory.INSTANCE.schemaFactory().newDatabaseSchema("db");
    dbSchema.append(Test.getGraph());
    dbSchema.append(Test.getTrue());
    System.out.println("------------------------------------------------------------\n");
    RecursiveTest.testTranslation(dbSchema, Test.TEMPORAL_RECURSIVE);
    System.out.println("------------------------------------------------------------\n");
  }

  static void testTranslation(final IDatabaseSchema dbSchema, final String[] queries)
      throws ParserException, QueryException
  {
    int i = 0;
    final IParser parser = Factory.INSTANCE.createParser();
    for (final String exp : queries)
    {
      System.out.format("%2d) %s\n", ++i, exp);
      final IQueryExpression expression = parser.parse(exp, dbSchema);
      final IQueryCompiler qc = Factory.INSTANCE.createCompiler(dbSchema);
      qc.compile(expression);
      System.out.format("%s\n", qc.queryString());
      System.out.format("%s\n\n", qc.queryExpression() != null ? qc.queryExpression().getQuery()
          .getSignature().toString() : "");
    }
  }
}