package edu.buffalo.cse.jive.internal.practql.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.buffalo.cse.jive.practql.schema.IFieldReference;
import edu.buffalo.cse.jive.practql.schema.IRelationReference;
import edu.buffalo.cse.jive.practql.schema.IRelationSchema;
import edu.buffalo.cse.jive.practql.schema.ISchemaSignature;
import edu.buffalo.cse.jive.practql.schema.SchemaException;

class RelationReference implements IRelationReference
{
  private final List<IFieldReference> fieldReferences;
  private final IRelationSchema relSchema;
  private final String relVar;

  RelationReference(final String relVar, final IRelationSchema relSchema) throws SchemaException
  {
    // catch null relation variable
    if (relVar == null)
    {
      throw new SchemaException("Error creating relation reference: null relation variable.");
    }
    // catch null relation schema
    if (relSchema == null)
    {
      throw new SchemaException("Error creating relation reference: null relation schema.");
    }
    // catch null relation schema
    if (relSchema.getSignature().size() == 0)
    {
      throw new SchemaException("Error creating relation reference: empty schema signature.");
    }
    this.relVar = relVar;
    this.relSchema = relSchema;
    final List<IFieldReference> frs = new ArrayList<IFieldReference>();
    final ISchemaSignature ss = relSchema.getSignature();
    for (int i = 0; i < ss.size(); i++)
    {
      frs.add(new FieldReference(this, ss.getFieldSchema(i)));
    }
    this.fieldReferences = Collections.unmodifiableList(frs);
  }

  @Override
  public boolean equals(final Object other)
  {
    if (other == this)
    {
      return true;
    }
    if (!(other instanceof IRelationReference))
    {
      return false;
    }
    final IRelationReference rr = (IRelationReference) other;
    return relVar.equalsIgnoreCase(rr.getVariable()) && relSchema.equals(rr.getSchema());
  }

  @Override
  public IFieldReference getFieldReference(final int index)
  {
    return this.fieldReferences.get(index);
  }

  @Override
  public IRelationSchema getSchema()
  {
    return this.relSchema;
  }

  @Override
  public String getVariable()
  {
    return this.relVar;
  }

  @Override
  public int hashCode()
  {
    return 23 * relVar.toUpperCase().hashCode() + 31 * relSchema.hashCode();
  }

  @Override
  public IFieldReference lookupFieldReference(final String fieldName)
  {
    for (final IFieldReference fr : fieldReferences)
    {
      if (fr.getSchema().getName().equalsIgnoreCase(fieldName))
      {
        return fr;
      }
    }
    return null;
  }

  @Override
  public int size()
  {
    return this.fieldReferences.size();
  }

  @Override
  public String toString()
  {
    if (relSchema.getName().equalsIgnoreCase(relVar))
    {
      return relSchema.getName();
    }
    return relSchema.getName() + " AS " + relVar;
  }
}