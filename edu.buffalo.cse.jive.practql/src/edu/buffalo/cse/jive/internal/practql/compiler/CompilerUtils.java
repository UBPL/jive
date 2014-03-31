package edu.buffalo.cse.jive.internal.practql.compiler;

import edu.buffalo.cse.jive.internal.practql.Factory;
import edu.buffalo.cse.jive.practql.IQueryCompiler;
import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.atomic.IFieldExpression;
import edu.buffalo.cse.jive.practql.expression.query.INamedQuery;
import edu.buffalo.cse.jive.practql.expression.query.IQuery;
import edu.buffalo.cse.jive.practql.expression.query.ISimpleQuery;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.schema.IDatabaseSchema;
import edu.buffalo.cse.jive.practql.schema.IFieldReference;
import edu.buffalo.cse.jive.practql.schema.IFieldSchema;
import edu.buffalo.cse.jive.practql.schema.IRelationReference;
import edu.buffalo.cse.jive.practql.schema.IRelationSchema;
import edu.buffalo.cse.jive.practql.schema.SchemaException;

public enum CompilerUtils
{
  INSTANCE;
  static final boolean DEBUG_MODE = false;
  static final boolean ENFORCE_ATTRIBUTE_INDEPENDENCE = false;
  private static int FIELD_ID = 0;
  private static int FUNCTION_ID = 0;
  private static int PARTITION_ID = 0;
  private static int PROJECTION_ID = 0;
  private static int QUERY_ID = 0;
  private static int RELATION_ID = 0;
  private static int UNION_ID = 0;

  public static IQueryCompiler createCompiler(final IDatabaseSchema schema)
  {
    return new QueryCompilerVisitor(schema);
  }

  static IFieldExpression createFieldExpression(final IRelationReference rel, final IFieldSchema fs)
      throws QueryException
  {
    // field reference
    final IFieldReference fr;
    try
    {
      fr = Factory.INSTANCE.schemaFactory().newFieldReference(rel, fs);
    }
    catch (final SchemaException e)
    {
      throw new QueryException(String.format(
          "Error creating reference for field '%s' and relation reference '%s'.", fs.toString(),
          rel.toString()), e);
    }
    try
    {
      // expression encapsulating the field reference
      return Factory.INSTANCE.expressionFactory().newFieldExpression(fr);
    }
    catch (final ExpressionException e)
    {
      throw new QueryException(String.format(
          "Error creating expression for field '%s' and relation reference '%s'.", fs.toString(),
          rel.toString()), e);
    }
  }

  static INamedQuery createNamedQuery(final String name, final ISimpleQuery query)
      throws QueryException
  {
    final Factory factory = Factory.INSTANCE;
    // create the container query
    final IQuery q = factory.queryExpressionFactory().newQuery(query);
    IRelationSchema schema;
    try
    {
      schema = factory.schemaFactory().newRelationSchema(name, query.getSelect());
    }
    catch (final SchemaException e)
    {
      throw new QueryException("Error creating query schema.", e);
    }
    // create the named query
    final INamedQuery result = factory.queryExpressionFactory().newNamedQuery(schema, q);
    // return the named query
    return result;
  }

  static String newBaseQueryName()
  {
    return "__BASE" + (++CompilerUtils.QUERY_ID);
  }

  static String newExceptQueryName()
  {
    return "__EXCEPT" + (++CompilerUtils.QUERY_ID);
  }

  static String newFieldName()
  {
    return "__F" + (++CompilerUtils.FIELD_ID);
  }

  static String newFunctionName()
  {
    return "__FN" + (++CompilerUtils.FUNCTION_ID);
  }

  static String newPartitionName()
  {
    return "__PAR" + (++CompilerUtils.PARTITION_ID);
  }

  static String newProjectionName()
  {
    return "__PRJ" + (++CompilerUtils.PROJECTION_ID);
  }

  static String newRecursiveQueryName()
  {
    return "__TPr" + (++CompilerUtils.QUERY_ID);
  }

  static String newRelationName()
  {
    return "__R" + (++CompilerUtils.RELATION_ID);
  }

  static String newRoundsName()
  {
    return "__ROUNDS" + (++CompilerUtils.RELATION_ID);
  }

  static String newTNPName()
  {
    return "__TNP" + (++CompilerUtils.RELATION_ID);
  }

  static String newUnionName()
  {
    return "__U" + (++CompilerUtils.UNION_ID);
  }
}