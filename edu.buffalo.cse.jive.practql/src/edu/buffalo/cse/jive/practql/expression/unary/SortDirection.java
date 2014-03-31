package edu.buffalo.cse.jive.practql.expression.unary;

public enum SortDirection
{
  SD_ASC("ASC"),
  SD_DESC("DESC");
  private final String value;

  private SortDirection(final String value)
  {
    this.value = value;
  }

  @Override
  public String toString()
  {
    return value;
  }
}