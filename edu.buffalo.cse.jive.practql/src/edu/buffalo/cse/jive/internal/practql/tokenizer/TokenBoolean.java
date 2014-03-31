package edu.buffalo.cse.jive.internal.practql.tokenizer;

import edu.buffalo.cse.jive.practql.tokenizer.IToken;
import edu.buffalo.cse.jive.practql.tokenizer.TokenClass;

public enum TokenBoolean implements IToken
{
  TK_FALSE("FALSE")
  {
    @Override
    public boolean getValue()
    {
      return false;
    }
  },
  TK_TRUE("TRUE")
  {
    @Override
    public boolean getValue()
    {
      return true;
    }
  };
  static TokenBoolean getToken(final String token)
  {
    for (final TokenBoolean tk : TokenBoolean.values())
    {
      if (tk.keyword.equalsIgnoreCase(token))
      {
        return tk;
      }
    }
    return null;
  }

  private final String keyword;

  private TokenBoolean(final String token)
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
    return TokenClass.TC_LITERAL_BOOLEAN;
  }

  public abstract boolean getValue();

  @Override
  public String toString()
  {
    return String.format("%s(%s)", getTokenClass(), getText());
  }
}