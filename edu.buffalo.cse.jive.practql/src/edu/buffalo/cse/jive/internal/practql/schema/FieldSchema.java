package edu.buffalo.cse.jive.internal.practql.schema;

import edu.buffalo.cse.jive.practql.schema.IFieldSchema;
import edu.buffalo.cse.jive.practql.schema.SchemaException;
import edu.buffalo.cse.jive.practql.schema.Type;

class FieldSchema implements IFieldSchema
{
  private final String name;
  private final Type type;

  FieldSchema(final String name, final Type type) throws SchemaException
  {
    // catch null name
    if (name == null)
    {
      throw new SchemaException("Error creating field schema: null name.");
    }
    // catch null type
    if (type == null)
    {
      throw new SchemaException("Error creating field schema: null type.");
    }
    // catch invalid type in field reference
    if (type == Type.INVALID)
    {
      throw new SchemaException(String.format(
          "Error creating field schema '%s': invalid type '%s'.", name, type.toString()));
    }
    this.name = name;
    this.type = type;
  }

  @Override
  public boolean equals(final Object other)
  {
    if (other == this)
    {
      return true;
    }
    if (!(other instanceof IFieldSchema))
    {
      return false;
    }
    final IFieldSchema fs = (IFieldSchema) other;
    return name.equalsIgnoreCase(fs.getName()) && type == fs.getType();
  }

  @Override
  public String getName()
  {
    return name;
  }

  @Override
  public Type getType()
  {
    return type;
  }

  @Override
  public int hashCode()
  {
    return 17 * name.toUpperCase().hashCode() + 23 * type.hashCode();
  }

  @Override
  public String toString()
  {
    return name + " " + type.toString();
  }
}