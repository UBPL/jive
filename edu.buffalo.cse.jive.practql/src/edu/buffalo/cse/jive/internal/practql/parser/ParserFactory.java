package edu.buffalo.cse.jive.internal.practql.parser;

import edu.buffalo.cse.jive.practql.parser.IParser;
import edu.buffalo.cse.jive.practql.parser.IParserFactory;

public enum ParserFactory implements IParserFactory
{
  INSTANCE;
  @Override
  public IParser createParser()
  {
    return new Parser();
  }
}