package org.lessa.demian.internal.tokenizer;

import org.lessa.demian.tokenizer.IToken;

abstract class TokenNumber implements IToken
{
  @Override
  public String toString()
  {
    return String.format("%s(%s)", getTokenClass(), getText());
  }

  abstract Number getValue();
}