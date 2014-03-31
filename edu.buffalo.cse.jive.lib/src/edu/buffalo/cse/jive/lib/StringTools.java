package edu.buffalo.cse.jive.lib;

import java.util.List;
import java.util.Set;

import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IEventModel.IAssignEvent;
import edu.buffalo.cse.jive.model.IEventModel.IExceptionCatchEvent;
import edu.buffalo.cse.jive.model.IEventModel.IExceptionThrowEvent;
import edu.buffalo.cse.jive.model.IEventModel.IFieldReadEvent;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.ILineStepEvent;
import edu.buffalo.cse.jive.model.IEventModel.ILockEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodExitEvent;
import edu.buffalo.cse.jive.model.IEventModel.INewObjectEvent;
import edu.buffalo.cse.jive.model.IEventModel.IThreadStartEvent;
import edu.buffalo.cse.jive.model.IEventModel.ITypeLoadEvent;
import edu.buffalo.cse.jive.model.IEventModel.IVarDeleteEvent;
import edu.buffalo.cse.jive.model.IExecutionModel.IProgramSlice;
import edu.buffalo.cse.jive.model.IModel.ILineValue;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedCall;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedData;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedLazyData;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedLine;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedNode;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedThis;
import edu.buffalo.cse.jive.model.IStaticModel.INode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNodeRef;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;

public class StringTools
{
  public static String contourMemberToString(final IContourMember member)
  {
    return String.format("<name=%s,type=%s,value=%s,index=%d>", member.name(), StringTools
        .getRefSignature(member.schema().type()), member.value(), member.schema().index());
  }

  public static String contourToString(final IContour contour)
  {
    final StringBuffer sb = new StringBuffer(contour.signature());
    sb.append("={");
    sb.append("");
    for (final IContourMember m : contour.members())
    {
      sb.append(StringTools.contourMemberToString(m));
      sb.append(",");
    }
    sb.append("}");
    return sb.toString();
  }

  public static String eventDetails(final IJiveEvent event)
  {
    final StringBuffer buffer = new StringBuffer("");
    StringTools.eventBodyToString(buffer, event);
    return buffer.toString();
  }

  public static String eventToCSV(final IJiveEvent event)
  {
    final StringBuffer buffer = new StringBuffer("");
    EventCSV.eventToCSV(buffer, event);
    return buffer.toString();
  }

  public static String eventToString(final IJiveEvent event)
  {
    final StringBuffer buffer = new StringBuffer("");
    EventString.eventHeaderToString(buffer, event);
    StringTools.eventBodyToString(buffer, event);
    return buffer.toString();
  }

  public static String lineToString(final ILineValue node)
  {
    return node.lineNumber() <= 0 ? "SYSTEM" : String.format("%s:%d", node.file().name(),
        node.lineNumber());
  }

  public static String resolvedCallToString(final IResolvedCall node)
  {
    final StringBuffer buffer = new StringBuffer(String.format(
        "index=%d, kind=%s, qualifierOf=%s, lhs=%s, actual=%s, %s", node.sourceIndex(), "call",
        (node.qualifierOf() == null ? "null" : node.qualifierOf().sourceIndex()), node.isLHS(),
        node.isActual(), node.methodName()));
    buffer.append(buffer.toString().contains("(") ? (node.size() > 0 ? ", " : "") : "(");
    for (int i = 0; i < node.size(); i++)
    {
      StringTools.appendFlat(node, i, buffer);
      if (i < node.size() - 1)
      {
        buffer.append(", ");
      }
    }
    buffer.append(")");
    return buffer.toString();
  }

  public static String resolvedDataToString(final IResolvedData node)
  {
    if (node.data().kind() == NodeKind.NK_FIELD)
    {
      return String
          .format(
              "index=%d, kind=field%s, qualifierOf=%s, lhs=%s, actual=%s, name=%s, id=%d, declaration=%s",
              node.sourceIndex(), (node.isDef() ? " def" : ""),
              (node.qualifierOf() == null ? "null" : node.qualifierOf().sourceIndex()), node
                  .isLHS(), node.isActual(), node.name(), node.data().id(), node.data().parent()
                  .name());
    }
    return String.format("index=%d, kind=%s%s, qualifierOf=%s, lhs=%s, actual=%s, name=%s, id=%d",
        node.sourceIndex(),
        (node.data().modifiers().contains(NodeModifier.NM_ARGUMENT) ? "argument" : "variable"),
        (node.isDef() ? " def" : ""), (node.qualifierOf() == null ? "null" : node.qualifierOf()
            .sourceIndex()), node.isLHS(), node.isActual(), node.name(), node.data().id());
  }

  public static String resolvedLazyFieldToString(final IResolvedLazyData node)
  {
    return String.format(
        "index=%d, kind=lazy field%s, qualifierOf=%s, lhs=%s, actual=%s, name=%s, type=%s", node
            .sourceIndex(), (node.isDef() ? " def" : ""), (node.qualifierOf() == null ? "null"
            : node.qualifierOf().sourceIndex()), node.isLHS(), node.isActual(), node.name(), node
            .type().name());
  }

  public static String resolvedLineToString(final IResolvedLine line)
  {
    final StringBuffer buffer = new StringBuffer(StringTools.header(line));
    buffer.append(":");
    buffer.append("\n  definitions => ");
    if (line.definitions().isEmpty())
    {
      buffer.append("{}");
    }
    else
    {
      StringTools.toVarsList(buffer, line.definitions());
    }
    buffer.append("\n  uses => ");
    if (line.uses().isEmpty())
    {
      buffer.append("{}");
    }
    else
    {
      StringTools.toUsesList(buffer, line.uses());
    }
    final List<IResolvedLine> inheritedSet = TypeTools.newArrayList();
    IResolvedLine inherited = line.parent();
    while (inherited != null)
    {
      inheritedSet.add(inherited);
      inherited = inherited.parent();
    }
    buffer.append("\n  inherited-dependences => ");
    if (inheritedSet.isEmpty())
    {
      buffer.append("{}");
    }
    else
    {
      StringTools.toDependenceList(buffer, inheritedSet);
    }
    buffer.append("\n  propagated-dependences => ");
    if (line.jumpDependences().isEmpty())
    {
      buffer.append("{}");
    }
    else
    {
      StringTools.toDependenceList(buffer, line.jumpDependences());
    }
    return buffer.toString();
  }

  public static String resolvedThisToString(final IResolvedThis node)
  {
    return String.format("index=%d, kind=this, qualifierOf=%s, lhs=%s, actual=%s, type=%s",
        node.sourceIndex(),
        (node.qualifierOf() == null ? "null" : node.qualifierOf().sourceIndex()), node.isLHS(),
        node.isActual(), node.type().name());
  }

  public static String sliceToString(final IProgramSlice slice)
  {
    final StringBuffer buffer = new StringBuffer("");
    SliceString.sliceToString(buffer, slice);
    return buffer.toString();
  }

  private static String appendFlat(final IResolvedCall call, final int index,
      final StringBuffer buffer)
  {
    buffer.append("[vars: {");
    int i = 0;
    for (final IResolvedNode rv : call.uses(index))
    {
      if (rv instanceof IResolvedData)
      {
        i++;
        buffer.append(rv.toString());
        if (i != call.uses(index).size())
        {
          buffer.append(", ");
        }
      }
    }
    buffer.append("}, calls: {");
    i = 0;
    for (final IResolvedNode rc : call.uses(index))
    {
      if (rc instanceof IResolvedCall)
      {
        i++;
        buffer.append(rc.toString());
        if (i != call.uses(index).size())
        {
          buffer.append(", ");
        }
      }
    }
    buffer.append("}]");
    return buffer.toString();
  }

  /**
   * TODO: reduce the cyclomatic complexity.
   */
  private static void eventBodyToString(final StringBuffer buffer, final IJiveEvent event)
  {
    buffer.append("");
    if (event instanceof IFieldReadEvent)
    {
      EventString.fieldReadToString(buffer, (IFieldReadEvent) event);
    }
    else if (event instanceof IAssignEvent)
    {
      EventString.assignToString(buffer, (IAssignEvent) event);
    }
    else if (event instanceof IExceptionCatchEvent)
    {
      EventString.exceptionCatchToString(buffer, (IExceptionCatchEvent) event);
    }
    else if (event instanceof IExceptionThrowEvent)
    {
      EventString.exceptionThrowToString(buffer, (IExceptionThrowEvent) event);
    }
    else if (event instanceof ILineStepEvent)
    {
      EventString.lineStepToString(buffer, (ILineStepEvent) event);
    }
    else if (event instanceof ILockEvent)
    {
      EventString.lockToString(buffer, (ILockEvent) event);
    }
    else if (event instanceof IMethodCallEvent)
    {
      EventString.methodCallToString(buffer, (IMethodCallEvent) event);
    }
    else if (event instanceof IMethodExitEvent)
    {
      EventString.methodExitToString(buffer, (IMethodExitEvent) event);
    }
    else if (event instanceof INewObjectEvent)
    {
      EventString.newObjectToString(buffer, (INewObjectEvent) event);
    }
    else if (event instanceof IThreadStartEvent)
    {
      EventString.threadStartToString(buffer, (IThreadStartEvent) event);
    }
    else if (event instanceof ITypeLoadEvent)
    {
      EventString.typeLoadToString(buffer, (ITypeLoadEvent) event);
    }
    else if (event instanceof IVarDeleteEvent)
    {
      EventString.varDeleteToString(buffer, (IVarDeleteEvent) event);
    }
  }

  private static String header(final IResolvedLine line)
  {
    final StringBuffer buffer = new StringBuffer(line.kind().toString()).append(" @").append(
        line.lineNumber());
    return buffer.toString();
  }

  private static void toDependenceList(final StringBuffer buffer, final List<IResolvedLine> deps)
  {
    int i = 0;
    buffer.append("{");
    for (final IResolvedLine rd : deps)
    {
      i++;
      buffer.append(StringTools.header(rd));
      if (i != deps.size())
      {
        buffer.append(", ");
      }
    }
    buffer.append("}");
  }

  private static void toUsesList(final StringBuffer buffer, final List<IResolvedNode> uses)
  {
    buffer.append("{\n");
    for (int i = 0; i < uses.size(); i++)
    {
      buffer.append("    #").append(i).append(": ").append(uses.get(i).toString()).append("\n");
    }
    buffer.append("  }");
  }

  private static void toVarsList(final StringBuffer buffer, final List<IResolvedData> uses)
  {
    int i = 0;
    buffer.append("{");
    for (final IResolvedData rv : uses)
    {
      i++;
      buffer.append(rv.toString());
      if (i != uses.size())
      {
        buffer.append(", ");
      }
    }
    buffer.append("}");
  }

  private static final class EventCSV
  {
    private static String escapeQuotes(final String value)
    {
      return value.replace("\"", "\"\"");
    }

    private static void eventToCSV(final StringBuffer buffer, final IJiveEvent event)
    {
      final StringBuffer details = new StringBuffer("");
      StringTools.eventBodyToString(details, event);
      buffer.append(String.format("\"%s\", \"%d\", \"%s\", \"%s\", \"%s\"\n",
          event.thread().name(), event.eventId(), event.line().toString(),
          event.kind().eventName(), EventCSV.escapeQuotes(details.toString())));
    }
  }

  private static final class EventString
  {
    private static void assignToString(final StringBuffer buffer, final IAssignEvent event)
    {
      buffer.append("context=").append(event.contour().signature());
      buffer.append(", ").append(event.member().name());
      buffer.append("=").append(event.newValue().toString());
    }

    private static void eventHeaderToString(final StringBuffer buffer, final IJiveEvent event)
    {
      // buffer.append(event.line().toString()).append(" ");
      buffer.append(event.kind());
      buffer.append(" [id=").append(event.eventId()).append("]");
    }

    private static void exceptionCatchToString(final StringBuffer buffer,
        final IExceptionCatchEvent event)
    {
      buffer.append("exception=").append(event.exception().toString());
      buffer.append(", catcher=");
      if (event.contour() != null)
      {
        buffer.append(event.contour().signature());
      }
      else
      {
        buffer.append("<uncaught>");
      }
      buffer.append(", variable=");
      if (event.member() != null)
      {
        buffer.append(event.member().name());
      }
      else
      {
        buffer.append("<unknown>");
      }
    }

    private static void exceptionThrowToString(final StringBuffer buffer,
        final IExceptionThrowEvent event)
    {
      buffer.append("exception=").append(event.exception().toString());
      buffer.append(", thrower=").append(event.thrower().toString());
      buffer.append(", framePopped=").append(event.framePopped());
    }

    private static void fieldReadToString(final StringBuffer buffer, final IFieldReadEvent event)
    {
      buffer.append("context=").append(event.contour().signature());
      buffer.append(", ").append(event.member().name());
    }

    private static void lineStepToString(final StringBuffer buffer, final ILineStepEvent event)
    {
      if (event.line().lineNumber() > 0)
      {
        buffer.append("file=").append(event.line().file().name());
        buffer.append(", line=").append(event.line().lineNumber());
      }
      else
      {
        buffer.append("<source unavailable>");
      }
    }

    private static void lockToString(final StringBuffer buffer, final ILockEvent event)
    {
      buffer.append("operation=").append(event.lockOperation().toString());
      buffer.append(", lock=").append(event.lockDescription());
    }

    private static void methodCallToString(final StringBuffer buffer, final IMethodCallEvent event)
    {
      buffer.append("caller=").append(event.caller().toString());
      buffer.append(", target=").append(event.target().toString());
    }

    private static void methodExitToString(final StringBuffer buffer, final IMethodExitEvent event)
    {
      buffer.append("returner=").append(event.returnContext().toString());
      buffer.append(", value=");
      if (event.returnValue() != null && !event.returnValue().isUninitialized())
      {
        buffer.append(event.returnValue().toString());
      }
      else
      {
        buffer.append("<void>");
      }
    }

    private static void newObjectToString(final StringBuffer buffer, final INewObjectEvent event)
    {
      buffer.append("object=").append(event.newContour().signature());
    }

    private static void threadStartToString(final StringBuffer buffer, final IThreadStartEvent event)
    {
      buffer.append(event.thread().toString());
    }

    private static void typeLoadToString(final StringBuffer buffer, final ITypeLoadEvent event)
    {
      buffer.append("class=").append(event.newContour().signature());
    }

    private static void varDeleteToString(final StringBuffer buffer, final IVarDeleteEvent event)
    {
      buffer.append("method=").append(event.contour().signature());
      buffer.append(" ").append(event.member().toString());
    }
  }

  private static final class SliceString
  {
    private static void addLine(final Set<ILineValue> lines, final ILineValue line)
    {
      if (line != null && line.lineNumber() > 0)
      {
        lines.add(line);
      }
    }

    private static void printContexts(final StringBuffer buffer, final IProgramSlice slice)
    {
      buffer.append("\nRelevant Contexts:\n==================\n");
      for (final IContextContour context : slice.contexts())
      {
        buffer.append(context.signature()).append("\n");
      }
    }

    private static void printEvents(final StringBuffer buffer, final IProgramSlice slice,
        final Set<ILineValue> lines)
    {
      buffer.append("\nRelevant Events:\n================\n");
      for (final IJiveEvent event : slice.events())
      {
        buffer.append(StringTools.eventToString(event)).append("\n");
        // add the line to the slice (for printing purposes only)
        SliceString.addLine(lines, event.line());
      }
    }

    private static void printLines(final StringBuffer buffer, final Set<ILineValue> lines)
    {
      buffer.append(("\nRelevant Lines:\n===============\n"));
      for (final ILineValue line : lines)
      {
        buffer.append(line).append("\n");
      }
    }

    private static void printMembers(final StringBuffer buffer, final IProgramSlice slice)
    {
      buffer.append("\nRelevant Members:\n=================\n");
      for (final IContourMember field : slice.members())
      {
        buffer.append(field.toString()).append("\n");
      }
    }

    private static void printMethods(final StringBuffer buffer, final IProgramSlice slice)
    {
      buffer.append("\nRelevant Methods:\n=================\n");
      for (final IMethodContour method : slice.methods())
      {
        buffer.append(method.signature()).append("\n");
      }
    }

    private static void sliceToString(final StringBuffer buffer, final IProgramSlice slice)
    {
      final Set<ILineValue> lines = TypeTools.newLinkedHashSet();
      SliceString.printContexts(buffer, slice);
      SliceString.printMembers(buffer, slice);
      SliceString.printMethods(buffer, slice);
      SliceString.printEvents(buffer, slice, lines);
      SliceString.printLines(buffer, lines);
    }
  }

  private static String getNodeSignature(final INode node)
  {
    if (node == null)
    {
      return "<>";
    }
    final StringBuffer buffer = new StringBuffer(node.name());
    buffer.append(" (id=").append(node.id()).append(")");
    return buffer.toString();
  }

  private static String getRefSignature(final ITypeNodeRef ref)
  {
    if (ref == null)
    {
      return "<>";
    }
    if (ref.node() == null)
    {
      return ref.name() + " (unresolved)";
    }
    return getNodeSignature(ref.node());
  }
}
