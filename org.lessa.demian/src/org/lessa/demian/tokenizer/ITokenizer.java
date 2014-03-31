package org.lessa.demian.tokenizer;

import java.util.List;

public interface ITokenizer
{
  public List<IToken> tokenize(String source) throws TokenizerException;
}