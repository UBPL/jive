package edu.buffalo.cse.jive.debug.jdi.model;

import com.sun.jdi.Field;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.StepRequest;

import edu.buffalo.cse.jive.model.IModelCache;

/**
 * Defines an object that is capable of applying filters to JPDA event requests. Specific
 * implementors may use inclusion or exclusion filters at any level; this interface only defines an
 * object that can apply filters to the types of requests corresponding to the defined methods.
 */
public interface IModelFilter
{
  /**
   * Check if this filter accepts the field
   */
  public boolean acceptsField(Field field);

  /**
   * Check if this filter accepts the method in the given thread
   */
  public boolean acceptsMethod(Method method, ThreadReference thread);

  /**
   * Check if this filter accepts the step event
   */
  public boolean acceptsStep(StepEvent event);

  /**
   * Check if this filter accepts the thread
   */
  public boolean acceptsThread(ThreadReference thread);

  /**
   * Check if this filter accepts the type name
   */
  public boolean acceptsType(ReferenceType refType);

  /**
   * Apply this object's filter to the given event request.
   */
  public void filter(ClassPrepareRequest request);

  /**
   * Apply this object's filter to the given event request.
   */
  public void filter(MethodEntryRequest request);

  /**
   * Apply this object's filter to the given event request.
   */
  public void filter(MethodExitRequest request);

  /**
   * Apply this object's filter to the given event request.
   */
  public void filter(StepRequest request);

  /**
   * Retrieve the current cache.
   */
  public IModelCache modelCache();
}
