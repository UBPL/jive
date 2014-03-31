package edu.buffalo.cse.jive.practql.parser;

// parser specific exception type
public class ParserException extends Exception
{
  private static final long serialVersionUID = 2545090278972053744L;

  public ParserException(final String message)
  {
    super(message);
  }

  public ParserException(final String message, final Throwable cause)
  {
    super(message, cause);
  }
}