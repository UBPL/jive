package edu.buffalo.cse.jive.internal.practql.tokenizer;

import edu.buffalo.cse.jive.practql.tokenizer.TokenClass;

public class TokenInteger extends TokenNumber
{
  private final Integer value;

  TokenInteger(final Integer value)
  {
    this.value = value;
  }

  @Override
  public String getText()
  {
    return String.valueOf(value);
  }

  @Override
  public TokenClass getTokenClass()
  {
    return TokenClass.TC_LITERAL_INTEGER;
  }

  @Override
  public Integer getValue()
  {
    return value;
  }
}