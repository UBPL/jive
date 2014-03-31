package edu.buffalo.cse.jive.practql.expression;

// expression specific exception type
public class ExpressionException extends Exception
{
  private static final long serialVersionUID = -8170636389905530559L;

  public ExpressionException(final String message)
  {
    super(message);
  }

  public ExpressionException(final String message, final Throwable cause)
  {
    super(message, cause);
  }
}