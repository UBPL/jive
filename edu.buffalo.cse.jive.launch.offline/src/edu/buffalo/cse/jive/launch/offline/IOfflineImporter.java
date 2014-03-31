package edu.buffalo.cse.jive.launch.offline;

import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory.IStaticModelDelegate;

public interface IOfflineImporter
{
  public void process(String url, IExecutionModel model, IStaticModelDelegate upstream)
      throws OfflineImporterException;
}