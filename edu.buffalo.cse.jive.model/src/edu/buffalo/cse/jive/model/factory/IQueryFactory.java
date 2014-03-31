package edu.buffalo.cse.jive.model.factory;

import edu.buffalo.cse.jive.model.IQueryModel;

public interface IQueryFactory extends IQueryModel
{
  public EventQuery createExceptionCaughtQuery(final ExceptionQueryParams params);

  public EventQuery createExceptionThrownQuery(final ExceptionQueryParams params);

  public EventQuery createInvariantViolatedQuery(final InvariantViolatedQueryParams params);

  public EventQuery createLineExecutedQuery(final LineExecutedQueryParams params);

  public EventQuery createMethodCalledQuery(final MethodQueryParams params);

  public EventQuery createMethodReturnedQuery(final MethodReturnedQueryParams params);

  public EventQuery createObjectCreatedQuery(final ObjectCreatedQueryParams params);

  public EventQuery createSlicingQuery(final SlicingQueryParams params);

  public EventQuery createVariableChangedQuery(final VariableChangedQueryParams params);
}