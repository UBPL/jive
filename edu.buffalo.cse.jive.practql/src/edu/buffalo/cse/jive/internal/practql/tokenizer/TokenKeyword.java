package edu.buffalo.cse.jive.internal.practql.tokenizer;

import edu.buffalo.cse.jive.practql.tokenizer.IToken;
import edu.buffalo.cse.jive.practql.tokenizer.TokenClass;

public enum TokenKeyword implements IToken
{
  TK_ABS("ABS", TokenClass.TC_FUNCTION),
  TK_ALL("ALL", TokenClass.TC_KEYWORD),
  TK_AND("AND", TokenClass.TC_KEYWORD),
  TK_AS("AS", TokenClass.TC_KEYWORD),
  TK_ASC("ASC", TokenClass.TC_KEYWORD),
  TK_AVG("AVG", TokenClass.TC_AGGREGATE),
  TK_BY("BY", TokenClass.TC_KEYWORD),
  TK_CEIL("CEIL", TokenClass.TC_FUNCTION),
  TK_COUNT("COUNT", TokenClass.TC_AGGREGATE),
  TK_DESC("DESC", TokenClass.TC_KEYWORD),
  TK_DISTINCT("DISTINCT", TokenClass.TC_KEYWORD),
  TK_EXCEPT("EXCEPT", TokenClass.TC_KEYWORD),
  TK_FLOOR("FLOOR", TokenClass.TC_FUNCTION),
  TK_FROM("FROM", TokenClass.TC_KEYWORD),
  TK_GREATEST("GREATEST", TokenClass.TC_FUNCTION),
  TK_GROUP("GROUP", TokenClass.TC_KEYWORD),
  TK_HAVING("HAVING", TokenClass.TC_KEYWORD),
  TK_INTERSECT("INTERSECT", TokenClass.TC_KEYWORD),
  TK_IS("IS", TokenClass.TC_KEYWORD),
  TK_LEAST("LEAST", TokenClass.TC_FUNCTION),
  TK_LIKE("LIKE", TokenClass.TC_KEYWORD),
  TK_MAX("MAX", TokenClass.TC_AGGREGATE),
  TK_MIN("MIN", TokenClass.TC_AGGREGATE),
  TK_NOT("NOT", TokenClass.TC_KEYWORD),
  TK_OR("OR", TokenClass.TC_KEYWORD),
  TK_ORDER("ORDER", TokenClass.TC_KEYWORD),
  TK_RECURSIVE("RECURSIVE", TokenClass.TC_KEYWORD),
  TK_SELECT("SELECT", TokenClass.TC_KEYWORD),
  // TK_STDEV("STDEV", TokenClass.TC_AGGREGATE),
  TK_SUM("SUM", TokenClass.TC_AGGREGATE),
  TK_UNION("UNION", TokenClass.TC_KEYWORD),
  TK_WHERE("WHERE", TokenClass.TC_KEYWORD),
  TK_WITH("WITH", TokenClass.TC_KEYWORD);
  static TokenKeyword getToken(final String token)
  {
    for (final TokenKeyword tk : TokenKeyword.values())
    {
      if (tk.keyword.equalsIgnoreCase(token))
      {
        return tk;
      }
    }
    return null;
  }

  private final String keyword;
  private final TokenClass tokenClass;

  private TokenKeyword(final String token, final TokenClass tokenClass)
  {
    this.keyword = token;
    this.tokenClass = tokenClass;
  }

  @Override
  public String getText()
  {
    return keyword;
  }

  @Override
  public TokenClass getTokenClass()
  {
    return tokenClass;
  }

  @Override
  public String toString()
  {
    return String.format("%s(%s)", getTokenClass(), getText());
  }
}