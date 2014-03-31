package edu.buffalo.cse.jive.practql.expression.query;

import java.util.List;

import edu.buffalo.cse.jive.practql.expression.atomic.IFieldExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.IWildcardExpression;
import edu.buffalo.cse.jive.practql.schema.IFieldReference;
import edu.buffalo.cse.jive.practql.schema.IRelationReference;
import edu.buffalo.cse.jive.practql.schema.IRelationSchema;

public interface IClauseFrom extends IQueryClause
{
  public void append(String name, IRelationSchema schema) throws QueryException;

  public List<IFieldReference> expandWildcard(IWildcardExpression wildcard) throws QueryException;

  public IRelationReference getMember(int index);

  public IFieldExpression resolveField(String fieldName) throws QueryException;

  public IFieldExpression resolveField(String fieldName, String relVar) throws QueryException;

  public int size();
}