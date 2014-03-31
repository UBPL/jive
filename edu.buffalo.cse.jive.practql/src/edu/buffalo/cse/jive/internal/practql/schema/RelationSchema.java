package edu.buffalo.cse.jive.internal.practql.schema;

import edu.buffalo.cse.jive.practql.schema.IRelationSchema;
import edu.buffalo.cse.jive.practql.schema.ISchemaSignature;
import edu.buffalo.cse.jive.practql.schema.RelationKind;
import edu.buffalo.cse.jive.practql.schema.SchemaException;
import edu.buffalo.cse.jive.practql.schema.Type;

class RelationSchema implements IRelationSchema
{
  private final RelationKind kind;
  private final String name;
  private final ISchemaSignature signature;

  RelationSchema(final String name, final ISchemaSignature signature) throws SchemaException
  {
    // catch null name
    if (name == null)
    {
      throw new SchemaException("Error creating relation schema: null name.");
    }
    // catch null signature
    if (signature == null)
    {
      throw new SchemaException("Error creating relation schema: null signature.");
    }
    // catch null relation schema
    if (signature.size() == 0)
    {
      throw new SchemaException("Error creating relation schema: empty signature.");
    }
    // catch invalid type in field schema
    for (int i = 0; i < signature.size(); i++)
    {
      final Type t = signature.getFieldSchema(i).getType();
      if (t == Type.INVALID || t == Type.NULL)
      {
        throw new SchemaException(String.format(
            "Error creating relations schema '%s': invalid field type '%s'.", name, t.toString()));
      }
    }
    this.kind = RelationKind.RK_TABLE;
    this.name = name;
    this.signature = signature;
  }

  @Override
  public boolean equals(final Object other)
  {
    if (other == this)
    {
      return true;
    }
    if (!(other instanceof IRelationSchema))
    {
      return false;
    }
    final IRelationSchema rs = (IRelationSchema) other;
    return name.equalsIgnoreCase(rs.getName()) && kind.equals(rs.getKind())
        && signature.equals(rs.getSignature());
  }

  @Override
  public RelationKind getKind()
  {
    return this.kind;
  }

  @Override
  public String getName()
  {
    return this.name;
  }

  @Override
  public ISchemaSignature getSignature()
  {
    return signature;
  }

  @Override
  public int hashCode()
  {
    return 17 * name.toUpperCase().hashCode() + 23 * kind.hashCode() + 37 * signature.hashCode();
  }

  @Override
  public String toString()
  {
    return name + signature.toString();
  }
}