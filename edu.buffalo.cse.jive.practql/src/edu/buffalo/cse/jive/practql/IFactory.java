package edu.buffalo.cse.jive.practql;

import edu.buffalo.cse.jive.practql.expression.IExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpressionFactory;
import edu.buffalo.cse.jive.practql.parser.IParser;
import edu.buffalo.cse.jive.practql.schema.IDatabaseSchema;
import edu.buffalo.cse.jive.practql.schema.ISchemaFactory;

public interface IFactory
{
  public IQueryCompiler createCompiler(IDatabaseSchema schema);

  public IParser createParser();

  public IExpressionFactory expressionFactory();

  public IQueryExpressionFactory queryExpressionFactory();

  public ISchemaFactory schemaFactory();
}