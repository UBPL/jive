package edu.buffalo.cse.jive.internal.practql.expression.query;

import java.util.ArrayList;
import java.util.List;

import edu.buffalo.cse.jive.internal.practql.TypeUtils;
import edu.buffalo.cse.jive.practql.expression.query.IQuery;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.query.ISimpleQuery;
import edu.buffalo.cse.jive.practql.expression.query.QueryConnective;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.schema.IFieldSchema;
import edu.buffalo.cse.jive.practql.schema.ISchemaSignature;
import edu.buffalo.cse.jive.practql.schema.SchemaException;
import edu.buffalo.cse.jive.practql.schema.Type;

class Query implements IQuery
{
  private final List<QueryConnective> connectives;
  private final List<ISimpleQuery> members;
  private ISchemaSignature signature;

  Query(final ISimpleQuery headQuery) throws QueryException
  {
    this.members = new ArrayList<ISimpleQuery>();
    this.connectives = new ArrayList<QueryConnective>();
    resolveSignature(headQuery);
    this.members.add(headQuery);
  }

  @Override
  public void accept(final IQueryExpressionVisitor visitor) throws QueryException
  {
    if (visitor.visitQuery(this, false))
    {
      for (int i = 0; i < members.size(); i++)
      {
        members.get(i).accept(visitor);
      }
      // allow post-processing
      visitor.visitQuery(this, true);
    }
  }

  @Override
  public void append(final QueryConnective connective, final ISimpleQuery newMember)
      throws QueryException
  {
    resolveSignature(newMember);
    connectives.add(connective);
    members.add(newMember);
  }

  @Override
  public QueryConnective getConnective(final int index)
  {
    return connectives.get(index);
  }

  @Override
  public ISimpleQuery getMember(final int index)
  {
    return members.get(index);
  }

  @Override
  public ISchemaSignature getSignature()
  {
    return this.signature;
  }

  @Override
  public void schemaResolved() throws QueryException
  {
    // is any field unresolved in the signature?
    for (int i = 0; i < signature.size(); i++)
    {
      if (signature.getFieldSchema(i).getType() == Type.NULL)
      {
        throw new QueryException(String.format(QueryExpression.ERR_INVALID_FIELD_TYPE, signature
            .getFieldSchema(i).getName()));
      }
    }
  }

  @Override
  public int size()
  {
    return members.size();
  }

  @Override
  public String toString()
  {
    final StringBuffer buffer = new StringBuffer("");
    for (int i = 0; i < members.size(); i++)
    {
      buffer.append(members.get(i).toString());
      if (i < members.size() - 1)
      {
        buffer.append("\n");
        if (i < connectives.size())
        {
          buffer.append(connectives.get(i));
          buffer.append("\n");
        }
      }
    }
    return buffer.toString();
  }

  /**
   * TODO: reduce the cyclomatic complexity.
   */
  private void resolveSignature(final ISimpleQuery newMember) throws QueryException
  {
    final ISchemaSignature sigOld = getSignature();
    ISchemaSignature sigNew;
    sigNew = newMember.getSignature();
    // a head query defines the initial query signature
    if (size() == 0)
    {
      signature = sigNew;
      return;
    }
    // updated signature
    ISchemaSignature sigUpdated = null;
    // new member must have the same arity as the current signature
    if (sigNew.size() != sigOld.size())
    {
      throw new QueryException(String.format(QueryExpression.ERR_MEMBER_ARITY_MISMATCH,
          sigOld.size(), newMember.getSelect().size()));
    }
    // the new member may update the signature (force a type widening) or make it invalid
    for (int i = 0; i < sigNew.size(); i++)
    {
      IFieldSchema fsUpdated = null;
      final IFieldSchema fsOld = sigOld.getFieldSchema(i);
      final IFieldSchema fsNew = sigNew.getFieldSchema(i);
      if (!TypeUtils.isSchemaCompatible(fsNew.getType(), fsOld.getType()))
      {
        throw new QueryException(String.format(QueryExpression.ERR_MEMBER_TYPE_MISMATCH,
            fsNew.getType(), sigOld.getFieldSchema(i).getType(), size()));
      }
      // type from the new member, field name from the signature
      if (fsOld.getType() == Type.NULL)
      {
        try
        {
          fsUpdated = QueryExpression.sf.newFieldSchema(fsOld.getName(), fsNew.getType());
        }
        catch (final SchemaException e)
        {
          throw new QueryException("Error creating field schema.", e);
        }
      }
      // no numeric narrowing
      else if (fsNew.getType() == Type.NULL || fsNew.getType() == Type.INTEGER)
      {
        fsUpdated = fsOld;
      }
      // no temporal narrowing
      else if (fsNew.getType() == Type.TP && fsOld.getType() == Type.TP_ENCODED)
      {
        fsUpdated = fsOld;
      }
      // otherwise, widen the type but keep the field name from the signature
      else
      {
        try
        {
          fsUpdated = QueryExpression.sf.newFieldSchema(fsOld.getName(), fsNew.getType());
        }
        catch (final SchemaException e)
        {
          throw new QueryException("Error creating field schema.", e);
        }
      }
      try
      {
        if (sigUpdated == null)
        {
          sigUpdated = QueryExpression.sf.newSchemaSignature(fsUpdated);
        }
        else
        {
          sigUpdated.append(fsUpdated);
        }
      }
      catch (final SchemaException e)
      {
        throw new QueryException("Error updating the query signature.", e);
      }
    }
    signature = sigUpdated;
  }
}