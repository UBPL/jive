package edu.buffalo.cse.jive.console.command;

import edu.buffalo.cse.jive.command.JiveCommand;

public class JiveDumpCommand extends JiveCommand
{
  private static final String CMD_NAME = "dump";
  private static final String CMD_HELP = "Displays detailed information about the specified element in textual form. The 'kind' can be EVENT, CONTOUR, or NODE. The 'id' is the integer identifier of the element.";
  private static final String CMD_SYNTAX = CMD_NAME + " <tid> <kind> <id>";

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