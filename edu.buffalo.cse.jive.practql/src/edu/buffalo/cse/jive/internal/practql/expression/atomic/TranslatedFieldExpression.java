package edu.buffalo.cse.jive.internal.practql.expression.atomic;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.atomic.ITranslatedFieldExpression;
import edu.buffalo.cse.jive.practql.schema.IFieldReference;
import edu.buffalo.cse.jive.practql.schema.Type;

class TranslatedFieldExpression extends FieldExpression implements ITranslatedFieldExpression
{
  TranslatedFieldExpression(final IFieldReference fieldReference) throws ExpressionException
  {
    super(fieldReference);
    // catch invalid type in field reference
    if (fieldReference.getSchema().getType() != Type.TP
        && fieldReference.getSchema().getType() != Type.CINTERVAL)
    {
      throw new ExpressionException(String.format(
          "Error creating translated field expression: '%s' field reference type.", fieldReference
              .getSchema().getType()));
    }
  }

  @Override
  public String toStringTyped()
  {
    return toString() + "{tp_translated}";
  }
}