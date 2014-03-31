package edu.buffalo.cse.jive.practql.schema;

import java.util.List;

import edu.buffalo.cse.jive.practql.expression.query.IClauseSelect;

// factory for schemas
public interface ISchemaFactory
{
  public IDatabaseSchema copyDatabaseSchema(IDatabaseSchema schema) throws SchemaException;

  public IDatabaseSchema newDatabaseSchema(String schemaName) throws SchemaException;

  public IFieldReference newFieldReference(IRelationReference relRef, IFieldSchema fieldSchema)
      throws SchemaException;

  public IFieldSchema newFieldSchema(String fieldName, Type fieldType) throws SchemaException;

  public IRelationReference newRelationReference(String relVar, IRelationSchema schema)
      throws SchemaException;

  public IRelationSchema newRelationSchema(String relName, IClauseSelect select)
      throws SchemaException;

  public IRelationSchema newRelationSchema(String relName, IClauseSelect select,
      List<String> fieldNames) throws SchemaException;

  public IRelationSchema newRelationSchema(String relName, IFieldSchema fieldSchema)
      throws SchemaException;

  public IRelationSchema newRelationSchema(String relName, IRelationSchema schema)
      throws SchemaException;

  public IRelationSchema newRelationSchema(String relName, ISchemaSignature signature)
      throws SchemaException;

  public ISchemaSignature newSchemaSignature(IFieldSchema fieldSchema) throws SchemaException;
}