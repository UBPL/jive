package edu.buffalo.cse.jive.console.command;

import edu.buffalo.cse.jive.command.JiveCommand;

public class JiveStepCommand extends JiveCommand
{
  private static final String CMD_NAME = "step";
  private static final String CMD_HELP = "Steps the debug target the given number of steps, which is either a positive or negative integer, or the symbolic values BOF and EOF.";
  private static final String CMD_SYNTAX = CMD_NAME + " <tid> <delta>";

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