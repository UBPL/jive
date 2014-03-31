package edu.buffalo.cse.jive.model.store.memory;

import edu.buffalo.cse.jive.internal.model.store.memory.ExecutionModelFactory;
import edu.buffalo.cse.jive.model.factory.IExecutionModelFactory;
import edu.buffalo.cse.jive.model.factory.IStoreFactory;

/**
 * Entry point for creating execution model factories.
 */
public final class MemoryStoreFactory implements IStoreFactory
{
  @Override
  public IExecutionModelFactory executionModelFactory()
  {
    return ExecutionModelFactory.INSTANCE;
  }
}