package edu.buffalo.cse.jive.model;

import java.util.HashMap;
import java.util.Map;

import edu.buffalo.cse.jive.model.IModel.IValue;

/**
 * A representation of a relational operator for use by an {@code IJiveSearchQuery}. The operator
 * can be used to compare a {@link IValue} object with either another {@code Value} or with a
 * {@code String}.
 * <p>
 * {@code Value#Encoded} objects may be compared using any of the available operators. All other
 * {@code Value} objects can only be compared with {@link #NONE}, {@link #EQUAL}, or
 * {@link #NOT_EQUAL}.
 * 
 * @see #evaluate(IValue, IValue)
 * @see #evaluate(IValue, String)
 */
public enum RelationalOperator
{
  /**
   * A relational operator used to determine if two values are equal.
   */
  EQUAL("==")
  {
    @Override
    public boolean evaluate(final IValue left, final IValue right)
    {
      if (left.isResolved() && right.isResolved())
      {
        return compare(left.value(), right.value());
      }
      else
      {
        return compare(left.value(), right.value());
      }
    }

    @Override
    public boolean evaluate(final IValue left, final String right)
    {
      if (left.isNull())
      {
        return right.equalsIgnoreCase("NULL");
      }
      if (left.isResolved())
      {
        return compare(left.value(), right);
      }
      else
      {
        return compare(left.value(), right);
      }
    }
  },
  /**
   * A relational operator used to determine if one value is greater than another value.
   */
  GREATER_THAN(">")
  {
    @Override
    public boolean evaluate(final IValue left, final IValue right)
    {
      if (left.isPrimitive() && right.isPrimitive())
      {
        return compare(left.value(), right.value());
      }
      else if (left.isResolved() && right.isResolved())
      {
        return compare(left.value(), right.value());
      }
      else
      {
        return false;
      }
    }

    @Override
    public boolean evaluate(final IValue left, final String right)
    {
      if (left.isPrimitive())
      {
        return compare(left.value(), right);
      }
      else if (left.isResolved())
      {
        return compare(left.value(), right);
      }
      else
      {
        return false;
      }
    }
  },
  /**
   * A relational operator used to determine if one value is greater than or equal to another value.
   */
  GREATER_THAN_OR_EQUAL(">=")
  {
    @Override
    public boolean evaluate(final IValue left, final IValue right)
    {
      return GREATER_THAN.evaluate(left, right) || EQUAL.evaluate(left, right);
    }

    @Override
    public boolean evaluate(final IValue left, final String right)
    {
      return GREATER_THAN.evaluate(left, right) || EQUAL.evaluate(left, right);
    }
  },
  /**
   * A relational operator used to determine if one value is less than another value.
   */
  LESS_THAN("<")
  {
    @Override
    public boolean evaluate(final IValue left, final IValue right)
    {
      if (left.isPrimitive() && right.isPrimitive())
      {
        return compare(left.value(), right.value());
      }
      else if (left.isResolved() && right.isResolved())
      {
        return compare(left.value(), right.value());
      }
      else
      {
        return false;
      }
    }

    @Override
    public boolean evaluate(final IValue left, final String right)
    {
      if (left.isPrimitive() || left.isResolved())
      {
        return compare(left.value(), right);
      }
      else
      {
        return false;
      }
    }
  },
  /**
   * A relational operator used to determine if one value is less than or equal to another value.
   */
  LESS_THAN_OR_EQUAL("<=")
  {
    @Override
    public boolean evaluate(final IValue left, final IValue right)
    {
      return LESS_THAN.evaluate(left, right) || EQUAL.evaluate(left, right);
    }

    @Override
    public boolean evaluate(final IValue left, final String right)
    {
      return LESS_THAN.evaluate(left, right) || EQUAL.evaluate(left, right);
    }
  },
  /**
   * A relational operator which always evaluates to <code>true</code>.
   */
  NONE("")
  {
    @Override
    public boolean evaluate(final IValue left, final IValue right)
    {
      return true;
    }

    @Override
    public boolean evaluate(final IValue left, final String right)
    {
      return true;
    }
  },
  /**
   * A relational operator used to determine if two values are not equal.
   */
  NOT_EQUAL("!=")
  {
    @Override
    public boolean evaluate(final IValue left, final IValue right)
    {
      return !EQUAL.evaluate(left, right);
    }

    @Override
    public boolean evaluate(final IValue left, final String right)
    {
      return !EQUAL.evaluate(left, right);
    }
  };
  /**
   * The single quote used to demarcate a string literal.
   */
  private static final String DOUBLE_QUOTE = "\"";
  /**
   * The single quote used to demarcate a character literal.
   */
  private static final String SINGLE_QUOTE = "'";
  /**
   * A mapping between operator representations and the actual operator.
   */
  private static Map<String, RelationalOperator> stringToEnumMap = new HashMap<String, RelationalOperator>();
  static
  {
    RelationalOperator.stringToEnumMap.put(NONE.toString(), NONE);
    RelationalOperator.stringToEnumMap.put(EQUAL.toString(), EQUAL);
    RelationalOperator.stringToEnumMap.put(NOT_EQUAL.toString(), NOT_EQUAL);
    RelationalOperator.stringToEnumMap.put(LESS_THAN.toString(), LESS_THAN);
    RelationalOperator.stringToEnumMap.put(LESS_THAN_OR_EQUAL.toString(), LESS_THAN_OR_EQUAL);
    RelationalOperator.stringToEnumMap.put(GREATER_THAN.toString(), GREATER_THAN);
    RelationalOperator.stringToEnumMap.put(GREATER_THAN_OR_EQUAL.toString(), GREATER_THAN_OR_EQUAL);
  }

  /**
   * Returns the {@link RelationalOperator} represented by the supplied string.
   * 
   * @param operator
   *          the string representation
   * @return the operator represented by the string
   */
  public static RelationalOperator fromString(final String operator)
  {
    if (RelationalOperator.stringToEnumMap.containsKey(operator))
    {
      return RelationalOperator.stringToEnumMap.get(operator);
    }
    throw new IllegalArgumentException();
  }

  /**
   * The string representation of the operator.
   */
  private String operator;

  /**
   * Constructs a new operator with the supplied representation.
   * 
   * @param operator
   *          the operator representation
   */
  private RelationalOperator(final String operator)
  {
    this.operator = operator;
  }

  /**
   * Evaluates the operator on the supplied values.
   * 
   * @param left
   *          the left operand
   * @param right
   *          the right operand
   * @return the evaluation result
   */
  public abstract boolean evaluate(IValue left, IValue right);

  /**
   * Evaluates the operator on the supplied values.
   * 
   * @param left
   *          the left operand
   * @param right
   *          the right operand
   * @return the evaluation result
   */
  public abstract boolean evaluate(IValue left, String right);

  @Override
  public String toString()
  {
    return operator;
  }

  private boolean charCompare(final String left, final String right)
  {
    return isChar(left) == isChar(right) && baseCompare(unquoteChar(left), unquoteChar(right));
  }

  private boolean isBoolean(final String value)
  {
    return value != null
        && (value.equalsIgnoreCase(Boolean.TRUE.toString()) || value.equalsIgnoreCase(Boolean.FALSE
            .toString()));
  }

  private boolean isChar(final String value)
  {
    return value != null && value.length() == 3
        && value.startsWith(RelationalOperator.SINGLE_QUOTE)
        && value.endsWith(RelationalOperator.SINGLE_QUOTE);
  }

  private boolean isNumeric(final String value)
  {
    try
    {
      Integer.parseInt(value);
      return true;
    }
    catch (final NumberFormatException e)
    {
    }
    try
    {
      Long.parseLong(value);
      return true;
    }
    catch (final NumberFormatException e)
    {
    }
    try
    {
      Float.parseFloat(value);
      return true;
    }
    catch (final NumberFormatException e)
    {
    }
    try
    {
      Double.parseDouble(value);
      return true;
    }
    catch (final NumberFormatException e)
    {
    }
    return false;
  }

  private boolean isString(final String value)
  {
    return value != null && value.length() >= 2
        && value.startsWith(RelationalOperator.DOUBLE_QUOTE)
        && value.endsWith(RelationalOperator.DOUBLE_QUOTE);
  }

  private boolean numericCompare(final String left, final String right)
  {
    if (isNumeric(left) != isNumeric(right))
    {
      return false;
    }
    try
    {
      return baseCompare(Integer.parseInt(left), Integer.parseInt(right));
    }
    catch (final NumberFormatException e)
    {
    }
    try
    {
      return baseCompare(Long.parseLong(left), Long.parseLong(right));
    }
    catch (final NumberFormatException e)
    {
    }
    try
    {
      return baseCompare(Float.parseFloat(left), Float.parseFloat(right));
    }
    catch (final NumberFormatException e)
    {
    }
    try
    {
      return baseCompare(Double.parseDouble(left), Double.parseDouble(right));
    }
    catch (final NumberFormatException e)
    {
    }
    return false;
  }

  private boolean stringCompare(final String left, final String right)
  {
    return isString(left) == isString(right)
        && baseCompare(unquoteString(left), unquoteString(right));
  }

  private Character unquoteChar(final String value)
  {
    return value.charAt(1);
  }

  private String unquoteString(final String value)
  {
    return value.substring(1, value.length() - 1);
  }

  /**
   * Compares the two operands using the {@link Comparable} interface.
   * 
   * @param <T>
   *          the type of the operands
   * @param left
   *          the left operand
   * @param right
   *          the right operand
   * @return the comparison result
   */
  protected <T> boolean baseCompare(final Comparable<T> left, final T right)
  {
    switch (this)
    {
      case LESS_THAN:
        return left.compareTo(right) < 0;
      case GREATER_THAN:
        return left.compareTo(right) > 0;
      case EQUAL:
        return left.compareTo(right) == 0;
      default:
        throw new UnsupportedOperationException();
    }
  }

  /**
   * Compares the supplied strings by first converting them to the appropriate type.
   * 
   * @param left
   *          the left operand
   * @param right
   *          the right operand
   * @return the comparison result
   */
  protected boolean compare(final String left, final String right)
  {
    if (isString(left) || isString(right))
    {
      return stringCompare(left, right);
    }
    else if (isChar(left) || isChar(right))
    {
      return charCompare(left, right);
    }
    else if (isNumeric(left) || isNumeric(right))
    {
      return numericCompare(left, right);
    }
    else if (isBoolean(left) || isBoolean(right))
    {
      return baseCompare(Boolean.parseBoolean(left), Boolean.parseBoolean(right));
    }
    return false;
  }
}