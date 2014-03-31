package org.lessa.demian.internal.tokenizer;

import org.lessa.demian.tokenizer.IToken;
import org.lessa.demian.tokenizer.TokenClass;

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