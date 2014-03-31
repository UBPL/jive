package edu.buffalo.cse.jive.internal.practql.tokenizer;

import edu.buffalo.cse.jive.practql.tokenizer.IToken;
import edu.buffalo.cse.jive.practql.tokenizer.TokenClass;

class TokenIdentifier implements IToken
{
  private final String token;

  TokenIdentifier(final String token)
  {
    this.token = token;
  }

  @Override
  public String getText()
  {
    return token;
  }

  @Override
  public TokenClass getTokenClass()
  {
    return TokenClass.TC_IDENTIFIER;
  }

  @Override
  public String toString()
  {
    return String.format("%s(%s)", getTokenClass(), getText());
  }
}