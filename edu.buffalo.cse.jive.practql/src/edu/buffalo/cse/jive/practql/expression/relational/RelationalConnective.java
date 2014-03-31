package edu.buffalo.cse.jive.practql.expression.relational;

public enum RelationalConnective
{
  RC_EQ("="),
  RC_GE(">="),
  RC_GT(">"),
  RC_IS("IS"),
  RC_IS_NOT("IS NOT"),
  RC_LE("<="),
  RC_LIKE("LIKE"),
  RC_LT("<"),
  RC_NE("<>"),
  RC_NOT_LIKE("NOT LIKE");
  public static RelationalConnective getValue(final String value)
  {
    if (RC_EQ.toString().equals(value))
    {
      return RC_EQ;
    }
    else if (RC_GE.toString().equals(value))
    {
      return RC_GE;
    }
    else if (RC_GT.toString().equals(value))
    {
      return RC_GT;
    }
    else if (RC_IS_NOT.toString().equalsIgnoreCase(value))
    {
      return RC_IS_NOT;
    }
    else if (RC_IS.toString().equalsIgnoreCase(value))
    {
      return RC_IS;
    }
    else if (RC_LE.toString().equals(value))
    {
      return RC_LE;
    }
    else if (RC_LIKE.toString().equalsIgnoreCase(value))
    {
      return RC_LIKE;
    }
    else if (RC_LT.toString().equals(value))
    {
      return RC_LT;
    }
    else if (RC_NE.toString().equals(value))
    {
      return RC_NE;
    }
    else if (RC_NOT_LIKE.toString().equalsIgnoreCase(value))
    {
      return RC_NOT_LIKE;
    }
    return null;
  }

  private final String value;

  private RelationalConnective(final String value)
  {
    this.value = value;
  }

  @Override
  public String toString()
  {
    return value;
  }
}