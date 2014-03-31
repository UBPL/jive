package edu.buffalo.cse.jive.internal.practql.expression.query;

import edu.buffalo.cse.jive.practql.expression.query.INamedQuery;
import edu.buffalo.cse.jive.practql.expression.query.IQuery;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.query.ISimpleQuery;
import edu.buffalo.cse.jive.practql.expression.query.QueryConnective;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.schema.IRelationReference;
import edu.buffalo.cse.jive.practql.schema.IRelationSchema;
import edu.buffalo.cse.jive.practql.schema.ISchemaSignature;

class NamedQuery implements INamedQuery
{
  private final IQuery query;
  private final IRelationSchema schema;

  NamedQuery(final IRelationSchema schema, final IQuery query)
  {
    this.query = query;
    this.schema = schema;
  }

  @Override
  public void accept(final IQueryExpressionVisitor visitor) throws QueryException
  {
    if (visitor.visitCTE(this, false))
    {
      query.accept(visitor);
      // allows post-processing
      visitor.visitCTE(this, true);
    }
  }

  @Override
  public void append(final QueryConnective connective, final ISimpleQuery newMember)
      throws QueryException
  {
    this.query.append(connective, newMember);
  }

  @Override
  public QueryConnective getConnective(final int index)
  {
    return this.query.getConnective(index);
  }

  @Override
  public ISimpleQuery getMember(final int index)
  {
    return this.query.getMember(index);
  }

  @Override
  public IRelationSchema getSchema()
  {
    return this.schema;
  }

  @Override
  public ISchemaSignature getSignature()
  {
    return query.getSignature();
  }

  @Override
  public boolean isRecursive()
  {
    // traverse all simple queries
    for (int i = 0; i < query.size(); i++)
    {
      final ISimpleQuery sq = query.getMember(i);
      // traverse all FROM clause relation references
      for (int j = 0; j < sq.getFrom().size(); j++)
      {
        final IRelationReference rel = sq.getFrom().getMember(j);
        // a relation reference to the schema of this named query implies recursion
        if (rel.getSchema().equals(schema))
        {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void schemaResolved() throws QueryException
  {
    this.query.schemaResolved();
  }

  @Override
  public int size()
  {
    return this.query.size();
  }

  @Override
  public String toString()
  {
    final StringBuffer buffer = new StringBuffer(schema.getName());
    buffer.append("(");
    final ISchemaSignature sig = schema.getSignature();
    for (int i = 0; i < sig.size(); i++)
    {
      buffer.append(sig.getFieldSchema(i).getName());
      if (i < sig.size() - 1)
      {
        buffer.append(", ");
      }
    }
    buffer.append(") AS ( \n");
    buffer.append(query.toString());
    buffer.append("\n)");
    return buffer.toString();
  }
}