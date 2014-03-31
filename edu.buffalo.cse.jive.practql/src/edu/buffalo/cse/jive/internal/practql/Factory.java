package edu.buffalo.cse.jive.internal.practql;

import edu.buffalo.cse.jive.internal.practql.compiler.CompilerUtils;
import edu.buffalo.cse.jive.internal.practql.expression.ExpressionFactory;
import edu.buffalo.cse.jive.internal.practql.expression.query.QueryExpressionFactory;
import edu.buffalo.cse.jive.internal.practql.parser.ParserFactory;
import edu.buffalo.cse.jive.internal.practql.schema.SchemaFactory;
import edu.buffalo.cse.jive.practql.IFactory;
import edu.buffalo.cse.jive.practql.IQueryCompiler;
import edu.buffalo.cse.jive.practql.expression.IExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpressionFactory;
import edu.buffalo.cse.jive.practql.parser.IParser;
import edu.buffalo.cse.jive.practql.parser.IParserFactory;
import edu.buffalo.cse.jive.practql.schema.IDatabaseSchema;
import edu.buffalo.cse.jive.practql.schema.ISchemaFactory;

public enum Factory implements IFactory
{
  INSTANCE;
  private final IExpressionFactory expressionFactory = ExpressionFactory.INSTANCE;
  private final IParserFactory parserFactory = ParserFactory.INSTANCE;
  private final IQueryExpressionFactory queryExpressionFactory = QueryExpressionFactory.INSTANCE;
  private final ISchemaFactory schemaFactory = SchemaFactory.INSTANCE;

  @Override
  public IQueryCompiler createCompiler(final IDatabaseSchema schema)
  {
    return CompilerUtils.createCompiler(schema);
  }

  @Override
  public IParser createParser()
  {
    return parserFactory.createParser();
  }

  @Override
  public IExpressionFactory expressionFactory()
  {
    return expressionFactory;
  }

  @Override
  public IQueryExpressionFactory queryExpressionFactory()
  {
    return queryExpressionFactory;
  }

  @Override
  public ISchemaFactory schemaFactory()
  {
    return schemaFactory;
  }
}