package edu.buffalo.cse.jive.exporter.mdg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.buffalo.cse.jive.lib.StringTools;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IMethodDependenceGraph;
import edu.buffalo.cse.jive.model.IStaticModel.IFileNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.IRootNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.NodeOrigin;

public class StringExporter
{
  public static String export(final IRootNode root)
  {
    final StringBuffer buffer = new StringBuffer("");
    boolean isFirst = true;
    for (final IFileNode fn : root.files())
    {
      if (!isFirst)
      {
        buffer.append("\n\n");
      }
      buffer.append(String.format("File: %s (id=%d, kind=%s)", fn.name(), fn.id(), fn.kind()
          .toString()));
      for (final ITypeNode tn : fn.types())
      {
        typeMDGsToString(buffer, tn);
      }
      isFirst = false;
    }
    return buffer.toString();
  }

  private static void typeMDGsToString(final StringBuffer buffer, final ITypeNode tn)
  {
    buffer.append(String.format("\n\nType: %s (id=%d, kind=%s)", tn.name(), tn.id(), tn.kind()
        .toString()));
    if (tn.origin() == NodeOrigin.NO_AST)
    {
      for (final IMethodNode mn : tn.methodMembers().values())
      {
        buffer.append(String.format("\n\nMethod: %s (id=%d, kind=%s)\n", mn.name(), mn.id(), mn
            .kind().toString()));
        if (mn.origin() == NodeOrigin.NO_AST && mn.getDependenceGraph() != null)
        {
          mdgToString(buffer, mn.getDependenceGraph());
        }
      }
    }
    // nested member types
    for (final ITypeNode ntn : tn.typeMembers())
    {
      typeMDGsToString(buffer, ntn);
    }
  }

  private static void mdgToString(final StringBuffer buffer, final IMethodDependenceGraph mdg)
  {
    final List<Integer> allLines = new ArrayList<Integer>(mdg.dependenceMap().keySet());
    Collections.sort(allLines);
    for (final Integer key : allLines)
    {
      buffer.append(StringTools.resolvedLineToString(mdg.dependenceMap().get(key))).append("\n");
    }
  }
}
