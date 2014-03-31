package edu.buffalo.cse.jive.launch.offline;

public class OfflineImporterException extends Exception
{
  private static final long serialVersionUID = 411320097536003885L;

  public OfflineImporterException(final String message)
  {
    super(message);
  }

  public OfflineImporterException(final String message, final Throwable t)
  {
    super(message, t);
  }

  public OfflineImporterException(final Throwable t)
  {
    super(t);
  }
}
