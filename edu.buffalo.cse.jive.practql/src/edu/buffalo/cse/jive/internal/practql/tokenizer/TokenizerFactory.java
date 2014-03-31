package edu.buffalo.cse.jive.internal.practql.tokenizer;

import edu.buffalo.cse.jive.practql.tokenizer.ITokenizer;

public enum TokenizerFactory
{
  INSTANCE;
  public ITokenizer createTokenizer()
  {
    return new Tokenizer();
  }
}
