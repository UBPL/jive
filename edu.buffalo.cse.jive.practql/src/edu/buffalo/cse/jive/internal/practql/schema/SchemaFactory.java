package edu.buffalo.cse.jive.internal.practql.schema;

import java.util.List;

import edu.buffalo.cse.jive.practql.expression.query.IClauseSelect;
import edu.buffalo.cse.jive.practql.schema.IDatabaseSchema;
import edu.buffalo.cse.jive.practql.schema.IFieldReference;
import edu.buffalo.cse.jive.practql.schema.IFieldSchema;
import edu.buffalo.cse.jive.practql.schema.IRelationReference;
import edu.buffalo.cse.jive.practql.schema.IRelationSchema;
import edu.buffalo.cse.jive.practql.schema.ISchemaFactory;
import edu.buffalo.cse.jive.practql.schema.ISchemaSignature;
import edu.buffalo.cse.jive.practql.schema.SchemaException;
import edu.buffalo.cse.jive.practql.schema.Type;

public enum SchemaFactory implements ISchemaFactory
{
  INSTANCE;
  @Override
  public IDatabaseSchema copyDatabaseSchema(final IDatabaseSchema schema) throws SchemaException
  {
    return new DatabaseSchema(schema);
  }

  @Override
  public IDatabaseSchema newDatabaseSchema(final String schemaName) throws SchemaException
  {
    return new DatabaseSchema(schemaName);
  }

  @Override
  public IFieldReference newFieldReference(final IRelationReference relRef,
      final IFieldSchema fieldSchema) throws SchemaException
  {
    return new FieldReference(relRef, fieldSchema);
  }

  @Override
  public IFieldSchema newFieldSchema(final String name, final Type type) throws SchemaException
  {
    return new FieldSchema(name, type);
  }

  @Override
  public IRelationReference newRelationReference(final String relVar,
      final IRelationSchema relSchema) throws SchemaException
  {
    return new RelationReference(relVar, relSchema);
  }

  @Override
  public IRelationSchema newRelationSchema(final String relName, final IClauseSelect select)
      throws SchemaException
  {
    if (select == null)
    {
      throw new SchemaException(String.format("Invalid (null) projection list for '%s'.", relName));
    }
    final ISchemaSignature signature = newSchemaSignature(newFieldSchema(select.getMember(0)
        .getName(), select.getMember(0).getType()));
    for (int i = 1; i < select.size(); i++)
    {
      signature
          .append(newFieldSchema(select.getMember(i).getName(), select.getMember(i).getType()));
    }
    return new RelationSchema(relName, signature);
  }

  @Override
  public IRelationSchema newRelationSchema(final String relName, final IClauseSelect select,
      final List<String> fieldNames) throws SchemaException
  {
    if (select == null)
    {
      throw new SchemaException(String.format("Invalid (null) projection list for '%s'.", relName));
    }
    if (fieldNames == null)
    {
      throw new SchemaException(String.format(
          "Invalid (null) argument list for the signature of '%s'.", relName));
    }
    if (fieldNames.size() != select.size())
    {
      throw new SchemaException(
          String
              .format(
                  "The number of arguments in the signature of '%s' (%d) must match the number of fields in the projection list (%d).",
                  relName, fieldNames.size(), select.size()));
    }
    final ISchemaSignature signature = newSchemaSignature(newFieldSchema(fieldNames.get(0), select
        .getMember(0).getType()));
    for (int i = 1; i < select.size(); i++)
    {
      signature.append(newFieldSchema(fieldNames.get(i), select.getMember(i).getType()));
    }
    return new RelationSchema(relName, signature);
  }

  @Override
  public IRelationSchema newRelationSchema(final String relName, final IFieldSchema fieldSchema)
      throws SchemaException
  {
    return new RelationSchema(relName, new SchemaSignature(fieldSchema));
  }

  @Override
  public IRelationSchema newRelationSchema(final String relName, final IRelationSchema schema)
      throws SchemaException
  {
    if (schema == null)
    {
      throw new SchemaException(String.format("Invalid (null) schema for '%s'.", relName));
    }
    final ISchemaSignature signature = newSchemaSignature(newFieldSchema(schema.getSignature()
        .getFieldSchema(0).getName(), schema.getSignature().getFieldSchema(0).getType()));
    for (int i = 1; i < schema.getSignature().size(); i++)
    {
      signature.append(newFieldSchema(schema.getSignature().getFieldSchema(i).getName(), schema
          .getSignature().getFieldSchema(i).getType()));
    }
    return new RelationSchema(relName, signature);
  }

  @Override
  public IRelationSchema newRelationSchema(final String relName, final ISchemaSignature signature)
      throws SchemaException
  {
    return new RelationSchema(relName, signature);
  }

  @Override
  public ISchemaSignature newSchemaSignature(final IFieldSchema fieldSchema) throws SchemaException
  {
    return new SchemaSignature(fieldSchema);
  }
}