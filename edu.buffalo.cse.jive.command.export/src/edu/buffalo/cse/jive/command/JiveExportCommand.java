package edu.buffalo.cse.jive.command;

import java.util.List;
import edu.buffalo.cse.jive.command.JiveCommand;
import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.exporter.IJiveExporter;
import edu.buffalo.cse.jive.exporter.JiveExporterPlugin;

public class JiveExportCommand extends JiveCommand
{
  private static final String CMD_NAME = "export";
  private static final String CMD_HELP = "Exports the specified model kind (one of: AST, MDG, TRACE) to the specified target. If the target is a file, its extension determines the output format-- XML (*.xml) or textual (any other extension); for TRACE, a JDBC url is also supported (the respective JDBC driver must be in the class path).";
  private static final String CMD_SYNTAX = CMD_NAME + " <tid> <kind> <target>";

  private String invalidExporter(final String element, final Integer targetId)
  {
    return String.format("Could not find an exporter for 'jive export %s'.\n", targetId, element);
  }

  private String invalidTarget(final String element, final Integer targetId)
  {
    return String.format("Could not find a target for 'jive export %d %s'.\n", targetId, element);
  }

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
    if (args.size() != 3)
    {
      return invalidArguments(3, args.size());
    }
    // targetId
    Integer targetId = toInteger(args.get(0));
    if (targetId == null)
    {
      return invalidIntegerArgument(args.get(0));
    }
    // element to export
    final String element = args.get(1).toUpperCase();
    // path to export
    final String path = args.get(2);
    // target
    final IJiveDebugTarget target = getTarget(targetId);
    if (target == null)
    {
      return invalidTarget(element, targetId);
    }
    // element
    JiveExporterPlugin jep = JiveExporterPlugin.getDefault();
    IJiveExporter je = jep.getJiveExporters().get(element);
    if (je == null)
    {
      return invalidExporter(element, targetId);
    }
    // export
    if (je.export(target.model(), path, null))
    {
      return String.format("%s export completed successfully.\n", element);
    }
    return "Unable to export " + args.get(1) + " to '" + path + "'.\n";
  }
}