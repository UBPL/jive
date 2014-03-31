package org.lessa.demian.internal;

import org.lessa.demian.IFactory;
import org.lessa.demian.internal.expression.ExpressionFactory;
import org.lessa.demian.internal.parser.ParserFactory;
import org.lessa.demian.expression.IExpressionFactory;
import org.lessa.demian.parser.IParser;
import org.lessa.demian.parser.IParserFactory;

public enum Factory implements IFactory
{
  INSTANCE;
  private final IExpressionFactory expressionFactory = ExpressionFactory.INSTANCE;
  private final IParserFactory parserFactory = ParserFactory.INSTANCE;

  @Override
  public IParser createParser()
  {
    return parserFactory.createParser();
  }

  @Override
  public IExpressionFactory expressionFactory()
  {
    return expressionFactory;
  }
}