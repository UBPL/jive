package edu.buffalo.cse.jive.console.command;

import edu.buffalo.cse.jive.command.JiveCommand;

public class JiveStatisticsCommand extends JiveCommand
{
  private static final String CMD_NAME = "statistics";
  private static final String CMD_HELP = "Shows available statistics for the current debug target, including number of threads, start/end events for each thread, number of events and model elements created.";
  private static final String CMD_SYNTAX = CMD_NAME + "<tid>";

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