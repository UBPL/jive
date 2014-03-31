package edu.buffalo.cse.jive.internal.practql.schema;

import java.util.ArrayList;
import java.util.List;

import edu.buffalo.cse.jive.practql.schema.IDatabaseSchema;
import edu.buffalo.cse.jive.practql.schema.IRelationSchema;
import edu.buffalo.cse.jive.practql.schema.RelationKind;
import edu.buffalo.cse.jive.practql.schema.SchemaException;

class DatabaseSchema implements IDatabaseSchema
{
  private final String name;
  private final List<IRelationSchema> schemas;

  DatabaseSchema(final IDatabaseSchema schema) throws SchemaException
  {
    // catch null schema
    if (schema == null)
    {
      throw new SchemaException("Error creating database schema: null database schema.");
    }
    // we can proceed
    this.name = schema.getName();
    this.schemas = new ArrayList<IRelationSchema>();
    for (int i = 0; i < schema.size(); i++)
    {
      append(schema.getRelationSchema(i));
    }
  }

  DatabaseSchema(final String name) throws SchemaException
  {
    // catch null name
    if (name == null)
    {
      throw new SchemaException("Error creating database schema: null database schema name.");
    }
    // we can proceed
    this.name = name;
    this.schemas = new ArrayList<IRelationSchema>();
  }

  @Override
  public void append(final IRelationSchema schema) throws SchemaException
  {
    // catch null schema
    if (schema == null)
    {
      throw new SchemaException("Error appending to database schema: null relation schema.");
    }
    // duplicate name
    for (final IRelationSchema rs : schemas)
    {
      if (rs.getName().equalsIgnoreCase(schema.getName()))
      {
        throw new SchemaException(String.format(
            "Error appending to database schema: duplicate relation name '%s'.", schema.getName()));
      }
    }
    // we can proceed
    schemas.add(schema);
  }

  @Override
  public boolean equals(final Object other)
  {
    if (other == this)
    {
      return true;
    }
    if (!(other instanceof IDatabaseSchema))
    {
      return false;
    }
    final IDatabaseSchema dbs = (IDatabaseSchema) other;
    return name.equalsIgnoreCase(dbs.getName());
  }

  @Override
  public String getName()
  {
    return this.name;
  }

  @Override
  public IRelationSchema getRelationSchema(final int index)
  {
    return this.schemas.get(index);
  }

  @Override
  public int hashCode()
  {
    return 17 * name.toUpperCase().hashCode();
  }

  @Override
  public IRelationSchema lookupRelation(final String name)
  {
    for (final IRelationSchema rs : schemas)
    {
      if (rs.getName().equalsIgnoreCase(name))
      {
        return rs;
      }
    }
    return null;
  }

  @Override
  public IRelationSchema lookupRelation(final String name, final RelationKind kind)
  {
    for (final IRelationSchema rs : schemas)
    {
      if (rs.getKind() == kind && rs.getName().equalsIgnoreCase(name))
      {
        return rs;
      }
    }
    return null;
  }

  @Override
  public void remove(final IRelationSchema schema)
  {
    schemas.remove(schema);
  }

  @Override
  public int size()
  {
    return schemas.size();
  }
}