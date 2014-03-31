package edu.buffalo.cse.jive.internal.practql.tokenizer;

import edu.buffalo.cse.jive.practql.tokenizer.IToken;
import edu.buffalo.cse.jive.practql.tokenizer.TokenClass;

enum TokenNull implements IToken
{
  TK_NULL("NULL");
  static TokenNull getToken(final String token)
  {
    for (final TokenNull tk : TokenNull.values())
    {
      if (tk.keyword.equalsIgnoreCase(token))
      {
        return tk;
      }
    }
    return null;
  }

  private final String keyword;

  private TokenNull(final String token)
  {
    this.keyword = token;
  }

  @Override
  public String getText()
  {
    return keyword;
  }

  @Override
  public TokenClass getTokenClass()
  {
    return TokenClass.TC_LITERAL_NULL;
  }

  @Override
  public String toString()
  {
    return String.format("%s(%s)", getTokenClass(), getText());
  }
}