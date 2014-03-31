package edu.buffalo.cse.jive.practql.expression.nary;

public enum MultiplicationConnective
{
  AC_DIVIDE("/"),
  AC_TIMES("*");
  public static MultiplicationConnective getValue(final String value)
  {
    if (AC_DIVIDE.toString().equals(value))
    {
      return AC_DIVIDE;
    }
    else if (AC_TIMES.toString().equals(value))
    {
      return AC_TIMES;
    }
    return null;
  }

  private final String value;

  private MultiplicationConnective(final String value)
  {
    this.value = value;
  }

  @Override
  public String toString()
  {
    return value;
  }
}