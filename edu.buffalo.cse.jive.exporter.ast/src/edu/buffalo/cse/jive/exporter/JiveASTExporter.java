package edu.buffalo.cse.jive.exporter;

import java.io.FileOutputStream;
import java.io.IOException;

import edu.buffalo.cse.jive.exporter.ast.StringExporter;
import edu.buffalo.cse.jive.exporter.ast.XMLExporter;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IStaticModel.IRootNode;

public class JiveASTExporter implements IJiveExporter
{
  @Override
  public String getElement()
  {
    return "AST";
  }

  @Override
  public boolean export(final IExecutionModel model, final String path,
      final IExportModelFilter filter)
  {
    FileOutputStream fos = null;
    try
    {
      fos = new FileOutputStream(path);
      final boolean isXML = path.endsWith("xml");
      final IRootNode root = model.staticModelFactory().lookupRoot();
      String data = isXML ? XMLExporter.export(root) : StringExporter.export(root);
      fos.write(data.getBytes());
      fos.flush();
      return true;
    }
    catch (final IOException e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (fos != null)
      {
        try
        {
          fos.close();
        }
        catch (final IOException e)
        {
          e.printStackTrace();
        }
      }
    }
    return false;
  }
}