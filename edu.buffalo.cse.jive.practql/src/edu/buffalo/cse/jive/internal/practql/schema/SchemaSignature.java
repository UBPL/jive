package edu.buffalo.cse.jive.internal.practql.schema;

import java.util.ArrayList;
import java.util.List;

import edu.buffalo.cse.jive.practql.schema.IFieldSchema;
import edu.buffalo.cse.jive.practql.schema.ISchemaSignature;
import edu.buffalo.cse.jive.practql.schema.SchemaException;

class SchemaSignature implements ISchemaSignature
{
  private final List<IFieldSchema> fieldSchemas;

  SchemaSignature(final IFieldSchema fieldSchema) throws SchemaException
  {
    // catch null field schema
    if (fieldSchema == null)
    {
      throw new SchemaException("Error creating schema signature: null field schema.");
    }
    // we can proceed
    this.fieldSchemas = new ArrayList<IFieldSchema>();
    fieldSchemas.add(fieldSchema);
  }

  @Override
  public void append(final IFieldSchema fieldSchema) throws SchemaException
  {
    // catch null field schema
    if (fieldSchema == null)
    {
      throw new SchemaException("Error creating schema signature: null field schema.");
    }
    // duplicate name
    for (final IFieldSchema rs : fieldSchemas)
    {
      if (rs.getName().equalsIgnoreCase(fieldSchema.getName()))
      {
        throw new SchemaException(String.format(
            "Error appending to schema signature: duplicate field name '%s'.",
            fieldSchema.getName()));
      }
    }
    // we can proceed
    fieldSchemas.add(fieldSchema);
  }

  @Override
  public boolean equals(final Object other)
  {
    if (other == this)
    {
      return true;
    }
    if (!(other instanceof ISchemaSignature))
    {
      return false;
    }
    final ISchemaSignature ss = (ISchemaSignature) other;
    boolean equals = size() == ss.size();
    for (int i = 0; equals && i < size(); i++)
    {
      equals = equals
          && fieldSchemas.get(i).equals(ss.lookupFieldSchema(fieldSchemas.get(i).getName()));
    }
    return equals;
  }

  @Override
  public IFieldSchema getFieldSchema(final int index)
  {
    return this.fieldSchemas.get(index);
  }

  @Override
  public int hashCode()
  {
    return 17 * System.identityHashCode(this);
  }

  @Override
  public IFieldSchema lookupFieldSchema(final String fieldName)
  {
    for (final IFieldSchema fs : fieldSchemas)
    {
      if (fs.getName().equalsIgnoreCase(fieldName))
      {
        return fs;
      }
    }
    return null;
  }

  @Override
  public int size()
  {
    return fieldSchemas.size();
  }

  @Override
  public String toString()
  {
    final StringBuffer result = new StringBuffer(fieldSchemas.get(0).toString());
    for (int i = 1; i < fieldSchemas.size(); i++)
    {
      result.append(", " + fieldSchemas.get(i));
    }
    return "(" + result.toString() + ")";
  }
}