package edu.buffalo.cse.jive.model.store;

import edu.buffalo.cse.jive.model.IEventModel.IEventProducer;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.factory.IExecutionModelFactory;
import edu.buffalo.cse.jive.model.factory.IStoreFactory;

public final class Factory
{
  public static IExecutionModel memoryExecutionModel(final IEventProducer provider)
  {
    return Factory.memoryExecutionModelFactory().executionModel(provider);
  }

  private static IExecutionModelFactory memoryExecutionModelFactory()
  {
    return Factory.memoryStoreFactory().executionModelFactory();
  }

  private static IStoreFactory memoryStoreFactory()
  {
    return new edu.buffalo.cse.jive.model.store.memory.MemoryStoreFactory();
  }
}
