package edu.buffalo.cse.jive.internal.practql.tokenizer;

import edu.buffalo.cse.jive.practql.tokenizer.IToken;

abstract class TokenNumber implements IToken
{
  @Override
  public String toString()
  {
    return String.format("%s(%s)", getTokenClass(), getText());
  }

  abstract Number getValue();
}