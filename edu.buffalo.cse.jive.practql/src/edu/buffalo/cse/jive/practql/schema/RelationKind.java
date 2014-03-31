package edu.buffalo.cse.jive.practql.schema;

public enum RelationKind
{
  RK_CTE("common table expression"),
  RK_TABLE("table"),
  RK_VIEW("view");
  private final String value;

  private RelationKind(final String value)
  {
    this.value = value;
  }

  @Override
  public String toString()
  {
    return value;
  }
}