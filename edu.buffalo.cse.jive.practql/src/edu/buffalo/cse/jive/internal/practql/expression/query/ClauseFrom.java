package edu.buffalo.cse.jive.internal.practql.expression.query;

import java.util.ArrayList;
import java.util.List;

import edu.buffalo.cse.jive.internal.practql.expression.atomic.AtomicExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.atomic.IAtomicExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.atomic.IFieldExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.IWildcardExpression;
import edu.buffalo.cse.jive.practql.expression.query.IClauseFrom;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.schema.IFieldReference;
import edu.buffalo.cse.jive.practql.schema.IRelationReference;
import edu.buffalo.cse.jive.practql.schema.IRelationSchema;
import edu.buffalo.cse.jive.practql.schema.SchemaException;

class ClauseFrom implements IClauseFrom
{
  private final IAtomicExpressionFactory factory;
  private final List<IRelationReference> from;

  ClauseFrom()
  {
    this.factory = AtomicExpressionFactory.INSTANCE;
    this.from = new ArrayList<IRelationReference>();
  }

  @Override
  public void accept(final IQueryExpressionVisitor visitor) throws QueryException
  {
    visitor.visitClauseFrom(this, false);
  }

  @Override
  public void append(final String name, final IRelationSchema schema) throws QueryException
  {
    for (final IRelationReference rr : from)
    {
      if (rr.getVariable().equalsIgnoreCase(name))
      {
        throw new QueryException(String.format(QueryExpression.ERR_REPEATED_TABLENAME, name));
      }
    }
    try
    {
      from.add(QueryExpression.sf.newRelationReference(name, schema));
    }
    catch (final SchemaException e)
    {
      throw new QueryException("Error creating relation reference.", e);
    }
  }

  @Override
  public List<IFieldReference> expandWildcard(final IWildcardExpression wildcard)
      throws QueryException
  {
    final String relVar = wildcard.getRelationVariable();
    final List<IFieldReference> expanded = new ArrayList<IFieldReference>();
    // expand all fields of all relations of the FROM list
    if (relVar.length() == 0)
    {
      for (final IRelationReference rr : from)
      {
        for (int i = 0; i < rr.size(); i++)
        {
          expanded.add(rr.getFieldReference(i));
        }
      }
      return expanded;
    }
    // expand all fields of all relations of the FROM list
    for (final IRelationReference rr : from)
    {
      boolean found = false;
      // the qualifier must be among the relations in the FROM list
      if (rr.getVariable().equalsIgnoreCase(relVar))
      {
        found = true;
        for (int i = 0; i < rr.size(); i++)
        {
          expanded.add(rr.getFieldReference(i));
        }
        break;
      }
      // the qualifier was not found
      if (!found)
      {
        throw new QueryException(String.format(QueryExpression.ERR_UNDEFINED_TUPLEVAR, relVar));
      }
    }
    return expanded;
  }

  @Override
  public IRelationReference getMember(final int index)
  {
    return from.get(index);
  }

  @Override
  public IFieldExpression resolveField(final String fieldName) throws QueryException
  {
    IFieldExpression fe = null;
    for (final IRelationReference rr : from)
    {
      final IFieldReference fr = rr.lookupFieldReference(fieldName);
      // found the field in the current relation reference
      if (fr != null)
      {
        // field found
        if (fe == null)
        {
          try
          {
            fe = factory.newFieldExpression(fr);
          }
          catch (final ExpressionException e)
          {
            throw new QueryException("Error creating new field expression", e);
          }
        }
        // field has already been found, so it is ambiguous
        else
        {
          throw new QueryException(String.format(QueryExpression.ERR_FROM_FIELD_AMBIGUOUS,
              fieldName));
        }
      }
    }
    // field was not found
    if (fe == null)
    {
      throw new QueryException(String.format(QueryExpression.ERR_FROM_FIELD_NOT_FOUND, fieldName));
    }
    return fe;
  }

  @Override
  public IFieldExpression resolveField(final String fieldName, final String relVar)
      throws QueryException
  {
    IFieldExpression fe = null;
    for (final IRelationReference rr : from)
    {
      if (rr.getVariable().equalsIgnoreCase(relVar))
      {
        final IFieldReference fr = rr.lookupFieldReference(fieldName);
        // found the field in the current relation reference
        if (fr != null)
        {
          try
          {
            fe = factory.newFieldExpression(fr);
          }
          catch (final ExpressionException e)
          {
            throw new QueryException("Error creating new field expression", e);
          }
        }
        break;
      }
    }
    // field was not found
    if (fe == null)
    {
      throw new QueryException(String.format(QueryExpression.ERR_FROM_FIELD_NOT_FOUND, fieldName));
    }
    return fe;
  }

  @Override
  public int size()
  {
    return from.size();
  }

  @Override
  public String toString()
  {
    final StringBuffer buffer = new StringBuffer("FROM ");
    for (int i = 0; i < from.size(); i++)
    {
      buffer.append(from.get(i).toString());
      if (i < from.size() - 1)
      {
        buffer.append(", ");
      }
    }
    return buffer.toString();
  }
}