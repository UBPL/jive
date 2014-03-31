package edu.buffalo.cse.jive.practql.schema;

public interface IDatabaseSchema
{
  public void append(IRelationSchema schema) throws SchemaException;

  public String getName();

  public IRelationSchema getRelationSchema(int index);

  public IRelationSchema lookupRelation(String name);

  public IRelationSchema lookupRelation(String name, RelationKind kind);

  public void remove(IRelationSchema schema);

  public int size();
}