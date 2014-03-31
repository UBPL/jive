package org.lessa.demian.internal.parser;

import org.lessa.demian.parser.IParser;
import org.lessa.demian.parser.IParserFactory;

public enum ParserFactory implements IParserFactory
{
  INSTANCE;
  @Override
  public IParser createParser()
  {
    return new Parser();
  }
}