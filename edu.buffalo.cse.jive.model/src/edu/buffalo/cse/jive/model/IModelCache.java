package edu.buffalo.cse.jive.model;

import java.util.List;

/**
 * This class' role is to answer queries regarding type/method names and their presence/absence in
 * the model. If acceptsClass returns false on a string, the given class has been declared to be
 * irrelevant by the user.
 * 
 * TODO: move the model cache features into IExecutionModel-- the cache should be created and live
 * in the execution model. Interested parties can access the cache through it.
 */
public interface IModelCache
{
  public boolean acceptsClass(String clazz);

  public void addExclusionFilter(String filter);

  public void addMethodExclusionPattern(String pattern);

  public List<String> exclusionList();

  public boolean match(String input, String pattern);
}
