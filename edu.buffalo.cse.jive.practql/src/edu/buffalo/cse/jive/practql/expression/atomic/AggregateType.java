package edu.buffalo.cse.jive.practql.expression.atomic;

public enum AggregateType
{
  AVG("AVG"),
  COUNT("COUNT"),
  MAX("MAX"),
  MIN("MIN"),
  SUM("SUM");
  public static AggregateType getValue(final String value)
  {
    if (AVG.toString().equalsIgnoreCase(value))
    {
      return AVG;
    }
    if (COUNT.toString().equalsIgnoreCase(value))
    {
      return COUNT;
    }
    if (MAX.toString().equalsIgnoreCase(value))
    {
      return MAX;
    }
    if (MIN.toString().equalsIgnoreCase(value))
    {
      return MIN;
    }
    if (SUM.toString().equalsIgnoreCase(value))
    {
      return SUM;
    }
    return null;
  }

  private final String value;

  private AggregateType(final String value)
  {
    this.value = value;
  }

  @Override
  public String toString()
  {
    return value;
  }
}