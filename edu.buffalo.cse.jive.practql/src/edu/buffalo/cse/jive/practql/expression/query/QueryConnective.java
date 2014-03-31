package edu.buffalo.cse.jive.practql.expression.query;

public enum QueryConnective
{
  QC_BAG_DIFFERENCE("EXCEPT ALL"),
  QC_BAG_INTERSECTION("INTERSECT ALL"),
  QC_BAG_UNION("UNION ALL"),
  QC_SET_DIFFERENCE("EXCEPT"),
  QC_SET_INTERSECTION("INTERSECT"),
  QC_SET_UNION("UNION");
  private final String value;

  private QueryConnective(final String value)
  {
    this.value = value;
  }

  public boolean isSet()
  {
    return this == QC_SET_DIFFERENCE || this == QC_SET_INTERSECTION || this == QC_SET_UNION;
  }

  @Override
  public String toString()
  {
    return value;
  }
}