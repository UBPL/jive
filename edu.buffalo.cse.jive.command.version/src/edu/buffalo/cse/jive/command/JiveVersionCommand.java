package edu.buffalo.cse.jive.command;

import java.util.List;

import edu.buffalo.cse.jive.command.JiveCommand;

public class JiveVersionCommand extends JiveCommand
{
  private static final String CMD_NAME = "version";
  private static final String CMD_HELP = "Shows Jive's version information.";
  private static final String CMD_SYNTAX = CMD_NAME;

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

  @Override
  public String handle(final List<String> args)
  {
    return "Jive Platform version 1.9.29b.\n";
  }
}