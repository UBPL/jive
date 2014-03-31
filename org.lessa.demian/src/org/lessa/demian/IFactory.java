package org.lessa.demian;

import org.lessa.demian.expression.IExpressionFactory;
import org.lessa.demian.parser.IParser;

public interface IFactory
{
  public IParser createParser();

  public IExpressionFactory expressionFactory();
}