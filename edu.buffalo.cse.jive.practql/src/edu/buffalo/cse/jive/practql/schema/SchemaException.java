package edu.buffalo.cse.jive.practql.schema;

// schema specific exception type
public class SchemaException extends Exception
{
  private static final long serialVersionUID = -9161334117325074259L;

  public SchemaException(final String message)
  {
    super(message);
  }

  public SchemaException(final String message, final Throwable cause)
  {
    super(message, cause);
  }
}