package edu.buffalo.cse.jive.internal.model.store.memory;

import java.util.concurrent.ConcurrentMap;

import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.model.IEventModel.IEventProducer;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.factory.IExecutionModelFactory;

public enum ExecutionModelFactory implements IExecutionModelFactory
{
  INSTANCE;
  private final ConcurrentMap<Object, IExecutionModel> cache;

  private ExecutionModelFactory()
  {
    cache = TypeTools.newConcurrentHashMap(8);
  }

  /**
   * Retrieve the execution model for the given event provider. If an execution model does not yet
   * exist, one is created.
   * 
   * @param provider
   *          source of low-level events for the execution model
   * @return execution model associated with the event provider
   */
  @Override
  public IExecutionModel executionModel(final IEventProducer provider)
  {
    if (cache.get(provider) == null)
    {
      final IExecutionModel executionModel = new ExecutionModel();
      cache.putIfAbsent(provider, executionModel);
      provider.subscribe(executionModel);
      return executionModel;
    }
    return cache.get(provider);
  }
}
