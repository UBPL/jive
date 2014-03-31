package edu.buffalo.cse.jive.practql.expression.query;

// query specific exception type
public class QueryException extends Exception
{
  private static final long serialVersionUID = -1835239178057553163L;

  public QueryException(final String message)
  {
    super(message);
  }

  public QueryException(final String message, final Throwable cause)
  {
    super(message, cause);
  }
}