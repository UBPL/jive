package edu.buffalo.cse.jive.practql.schema;

// tables, views, common table expressions
public interface IRelationSchema
{
  public RelationKind getKind();

  public String getName();

  public ISchemaSignature getSignature();
}