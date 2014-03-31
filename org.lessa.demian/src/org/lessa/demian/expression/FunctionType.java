package org.lessa.demian.expression;

public enum FunctionType
{
  FT_ADD("add"),
  FT_DIV("div"),
  FT_LET("let"),
  FT_MULT("mult"),
  FT_SUB("sub");
  public static FunctionType getValue(final String value)
  {
    if (FT_ADD.toString().equalsIgnoreCase(value))
    {
      return FT_ADD;
    }
    if (FT_DIV.toString().equalsIgnoreCase(value))
    {
      return FT_DIV;
    }
    if (FT_LET.toString().equalsIgnoreCase(value))
    {
      return FT_LET;
    }
    if (FT_MULT.toString().equalsIgnoreCase(value))
    {
      return FT_MULT;
    }
    if (FT_SUB.toString().equalsIgnoreCase(value))
    {
      return FT_SUB;
    }
    return null;
  }

  private final String value;

  private FunctionType(final String value)
  {
    this.value = value;
  }

  @Override
  public String toString()
  {
    return value;
  }
}