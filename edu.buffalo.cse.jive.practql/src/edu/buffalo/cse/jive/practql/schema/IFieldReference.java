package edu.buffalo.cse.jive.practql.schema;

// a reference to a field within a query
public interface IFieldReference
{
  public String getQualifiedName();

  public IRelationReference getRelationReference();

  public IFieldSchema getSchema();
}