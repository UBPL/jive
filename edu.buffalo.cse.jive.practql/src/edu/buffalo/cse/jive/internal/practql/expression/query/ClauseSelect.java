package edu.buffalo.cse.jive.internal.practql.expression.query;

import java.util.ArrayList;
import java.util.List;

import edu.buffalo.cse.jive.internal.practql.visitor.UniformExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.IAtomicExpression;
import edu.buffalo.cse.jive.practql.expression.query.IClauseSelect;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.expression.unary.INamedExpression;
import edu.buffalo.cse.jive.practql.schema.ISchemaSignature;
import edu.buffalo.cse.jive.practql.schema.SchemaException;

class ClauseSelect implements IClauseSelect
{
  private final boolean isDistinct;
  private final List<INamedExpression> select;

  ClauseSelect(final boolean isDistinct)
  {
    this.isDistinct = isDistinct;
    this.select = new ArrayList<INamedExpression>();
  }

  @Override
  public void accept(final IQueryExpressionVisitor visitor) throws QueryException
  {
    visitor.visitClauseSelect(this, false);
  }

  @Override
  public void append(final String name, final IExpression expression) throws QueryException
  {
    // catch null name
    if (name == null)
    {
      throw new QueryException("Invalid projected expression: null name.");
    }
    // catch null expression
    if (expression == null)
    {
      throw new QueryException("Invalid projected expression: null expression.");
    }
    // projected temporal expressions must be either atomic
    if (expression.getType().isStrictTemporal() && !(expression instanceof IAtomicExpression))
    {
      throw new QueryException("Invalid projected temporal expression: expression must be atomic.");
    }
    // projected non-temporal expressions must not contain temporal subexpressions
    if (!expression.getType().isStrictTemporal())
    {
      try
      {
        expression.accept(new UniformExpressionVisitor()
          {
            @Override
            protected boolean visit(final IExpression expression, final Object arg)
                throws ExpressionException
            {
              // projected temporal expressions must be either atomic
              if (expression.getType().isStrictTemporal())
              {
                throw new ExpressionException(String.format(
                    "Expression '%s' is abstract temporal.", expression.toStringTyped()));
              }
              return true;
            }
          }, null);
      }
      catch (final ExpressionException e)
      {
        throw new QueryException(
            String.format(
                "Invalid projected expression: no temporal subexpressions allowed in a non-temporal expression '%s'.",
                expression.toStringTyped()), e);
      }
    }
    // check that a field with the same name has not been defined in the projection list
    for (final INamedExpression ne : select)
    {
      if (ne.getName().equalsIgnoreCase(name))
      {
        throw new QueryException(String.format(QueryExpression.ERR_REPEATED_FIELDNAME, name));
      }
    }
    // append to the projection list
    try
    {
      select.add(QueryExpression.ef.newNamedExpression(expression, name));
    }
    catch (final ExpressionException e)
    {
      throw new QueryException("Error creating named expression.", e);
    }
  }

  @Override
  public INamedExpression getMember(final int index)
  {
    return select.get(index);
  }

  @Override
  public ISchemaSignature getSignature() throws QueryException
  {
    if (select.size() == 0)
    {
      return null;
    }
    ISchemaSignature sig;
    try
    {
      sig = QueryExpression.sf.newSchemaSignature(QueryExpression.sf.newFieldSchema(getMember(0)
          .getName(), getMember(0).getType()));
      for (int i = 1; i < size(); i++)
      {
        sig.append(QueryExpression.sf
            .newFieldSchema(getMember(i).getName(), getMember(i).getType()));
      }
      return sig;
    }
    catch (final SchemaException e)
    {
      throw new QueryException("Error creating signature.", e);
    }
  }

  @Override
  public boolean isDistinct()
  {
    return this.isDistinct;
  }

  @Override
  public INamedExpression resolveAlias(final String fieldName) throws QueryException
  {
    for (final INamedExpression ne : select)
    {
      if (ne.getName().equalsIgnoreCase(fieldName))
      {
        try
        {
          return QueryExpression.ef.newAlias(ne);
        }
        catch (final ExpressionException e)
        {
          throw new QueryException(String.format("Error creating new alias for '%s'.", fieldName),
              e);
        }
      }
    }
    return null;
  }

  @Override
  public int size()
  {
    return select.size();
  }

  @Override
  public String toString()
  {
    final StringBuffer buffer = new StringBuffer("SELECT " + (isDistinct ? "DISTINCT " : ""));
    for (int i = 0; i < select.size(); i++)
    {
      buffer.append(select.get(i).toString());
      if (i < select.size() - 1)
      {
        buffer.append(", ");
      }
    }
    return buffer.toString();
  }
}