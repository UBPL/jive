package edu.buffalo.cse.jive.internal.practql;

import static edu.buffalo.cse.jive.practql.schema.Type.BOOLEAN;
import static edu.buffalo.cse.jive.practql.schema.Type.DECIMAL;
import static edu.buffalo.cse.jive.practql.schema.Type.INTEGER;
import static edu.buffalo.cse.jive.practql.schema.Type.INVALID;
import static edu.buffalo.cse.jive.practql.schema.Type.NULL;
import static edu.buffalo.cse.jive.practql.schema.Type.STRING;
import static edu.buffalo.cse.jive.practql.schema.Type.TP;
import static edu.buffalo.cse.jive.practql.schema.Type.TP_ENCODED;
import edu.buffalo.cse.jive.practql.expression.nary.AdditionConnective;
import edu.buffalo.cse.jive.practql.expression.relational.RelationalConnective;
import edu.buffalo.cse.jive.practql.schema.Type;

public class TypeUtils
{
  public static Type inferForAddtion(final Type t1, final Type t2,
      final AdditionConnective connective)
  {
    // _ x * --> _
    // * x _ --> _
    if (!t1.supportsAddition() || !t2.supportsAddition())
    {
      return INVALID;
    }
    // numeric(t1) x numeric(t2) x connective(+, -) --> wider(t1, t2)
    if (t1.isNumeric() && t2.isNumeric() && connective != AdditionConnective.AC_CONCATENATE)
    {
      return t1 == DECIMAL || t2 == DECIMAL ? DECIMAL : INTEGER;
    }
    // temporal(t1) x temporal(t2) x connective(+) --> wider(t1, t2)
    if (t1.isTemporal() && t2.isTemporal() && connective != AdditionConnective.AC_CONCATENATE)
    {
      return t1 == TP_ENCODED || t2 == TP_ENCODED ? TP_ENCODED : TP;
    }
    // string(t1) x string(t2) x connective(||) --> string
    if (t1 == STRING && t2 == STRING && connective == AdditionConnective.AC_CONCATENATE)
    {
      return STRING;
    }
    // incompatible
    return INVALID;
  }

  public static Type inferForConjunction(final Type t1, final Type t2)
  {
    // _ x * --> _
    // * x _ --> _
    if (!TypeUtils.supportsFormula(t1) || !TypeUtils.supportsFormula(t2))
    {
      return INVALID;
    }
    // t x t --> t
    if (t1 == BOOLEAN && t2 == BOOLEAN)
    {
      return BOOLEAN;
    }
    // incompatible
    return INVALID;
  }

  public static Type inferForDisjunction(final Type t1, final Type t2)
  {
    // _ x * --> _
    // * x _ --> _
    if (!TypeUtils.supportsFormula(t1) || !TypeUtils.supportsFormula(t2))
    {
      return INVALID;
    }
    // t x t --> t
    if (t1 == BOOLEAN && t2 == BOOLEAN)
    {
      return BOOLEAN;
    }
    // incompatible
    return INVALID;
  }

  public static Type inferForMultiplication(final Type t1, final Type t2)
  {
    // _ x * --> _
    // * x _ --> _
    if (!TypeUtils.supportsMultiplication(t1) || !TypeUtils.supportsMultiplication(t2))
    {
      return INVALID;
    }
    // t x t --> t
    if (t1 == t2)
    {
      return t1;
    }
    // numeric(t1) x numeric(t2) --> wider(t1, t2)
    if (t1.isNumeric() && t2.isNumeric())
    {
      return t1 == DECIMAL || t2 == DECIMAL ? DECIMAL : INTEGER;
    }
    // incompatible
    return INVALID;
  }

  public static Type initAddition(final Type t)
  {
    return t.supportsAddition() ? t : INVALID;
  }

  public static Type initFormula(final Type t)
  {
    return TypeUtils.supportsFormula(t) ? t : INVALID;
  }

  public static Type initForNegation(final Type t)
  {
    return TypeUtils.supportsFormula(t) ? t : INVALID;
  }

  public static Type initForRelationalExpression(final Type t1, final RelationalConnective rop,
      final Type t2)
  {
    // _ x * --> _
    // * x _ --> _
    if (t1 == INVALID || t2 == INVALID)
    {
      return INVALID;
    }
    // * x NULL --> BOOLEAN
    // * x * --> INVALID
    if (rop == RelationalConnective.RC_IS || rop == RelationalConnective.RC_IS_NOT)
    {
      return t2 == NULL ? BOOLEAN : INVALID;
    }
    // NULL x * --> NULL
    // * x NULL --> NULL
    if (t1 == NULL || t2 == NULL)
    {
      return NULL;
    }
    if (rop == RelationalConnective.RC_LIKE || rop == RelationalConnective.RC_NOT_LIKE)
    {
      return TypeUtils.supportsStringRelOp(t1) && TypeUtils.supportsStringRelOp(t2) ? BOOLEAN
          : INVALID;
    }
    if (t1 == t2)
    {
      return BOOLEAN;
    }
    if (t1.isNumeric() && t2.isNumeric())
    {
      return BOOLEAN;
    }
    if (t1.isTemporal() && t2.isTemporal() && rop != RelationalConnective.RC_NE)
    {
      return BOOLEAN;
    }
    return INVALID;
  }

  public static Type initForSigned(final Type t)
  {
    return TypeUtils.supportsSign(t) ? t : INVALID;
  }

  public static Type initMultiplication(final Type t)
  {
    return TypeUtils.supportsMultiplication(t) ? t : INVALID;
  }

  public static boolean isSchemaCompatible(final Type t1, final Type t2)
  {
    final Type comparable = TypeUtils.initForRelationalExpression(t1, RelationalConnective.RC_EQ,
        t2);
    return comparable == BOOLEAN || t1 == NULL || t2 == NULL;
  }

  private static boolean supportsFormula(final Type t)
  {
    // boolean expressions are supported for: boolean;
    return t == BOOLEAN;
  }

  private static boolean supportsMultiplication(final Type t)
  {
    // multiplication is supported for: numeric, string, and temporal types;
    return t == DECIMAL || t == INTEGER;
  }

  private static boolean supportsSign(final Type t)
  {
    // sign is supported for: numeric and temporal types;
    return t == DECIMAL || t == INTEGER;
  }

  private static boolean supportsStringRelOp(final Type t)
  {
    // string operations are supported for: string;
    return t == STRING;
  }
}
