package edu.buffalo.cse.jive.internal.practql.compiler;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.buffalo.cse.jive.internal.practql.Factory;
import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.atomic.IFieldExpression;
import edu.buffalo.cse.jive.practql.expression.query.IClauseSelect;
import edu.buffalo.cse.jive.practql.expression.query.IClauseWhere;
import edu.buffalo.cse.jive.practql.expression.query.ISimpleQuery;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.expression.unary.INamedExpression;
import edu.buffalo.cse.jive.practql.schema.IFieldReference;
import edu.buffalo.cse.jive.practql.schema.Type;

public class SimpleQueryCompiler
{
  private static void println(final String value)
  {
    System.err.println(value);
  }

  private final Factory factory;

  SimpleQueryCompiler()
  {
    this.factory = Factory.INSTANCE;
  }

  private void checkAttributeIndependence(final IClauseSelect select,
      final Map<IFieldReference, Set<IFieldReference>> referenced) throws QueryException
  {
    // all TP_ENCODED fields in the original select list
    final Set<IFieldReference> selected = new HashSet<IFieldReference>();
    for (int i = 0; i < select.size(); i++)
    {
      final INamedExpression ne = select.getMember(i);
      if (ne.getExpression() instanceof IFieldExpression
          && ((IFieldExpression) ne.getExpression()).getType() == Type.TP_ENCODED)
      {
        selected.add(((IFieldExpression) ne.getExpression()).getFieldReference());
      }
    }
    // for each TP_ENCODED field in the original select list
    for (final IFieldReference key : selected)
    {
      // find the set of fields that are dependent
      final Set<IFieldReference> all = referenced.get(key);
      all.retainAll(selected);
      final Set<IFieldReference> sfr = new HashSet<IFieldReference>();
      for (final IFieldReference fr : all)
      {
        if (fr.getSchema().getType() == Type.TP_ENCODED)
        {
          sfr.add(fr);
        }
      }
      if (sfr.size() > 0)
      {
        final StringBuffer buffer = new StringBuffer(String.format(
            "Attribute '%s' depends on attribute(s) ", key.toString()));
        for (final Iterator<IFieldReference> ifr = sfr.iterator(); ifr.hasNext();)
        {
          final IFieldReference fr = ifr.next();
          buffer.append("'" + fr.toString() + "'");
          if (ifr.hasNext())
          {
            buffer.append(", ");
          }
        }
        final String message = String.format(
            "Unsafe query: attribute dependence detected on the projection list. %s.",
            buffer.toString());
        if (CompilerUtils.ENFORCE_ATTRIBUTE_INDEPENDENCE)
        {
          throw new QueryException(message);
        }
        else
        {
          SimpleQueryCompiler.println(message);
        }
      }
    }
  }

  ISimpleQuery compile(final ISimpleQuery query) throws QueryException
  {
    final SimpleQueryVisitor translator = new SimpleQueryVisitor(query.getFrom());
    try
    {
      final IClauseWhere newWhere = query.getWhere() == null ? null : translator
          .translateWhere(query.getWhere());
      final IClauseSelect newSelect = translator.translateSelect(query.getSelect());
      checkAttributeIndependence(query.getSelect(), translator.getReference());
      final ISimpleQuery result = factory.queryExpressionFactory().newSimpleQuery(newSelect,
          query.getFrom(), newWhere, query.getGroupBy(), query.getHaving(), query.getOrderBy());
      if (CompilerUtils.DEBUG_MODE)
      {
        SimpleQueryCompiler.println("source (point-based):");
        SimpleQueryCompiler.println(query.toString());
        SimpleQueryCompiler.println("compiled (interval-based):");
        SimpleQueryCompiler.println(result.toString());
      }
      return result;
    }
    catch (final ExpressionException e)
    {
      throw new QueryException(String.format("Error translating query '%s'.", query.toString()), e);
    }
  }
}
