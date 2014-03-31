package edu.buffalo.cse.jive.practql.tokenizer;

// tokenizer specific exception type
public class TokenizerException extends Exception
{
  private static final long serialVersionUID = 1144957966131352362L;

  public TokenizerException(final String message)
  {
    super(message);
  }

  public TokenizerException(final String message, final Throwable cause)
  {
    super(message, cause);
  }
}