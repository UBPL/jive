package edu.buffalo.cse.jive.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.buffalo.cse.jive.command.JiveCommand;
import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;

public class JiveStatusCommand extends JiveCommand
{
  private static final String CMD_NAME = "status";
  private static final String CMD_HELP = "Shows the current debug target's status.";
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
    if (args.size() != 0)
    {
      return invalidArguments(0, args.size());
    }
    final List<String> targets = targetList();
    // header only
    if (targets.size() == 1)
    {
      return "No Jive debug targets found.\n";
    }
    // header and targets
    String result = "";
    for (final String target : targets)
    {
      result += target + "\n";
    }
    return result;
  }

  private List<String> targetList()
  {
    final Collection<IJiveDebugTarget> targets = JiveLaunchPlugin.getDefault().getLaunchManager()
        .lookupTargets();
    final List<String> result = new ArrayList<String>();
    result.add("  TID   STATUS    CLASS");
    final IJiveDebugTarget active = JiveLaunchPlugin.getDefault().getLaunchManager().activeTarget();
    for (final IJiveDebugTarget t : targets)
    {
      result.add(String.format("%5d%s  %s   %s", t.targetId(), t == active ? "*" : " ",
          (t.isStopped() ? "stopped" : t.isStarted() ? "started" : "manual "), t.getName()));
    }
    return result;
  }
}