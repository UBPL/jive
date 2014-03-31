package org.lessa.demian.internal.parser;

import static org.lessa.demian.internal.tokenizer.TokenSymbol.TK_COMMA;
import static org.lessa.demian.internal.tokenizer.TokenSymbol.TK_LPARENS;
import static org.lessa.demian.internal.tokenizer.TokenSymbol.TK_MINUS;
import static org.lessa.demian.internal.tokenizer.TokenSymbol.TK_RPARENS;
import static org.lessa.demian.tokenizer.TokenClass.TC_IDENTIFIER;
import static org.lessa.demian.tokenizer.TokenClass.TC_LITERAL_INTEGER;

import java.util.ArrayList;
import java.util.List;

import org.lessa.demian.internal.Factory;
import org.lessa.demian.internal.tokenizer.TokenInteger;
import org.lessa.demian.expression.ExpressionException;
import org.lessa.demian.expression.FunctionType;
import org.lessa.demian.expression.IExpression;
import org.lessa.demian.expression.IExpressionFactory;
import org.lessa.demian.parser.ParserException;

class Parser extends AbstractParser
{
  // determines whether CTE names are replaced with freshly generated ones
  private static final String ERR_EXPRESSION_RESOLUTION = "Expression validation error.";
  private static final String ERR_INVALID_EXPRESSION = "Invalid expression '%s'.";
  private final IExpressionFactory ef;

  Parser()
  {
    ef = Factory.INSTANCE.expressionFactory();
  }

  @Override
  public IExpression doParse() throws ParserException
  {
    try
    {
      return expression();
    }
    catch (final ExpressionException ee)
    {
      die(Parser.ERR_EXPRESSION_RESOLUTION, ee);
    }
    return null;
  }

  private IExpression expression() throws ParserException, ExpressionException
  {
    // must be an identifier
    test(TC_IDENTIFIER);
    String value = token.getText();
    return ef.newFunction(FunctionType.getValue(value), argumentList());
  }

  private List<IExpression> argumentList() throws ParserException, ExpressionException
  {
    final List<IExpression> args = new ArrayList<IExpression>();
    // open argument list
    check(TK_LPARENS);
    // test for non-empty argument list
    boolean nextArg = !test(TK_RPARENS);
    // proceed with at least one argument
    while (nextArg)
    {
      args.add(argExpression());
      nextArg = test(TK_COMMA);
    }
    // close argument list
    check(TK_RPARENS);
    return args;
  }

  private IExpression argExpression() throws ParserException, ExpressionException
  {
    final boolean isSigned = test(TK_MINUS);
    // integer literal
    if (test(TC_LITERAL_INTEGER))
    {
      return ef.newInteger((isSigned ? -1 : +1) * ((TokenInteger) token).getValue());
    }
    // identifier-- variable or expression
    else if (test(TC_IDENTIFIER))
    {
      String value = token.getText();
      if (FunctionType.getValue(value) == null)
      {
        return ef.newVariable(value);
      }
      else
      {
        return ef.newFunction(FunctionType.getValue(value), argumentList());
      }
    }
    die(Parser.ERR_INVALID_EXPRESSION, tokens.get(mark()).getText());
    return null;
  }

  @Override
  protected void beforeParse()
  {
  }
}