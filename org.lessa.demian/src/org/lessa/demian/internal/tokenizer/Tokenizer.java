package org.lessa.demian.internal.tokenizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.lessa.demian.tokenizer.IToken;
import org.lessa.demian.tokenizer.ITokenizer;
import org.lessa.demian.tokenizer.TokenizerException;

import org.lessa.demian.internal.tokenizer.TokenInteger;

import org.lessa.demian.internal.tokenizer.TokenSymbol;

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
      // symbol in the output stream
      addToken(tk);
      return;
    }
    // try integer
    try
    {
      // integer in the output stream
      addToken(new TokenInteger(Integer.parseInt(tkString)));
      return;
    }
    catch (final NumberFormatException nfe)
    {
      // proceed to the next parsing attempt
    }
    // try identifier
    boolean isIdent = Character.isLetter(tkString.charAt(0));
    if (isIdent)
    {
      for (int i = 1; i < tkString.length(); i++)
      {
        isIdent = isIdent && Character.isLetter(tkString.charAt(i));
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
}