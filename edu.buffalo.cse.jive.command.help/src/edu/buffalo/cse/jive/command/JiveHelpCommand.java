package edu.buffalo.cse.jive.command;

import java.util.List;

import edu.buffalo.cse.jive.command.IJiveCommand;
import edu.buffalo.cse.jive.command.JiveCommand;
import edu.buffalo.cse.jive.command.JiveCommandPlugin;

public class JiveHelpCommand extends JiveCommand
{
  private static final String CMD_NAME = "help";
  private static final String CMD_HELP = "If no command is specified, shows the help for all Jive-specific commands, otherwise shows the help only for the specified command.";
  private static final String CMD_SYNTAX = CMD_NAME + " [command]";

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
    if (args.size() > 1)
    {
      return invalidArguments(1, args.size());
    }
    if (args.size() == 0)
    {
      final StringBuffer buffer = new StringBuffer("---JIVE commands---\n");
      for (IJiveCommand command : JiveCommandPlugin.getDefault().getJiveCommands().values())
      {
        buffer.append(String.format("   jive %s - %s\n", command.getSyntax(), command.getHelp()));
      }
      return buffer.toString();
    }
    IJiveCommand command = JiveCommandPlugin.getDefault().getJiveCommands().get(args.get(0));
    if (command != null)
    {
      final String commandSyntax = "jive " + command.getSyntax();
      final String commandHelp = command.getHelp();
      return commandSyntax + " - " + commandHelp + "\n";
    }
    return "Unknown Jive command: '" + args.get(0) + "'.\n";
  }
}