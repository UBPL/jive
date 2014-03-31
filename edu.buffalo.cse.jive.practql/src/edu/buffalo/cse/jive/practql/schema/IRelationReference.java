package edu.buffalo.cse.jive.practql.schema;

// a reference to a relation within a query
public interface IRelationReference
{
  public IFieldReference getFieldReference(int index);

  public IRelationSchema getSchema();

  public String getVariable();

  public IFieldReference lookupFieldReference(String fieldName);

  public int size();
}