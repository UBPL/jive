package edu.buffalo.cse.jive.internal.practql.parser;

import java.util.List;

import edu.buffalo.cse.jive.internal.practql.tokenizer.TokenizerFactory;
import edu.buffalo.cse.jive.practql.IFactory;
import edu.buffalo.cse.jive.practql.expression.query.IQueryExpression;
import edu.buffalo.cse.jive.practql.parser.IParser;
import edu.buffalo.cse.jive.practql.parser.ParserException;
import edu.buffalo.cse.jive.practql.schema.IDatabaseSchema;
import edu.buffalo.cse.jive.practql.schema.SchemaException;
import edu.buffalo.cse.jive.practql.tokenizer.IToken;
import edu.buffalo.cse.jive.practql.tokenizer.ITokenizer;
import edu.buffalo.cse.jive.practql.tokenizer.TokenClass;
import edu.buffalo.cse.jive.practql.tokenizer.TokenizerException;

abstract class AbstractParser implements IParser
{
  protected static final boolean DEBUG_MODE = true;
  private static final String ERR_INVALID_POSITION = "Parser error: repositioning beyond token stream boundaries (%d).";
  private static final String ERR_TOKENIZER = "Syntax error while tokenizing source string.";
  private static final String ERR_UNEXPECTED_EOS = "Syntax error. Expected some token  but found end of token stream instad.";
  private static final String ERR_UNEXPECTED_TOKEN = "Syntax error. Expected token '%s' but found token '%s' instead.";
  private static final String ERR_UNEXPECTED_TOKEN_CLASS = "Syntax error. Expected token of type '%s' but found token '%s' instead.";
  private static final String ERR_UNEXPECTED_TOKENS = "Syntax error. Expected end of token stream but found %d tokens instead.";
  protected IDatabaseSchema dbSchema;
  private int tk;
  protected IToken token;
  protected List<IToken> tokens;
  private final IFactory factory;

  AbstractParser(final IFactory factory)
  {
    this.factory = factory;
  }

  @Override
  public final IQueryExpression parse(final String source, final IDatabaseSchema schema)
      throws ParserException
  {
    tokenize(source);
    beforeParse();
    try
    {
      dbSchema = factory.schemaFactory().copyDatabaseSchema(schema);
      return doParse();
    }
    catch (final SchemaException e)
    {
      throw new ParserException("Error creating a local copy of the database schema.", e);
    }
    finally
    {
      afterParse();
    }
  }

  private void tokenize(final String source) throws ParserException
  {
    final ITokenizer t = TokenizerFactory.INSTANCE.createTokenizer();
    try
    {
      resetTokenStream();
      tokens = t.tokenize(source);
      advance();
    }
    catch (final TokenizerException te)
    {
      die(AbstractParser.ERR_TOKENIZER, te);
    }
  }

  protected boolean advance()
  {
    // token before advancing
    if (tk > -1)
    {
      token = tokens.get(tk);
    }
    else
    {
      token = null;
    }
    // updated pointer into the token list
    tk += 1;
    return true;
  }

  protected boolean advanceTo(final IToken token)
  {
    while (testToken() && !isToken(token) && advance())
    {
      // no-op
    }
    return isToken(token) && advance();
  }

  protected void afterParse()
  {
    resetTokenStream();
  }

  protected void beforeParse()
  {
  }

  protected boolean check(final IToken token) throws ParserException
  {
    return checkToken()
        && (test(token) || die(AbstractParser.ERR_UNEXPECTED_TOKEN, token.getText(), tokens.get(tk)
            .getText()));
  }

  protected boolean check(final TokenClass tokenClass) throws ParserException
  {
    return checkToken()
        && (test(tokenClass) || die(AbstractParser.ERR_UNEXPECTED_TOKEN_CLASS,
            tokenClass.getText(), tokens.get(tk).getText()));
  }

  protected boolean checkEOS() throws ParserException
  {
    return (tk == tokens.size())
        || die(AbstractParser.ERR_UNEXPECTED_TOKENS, tokens.size() - tk - 1);
  }

  protected boolean checkToken() throws ParserException
  {
    return testToken() || die(AbstractParser.ERR_UNEXPECTED_EOS);
  }

  protected void debug(final String message)
  {
    if (AbstractParser.DEBUG_MODE)
    {
      System.err.println(message);
    }
  }

  protected void debug(final String message, final Object... args)
  {
    if (AbstractParser.DEBUG_MODE)
    {
      System.err.format(message, args);
    }
  }

  protected boolean die(final String message) throws ParserException
  {
    debug("Dying at token (id: %d, value: %s)\n", tk, tokens.get(tk));
    throw new ParserException(message);
  }

  protected boolean die(final String message, final Object... args) throws ParserException
  {
    debug("Dying at token (id: %d, value: %s)\n", tk, tokens.get(tk));
    throw new ParserException(String.format(message, args));
  }

  protected boolean die(final String message, final Throwable cause) throws ParserException
  {
    debug("Dying at token (id: %d, value: %s)\n", tk, tokens.get(tk));
    throw new ParserException(message, cause);
  }

  protected abstract IQueryExpression doParse() throws ParserException;

  protected boolean isToken(final IToken token)
  {
    return testToken() && tokens.get(tk).equals(token);
  }

  protected boolean isTokenClass(final TokenClass tokenClass)
  {
    return testToken() && tokens.get(tk).getTokenClass() == tokenClass;
  }

  protected int mark()
  {
    return tk;
  }

  protected void moveTo(final int position) throws ParserException
  {
    if (position < 0 || position >= tokens.size())
    {
      die(AbstractParser.ERR_INVALID_POSITION, position);
    }
    tk = position;
  }

  protected void resetTokenStream()
  {
    tokens = null;
    tk = -1;
  }

  protected boolean test(final IToken token)
  {
    return isToken(token) && advance();
  }

  protected boolean test(final TokenClass tokenClass)
  {
    return isTokenClass(tokenClass) && advance();
  }

  protected boolean testToken()
  {
    return (tk > -1 && tk < tokens.size());
  }
}
