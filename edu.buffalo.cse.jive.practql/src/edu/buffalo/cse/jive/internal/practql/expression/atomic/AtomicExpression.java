package edu.buffalo.cse.jive.internal.practql.expression.atomic;

import edu.buffalo.cse.jive.practql.expression.atomic.IAtomicExpression;

public abstract class AtomicExpression implements IAtomicExpression
{
  /**
   * Expressions are compared based on string equality.
   */
  @Override
  public boolean equals(final Object other)
  {
    if (other == this)
    {
      return true;
    }
    if (other == null)
    {
      return false;
    }
    if (other.getClass().isAssignableFrom(getClass())
        || getClass().isAssignableFrom(other.getClass()))
    {
      return toString().equalsIgnoreCase(other.toString());
    }
    return false;
  }

  /**
   * Hashcode is based on string representation to satisfy equals/hashCode contract.
   */
  @Override
  public int hashCode()
  {
    return toString().toUpperCase().hashCode();
  }
}