package edu.buffalo.cse.jive.practql.expression.atomic;

import edu.buffalo.cse.jive.practql.schema.Type;

public enum CFunctionType
{
  FT_HAS_PRED("has_pred"),
  FT_HAS_PRED_INCLUSIVE("has_predi"),
  FT_HAS_SUCC("has_succ"),
  FT_HAS_SUCC_INCLUSIVE("has_succi"),
  FT_OVERLAPPING("overlapping"),
  FT_PROJECT("project");
  private final String value;

  private CFunctionType(final String value)
  {
    this.value = value;
  }

  public Type returnType()
  {
    return this == FT_PROJECT ? Type.CINTERVAL : Type.BOOLEAN;
  }

  @Override
  public String toString()
  {
    return value;
  }
}