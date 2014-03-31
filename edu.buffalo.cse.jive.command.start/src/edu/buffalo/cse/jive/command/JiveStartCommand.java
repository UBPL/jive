package edu.buffalo.cse.jive.command;

import java.util.List;

import edu.buffalo.cse.jive.command.JiveCommand;
import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;

public class JiveStartCommand extends JiveCommand
{
  private static final String CMD_NAME = "start";
  private static final String CMD_HELP = "Starts Jive on the given target if it is not already running.";
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
    if (startTarget(targetId))
    {
      return "Snapshot created and target started.";
    }
    return "Target unavailable or already started.";
  }

  private boolean startTarget(final int targetId)
  {
    final IJiveDebugTarget target = getTarget(targetId);
    if (target == null || (target.isStarted() && !target.isStopped()))
    {
      return false;
    }
    // start this target
    target.start();
    // signal that the target is started
    return true;
  }
}