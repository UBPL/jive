package edu.buffalo.cse.jive.command;

import java.util.List;

public interface IJiveCommand
{
  public String getCommand();

  public String getHelp();

  public String getSyntax();

  public String handle(final List<String> args);
}
