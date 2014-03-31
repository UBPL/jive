package edu.buffalo.cse.jive.exporter.ast;

import edu.buffalo.cse.jive.model.IStaticModel.IDataNode;
import edu.buffalo.cse.jive.model.IStaticModel.IFileNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.INode;
import edu.buffalo.cse.jive.model.IStaticModel.IRootNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNodeRef;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;

public class StringExporter
{
  private final static String INDENT = "  ";

  public static String export(final INode node)
  {
    final String indent = "";
    final StringBuffer buffer = new StringBuffer("");
    if (node instanceof IRootNode)
    {
      NodeString.rootToString(buffer, (IRootNode) node, indent);
    }
    else if (node instanceof IFileNode)
    {
      NodeString.fileToString(buffer, (IFileNode) node, indent);
    }
    else if (node instanceof ITypeNode)
    {
      NodeString.typeToString(buffer, (ITypeNode) node, indent);
    }
    else if (node instanceof IDataNode)
    {
      NodeString.dataToString(buffer, (IDataNode) node, indent);
    }
    else if (node instanceof IMethodNode)
    {
      NodeString.methodToString(buffer, (IMethodNode) node, indent);
    }
    else
    {
      NodeString.nodeToString(buffer, node, indent);
    }
    return buffer.toString();
  }

  private final static class NodeString
  {
    private static void dataToString(final StringBuffer buffer, final IDataNode node,
        final String prefix)
    {
      // common
      NodeString.nodeToString(buffer, node, prefix);
      final String indent = prefix + INDENT;
      buffer.append(indent).append("defaultValue = ").append(node.defaultValue()).append("\n");
      buffer.append(indent).append("index = ").append(node.index()).append("\n");
      buffer.append(indent).append("type = ").append(getRefSignature(node.type())).append("\n");
    }

    private static void fileToString(final StringBuffer buffer, final IFileNode node,
        final String prefix)
    {
      // common
      NodeString.nodeToString(buffer, node, prefix);
      final String indent = prefix + INDENT;
      buffer.append(indent).append("[TYPES]").append("\n");
      for (final ITypeNode type : node.types())
      {
        NodeString.typeToString(buffer, type, indent + INDENT);
      }
    }

    private static String getLinesString(final INode node)
    {
      if (node.lineFrom() == -1 || node.lineTo() == -1)
      {
        return "<>";
      }
      return "" + node.lineFrom() + "--" + node.lineTo();
    }

    private static String getModifiersString(final INode node)
    {
      final StringBuffer buffer = new StringBuffer("");
      for (final NodeModifier modifier : node.modifiers())
      {
        buffer.append(modifier).append(", ");
      }
      return buffer.toString();
    }

    private static void methodToString(final StringBuffer buffer, final IMethodNode node,
        final String prefix)
    {
      // common
      NodeString.nodeToString(buffer, node, prefix);
      final String indent = prefix + INDENT;
      buffer.append(indent).append("[TYPES]").append("\n");
      for (final ITypeNode type : node.localTypes())
      {
        buffer.append(indent).append(INDENT).append(getNodeSignature(type)).append("\n");
      }
      buffer.append(indent).append("[LOCAL VARIABLES]").append("\n");
      for (final Integer variableId : node.dataMembers().keySet())
      {
        NodeString.dataToString(buffer, node.dataMembers().get(variableId), indent + INDENT);
      }
    }

    private static void nodeToString(final StringBuffer buffer, final INode node,
        final String prefix)
    {
      final String indent = prefix + INDENT;
      buffer.append(prefix).append("[NODE ").append(node.kind()).append("]\n");
      buffer.append(indent).append("id = ").append(node.id()).append("\n");
      buffer.append(indent).append("lines = ").append(NodeString.getLinesString(node)).append("\n");
      buffer.append(indent).append("modifiers = ").append(NodeString.getModifiersString(node))
          .append("\n");
      buffer.append(indent).append("name = ").append(node.name()).append("\n");
      buffer.append(indent).append("origin = ").append(node.origin()).append("\n");
      buffer.append(indent).append("parent = ").append(getNodeSignature(node.parent()))
          .append("\n");
      buffer.append(indent).append("visibiliy = ").append(node.visibility()).append("\n");
    }

    private static void rootToString(final StringBuffer buffer, final IRootNode node,
        final String prefix)
    {
      // common
      NodeString.nodeToString(buffer, node, prefix);
      final String indent = prefix + INDENT;
      buffer.append(indent).append("[PRIMITIVE TYPES]").append("\n");
      for (final ITypeNode type : node.types())
      {
        if (type.kind() == NodeKind.NK_PRIMITIVE)
        {
          buffer.append(indent).append(INDENT).append(getNodeSignature(type)).append("\n");
        }
      }
      buffer.append(indent).append("[CLASSES]").append("\n");
      for (final ITypeNode type : node.types())
      {
        if (type.kind() == NodeKind.NK_CLASS)
        {
          NodeString.typeToString(buffer, type, prefix + INDENT);
        }
      }
      buffer.append(indent).append("[INTERFACES]").append("\n");
      for (final ITypeNode type : node.types())
      {
        if (type.kind() == NodeKind.NK_INTERFACE)
        {
          NodeString.typeToString(buffer, type, prefix + INDENT);
        }
      }
      buffer.append(indent).append("[ARRAYS]").append("\n");
      for (final ITypeNode type : node.types())
      {
        if (type.kind() == NodeKind.NK_ARRAY)
        {
          buffer.append(indent).append(INDENT).append(getNodeSignature(type)).append("\n");
        }
      }
      buffer.append(indent).append("[FILES]").append("\n");
      for (final IFileNode file : node.files())
      {
        NodeString.fileToString(buffer, file, prefix + INDENT);
      }
    }

    private static void typeToString(final StringBuffer buffer, final ITypeNode node,
        final String prefix)
    {
      // common
      NodeString.nodeToString(buffer, node, prefix);
      final String indent = prefix + INDENT;
      buffer.append(indent).append("defaultValue = ").append(node.defaultValue()).append("\n");
      // signature of the super class
      buffer.append(indent).append("super class = ").append(getRefSignature(node.superClass()))
          .append("\n");
      // complete information about member fields
      buffer.append(indent).append("[MEMBER FIELDS]").append("\n");
      for (final Integer fieldId : node.dataMembers().keySet())
      {
        NodeString.dataToString(buffer, node.dataMembers().get(fieldId), indent + INDENT);
      }
      // complete information about member methods
      buffer.append(indent).append("[MEMBER METHODS]").append("\n");
      for (final Integer methodId : node.methodMembers().keySet())
      {
        NodeString.methodToString(buffer, node.methodMembers().get(methodId), indent + INDENT);
      }
      // signature of the member types
      buffer.append(indent).append("[MEMBER TYPES]").append("\n");
      for (final ITypeNode type : node.typeMembers())
      {
        buffer.append(indent).append(INDENT).append(getNodeSignature(type)).append("\n");
      }
      // signature of the super interfaces
      buffer.append(indent).append("[SUPER INTERFACES]").append("\n");
      for (final ITypeNodeRef type : node.superInterfaces())
      {
        buffer.append(indent).append(INDENT).append(getRefSignature(type)).append("\n");
      }
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
