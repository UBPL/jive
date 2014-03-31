package edu.buffalo.cse.jive.internal.practql.tokenizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import edu.buffalo.cse.jive.practql.tokenizer.IToken;
import edu.buffalo.cse.jive.practql.tokenizer.ITokenizer;
import edu.buffalo.cse.jive.practql.tokenizer.TokenizerException;

final class Tokenizer implements ITokenizer
{
  private static final boolean DEBUG_MODE = false;
  private StringTokenizer tokenizer;
  private List<IToken> tokens;

  @Override
  public List<IToken> tokenize(final String source) throws TokenizerException
  {
    if (Tokenizer.DEBUG_MODE)
    {
      System.err.println("tokenize(String) enter: " + source);
    }
    tokens = new ArrayList<IToken>();
    tokenizer = new StringTokenizer(source, TokenSymbol.TOKENS + TokenWhiteSpace.TOKENS, true);
    try
    {
      while (tokenizer.hasMoreTokens())
      {
        consume(tokenizer.nextToken());
      }
      return Collections.unmodifiableList(tokens);
    }
    finally
    {
      tokens = null;
      tokenizer = null;
      if (Tokenizer.DEBUG_MODE)
      {
        System.err.println("tokenize(String) exit");
      }
    }
  }

  private void addToken(final IToken tk)
  {
    if (Tokenizer.DEBUG_MODE)
    {
      System.err.println("addToken(Token) enter: " + tk);
    }
    tokens.add(tk);
    if (Tokenizer.DEBUG_MODE)
    {
      System.err.println("addToken(Token) exit");
    }
  }

  private void consume(final String tkString) throws TokenizerException
  {
    IToken tk = null;
    // try white space
    tk = TokenWhiteSpace.getToken(tkString.charAt(0));
    // white space indeed
    if (tk != null)
    {
      // ignore the token and proceed
      return;
    }
    // try symbol
    tk = TokenSymbol.getToken(tkString);
    // symbol indeed
    if (tk != null)
    {
      // try quoted string
      if (tk == TokenSymbol.TK_QUOTE)
      {
        // quoted string in the output stream
        scanQString();
      }
      else if (tk == TokenSymbol.TK_LT)
      {
        // < or <= or <> in the output stream
        scanLT_LE_NE();
      }
      else if (tk == TokenSymbol.TK_GT)
      {
        // > or >= in the output stream
        scanGT_GE();
      }
      else if (tk == TokenSymbol.TK_VIRTUAL_1)
      {
        // || in the output stream
        scanConcatenate();
      }
      else
      {
        // symbol in the output stream
        addToken(tk);
      }
      // done
      return;
    }
    // try keyword
    tk = TokenKeyword.getToken(tkString);
    // keyword indeed
    if (tk != null)
    {
      // keyword in the output stream
      addToken(tk);
      return;
    }
    // try boolean
    tk = TokenBoolean.getToken(tkString);
    // boolean indeed
    if (tk != null)
    {
      // boolean in the output stream
      addToken(tk);
      return;
    }
    // try null
    tk = TokenNull.getToken(tkString);
    // null indeed
    if (tk != null)
    {
      // null in the output stream
      addToken(tk);
      return;
    }
    // try integer
    try
    {
      final Integer num = Integer.parseInt(tkString);
      // integer or decimal in the output stream
      scanDecimal(num);
      return;
    }
    catch (final NumberFormatException nfe)
    {
      // proceed to the next parsing attempt
    }
    // try identifier
    boolean isIdent = Character.isLetter(tkString.charAt(0)) || tkString.charAt(0) == '_';
    if (isIdent)
    {
      for (int i = 1; i < tkString.length(); i++)
      {
        isIdent = isIdent
            && (tkString.charAt(i) == '_' || Character.isLetterOrDigit(tkString.charAt(i)));
      }
    }
    // identifier indeed
    if (isIdent)
    {
      // identifier in the output stream
      addToken(new TokenIdentifier(tkString));
      return;
    }
    // token is not one of the recognizable ones
    throw new TokenizerException(String.format("Invalid token: %s", tkString));
  }

  private void scanConcatenate() throws TokenizerException
  {
    String tkString = null;
    // end of tokens-- unterminated concatenation
    if (!tokenizer.hasMoreTokens())
    {
      throw new TokenizerException(
          "Premature end of token stream: unterminated string concatenation.");
    }
    tkString = tokenizer.nextToken();
    final IToken tk = TokenSymbol.getToken(tkString);
    // invalid token-- unterminated concatenation
    if (tk != TokenSymbol.TK_VIRTUAL_1)
    {
      throw new TokenizerException(String.format(
          "Invalid token stream: expected string concatenation (%s) but found (%s%s).",
          TokenSymbol.TK_CONCATENATE.getText(), TokenSymbol.TK_VIRTUAL_1.getText(), tkString));
    }
    addToken(TokenSymbol.TK_CONCATENATE);
  }

  private void scanDecimal(final Integer intPart) throws TokenizerException
  {
    String tkString = null;
    if (!tokenizer.hasMoreTokens())
    {
      addToken(new TokenInteger(intPart));
    }
    else
    {
      tkString = tokenizer.nextToken();
      final IToken tk = TokenSymbol.getToken(tkString);
      // terminal INT or INT followed by something other than TK_DOT
      if (tk != TokenSymbol.TK_DOT || !tokenizer.hasMoreTokens())
      {
        addToken(new TokenInteger(intPart));
        consume(tkString);
      }
      // INT followed by TK_DOT and some token
      else
      {
        tkString = tokenizer.nextToken();
        try
        {
          // INT followed by TK_DOT and a valid decimal part
          final Double num = Double.parseDouble(intPart.toString() + "." + tkString);
          addToken(new TokenDecimal(num));
          return;
        }
        catch (final NumberFormatException nfe)
        {
          // not an integer, proceed to process the token
        }
        // INT followed by TK_DOT and some token
        addToken(new TokenInteger(intPart));
        addToken(TokenSymbol.TK_DOT);
        consume(tkString);
      }
    }
  }

  private void scanGT_GE() throws TokenizerException
  {
    String tkString = null;
    if (!tokenizer.hasMoreTokens())
    {
      addToken(TokenSymbol.TK_GT);
    }
    else
    {
      tkString = tokenizer.nextToken();
      final IToken tk = TokenSymbol.getToken(tkString);
      if (tk == TokenSymbol.TK_EQ)
      {
        addToken(TokenSymbol.TK_GE);
      }
      else
      {
        addToken(TokenSymbol.TK_GT);
        consume(tkString);
      }
    }
  }

  private void scanLT_LE_NE() throws TokenizerException
  {
    String tkString = null;
    if (!tokenizer.hasMoreTokens())
    {
      addToken(TokenSymbol.TK_LT);
    }
    else
    {
      tkString = tokenizer.nextToken();
      final IToken tk = TokenSymbol.getToken(tkString);
      if (tk == TokenSymbol.TK_EQ)
      {
        addToken(TokenSymbol.TK_LE);
      }
      else if (tk == TokenSymbol.TK_GT)
      {
        addToken(TokenSymbol.TK_NE);
      }
      else
      {
        addToken(TokenSymbol.TK_LT);
        consume(tkString);
      }
    }
  }

  private void scanQString() throws TokenizerException
  {
    String result = "";
    String tkString = null;
    boolean quotePending = false;
    while (tokenizer.hasMoreTokens())
    {
      tkString = tokenizer.nextToken();
      IToken tk = null;
      // quote pending from the previous iteration
      if (quotePending)
      {
        tk = TokenSymbol.getToken(tkString);
        // it was an escaped quote, so append the escaped quote to the string
        if (tk == TokenSymbol.TK_QUOTE)
        {
          result += tkString;
          quotePending = false;
          continue;
        }
        // the quote marked the end of the string
        addToken(new TokenQString(result));
        // consume the token string we advanced to
        consume(tkString);
        return;
      }
      // test length 1 tokens for illegal white space and/or closing quote
      if (tkString.length() == 1)
      {
        tk = TokenWhiteSpace.getToken(tkString.charAt(0));
        if (tk != null)
        {
          if (tk == TokenWhiteSpace.TK_CR || tk == TokenWhiteSpace.TK_NL)
          {
            throw new TokenizerException(String.format("Unexpected token within quoted string: %s",
                tk.toString()));
          }
        }
        else
        {
          tk = TokenSymbol.getToken(tkString);
          // symbol token
          if (tk != null)
          {
            // either closing quote or escaped quote within string
            if (tk == TokenSymbol.TK_QUOTE)
            {
              quotePending = true;
              continue;
            }
          }
        }
      }
      // append the token string only if a quote is not pending
      if (!quotePending)
      {
        result += tkString;
      }
    }
    // stream terminated with a pending quote
    if (quotePending)
    {
      // the quote marked the end of the string as well as the token stream
      addToken(new TokenQString(result));
      return;
    }
    // otherwise this is a malformed token stream
    throw new TokenizerException("Premature end of token stream: unterminated quoted string.");
  }
}