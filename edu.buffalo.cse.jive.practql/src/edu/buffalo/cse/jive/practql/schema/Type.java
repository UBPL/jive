package edu.buffalo.cse.jive.practql.schema;

public enum Type
{
  // ANY("any"),
  BOOLEAN("boolean"),
  CINTERVAL("cinterval"),
  CINTERVAL_ARRAY("cinterval[]"),
  DECIMAL("decimal")
  {
    @Override
    public boolean supportsAddition()
    {
      return true;
    }
  },
  INTEGER("integer")
  {
    @Override
    public boolean supportsAddition()
    {
      return true;
    }
  },
  INVALID("invalid"),
  NULL("null"),
  STRING("string")
  {
    @Override
    public boolean supportsAddition()
    {
      return true;
    }
  },
  TP("tp")
  {
    @Override
    public boolean supportsAddition()
    {
      return true;
    }
  },
  TP_ENCODED("tp_encoded")
  {
    @Override
    public boolean supportsAddition()
    {
      return true;
    }
  };
  private final String value;

  private Type(final String value)
  {
    this.value = value;
  }

  public boolean isNumeric()
  {
    return this == DECIMAL || this == INTEGER;
  }

  public boolean isStrictTemporal()
  {
    return this == TP || this == TP_ENCODED;
  }

  public boolean isTemporal()
  {
    return this == INTEGER || this == TP || this == TP_ENCODED;
  }

  // addition is supported for: numeric, string, and temporal types;
  public boolean supportsAddition()
  {
    return false;
  }

  @Override
  public String toString()
  {
    return this.value;
  }
}