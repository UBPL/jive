package edu.buffalo.cse.jive.exporter;

import edu.buffalo.cse.jive.model.IExecutionModel;

public interface IJiveExporter
{
  /**
   * The name of the export element supported by this exporter.
   */
  public String getElement();

  /**
   * Exports this exporter's element to the specified path. Typically supported paths include simple
   * text (e.g., CSV format), XML (e.g., using a well defined schema), or JDBC (e.g., a connection
   * with an optional schema specification).
   */
  public boolean export(final IExecutionModel model, final String path, final IExportModelFilter filter);
}