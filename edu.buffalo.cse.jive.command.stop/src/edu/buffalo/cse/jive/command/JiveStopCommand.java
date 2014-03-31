package edu.buffalo.cse.jive.command;

import java.util.List;

import edu.buffalo.cse.jive.command.JiveCommand;
import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;

public class JiveStopCommand extends JiveCommand
{
  private static final String CMD_NAME = "stop";
  private static final String CMD_HELP = "Stops Jive if it is running on the currently active debug target.";
  private static final String CMD_SYNTAX = CMD_NAME + " <tid>";

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
    if (args.size() != 1)
    {
      return invalidArguments(1, args.size());
    }
    final Integer targetId = toInteger(args.get(0));
    if (targetId == null)
    {
      return invalidIntegerArgument(args.get(0));
    }
    if (stopTarget(targetId))
    {
      return "Target stopped.";
    }
    return "Target unavailable or already stopped.";
  }

  private boolean stopTarget(final int targetId)
  {
    final IJiveDebugTarget target = getTarget(targetId);
    if (target == null || target.isStopped())
    {
      return false;
    }
    // stop this target
    target.stop();
    // signal that the target is stopped
    return true;
  }
}