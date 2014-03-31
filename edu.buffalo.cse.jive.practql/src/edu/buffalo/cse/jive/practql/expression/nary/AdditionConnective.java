package edu.buffalo.cse.jive.practql.expression.nary;

public enum AdditionConnective
{
  AC_CONCATENATE("||"),
  AC_MINUS("-"),
  AC_PLUS("+");
  public static AdditionConnective getValue(final String value)
  {
    if (AC_CONCATENATE.toString().equals(value))
    {
      return AC_CONCATENATE;
    }
    else if (AC_MINUS.toString().equals(value))
    {
      return AC_MINUS;
    }
    else if (AC_PLUS.toString().equals(value))
    {
      return AC_PLUS;
    }
    return null;
  }

  private final String value;

  private AdditionConnective(final String value)
  {
    this.value = value;
  }

  @Override
  public String toString()
  {
    return value;
  }
}