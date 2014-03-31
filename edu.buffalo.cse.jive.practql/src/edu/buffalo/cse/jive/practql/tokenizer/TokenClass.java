package edu.buffalo.cse.jive.practql.tokenizer;

public enum TokenClass
{
  TC_AGGREGATE("AGGREGATE"),
  TC_FUNCTION("FUNCTION"),
  TC_IDENTIFIER("IDENT"),
  TC_KEYWORD("KEYWORD"),
  TC_LITERAL_BOOLEAN("BOOL"),
  TC_LITERAL_DECIMAL("DEC"),
  TC_LITERAL_INTEGER("INT"),
  TC_LITERAL_NULL("NULL"),
  TC_LITERAL_STRING("STRING"),
  TC_SYMBOL("SYMB"),
  TC_WHITE("WHITE");
  private final String tokenClass;

  private TokenClass(final String tokenClass)
  {
    this.tokenClass = tokenClass;
  }

  public String getText()
  {
    return tokenClass;
  }

  @Override
  public String toString()
  {
    return tokenClass;
  }
}