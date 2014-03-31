package edu.buffalo.cse.jive.model.factory;

import edu.buffalo.cse.jive.model.IEventModel.IEventProducer;
import edu.buffalo.cse.jive.model.IExecutionModel;

public interface IExecutionModelFactory
{
  public IExecutionModel executionModel(final IEventProducer provider);
}
