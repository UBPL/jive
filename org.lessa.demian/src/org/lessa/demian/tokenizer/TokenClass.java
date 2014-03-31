package org.lessa.demian.tokenizer;

public enum TokenClass
{
  TC_IDENTIFIER("IDENT"),
  TC_LITERAL_INTEGER("INT"),
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