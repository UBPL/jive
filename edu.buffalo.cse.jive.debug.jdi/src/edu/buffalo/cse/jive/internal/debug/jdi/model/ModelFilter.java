package edu.buffalo.cse.jive.internal.debug.jdi.model;

import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadGroupReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.StepRequest;

import edu.buffalo.cse.jive.debug.jdi.model.IModelFilter;
import edu.buffalo.cse.jive.model.IModelCache;

/**
 * A high-level filtering mechanism applied over JPDA-filtered event streams. Method filters must
 * not block out assignment events. Type filters must block out all events associated with the type,
 * including calls, returns, and assignments.
 * 
 * Synthetic filters should behave as if the filtered method NEVER EXISTED in the first place.
 * However, the current stack management code gets in the way-- synthetic methods are in the actual
 * program's call stack and missing in the mirrored stack. Hence, they show up as out-of-model
 * calls. Ideally, they should be "merged" within the containing method context.
 */
class ModelFilter implements IModelFilter
{
  /**
   * In principle, synthetic methods only provide insight into how the compiler implements certain
   * language features, so it should be safe to omit them. Identified cases of synthetic method
   * creation:
   * 
   * <ol>
   * <li>covariant return types in overrides to handle type coercion;</li>
   * <li>accessor methods for nested classes accessing their containing contexts' members (these are
   * generated with a "$" in their names);</li>
   * </ol>
   */
  private final boolean filterSynthetics;
  private final IModelCache modelCache;

  ModelFilter()
  {
    filterSynthetics = true;
    modelCache = new ModelCache();
  }

  @Override
  public boolean acceptsField(final Field f)
  {
    if (filterSynthetics && f.isSynthetic())
    {
      // System.out.println("synthetic field: " + f.type() + "." + f);
      return false;
    }
    return true;
  }

  @Override
  public boolean acceptsMethod(final Method method, final ThreadReference thread)
  {
    if (filterSynthetics && (method.isSynthetic() || method.isBridge()))
    {
      // System.err.println("filtered out method: " + m.declaringType() + "." + m);
      return false;
    }
    if (!acceptsType(method.declaringType()))
    {
      return false;
    }
    return true;
  }

  /**
   * Step events are filtered according to accepted types and methods.
   */
  @Override
  public boolean acceptsStep(final StepEvent event)
  {
    if (!acceptsLocation(event.location()))
    {
      return false;
    }
    ObjectReference oref;
    try
    {
      oref = event.thread().frame(0).thisObject();
    }
    catch (final IncompatibleThreadStateException e)
    {
      oref = null;
    }
    // step's context type
    if (oref != null && !acceptsType(oref.referenceType()))
    {
      return false;
    }
    return true;
  }

  @Override
  public boolean acceptsThread(final ThreadReference thread)
  {
    if (thread.name().equals("DestroyJavaVM"))
    {
      return false;
    }
    final ThreadGroupReference group = (thread != null ? thread.threadGroup() : null);
    return group == null || !group.name().equalsIgnoreCase("system");
  }

  @Override
  public boolean acceptsType(final ReferenceType ref)
  {
    return modelCache.acceptsClass(ref.name());
  }

  /**
   * Adds a package filter to the exclusion list.
   * 
   * @param filter
   *          the regular expression filter, such as "java.*"
   */
  public void addExclusionFilter(final String filter)
  {
    modelCache.addExclusionFilter(filter);
  }

  /**
   * Adds a method exclusion pattern to the exclusion list.
   * 
   * @param pattern
   *          the regular expression filter, such as "get*"
   */
  public void addMethodExclusionPattern(final String pattern)
  {
    modelCache.addMethodExclusionPattern(pattern);
  }

  @Override
  public void filter(final ClassPrepareRequest request)
  {
    for (final String filter : modelCache.exclusionList())
    {
      request.addClassExclusionFilter(filter);
    }
  }

  @Override
  public void filter(final MethodEntryRequest request)
  {
    for (final String filter : modelCache.exclusionList())
    {
      request.addClassExclusionFilter(filter);
    }
  }

  @Override
  public void filter(final MethodExitRequest request)
  {
    for (final String filter : modelCache.exclusionList())
    {
      request.addClassExclusionFilter(filter);
    }
  }

  @Override
  public void filter(final StepRequest request)
  {
    for (final String filter : modelCache.exclusionList())
    {
      request.addClassExclusionFilter(filter);
    }
  }

  @Override
  public IModelCache modelCache()
  {
    return modelCache;
  }

  private boolean acceptsLocation(final Location location)
  {
    return location == null
        || ((location.method() == null && acceptsType(location.declaringType())) || acceptsType(location
            .method().declaringType()));
  }
}