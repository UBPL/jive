package org.lessa.demian.internal.tokenizer;

import org.lessa.demian.tokenizer.IToken;
import org.lessa.demian.tokenizer.TokenClass;

enum TokenWhiteSpace implements IToken
{
  TK_CR('\r'),
  TK_NL('\n'),
  // symbol: list construction
  TK_SPACE(' '),
  TK_TAB('\t');
  static final String TOKENS;
  static
  {
    String temp = "";
    for (final TokenWhiteSpace tk : TokenWhiteSpace.values())
    {
      temp += tk.symbol;
    }
    TOKENS = temp;
  }

  static TokenWhiteSpace getToken(final char token)
  {
    for (final TokenWhiteSpace tk : TokenWhiteSpace.values())
    {
      if (tk.symbol == token)
      {
        return tk;
      }
    }
    return null;
  }

  private final char symbol;

  private TokenWhiteSpace(final char symbol)
  {
    this.symbol = symbol;
  }

  @Override
  public String getText()
  {
    return String.valueOf(symbol);
  }

  @Override
  public TokenClass getTokenClass()
  {
    return TokenClass.TC_WHITE;
  }

  @Override
  public String toString()
  {
    return String.format("%s(%s)", getTokenClass(), getText());
  }
}