package edu.buffalo.cse.jive.internal.practql.schema;

import edu.buffalo.cse.jive.practql.schema.IFieldReference;
import edu.buffalo.cse.jive.practql.schema.IFieldSchema;
import edu.buffalo.cse.jive.practql.schema.IRelationReference;
import edu.buffalo.cse.jive.practql.schema.SchemaException;

class FieldReference implements IFieldReference
{
  private final IFieldSchema fieldSchema;
  private final IRelationReference relRef;

  FieldReference(final IRelationReference relRef, final IFieldSchema fieldSchema)
      throws SchemaException
  {
    // catch null relation reference
    if (relRef == null)
    {
      throw new SchemaException("Error creating field reference: null relation reference.");
    }
    // catch null field schema
    if (fieldSchema == null)
    {
      throw new SchemaException("Error creating field reference: null field schema.");
    }
    this.relRef = relRef;
    this.fieldSchema = fieldSchema;
  }

  @Override
  public boolean equals(final Object other)
  {
    if (other == this)
    {
      return true;
    }
    if (!(other instanceof IFieldReference))
    {
      return false;
    }
    final IFieldReference fr = (IFieldReference) other;
    return relRef.equals(fr.getRelationReference()) && fieldSchema.equals(fr.getSchema());
  }

  @Override
  public String getQualifiedName()
  {
    return String.format("\"%s.%s\"", relRef.getVariable(), fieldSchema.getName());
  }

  @Override
  public IRelationReference getRelationReference()
  {
    return this.relRef;
  }

  @Override
  public IFieldSchema getSchema()
  {
    return this.fieldSchema;
  }

  @Override
  public int hashCode()
  {
    return 23 * relRef.hashCode() + 31 * fieldSchema.hashCode();
  }

  @Override
  public String toString()
  {
    return relRef.getVariable() + "." + fieldSchema.getName();
  }
}