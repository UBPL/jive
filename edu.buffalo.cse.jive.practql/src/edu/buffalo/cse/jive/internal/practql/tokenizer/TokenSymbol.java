package edu.buffalo.cse.jive.internal.practql.tokenizer;

import edu.buffalo.cse.jive.practql.tokenizer.IToken;
import edu.buffalo.cse.jive.practql.tokenizer.TokenClass;

public enum TokenSymbol implements IToken
{
  TK_COMMA(","),
  TK_CONCATENATE("||"),
  TK_DIVIDE("/"),
  // symbol: name qualification
  TK_DOT("."),
  // symbol: relational operators
  TK_EQ("="),
  TK_GE(">="),
  TK_GT(">"),
  TK_LE("<="),
  // symbol: list construction
  TK_LPARENS("("),
  TK_LT("<"),
  TK_MINUS("-"),
  TK_NE("<>"),
  TK_PLUS("+"),
  // symbol: string construction and concatenation
  TK_QUOTE("'"),
  TK_RPARENS(")"),
  // symbol: query termination
  TK_SEMICOLON(";"),
  // symbol: arithmetic operators
  TK_TIMES("*"),
  // symbol: virtual symbol
  TK_VIRTUAL_1("|");
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