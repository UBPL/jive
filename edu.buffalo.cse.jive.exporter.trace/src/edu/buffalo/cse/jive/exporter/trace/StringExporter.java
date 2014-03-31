package edu.buffalo.cse.jive.exporter.trace;

import java.util.List;

import edu.buffalo.cse.jive.lib.StringTools;
import edu.buffalo.cse.jive.exporter.IExportModelFilter;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;

public class StringExporter
{
  public static String export(final List<? extends IJiveEvent> events, final IExportModelFilter filter)
  {
    final StringBuilder sb = new StringBuilder();
    for (final IJiveEvent event : events)
    {
      if (filter == null || filter.accepts(event))
      {
        sb.append(StringTools.eventToCSV(event));
      }
    }
    return sb.toString();
  }
}
