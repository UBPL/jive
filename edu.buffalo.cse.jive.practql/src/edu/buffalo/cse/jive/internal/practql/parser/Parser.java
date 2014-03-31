package edu.buffalo.cse.jive.internal.practql.parser;

import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_ALL;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_AND;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_AS;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_BY;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_DESC;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_DISTINCT;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_EXCEPT;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_FROM;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_GROUP;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_HAVING;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_INTERSECT;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_IS;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_LIKE;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_NOT;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_OR;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_ORDER;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_RECURSIVE;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_SELECT;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_UNION;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_WHERE;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenKeyword.TK_WITH;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenSymbol.TK_COMMA;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenSymbol.TK_CONCATENATE;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenSymbol.TK_DIVIDE;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenSymbol.TK_DOT;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenSymbol.TK_EQ;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenSymbol.TK_GE;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenSymbol.TK_GT;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenSymbol.TK_LE;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenSymbol.TK_LPARENS;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenSymbol.TK_LT;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenSymbol.TK_MINUS;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenSymbol.TK_NE;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenSymbol.TK_PLUS;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenSymbol.TK_RPARENS;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenSymbol.TK_SEMICOLON;
import static edu.buffalo.cse.jive.internal.practql.tokenizer.TokenSymbol.TK_TIMES;
import static edu.buffalo.cse.jive.practql.tokenizer.TokenClass.TC_AGGREGATE;
import static edu.buffalo.cse.jive.practql.tokenizer.TokenClass.TC_FUNCTION;
import static edu.buffalo.cse.jive.practql.tokenizer.TokenClass.TC_IDENTIFIER;
import static edu.buffalo.cse.jive.practql.tokenizer.TokenClass.TC_LITERAL_BOOLEAN;
import static edu.buffalo.cse.jive.practql.tokenizer.TokenClass.TC_LITERAL_DECIMAL;
import static edu.buffalo.cse.jive.practql.tokenizer.TokenClass.TC_LITERAL_INTEGER;
import static edu.buffalo.cse.jive.practql.tokenizer.TokenClass.TC_LITERAL_NULL;
import static edu.buffalo.cse.jive.practql.tokenizer.TokenClass.TC_LITERAL_STRING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.buffalo.cse.jive.internal.practql.Factory;
import edu.buffalo.cse.jive.internal.practql.tokenizer.TokenBoolean;
import edu.buffalo.cse.jive.internal.practql.tokenizer.TokenDecimal;
import edu.buffalo.cse.jive.internal.practql.tokenizer.TokenInteger;
import edu.buffalo.cse.jive.internal.practql.tokenizer.TokenQString;
import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpression;
import edu.buffalo.cse.jive.practql.expression.IExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.atomic.AggregateType;
import edu.buffalo.cse.jive.practql.expression.atomic.FunctionType;
import edu.buffalo.cse.jive.practql.expression.atomic.IFieldExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.IWildcardExpression;
import edu.buffalo.cse.jive.practql.expression.nary.AdditionConnective;
import edu.buffalo.cse.jive.practql.expression.nary.IAddition;
import edu.buffalo.cse.jive.practql.expression.nary.IConjunction;
import edu.buffalo.cse.jive.practql.expression.nary.IDisjunction;
import edu.buffalo.cse.jive.practql.expression.nary.IMultiplication;
import edu.buffalo.cse.jive.practql.expression.nary.MultiplicationConnective;
import edu.buffalo.cse.jive.practql.expression.query.IClauseFrom;
import edu.buffalo.cse.jive.practql.expression.query.IClauseGroupBy;
import edu.buffalo.cse.jive.practql.expression.query.IClauseHaving;
import edu.buffalo.cse.jive.practql.expression.query.IClauseOrderBy;
import edu.buffalo.cse.jive.practql.expression.query.IClauseSelect;
import edu.buffalo.cse.jive.practql.expression.query.IClauseWhere;
import edu.buffalo.cse.jive.practql.expression.query.INamedQuery;
import edu.buffalo.cse.jive.practql.expression.query.IQuery;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpression;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpressionFactory;
import edu.buffalo.cse.jive.practql.expression.query.ISimpleQuery;
import edu.buffalo.cse.jive.practql.expression.query.QueryConnective;
import edu.buffalo.cse.jive.practql.expression.query.QueryException;
import edu.buffalo.cse.jive.practql.expression.relational.RelationalConnective;
import edu.buffalo.cse.jive.practql.expression.unary.INamedExpression;
import edu.buffalo.cse.jive.practql.expression.unary.INegatedExpression;
import edu.buffalo.cse.jive.practql.expression.unary.INegativeExpression;
import edu.buffalo.cse.jive.practql.expression.unary.SortDirection;
import edu.buffalo.cse.jive.practql.parser.ParserException;
import edu.buffalo.cse.jive.practql.schema.IFieldReference;
import edu.buffalo.cse.jive.practql.schema.IRelationSchema;
import edu.buffalo.cse.jive.practql.schema.ISchemaFactory;
import edu.buffalo.cse.jive.practql.schema.SchemaException;
import edu.buffalo.cse.jive.practql.tokenizer.IToken;

class Parser extends AbstractParser
{
  // determines whether CTE names are replaced with freshly generated ones
  private static final boolean GENERATED_CTE_NAMES = false;
  private static final String ERR_DISJUNCTIVE = "Disjunctive formulas not supported. Please use a UNION of conjunctive queries.";
  private static final String ERR_CTE_NAME_CLASH = "CTE name '%s' clashes with an already existing %s.";
  private static final String ERR_CTE_REPEATED_FIELDNAME = "Field name '%s' specified more than once in the definition of '%s'.";
  private static final String ERR_INVALID_BOOLEAN = "Boolean literals cannot be signed.";
  private static final String ERR_INVALID_DECIMAL = "Decimal literals cannot be negated.";
  private static final String ERR_INVALID_EXP = "Invalid expression: expecting a literal, field reference, or parenthesized expression but found '%s'.";
  private static final String ERR_INVALID_FIELD_EXP = "Invalid field expression: expecting a field identifier or '*' at token '%s'.";
  private static final String ERR_INVALID_INTEGER = "Integer literals cannot be negated.";
  private static final String ERR_INVALID_NULL = "NULL cannot be signed or negated.";
  private static final String ERR_INVALID_RELNAME = "Relation name '%s' not found in database schema '%s'.";
  private static final String ERR_REL_NAME_CLASH = "Relation name '%s' clashes an existing CTE name.";
  private static final String ERR_INVALID_STRING = "String literals cannot be signed or negated.";
  private static final String ERR_INVALID_WILDCARD = "Wildcards only allowed in the SELECT clause as a simple reference ('*') or as a qualified reference ('R.*').";
  private static final String ERR_IS = "'NULL' OR 'NOT NULL' exped after 'IS'";
  private static final String ERR_LIKE_EXPECTED = "'LIKE' expected after 'NOT'.";
  private static final String ERR_QUERY_RESOLUTION = "Query validation error.";
  private static final String ERR_WILDCARD_ALIAS = "Wildcard expressions cannot be aliased.";
  private final IExpressionFactory ef;
  private final IQueryExpressionFactory qef;
  private final ISchemaFactory sf;
  private int relId = 0;
  private final Map<String, String> relNameMap;
  /**
   * The following three fields are needed for recursive CTEs. Parsing had to be generalized to
   * handle both recursive and non-recursive CTEs using a single logic.
   */
  private final List<String> cteFieldList;
  private String recursiveName;
  private IRelationSchema recursiveSchema;

  Parser()
  {
    super(Factory.INSTANCE);
    ef = Factory.INSTANCE.expressionFactory();
    qef = Factory.INSTANCE.queryExpressionFactory();
    sf = Factory.INSTANCE.schemaFactory();
    relNameMap = new HashMap<String, String>();
    cteFieldList = new ArrayList<String>();
    recursiveName = null;
    recursiveSchema = null;
  }

  @Override
  public IQueryExpression doParse() throws ParserException
  {
    try
    {
      boolean isRecursive = false;
      List<INamedQuery> wel = null;
      // optional
      if (test(TK_WITH))
      {
        isRecursive = test(TK_RECURSIVE);
        wel = WithExpressionList(isRecursive);
      }
      else
      {
        wel = Collections.unmodifiableList(new ArrayList<INamedQuery>());
      }
      // mandatory
      final IQuery q = Query();
      // mandatory
      QueryTerminator();
      // create and return the query expression
      return qef.newQueryExpression(isRecursive, wel, q);
    }
    catch (final QueryException qe)
    {
      die(Parser.ERR_QUERY_RESOLUTION, qe);
    }
    catch (final ExpressionException ee)
    {
      die(Parser.ERR_QUERY_RESOLUTION, ee);
    }
    catch (final SchemaException se)
    {
      die(Parser.ERR_QUERY_RESOLUTION, se);
    }
    return null;
  }

  private List<IExpression> ArgumentList(final IClauseFrom from, final boolean wildcardAllowed)
      throws ParserException, QueryException, ExpressionException
  {
    final List<IExpression> args = new ArrayList<IExpression>();
    // test for non-empty argument list
    boolean nextArg = !test(TK_RPARENS);
    // proceed with at least one argument
    while (nextArg)
    {
      args.add(BExpression(from, null, wildcardAllowed));
      nextArg = test(TK_COMMA);
    }
    // must close the argument list
    check(TK_RPARENS);
    return args;
  }

  private IExpression Atom(final IClauseFrom from, final IClauseSelect select,
      final boolean wildcardAllowed) throws ParserException, QueryException, ExpressionException
  {
    IExpression exp = null;
    final boolean isNegated = supportsNegation() && test(TK_NOT);
    final boolean isSigned = !isNegated && (test(TK_MINUS) || test(TK_PLUS));
    final boolean isMinus = isSigned && token == TK_MINUS;
    if (test(TC_LITERAL_BOOLEAN))
    {
      if (isSigned)
      {
        die(Parser.ERR_INVALID_BOOLEAN);
      }
      exp = ef.newBoolean(((TokenBoolean) token).getValue());
    }
    else if (test(TC_LITERAL_DECIMAL))
    {
      if (isNegated)
      {
        die(Parser.ERR_INVALID_DECIMAL);
      }
      exp = ef.newDecimal(((TokenDecimal) token).getValue());
    }
    else if (test(TC_LITERAL_INTEGER))
    {
      if (isNegated)
      {
        die(Parser.ERR_INVALID_INTEGER);
      }
      exp = ef.newInteger(((TokenInteger) token).getValue());
    }
    else if (test(TC_LITERAL_NULL))
    {
      if (isNegated || isSigned)
      {
        die(Parser.ERR_INVALID_NULL);
      }
      exp = ef.newNull();
    }
    else if (test(TC_LITERAL_STRING))
    {
      if (isNegated || isSigned)
      {
        die(Parser.ERR_INVALID_STRING);
      }
      exp = ef.newString(((TokenQString) token).getText());
    }
    else if (test(TC_IDENTIFIER) || test(TK_TIMES))
    {
      exp = FieldNameExpression(from, select, wildcardAllowed);
    }
    else if (supportsAggregate() && test(TC_AGGREGATE))
    {
      final String aggregateName = token.getText();
      check(TK_LPARENS);
      final boolean isDistinct = test(TK_DISTINCT);
      final IExpression arg = test(TK_TIMES) ? ef.newWildcardExpression() : BExpression(from, null,
          false);
      check(TK_RPARENS);
      exp = ef.newAggregate(AggregateType.getValue(aggregateName), arg, isDistinct);
    }
    else if (test(TC_FUNCTION))
    {
      final String functionName = token.getText();
      check(TK_LPARENS);
      final List<IExpression> args = ArgumentList(from, false);
      exp = ef.newFunctionCall(FunctionType.getValue(functionName), args);
    }
    else if (test(TK_LPARENS))
    {
      // wildcards not allowed in parenthesized expressions
      exp = BExpression(from, select, false);
      check(TK_RPARENS);
    }
    else
    {
      die(Parser.ERR_INVALID_EXP, tokens.get(mark()).getText());
    }
    // negated
    if (isNegated)
    {
      // double negation?
      if (exp instanceof INegatedExpression)
      {
        return ((INegatedExpression) exp).getExpression();
      }
      return ef.newNegatedExpression(exp);
    }
    // minus
    if (isMinus)
    {
      // double minus?
      if (exp instanceof INegativeExpression)
      {
        return ((INegativeExpression) exp).getExpression();
      }
      return ef.newNegativeExpression(exp);
    }
    return exp;
  }

  // 0:: atom:: literal, field reference, relation, (expression)
  // 1:: factor:: unary minus, NOT
  // 2:: term:: *, /, AND
  // 3:: expression:: +, -, OR, ||
  //
  // <b-expression> ::= <b-term> ['OR' <b-term>]*
  // <b-term> ::= <relation> ['AND' <relation>]*
  // <relation> ::= <expression> [<relop> <expression>]
  // <expression> ::= <term> [<add_op> <term>]*
  // <term> ::= <factor> [<mul_op> <factor>]*
  // <factor> ::= ['NOT' | <add_op>] <atom>
  // <atom> ::= <literal> | <identifier> | '(' <b-expression> ')' | <function call>
  // <literal> ::= <integer> | <decimal> | <string> | <boolean> | <null>
  // <function call> ::= <identifier> '(' [<arguments>] ')'
  // <arguments> ::= <b-expression> [',' <b-expression>]
  // <add_op> ::= '+', '-'
  // <mul_op> ::= '*', '/'
  // <rel_op> ::= '<', '<=', '>', '>=', '=', '<>', 'LIKE', 'NOT LIKE'
  //
  // the search for a boolean expression may degrade into a non-boolean expression
  // at the <relation> rule, that establishes that a relation may consist only of
  // one expression
  //
  private IExpression BExpression(final IClauseFrom from, final IClauseSelect select,
      final boolean wildcardAllowed) throws ParserException, QueryException, ExpressionException
  {
    final IExpression disjunct = BTerm(from, select, wildcardAllowed);
    if (!test(TK_OR))
    {
      return disjunct;
    }
    if (!supportsDisjunction())
    {
      die(Parser.ERR_DISJUNCTIVE);
    }
    final IDisjunction result = ef.newDisjunction(disjunct);
    do
    {
      result.append(BTerm(from, select, wildcardAllowed));
    } while (test(TK_OR));
    return result;
  }

  private IExpression BTerm(final IClauseFrom from, final IClauseSelect select,
      final boolean wildcardAllowed) throws ParserException, QueryException, ExpressionException
  {
    final IExpression conjunct = RelationalExpression(from, select, wildcardAllowed);
    if (!test(TK_AND))
    {
      return conjunct;
    }
    final IConjunction result = ef.newConjunction(conjunct);
    do
    {
      result.append(RelationalExpression(from, select, wildcardAllowed));
    } while (test(TK_AND));
    return result;
  }

  private INamedQuery CTE(final boolean isRecursive) throws ParserException, QueryException,
      ExpressionException, SchemaException
  {
    cteFieldList.clear();
    recursiveName = null;
    recursiveSchema = null;
    // CTE name
    check(TC_IDENTIFIER);
    final String name = token.getText();
    // map the defined name to an automatically generated name
    final String mappedName = mapRelationName(name);
    // CTE name must be distinct from previously defined tables, views, and CTEs
    final IRelationSchema rs = dbSchema.lookupRelation(mappedName);
    if (rs != null)
    {
      die(Parser.ERR_CTE_NAME_CLASH, name, rs.getKind());
    }
    // field list begins
    check(TK_LPARENS);
    do
    {
      // field name
      check(TC_IDENTIFIER);
      final String fieldName = token.getText();
      // check that a field with the same name has not been defined in the CTE
      for (final String ident : cteFieldList)
      {
        if (ident.equalsIgnoreCase(fieldName))
        {
          die(Parser.ERR_CTE_REPEATED_FIELDNAME, fieldName, mappedName);
        }
      }
      // add the field to the field list
      cteFieldList.add(fieldName);
    } while (test(TK_COMMA));
    // field list ends
    check(TK_RPARENS);
    // AS keyword
    check(TK_AS);
    // query definition begins
    check(TK_LPARENS);
    // query body
    IQuery query = null;
    try
    {
      // if this is a recursive CTE, update its recursive information
      if (isRecursive)
      {
        recursiveName = mappedName;
      }
      query = Query();
    }
    catch (final QueryException qe)
    {
      die(String.format("Error parsing CTE '%s'.", mappedName), qe);
    }
    // query definition ends
    check(TK_RPARENS);
    // the CTE schema may have been created in advance for recursive queries
    final IRelationSchema schema = recursiveSchema != null ? recursiveSchema : sf
        .newRelationSchema(mappedName, query.getMember(0).getSelect(), cteFieldList);
    // return the CTE
    return qef.newNamedQuery(schema, query);
  }

  private IExpression Expression(final IClauseFrom from, final IClauseSelect select,
      final boolean wildcardAllowed) throws ParserException, QueryException, ExpressionException
  {
    final IExpression term = Term(from, select, wildcardAllowed);
    if (test(TK_PLUS) || test(TK_MINUS) || test(TK_CONCATENATE))
    {
      final IAddition a = ef.newAddition(term);
      do
      {
        a.append(AdditionConnective.getValue(token.getText()), Term(from, select, wildcardAllowed));
      } while (test(TK_PLUS) || test(TK_MINUS) || test(TK_CONCATENATE));
      return a;
    }
    return term;
  }

  // semantic validation is performed at a later time, after syntactic validation
  private IExpression FieldNameExpression(final IClauseFrom from, final IClauseSelect select,
      final boolean wildcardAllowed) throws ParserException, QueryException, ExpressionException
  {
    // FieldReference ::= (TupleVar '.')? (FieldName | '*')
    final String prefix = token.getText();
    // qualified reference
    if (test(TK_DOT))
    {
      if (test(TC_IDENTIFIER))
      {
        // qualified field reference (R.F)
        return from.resolveField(token.getText(), prefix);
      }
      else if (test(TK_TIMES))
      {
        if (!wildcardAllowed)
        {
          die(Parser.ERR_INVALID_WILDCARD);
        }
        // qualified wildcard reference (R.*)
        return ef.newWildcardExpression(prefix);
      }
      die(Parser.ERR_INVALID_FIELD_EXP, tokens.get(mark() + 1));
    }
    // unqualified reference
    if (token == TK_TIMES)
    {
      if (!wildcardAllowed)
      {
        die(Parser.ERR_INVALID_WILDCARD);
      }
      // unqualified wildcard reference ('*') for a single relation
      if (from.size() == 1)
      {
        return ef.newWildcardExpression(from.getMember(0).getVariable());
      }
      // unqualified wildcard reference ('*') for multiple relations
      return ef.newWildcardExpression();
    }
    // unqualified field reference (F)
    // try to resolve as a projected field in the SELECT clause
    if (select != null)
    {
      final INamedExpression ne = select.resolveAlias(prefix);
      if (ne != null)
      {
        return ne;
      }
    }
    // must be resolved by the FROM clause
    return from.resolveField(prefix);
  }

  private IClauseFrom From() throws ParserException, QueryException
  {
    final IClauseFrom from = qef.newClauseFrom();
    String relVar = null;
    do
    {
      // relation name
      check(TC_IDENTIFIER);
      final String name = token.getText();
      // try to map the name given by the user with an automatically generated name
      final String mappedName = relNameMap.get(name.toUpperCase());
      // determine the relation schema based on the resolved name
      final IRelationSchema schema = dbSchema
          .lookupRelation(mappedName != null ? mappedName : name);
      if (schema == null)
      {
        die(Parser.ERR_INVALID_RELNAME, token.getText(), dbSchema.getName());
      }
      // optional alias
      if (test(TK_AS))
      {
        check(TC_IDENTIFIER);
        relVar = token.getText();
      }
      else
      {
        relVar = mappedName != null ? name : schema.getName();
      }
      // try appending this relation/alias to the FROM clause
      from.append(relVar, schema);
    } while (test(TK_COMMA));
    return from;
  }

  private IClauseGroupBy GroupBy(final IClauseFrom from, final IClauseSelect select)
      throws ParserException, QueryException, ExpressionException
  {
    if (supportsAggregate() && test(TK_GROUP) && check(TK_BY))
    {
      final IClauseGroupBy result = qef.newClauseGroupBy();
      do
      {
        // grouping expression
        result.append(Expression(from, select, false));
      } while (test(TK_COMMA));
      return result;
    }
    return null;
  }

  private IClauseHaving Having(final IClauseFrom from) throws ParserException, QueryException,
      ExpressionException
  {
    return supportsHaving() && test(TK_HAVING) ? qef
        .newClauseHaving(BExpression(from, null, false)) : null;
  }

  private String mapRelationName(final String userRelName) throws ParserException
  {
    final String prev = relNameMap.put(userRelName.toUpperCase(),
        Parser.GENERATED_CTE_NAMES ? "__R" + (++relId) : userRelName.toUpperCase());
    if (prev != null)
    {
      throw new ParserException(String.format(Parser.ERR_REL_NAME_CLASH, userRelName));
    }
    return relNameMap.get(userRelName.toUpperCase());
  }

  private IClauseOrderBy OrderBy(final IClauseFrom from, final IClauseSelect select)
      throws ParserException, QueryException, ExpressionException
  {
    if (supportsOrder() && test(TK_ORDER) && check(TK_BY))
    {
      final IClauseOrderBy result = qef.newClauseOrderBy();
      IExpression exp = null;
      do
      {
        // ordering expression
        exp = Expression(from, select, false);
        result.append(test(TK_DESC) ? SortDirection.SD_DESC : SortDirection.SD_ASC, exp);
      } while (test(TK_COMMA));
      return result;
    }
    return null;
  }

  private IQuery Query() throws ParserException, QueryException, ExpressionException,
      SchemaException
  {
    final IQuery result = qef.newQuery(SimpleQuery());
    // simple queries (additional)
    while (test(TK_UNION) || test(TK_EXCEPT) | test(TK_INTERSECT))
    {
      final QueryConnective qc = queryConnective(token, test(TK_ALL));
      final ISimpleQuery nextQuery = SimpleQuery();
      result.append(qc, nextQuery);
    }
    // make sure the schema of the query is fully resolved
    result.schemaResolved();
    // return the query
    return result;
  }

  private QueryConnective queryConnective(final IToken token, final boolean isBag)
  {
    if (token == TK_UNION)
    {
      return isBag ? QueryConnective.QC_BAG_UNION : QueryConnective.QC_SET_UNION;
    }
    else if (token == TK_EXCEPT)
    {
      return isBag ? QueryConnective.QC_BAG_DIFFERENCE : QueryConnective.QC_SET_DIFFERENCE;
    }
    else if (token == TK_INTERSECT)
    {
      return isBag ? QueryConnective.QC_BAG_INTERSECTION : QueryConnective.QC_SET_INTERSECTION;
    }
    return null;
  }

  private void QueryTerminator() throws ParserException
  {
    check(TK_SEMICOLON);
    checkEOS();
  }

  private IExpression RelationalExpression(final IClauseFrom from, final IClauseSelect select,
      final boolean wildcardAllowed) throws ParserException, QueryException, ExpressionException
  {
    final IExpression lhs = Expression(from, select, wildcardAllowed);
    RelationalConnective rc = null;
    if (test(TK_EQ) || test(TK_NE) || test(TK_LE) || test(TK_LT) || test(TK_GT) || test(TK_GE)
        || test(TK_LIKE))
    {
      rc = RelationalConnective.getValue(token.getText());
    }
    else if (test(TK_NOT))
    {
      rc = test(TK_LIKE) || die(Parser.ERR_LIKE_EXPECTED) ? RelationalConnective.RC_NOT_LIKE : null;
    }
    else if (test(TK_IS))
    {
      rc = test(TK_NOT) ? RelationalConnective.RC_IS_NOT : RelationalConnective.RC_IS;
      if (test(TC_LITERAL_NULL))
      {
        return ef.newRelationalExpression(lhs, rc, ef.newNull());
      }
      die(Parser.ERR_IS);
    }
    else
    {
      return lhs;
    }
    final IExpression rhs = Expression(from, select, wildcardAllowed);
    // reduce the number of relational connectives used
    if (rc == RelationalConnective.RC_GT)
    {
      return ef.newRelationalExpression(rhs, RelationalConnective.RC_LT, lhs);
    }
    if (rc == RelationalConnective.RC_GE)
    {
      return ef.newRelationalExpression(rhs, RelationalConnective.RC_LE, lhs);
    }
    return ef.newRelationalExpression(lhs, rc, rhs);
  }

  private IClauseSelect Select(final IClauseFrom from) throws ParserException, QueryException,
      ExpressionException
  {
    int expId = 1;
    String name = null;
    IExpression exp = null;
    final IClauseSelect result = qef.newClauseSelect(test(TK_DISTINCT));
    do
    {
      // projected expression
      exp = Expression(from, null, true);
      // expand wildcard expressions '*' or 'R.*'
      if (exp instanceof IWildcardExpression)
      {
        final List<IFieldReference> expanded = from.expandWildcard((IWildcardExpression) exp);
        for (final IFieldReference fr : expanded)
        {
          // try to add the field unqualified
          if (from.size() > 1)
          {
            result.append(fr.getQualifiedName(), ef.newFieldExpression(fr));
          }
          // add the field qualified
          else
          {
            result.append(fr.getSchema().getName(), ef.newFieldExpression(fr));
          }
        }
        // more meaningful error message
        if (test(TK_AS))
        {
          die(Parser.ERR_WILDCARD_ALIAS);
        }
      }
      else
      {
        // aliased by user
        if (test(TK_AS))
        {
          check(TC_IDENTIFIER);
          name = token.getText();
        }
        // default alias
        else if (exp instanceof IFieldExpression)
        {
          name = ((IFieldExpression) exp).getFieldReference().getSchema().getName();
        }
        // generated alias
        else
        {
          name = "exp" + expId++;
        }
        result.append(name, exp);
      }
    } while (test(TK_COMMA));
    // there must be a FROM token next
    check(TK_FROM);
    return result;
  }

  private ISimpleQuery SimpleQuery() throws ParserException, QueryException, ExpressionException,
      SchemaException
  {
    // SELECT clause
    check(TK_SELECT);
    // mark before SELECT
    final int ixSelect = mark();
    // advance to the next FROM token
    advanceTo(TK_FROM);
    // FROM clause
    final IClauseFrom from = From();
    // mark after FROM
    final int ixFrom = mark();
    // skip back to projection list
    moveTo(ixSelect);
    final IClauseSelect select = Select(from);
    // skip forward past the FROM clause
    moveTo(ixFrom);
    // optional WHERE clause
    final IClauseWhere where = Where(from);
    // optional GROUP BY clause (aliased fields supported!)
    final IClauseGroupBy groupBy = GroupBy(from, select);
    // optional HAVING clause
    final IClauseHaving having = Having(from);
    // type check the HAVING clause
    // optional ORDER BY clause (aliased fields supported!)
    final IClauseOrderBy orderBy = OrderBy(from, select);
    // create the query
    final ISimpleQuery sq = qef.newSimpleQuery(select, from, where, groupBy, having, orderBy);
    // check if this query defines the schema for a recursive CTE
    if (recursiveName != null && recursiveSchema == null)
    {
      recursiveSchema = sf.newRelationSchema(recursiveName, select, cteFieldList);
      // early append to the database schema so the recursive part of the query parses
      dbSchema.append(recursiveSchema);
    }
    // return query
    return sq;
  }

  private boolean supportsAggregate()
  {
    return true;
  }

  private boolean supportsDisjunction()
  {
    return false;
  }

  private boolean supportsHaving()
  {
    return supportsAggregate() && false;
  }

  private boolean supportsNegation()
  {
    return false;
  }

  private boolean supportsOrder()
  {
    return true;
  }

  private IExpression Term(final IClauseFrom from, final IClauseSelect select,
      final boolean wildcardAllowed) throws ParserException, QueryException, ExpressionException
  {
    final IExpression atom = Atom(from, select, wildcardAllowed);
    if (test(TK_TIMES) || test(TK_DIVIDE))
    {
      final IMultiplication m = ef.newMultiplication(atom);
      do
      {
        m.append(MultiplicationConnective.getValue(token.getText()),
            Atom(from, select, wildcardAllowed));
      } while (test(TK_TIMES) || test(TK_DIVIDE));
      return m;
    }
    return atom;
  }

  private IClauseWhere Where(final IClauseFrom from) throws ParserException, QueryException,
      ExpressionException
  {
    return test(TK_WHERE) ? qef.newClauseWhere(BExpression(from, null, false)) : null;
  }

  private List<INamedQuery> WithExpressionList(final boolean isRecursive) throws ParserException,
      QueryException, ExpressionException, SchemaException
  {
    final List<INamedQuery> result = new ArrayList<INamedQuery>();
    // CTE definition
    do
    {
      // parse and validate the CTE
      final INamedQuery nq = CTE(isRecursive);
      // add the CTE schema to the local schema
      if (recursiveSchema == null)
      {
        dbSchema.append(nq.getSchema());
      }
      // add the CTE to the query
      result.add(nq);
    } while (test(TK_COMMA));
    // return CTE list
    return Collections.unmodifiableList(result);
  }

  @Override
  protected void beforeParse()
  {
    dbSchema = null;
    relId = 0;
    relNameMap.clear();
  }
}