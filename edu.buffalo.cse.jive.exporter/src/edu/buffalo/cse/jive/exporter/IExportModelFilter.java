package edu.buffalo.cse.jive.exporter;

public interface IExportModelFilter
{
  public boolean accepts(final Object object);

  public void setFiltered(String data);
}
