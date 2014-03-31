package edu.buffalo.cse.jive.exporter.mdg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.exporter.XML;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IMethodDependenceGraph;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedCall;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedData;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedLine;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedNode;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedThis;
import edu.buffalo.cse.jive.model.IStaticModel.IFileNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.IRootNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.model.IStaticModel.NodeOrigin;

public class XMLExporter
{
  public static String export(final IRootNode root)
  {
    final Map<String, String> attributes = TypeTools.newLinkedHashMap();
    final StringBuffer buffer = new StringBuffer("");
    buffer.append(XML.tagOpen("jdg"));
    for (final IFileNode fn : root.files())
    {
      attributes.clear();
      attributes.put("id", String.valueOf(fn.id()));
      attributes.put("name", fn.name());
      buffer.append(XML.tagOpen("file", attributes));
      for (final ITypeNode tn : fn.types())
      {
        typeMDGsToXML(buffer, tn);
      }
      buffer.append(XML.tagClose("file"));
    }
    buffer.append(XML.tagClose("jdg"));
    return buffer.toString();
  }

  private static void typeMDGsToXML(final StringBuffer buffer, final ITypeNode tn)
  {
    final Map<String, String> attributes = TypeTools.newLinkedHashMap();
    attributes.put("id", String.valueOf(tn.id()));
    attributes.put("name", tn.name());
    buffer.append(XML.tagOpen("type", attributes));
    if (tn.origin() == NodeOrigin.NO_AST)
    {
      for (final IMethodNode method : tn.methodMembers().values())
      {
        if (method.origin() == NodeOrigin.NO_AST)
        {
          mdgToXML(buffer, method);
        }
      }
    }
    // nested member types
    for (final ITypeNode ntn : tn.typeMembers())
    {
      typeMDGsToXML(buffer, ntn);
    }
    buffer.append(XML.tagClose("type"));
  }

  @SuppressWarnings("unchecked")
  private static void mdgToXML(final StringBuffer buffer, final IMethodNode method)
  {
    final Map<String, String> attributes = TypeTools.newLinkedHashMap();
    attributes.put("id", String.valueOf(method.id()));
    attributes.put("method", method.name());
    buffer.append(XML.tagOpen("mdg", attributes));
    final IMethodDependenceGraph mdg = method.getDependenceGraph();
    final List<Integer> allLines = mdg != null ? new ArrayList<Integer>(mdg.dependenceMap()
        .keySet()) : (List<Integer>) Collections.EMPTY_LIST;
    Collections.sort(allLines);
    for (final Integer key : allLines)
    {
      resolvedLineToXML(buffer, mdg.dependenceMap().get(key));
    }
    buffer.append(XML.tagClose("mdg"));
  }

  public static String resolvedLineToXML(final StringBuffer buffer, final IResolvedLine line)
  {
    final Map<String, String> attributes = TypeTools.newLinkedHashMap();
    attributes.put("kind", line.kind().toString());
    attributes.put("line", String.valueOf(line.lineNumber()));
    buffer.append(XML.tagOpen("line", attributes));
    buffer.append(XML.tagOpen("defs"));
    for (final IResolvedData node : line.definitions())
    {
      resolvedNodeToXML(buffer, "def", node, true);
    }
    buffer.append(XML.tagClose("defs"));
    buffer.append(XML.tagOpen("uses"));
    for (final IResolvedNode node : line.uses())
    {
      resolvedNodeToXML(buffer, "use", node, true);
    }
    buffer.append(XML.tagClose("uses"));
    final List<IResolvedLine> inheritedSet = TypeTools.newArrayList();
    IResolvedLine inherited = line.parent();
    while (inherited != null)
    {
      inheritedSet.add(inherited);
      inherited = inherited.parent();
    }
    buffer.append(XML.tagOpen("inherited"));
    for (final IResolvedLine node : inheritedSet)
    {
      attributes.clear();
      attributes.put("kind", node.kind().toString());
      attributes.put("line", String.valueOf(node.lineNumber()));
      buffer.append(XML.tagOpen("line", attributes));
      buffer.append(XML.tagClose("line"));
    }
    buffer.append(XML.tagClose("inherited"));
    buffer.append(XML.tagOpen("propagated"));
    for (final IResolvedLine node : line.jumpDependences())
    {
      attributes.clear();
      attributes.put("kind", node.kind().toString());
      attributes.put("line", String.valueOf(node.lineNumber()));
      buffer.append(XML.tagOpen("line", attributes));
      buffer.append(XML.tagClose("line"));
    }
    buffer.append(XML.tagClose("propagated"));
    buffer.append(XML.tagClose("line"));
    return buffer.toString();
  }

  /**
   * TODO: reduce the cyclomatic complexity.
   */
  private static void resolvedNodeToXML(final StringBuffer buffer, final String tagName,
      final IResolvedNode node, final boolean exposeArguments)
  {
    final Map<String, String> attributes = TypeTools.newLinkedHashMap();
    attributes.put("index", String.valueOf(node.sourceIndex()));
    attributes.put("isLHS", String.valueOf(node.isLHS()));
    attributes.put("isActual", String.valueOf(node.isActual()));
    if (node.qualifierOf() != null)
    {
      attributes.put("qualifierOf", String.valueOf(node.qualifierOf().sourceIndex()));
    }
    if (node instanceof IResolvedCall)
    {
      final IResolvedCall call = (IResolvedCall) node;
      attributes.put("kind", "call");
      if (call.call().node() != null)
      {
        attributes.put("id", String.valueOf(call.call().node().id()));
      }
      attributes.put("method", call.methodName());
      buffer.append(XML.tagOpen(tagName, attributes));
      if (exposeArguments)
      {
        buffer.append(XML.tagOpen("arguments"));
        for (int i = 0; i < call.size(); i++)
        {
          attributes.clear();
          attributes.put("index", String.valueOf(i));
          buffer.append(XML.tagOpen("pos", attributes));
          argumentPositionToXML(buffer, call, i);
          buffer.append(XML.tagClose("pos"));
        }
        buffer.append(XML.tagClose("arguments"));
      }
    }
    else if (node instanceof IResolvedData)
    {
      final IResolvedData data = (IResolvedData) node;
      attributes.put("kind",
          data.data() == null ? "lazy field" : data.data().kind() == NodeKind.NK_FIELD ? "field"
              : (data.data().modifiers().contains(NodeModifier.NM_ARGUMENT) ? "argument"
                  : "variable"));
      if (data.data() != null)
      {
        attributes.put("id", String.valueOf(data.data().id()));
      }
      attributes.put("isDef", String.valueOf(data.isDef()));
      attributes.put("name", data.name());
      if (data.data() != null && data.data().kind() == NodeKind.NK_FIELD)
      {
        attributes.put("declaration", data.data().parent().name());
      }
      buffer.append(XML.tagOpen(tagName, attributes));
    }
    else if (node instanceof IResolvedThis)
    {
      final IResolvedThis rthis = (IResolvedThis) node;
      attributes.put("kind", "this");
      attributes.put("id", String.valueOf(rthis.type().id()));
      attributes.put("type", rthis.type().name());
      buffer.append(XML.tagOpen(tagName, attributes));
    }
    buffer.append(XML.tagClose(tagName));
  }

  private static void argumentPositionToXML(final StringBuffer buffer, final IResolvedCall call,
      final int index)
  {
    buffer.append(XML.tagOpen("vars"));
    for (final IResolvedNode rv : call.uses(index))
    {
      if (rv instanceof IResolvedData)
      {
        resolvedNodeToXML(buffer, "var", rv, false);
      }
    }
    buffer.append(XML.tagClose("vars"));
    buffer.append(XML.tagOpen("calls"));
    for (final IResolvedNode rc : call.uses(index))
    {
      if (rc instanceof IResolvedCall)
      {
        resolvedNodeToXML(buffer, "call", rc, false);
      }
    }
    buffer.append(XML.tagClose("calls"));
  }
}
