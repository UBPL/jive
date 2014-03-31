package edu.buffalo.cse.jive.console;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import edu.buffalo.cse.jive.command.IJiveCommand;
import edu.buffalo.cse.jive.command.JiveCommandPlugin;

public class JiveCommandProvider implements CommandProvider
{
  public Object _jive(final CommandInterpreter ci)
  {
    String argument = ci.nextArgument();
    if (argument == null)
    {
      invalid(ci, argument);
      return null;
    }
    IJiveCommand command = JiveCommandPlugin.getDefault().getJiveCommands().get(argument);
    if (command != null)
    {
      List<String> args = new ArrayList<String>();
      argument = ci.nextArgument();
      while (argument != null)
      {
        args.add(argument);
        argument = ci.nextArgument();
      }
      ci.println(command.handle(args));
      return null;
    }
    invalid(ci, argument);
    return null;
  }

  @Override
  public String getHelp()
  {
    final StringBuffer buffer = new StringBuffer("---JIVE commands---\n");
    for (IJiveCommand command : JiveCommandPlugin.getDefault().getJiveCommands().values())
    {
      buffer.append(String.format("\tjive %s - %s\n", command.getSyntax(), command.getHelp()));
    }
    return buffer.toString();
  }

  private void invalid(final CommandInterpreter ci, final String value)
  {
    ci.println(String
        .format(
            "'jive %s' is an unknown or invalid command. Usr 'jive help' to learn which commands are available and their syntax.",
            value));
  }
}