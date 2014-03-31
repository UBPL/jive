package edu.buffalo.cse.jive.practql.schema;

// tuples, queries
public interface ISchemaSignature
{
  public void append(IFieldSchema schema) throws SchemaException;

  public IFieldSchema getFieldSchema(int index);

  public IFieldSchema lookupFieldSchema(String fieldName);

  public int size();
}