package edu.buffalo.cse.jive.command;

import java.util.List;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;

public abstract class JiveCommand implements IJiveCommand
{
  protected String invalidArguments(int expected, int actual)
  {
    return String.format(
        "Unexpected argument list. 'jive %s' expects %d arguments but %d were passed.\n",
        getCommand(), expected, actual);
  }

  protected String invalidIntegerArgument(String arg)
  {
    return String.format("Invalid argument passed to 'jive %s': '%s' is not an integer.\n",
        getCommand(), arg);
  }

  protected Integer toInteger(String arg)
  {
    try
    {
      return Integer.valueOf(arg);
    }
    catch (final NumberFormatException nfe)
    {
      return null;
    }
  }

  @Override
  public String handle(final List<String> args)
  {
    return String.format("Command 'jive %s' is not yet implemented.\n", getCommand());
  }

  protected IJiveDebugTarget getTarget(final int targetId)
  {
    return JiveLaunchPlugin.getDefault().getLaunchManager().lookupTarget(targetId);
  }
}