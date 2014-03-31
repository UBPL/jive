package org.lessa.demian.internal.tokenizer;

import org.lessa.demian.tokenizer.ITokenizer;

public enum TokenizerFactory
{
  INSTANCE;
  public ITokenizer createTokenizer()
  {
    return new Tokenizer();
  }
}
