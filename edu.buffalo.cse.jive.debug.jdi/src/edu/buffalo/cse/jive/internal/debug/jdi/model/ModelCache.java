package edu.buffalo.cse.jive.internal.debug.jdi.model;

import java.util.List;
import java.util.Set;

import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.model.IModelCache;

public class ModelCache implements IModelCache
{
  /**
   * A set containing classes that are known to be accepted by the filter.
   */
  private final Set<String> acceptedClassCache;
  /**
   * A set containing method names that are known to be accepted by the filter.
   */
  private final Set<String> acceptedMethodsCache;
  /**
   * The list of excluded package filters. Each element is a String specifying a regular expression
   * filter, such as "java.*".
   */
  private final List<String> exclusionList;
  /**
   * The list of excluded method name filters. Each element is a String specifying a regular
   * expression filter, such as "get*".
   */
  private final List<String> exclusionPatterns;
  /**
   * A set containing classes that are known to be rejected by the filter.
   */
  private final Set<String> rejectedClassCache;
  /**
   * A set containing method names that are known to be rejected by the filter.
   */
  private final Set<String> rejectedMethodsCache;

  public ModelCache()
  {
    this.acceptedClassCache = TypeTools.newHashSet();
    this.acceptedMethodsCache = TypeTools.newHashSet();
    this.exclusionList = TypeTools.newArrayList();
    this.exclusionPatterns = TypeTools.newArrayList();
    this.rejectedClassCache = TypeTools.newHashSet();
    this.rejectedMethodsCache = TypeTools.newHashSet();
  }

  @Override
  public boolean acceptsClass(final String clazz)
  {
    if (acceptedClassCache.contains(clazz))
    {
      return true;
    }
    else if (rejectedClassCache.contains(clazz))
    {
      return false;
    }
    else
    {
      for (final String filter : exclusionList)
      {
        if (match(clazz, filter))
        {
          rejectedClassCache.add(clazz);
          return false;
        }
      }
      acceptedClassCache.add(clazz);
      return true;
    }
  }

  /**
   * Adds a package filter to the exclusion list.
   * 
   * @param filter
   *          the regular expression filter, such as "java.*"
   */
  @Override
  public void addExclusionFilter(final String filter)
  {
    if (!exclusionList.contains(filter))
    {
      exclusionList.add(filter);
    }
    acceptedClassCache.clear();
    rejectedClassCache.clear();
  }

  /**
   * Adds a method exclusion pattern to the exclusion list.
   * 
   * @param pattern
   *          the regular expression filter, such as "get*"
   */
  @Override
  public void addMethodExclusionPattern(final String pattern)
  {
    if (!exclusionPatterns.contains(pattern))
    {
      exclusionPatterns.add(pattern);
    }
    acceptedMethodsCache.clear();
    rejectedMethodsCache.clear();
  }

  @Override
  public List<String> exclusionList()
  {
    return exclusionList;
  }

  /**
   * Match an input string against a pattern. Patterns may contain the wildcard character '*' at
   * either the beginning or end of the pattern (but not both).
   * 
   * @param input
   *          the string to match against the pattern
   * @param pattern
   *          the pattern
   * @return true if 'in' matches 'pat'
   */
  @Override
  public boolean match(final String input, final String pattern)
  {
    final int wildcardIndex = pattern.indexOf('*');
    if (wildcardIndex == -1)
    {
      return input.equals(pattern);
    }
    else if (wildcardIndex == 0)
    {
      if (pattern.length() == 1)
      {
        return true;
      }
      else
      {
        return input.endsWith(pattern.substring(1));
      }
    }
    else if (wildcardIndex == pattern.length() - 1)
    {
      return input.startsWith(pattern.substring(0, wildcardIndex - 1));
    }
    else
    {
      return input.equals(pattern);
    }
  }
}
