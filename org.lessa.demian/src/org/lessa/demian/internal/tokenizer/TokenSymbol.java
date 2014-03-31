package org.lessa.demian.internal.tokenizer;

import org.lessa.demian.tokenizer.IToken;
import org.lessa.demian.tokenizer.TokenClass;

public enum TokenSymbol implements IToken
{
  // symbol: list separator
  TK_COMMA(","),
  // symbol: list construction start
  TK_LPARENS("("),
  // symbol: negative
  TK_MINUS("-"),
  // symbol: list construction end
  TK_RPARENS(")");
  static final String TOKENS;
  static
  {
    String temp = "";
    for (final TokenSymbol tk : TokenSymbol.values())
    {
      // all other symbols of length 2 are prefixed by a length 1 symbol
      if (tk.symbol.length() == 1)
      {
        temp += tk.symbol;
      }
    }
    TOKENS = temp;
  }

  static TokenSymbol getToken(final String token)
  {
    for (final TokenSymbol tk : TokenSymbol.values())
    {
      if (tk.symbol.equals(token))
      {
        return tk;
      }
    }
    return null;
  }

  private final String symbol;

  private TokenSymbol(final String symbol)
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
    return TokenClass.TC_SYMBOL;
  }

  @Override
  public String toString()
  {
    return String.format("%s(%s)", getTokenClass(), getText());
  }
}