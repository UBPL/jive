package edu.buffalo.cse.jive.command;

import java.util.List;

import edu.buffalo.cse.jive.command.JiveCommand;
import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IExecutionModel.IProgramSlice;

public class JiveSliceCommand extends JiveCommand
{
  private static final String CMD_NAME = "slice";
  private static final String CMD_HELP = "Computes the kind of dynamic slice specified over the execution trace, starting from the given eventId, which must represent an assignment event. If CLEAR is used instead, any active slices are removed from the model.";
  private static final String CMD_SYNTAX = CMD_NAME + " <tid> <eventId | CLEAR>";

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
    if (args.size() != 2)
    {
      return invalidArguments(2, args.size());
    }
    // targetId
    Integer targetId = toInteger(args.get(0));
    if (targetId == null)
    {
      invalidIntegerArgument(args.get(0));
    }
    // target
    final IJiveDebugTarget target = getTarget(targetId);
    if (target == null)
    {
      return invalidTarget(targetId);
    }
    // eventId
    Integer eventId = toInteger(args.get(1));
    // the next argument was indeed an integer
    if (eventId != null)
    {
      final IProgramSlice slice = target.model().sliceView().computeSlice(eventId);
      if (slice == null)
      {
        return "Invalid eventId for slice or no slice produced.";
      }
      else
      {
        return slice.toString();
      }
    }
    // the next argument was the CLEAR keyword
    else if (args.get(1).equalsIgnoreCase("CLEAR"))
    {
      target.model().sliceView().clearSlice();
      return "Slice cleared.";
    }
    return "Invalid slice type or no slice produced.";
  }

  private String invalidTarget(final Integer targetId)
  {
    return String.format("Could not find a target for 'jive slice %d'.\n", targetId);
  }
}