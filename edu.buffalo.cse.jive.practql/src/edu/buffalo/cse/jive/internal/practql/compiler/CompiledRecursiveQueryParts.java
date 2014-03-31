package edu.buffalo.cse.jive.internal.practql.compiler;

import edu.buffalo.cse.jive.practql.expression.query.INamedQuery;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpression;
import edu.buffalo.cse.jive.practql.schema.IFieldSchema;
import edu.buffalo.cse.jive.practql.schema.IRelationSchema;
import edu.buffalo.cse.jive.practql.schema.ISchemaSignature;
import edu.buffalo.cse.jive.practql.schema.Type;

/**
 * <pre>
 *   CREATE OR REPLACE FUNCTION compiled_CTEi() RETURNS TABLE(X,I) AS $BODY$
 *   DECLARE
 *     r INT;
 *     delta INT;
 *   BEGIN
 *     r = 1;
 *     
 *     declare temporary views and tables
 *     
 *     INSERT INTO __TNP(X, I, __round) SELECT X, I, -1 FROM QE;
 *     
 *     INSERT INTO __TNP(X, I, __round) SELECT X, I,  0 FROM TNP0;
 *     delta = tuples added to __TNP
 *     
 *     WHILE (delta > 0) DO
 *       INSERT INTO __TNP(X,I,__round) SELECT X, I, r FROM TNPr;
 *       delta = tuples added to __TNP
 *       r = r + 1;  
 *     END WHILE;
 *     
 *     RETURN QUERY SELECT X, I FROM __TNP WHERE round >= 0;
 *     
 *   END; $BODY$ LANGUAGE plpgsql;
 * 
 *   CTEi(X,T) AS (...) --> CTEi(X,I) AS (SELECT * FROM compiled_CTEi())
 * </pre>
 */
class CompiledRecursiveQueryParts
{
  private final String FMT_CTE = "WITH %s(%s) AS\n  (SELECT %s FROM %s())";
  private final String FMT_FUNCTION = "CREATE OR REPLACE FUNCTION %s() RETURNS TABLE%s AS $BODY$";
  private final String FMT_TABLE = "CREATE TEMP TABLE %s(%s) ON COMMIT DROP;";
  private final String FMT_VIEW = "CREATE TEMP VIEW %s;";
  private final String TABLE_INSERT = "INSERT INTO %s(%s) SELECT %s, %s FROM %s;";
  private final String TABLE_RETURN = "RETURN QUERY SELECT %s FROM %s WHERE __round >= 0;";
  private final IRelationSchema __TNP;
  private final INamedQuery source;
  private final INamedQuery TNP0;
  private final IQueryExpression TNP0_expression;
  private final INamedQuery DELTAr;
  private final IQueryExpression DELTAr_expression;
  private final INamedQuery QE;
  private final String compiledString;
  private final String functionName;

  public CompiledRecursiveQueryParts(final IRelationSchema __TNP, final INamedQuery source,
      final INamedQuery TNP0, final IQueryExpression TNP0_expression, final INamedQuery DELTAr,
      final IQueryExpression DELTAr_expression, final INamedQuery QE)
  {
    this.__TNP = __TNP;
    this.source = source;
    this.TNP0 = TNP0;
    this.TNP0_expression = TNP0_expression;
    this.DELTAr = DELTAr;
    this.DELTAr_expression = DELTAr_expression;
    this.QE = QE;
    this.functionName = CompilerUtils.newFunctionName();
    this.compiledString = compile();
  }

  private void appendCTE(final StringBuffer buffer)
  {
    // "WITH %s(%s) AS SELECT %s FROM %s();";
    buffer.append("\n\n");
    buffer.append(String.format(FMT_CTE, source.getSchema().getName(),
        fieldList(__TNP.getSignature(), 1), fieldList(__TNP.getSignature(), 1), functionName));
  }

  private void appendFooter(final StringBuffer buffer)
  {
    buffer.append("\nEND; $BODY$ LANGUAGE plpgsql;");
  }

  private void appendHeader(final StringBuffer buffer)
  {
    buffer.append(String.format(FMT_FUNCTION, functionName, DELTAr.getSignature()));
    buffer.append("\nDECLARE");
    buffer.append("\n  r INT;");
    buffer.append("\n  delta INT;");
    buffer.append("\nBEGIN;");
    buffer.append("\n  r = 1;");
  }

  private void appendInitialization(final StringBuffer buffer)
  {
    // "INSERT INTO __TNP(X, I, round) SELECT X, I, -1 FROM QE;";
    if (QE != null)
    {
      buffer.append("\n  ");
      buffer.append(String.format(TABLE_INSERT, __TNP.getName(),
          fieldList(__TNP.getSignature(), 0), fieldList(QE.getSignature(), 0), "-1", QE.getSchema()
              .getName()));
    }
    // INSERT INTO __TNP(X, I, round) SELECT X, I, 0 FROM TNP0;
    buffer.append("\n  ");
    buffer.append(String.format(TABLE_INSERT, __TNP.getName(), fieldList(__TNP.getSignature(), 0),
        fieldList(TNP0.getSignature(), 0), "0", TNP0.getSchema().getName()));
    // update delta
    buffer.append("\n  GET DIAGNOSTICS delta = ROW COUNT;");
  }

  private void appendLoop(final StringBuffer buffer)
  {
    // begin the loop
    buffer.append("\n  WHILE (delta > 0) DO");
    // INSERT INTO __TNP(X,I,round) SELECT X, I, r FROM TNPr;
    buffer.append("\n    ");
    buffer.append(String.format(TABLE_INSERT, __TNP.getName(), fieldList(__TNP.getSignature(), 0),
        fieldList(DELTAr.getSignature(), 0), "r", DELTAr.getSchema().getName()));
    // update delta
    buffer.append("\n    GET DIAGNOSTICS delta = ROW COUNT;");
    // update the round
    buffer.append("\n    r = r + 1;");
    // end the loop
    buffer.append("\n  END WHILE;");
  }

  private void appendNamedQueries(final StringBuffer buffer, final IQueryExpression qe)
  {
    for (int i = 0; i < qe.size(); i++)
    {
      final INamedQuery nq = qe.getMember(i);
      buffer.append("\n  ");
      buffer.append(String.format(FMT_VIEW, nq));
    }
  }

  private void appendReturn(final StringBuffer buffer)
  {
    // RETURN QUERY SELECT X, I FROM __TNP WHERE round >= 0;
    buffer.append("\n  ");
    buffer.append(String.format(TABLE_RETURN, fieldList(__TNP.getSignature(), 1), __TNP.getName()));
  }

  private void appendTables(final StringBuffer buffer)
  {
    buffer.append("\n  ");
    buffer.append(String.format(FMT_TABLE, __TNP.getName(), pointToInterval(__TNP.getSignature())));
  }

  private void appendViews(final StringBuffer buffer)
  {
    appendNamedQueries(buffer, TNP0_expression);
    buffer.append("\n  ");
    buffer.append(String.format(FMT_VIEW, TNP0));
    appendNamedQueries(buffer, DELTAr_expression);
    buffer.append("\n  ");
    buffer.append(String.format(FMT_VIEW, DELTAr));
    if (QE != null)
    {
      buffer.append("\n  ");
      buffer.append(String.format(FMT_VIEW, QE));
    }
  }

  private String compile()
  {
    final StringBuffer buffer = new StringBuffer("");
    appendHeader(buffer);
    appendTables(buffer);
    appendViews(buffer);
    appendInitialization(buffer);
    appendLoop(buffer);
    appendReturn(buffer);
    appendFooter(buffer);
    appendCTE(buffer);
    buffer.append("\n");
    return buffer.toString();
  }

  private String fieldList(final ISchemaSignature signature, final int delta)
  {
    final StringBuffer buffer = new StringBuffer("");
    for (int i = 0; i < signature.size() - delta; i++)
    {
      final IFieldSchema fs = signature.getFieldSchema(i);
      buffer.append(fs.getName());
      if (i < signature.size() - 1 - delta)
      {
        buffer.append(", ");
      }
    }
    return buffer.toString();
  }

  private String pointToInterval(final ISchemaSignature signature)
  {
    final StringBuffer buffer = new StringBuffer("");
    for (int i = 0; i < signature.size(); i++)
    {
      final IFieldSchema fs = signature.getFieldSchema(i);
      buffer.append(fs.getName());
      buffer.append(" ");
      if (fs.getType() == Type.TP_ENCODED)
      {
        buffer.append(Type.CINTERVAL);
      }
      else
      {
        buffer.append(fs.getType());
      }
      if (i < signature.size() - 1)
      {
        buffer.append(", ");
      }
    }
    return buffer.toString();
  }

  String compiledString()
  {
    return compiledString;
  }
}
