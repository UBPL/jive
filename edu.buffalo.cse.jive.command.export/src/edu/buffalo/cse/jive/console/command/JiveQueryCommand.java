package edu.buffalo.cse.jive.console.command;

import edu.buffalo.cse.jive.command.JiveCommand;

public class JiveQueryCommand extends JiveCommand
{
  private static final String CMD_NAME = "query";
  private static final String CMD_HELP = "Evaluates the query and displays the results on both the search answers window and the sequence diagram.";
  private static final String CMD_SYNTAX = CMD_NAME + " <tid> <query string>";

  @Override
  public String getCommand()
  {
    return CMD_NAME;
  }

  @Override
  public String getHelp()
  {
    return CMD_HELP;
  }

  @Override
  public String getSyntax()
  {
    return CMD_SYNTAX;
  }
}