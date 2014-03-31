package edu.buffalo.cse.jive.practql.expression.atomic;

public enum CAggregateType
{
  CCOUNT("AGG_COUNT"),
  CMAX("AGG_MAX"),
  CMIN("AGG_MIN"),
  CPARTITION("AGG_PARTITION");
  public static CAggregateType getValue(final AggregateType value)
  {
    if (AggregateType.COUNT == value)
    {
      return CCOUNT;
    }
    if (AggregateType.MAX == value)
    {
      return CMAX;
    }
    if (AggregateType.MIN == value)
    {
      return CMIN;
    }
    return null;
  }

  private final String value;

  private CAggregateType(final String value)
  {
    this.value = value;
  }

  @Override
  public String toString()
  {
    return value;
  }
}