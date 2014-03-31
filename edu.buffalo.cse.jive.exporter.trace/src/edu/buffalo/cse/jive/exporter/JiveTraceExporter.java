package edu.buffalo.cse.jive.exporter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import edu.buffalo.cse.jive.exporter.trace.JDBCExporter;
import edu.buffalo.cse.jive.exporter.trace.StringExporter;
import edu.buffalo.cse.jive.exporter.trace.XMLExporter;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;

public class JiveTraceExporter implements IJiveExporter
{
  @Override
  public String getElement()
  {
    return "TRACE";
  }

  @Override
  public boolean export(final IExecutionModel model, final String path,
      final IExportModelFilter filter)
  {
    if (path == null || path.trim().length() == 0)
    {
      return false;
    }
    model.readLock();
    try
    {
      if (path.startsWith("jdbc:"))
      {
        return new JDBCExporter(model).export(path);
      }
      final boolean isXML = path.endsWith("xml") || path.equals("copyToXML");
      final List<? extends IJiveEvent> events = model.traceView().events();
      String data = isXML ? XMLExporter.export(events, filter) : StringExporter.export(events,
          filter);
      if (path.equals("copyToXML") || path.equals("copyToCSV"))
      {
        filter.setFiltered(data);
        return true;
      }
      FileOutputStream fos = null;
      try
      {
        fos = new FileOutputStream(path);
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
    }
    finally
    {
      model.readUnlock();
    }
    return false;
  }
}