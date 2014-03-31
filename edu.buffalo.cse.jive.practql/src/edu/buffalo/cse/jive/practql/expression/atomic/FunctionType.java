package edu.buffalo.cse.jive.practql.expression.atomic;

public enum FunctionType
{
  FT_ABS("ABS"),
  FT_CEIL("CEIL"),
  FT_FLOOR("FLOOR"),
  FT_GREATEST("GREATEST"),
  FT_LEAST("LEAST");
  public static FunctionType getValue(final String value)
  {
    if (FT_ABS.toString().equalsIgnoreCase(value))
    {
      return FT_ABS;
    }
    if (FT_FLOOR.toString().equalsIgnoreCase(value))
    {
      return FT_FLOOR;
    }
    if (FT_CEIL.toString().equalsIgnoreCase(value))
    {
      return FT_CEIL;
    }
    if (FT_GREATEST.toString().equalsIgnoreCase(value))
    {
      return FT_GREATEST;
    }
    if (FT_LEAST.toString().equalsIgnoreCase(value))
    {
      return FT_LEAST;
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