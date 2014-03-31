package edu.buffalo.cse.jive.exporter.ast;

import java.util.Set;

import edu.buffalo.cse.jive.exporter.XML;
import edu.buffalo.cse.jive.model.IStaticModel.IDataNode;
import edu.buffalo.cse.jive.model.IStaticModel.IFileNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.INode;
import edu.buffalo.cse.jive.model.IStaticModel.IRootNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNodeRef;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.model.IStaticModel.NodeOrigin;
import edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility;

public class XMLExporter
{
  public static String export(final INode node)
  {
    final StringBuffer buffer = new StringBuffer(XML.tagOpen(NodeXML.nodeName(node)));
    if (node instanceof IRootNode)
    {
      NodeXML.rootToXML(buffer, (IRootNode) node);
    }
    else if (node instanceof IFileNode)
    {
      NodeXML.fileToXML(buffer, (IFileNode) node);
    }
    else if (node instanceof ITypeNode)
    {
      NodeXML.typeToXML(buffer, (ITypeNode) node);
    }
    else if (node instanceof IMethodNode)
    {
      NodeXML.methodToXML(buffer, (IMethodNode) node);
    }
    else if (node instanceof IDataNode)
    {
      NodeXML.dataToXML(buffer, (IDataNode) node);
    }
    else
    {
      NodeXML.nodeToXML(buffer, node);
    }
    buffer.append(XML.tagClose(NodeXML.nodeName(node)));
    return buffer.toString();
  }

  private static final class NodeXML
  {
    private static void dataToXML(final StringBuffer buffer, final IDataNode node)
    {
      // common
      NodeXML.nodeToXML(buffer, node);
      buffer.append(XML.tagOpen("defaultValue"));
      buffer.append(XML.CData(node.defaultValue().toString()));
      buffer.append(XML.tagClose("defaultValue"));
      buffer.append(XML.tagOpen("index"));
      buffer.append(XML.PCData(node.index()));
      buffer.append(XML.tagClose("index"));
      NodeXML.xmlNodeReference(buffer, "type", node.type().node());
    }

    private static void fileToXML(final StringBuffer buffer, final IFileNode node)
    {
      // common
      NodeXML.nodeToXML(buffer, node);
      buffer.append(XML.tagOpen("types"));
      NodeXML.xmlTypes(buffer, node.types());
      buffer.append(XML.tagClose("types"));
    }

    private static void methodToXML(final StringBuffer buffer, final IMethodNode node)
    {
      // common
      NodeXML.nodeToXML(buffer, node);
      buffer.append(XML.tagOpen("localVariables"));
      for (final Integer variableId : node.dataMembers().keySet())
      {
        buffer.append(XML.tagOpen("variable"));
        NodeXML.nodeToXML(buffer, node.dataMembers().get(variableId));
        buffer.append(XML.tagClose("variable"));
      }
      buffer.append(XML.tagClose("localVariables"));
      buffer.append(XML.tagOpen("localTypes"));
      NodeXML.xmlTypes(buffer, node.localTypes());
      buffer.append(XML.tagClose("localTypes"));
    }

    private static String nodeModifier(final NodeModifier modifier)
    {
      return modifier.name().substring(3).toLowerCase();
    }

    private static String nodeName(final INode node)
    {
      return node.kind().name().substring(3).toLowerCase();
    }

    private static String nodeOrigin(final NodeOrigin origin)
    {
      return origin.name().substring(3).toLowerCase();
    }

    private static void nodeToXML(final StringBuffer buffer, final INode node)
    {
      buffer.append(XML.tagOpen("id"));
      buffer.append(XML.PCData(node.id()));
      buffer.append(XML.tagClose("id"));
      buffer.append(XML.tagOpen("name"));
      buffer.append(XML.CData(node.name()));
      buffer.append(XML.tagClose("name"));
      buffer.append(XML.tagOpen("origin"));
      buffer.append(XML.CData(NodeXML.nodeOrigin(node.origin())));
      buffer.append(XML.tagClose("origin"));
      buffer.append(XML.tagOpen("visibility"));
      buffer.append(XML.CData(NodeXML.nodeVisibility(node.visibility())));
      buffer.append(XML.tagClose("visibility"));
      if (node.lineFrom() == -1 || node.lineTo() == -1)
      {
        buffer.append(XML.tagOpen("lines"));
        buffer.append(XML.tagClose("lines"));
      }
      else
      {
        buffer.append(XML.tagOpen("lines"));
        buffer.append(XML.tagOpen("from"));
        buffer.append(XML.PCData(node.lineFrom()));
        buffer.append(XML.tagClose("from"));
        buffer.append(XML.tagOpen("to"));
        buffer.append(XML.PCData(node.lineTo()));
        buffer.append(XML.tagClose("to"));
        buffer.append(XML.tagClose("lines"));
      }
      buffer.append(XML.tagOpen("modifiers"));
      for (final NodeModifier modifier : node.modifiers())
      {
        buffer.append(XML.tagOpen("modifier"));
        buffer.append(XML.CData(NodeXML.nodeModifier(modifier)));
        buffer.append(XML.tagClose("modifier"));
      }
      buffer.append(XML.tagClose("modifiers"));
    }

    private static String nodeVisibility(final NodeVisibility visibility)
    {
      return visibility.name().substring(3).toLowerCase();
    }

    private static void rootToXML(final StringBuffer buffer, final IRootNode node)
    {
      // common
      NodeXML.nodeToXML(buffer, node);
      buffer.append(XML.tagOpen("types"));
      // primitives
      buffer.append(XML.tagOpen("primitives"));
      for (final ITypeNode type : node.types())
      {
        if (type.kind() == NodeKind.NK_PRIMITIVE)
        {
          NodeXML.xmlNodeReference(buffer, "primitive", type);
        }
      }
      buffer.append(XML.tagClose("primitives"));
      // arrays
      buffer.append(XML.tagOpen("arrays"));
      for (final ITypeNode type : node.types())
      {
        if (type.kind() == NodeKind.NK_ARRAY)
        {
          NodeXML.xmlNodeReference(buffer, "array", type);
        }
      }
      buffer.append(XML.tagClose("arrays"));
      // classes and interfaces
      NodeXML.xmlTypes(buffer, node.types());
      buffer.append(XML.tagClose("types"));
      buffer.append(XML.tagOpen("files"));
      for (final IFileNode file : node.files())
      {
        buffer.append(XML.tagOpen("file"));
        NodeXML.fileToXML(buffer, file);
        buffer.append(XML.tagClose("file"));
      }
      buffer.append(XML.tagClose("files"));
    }

    private static void typeToXML(final StringBuffer buffer, final ITypeNode node)
    {
      // common
      NodeXML.nodeToXML(buffer, node);
      buffer.append(XML.tagOpen("defaultValue"));
      buffer.append(XML.CData(node.defaultValue().toString()));
      buffer.append(XML.tagClose("defaultValue"));
      NodeXML.xmlNodeReference(buffer, "superClass", node.superClass() != null ? node.superClass()
          .node() : null);
      // open members
      buffer.append(XML.tagOpen("members"));
      // field members
      buffer.append(XML.tagOpen("fields"));
      for (final Integer fieldId : node.dataMembers().keySet())
      {
        buffer.append(XML.tagOpen("field"));
        NodeXML.nodeToXML(buffer, node.dataMembers().get(fieldId));
        buffer.append(XML.tagClose("field"));
      }
      buffer.append(XML.tagClose("fields"));
      // method members
      buffer.append(XML.tagOpen("methods"));
      for (final Integer methodId : node.methodMembers().keySet())
      {
        buffer.append(XML.tagOpen("method"));
        NodeXML.nodeToXML(buffer, node.methodMembers().get(methodId));
        buffer.append(XML.tagClose("method"));
      }
      buffer.append(XML.tagClose("methods"));
      // type members
      buffer.append(XML.tagOpen("types"));
      for (final ITypeNode type : node.typeMembers())
      {
        NodeXML.xmlNodeReference(buffer, "type", type);
      }
      buffer.append(XML.tagClose("types"));
      // close members
      buffer.append(XML.tagClose("members"));
      buffer.append(XML.tagOpen("implements"));
      for (final ITypeNodeRef type : node.superInterfaces())
      {
        NodeXML.xmlNodeReference(buffer, "interface", type.node());
      }
      buffer.append(XML.tagClose("implements"));
    }

    private static void xmlNodeReference(final StringBuffer buffer, final String tagName,
        final INode node)
    {
      buffer.append(XML.tagOpen(tagName));
      if (node != null)
      {
        buffer.append(XML.tagOpen("id"));
        buffer.append(XML.PCData(node.id()));
        buffer.append(XML.tagClose("id"));
        buffer.append(XML.tagOpen("name"));
        buffer.append(XML.CData(node.name()));
        buffer.append(XML.tagClose("name"));
      }
      buffer.append(XML.tagClose(tagName));
    }

    private static void xmlTypes(final StringBuffer buffer, final Set<ITypeNode> types)
    {
      buffer.append(XML.tagOpen("classes"));
      for (final ITypeNode type : types)
      {
        if (type.kind() == NodeKind.NK_CLASS)
        {
          buffer.append(XML.tagOpen("class"));
          NodeXML.typeToXML(buffer, type);
          buffer.append(XML.tagClose("class"));
        }
      }
      buffer.append(XML.tagClose("classes"));
      buffer.append(XML.tagOpen("interfaces"));
      for (final ITypeNode type : types)
      {
        if (type.kind() == NodeKind.NK_INTERFACE)
        {
          buffer.append(XML.tagOpen("interface"));
          NodeXML.typeToXML(buffer, type);
          buffer.append(XML.tagClose("interface"));
        }
      }
      buffer.append(XML.tagClose("interfaces"));
    }
  }
}
