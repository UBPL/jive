package edu.buffalo.cse.jive.lib;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * One key (of type K) can be associated to multiple values (of type V), each of which is indexed by
 * an integer value (standing for an ordinal position). This type is useful when its children values
 * are only accessed through the parent.
 */
public class OneToMany<K, V> extends ReentrantReadWriteLock
{
  private static final long serialVersionUID = 5687278244634875844L;
  private static final int DEFAULT_CARDINALITY = 10;
  // expected cardinality of the relationship
  private final int cardinality;
  private final Map<K, Set<V>> delegate;

  public OneToMany()
  {
    this(OneToMany.DEFAULT_CARDINALITY);
  }

  public OneToMany(final int cardinality)
  {
    this.cardinality = cardinality;
    this.delegate = TypeTools.newHashMap();
  }

  /**
   * Concatenate the and return all values in the delegate map.
   */
  public Set<V> allValues()
  {
    readLock().lock();
    try
    {
      final Set<V> result = TypeTools.newHashSet(delegate.size() * cardinality);
      for (final Set<V> values : delegate.values())
      {
        result.addAll(values);
      }
      return result;
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * Clears the delegate map.
   */
  public void clear()
  {
    writeLock().lock();
    try
    {
      delegate.clear();
    }
    finally
    {
      writeLock().unlock();
    }
  }

  /**
   * True if the delegate map contains an entry for the given key.
   */
  public boolean containsKey(final K key)
  {
    readLock().lock();
    try
    {
      return delegate.containsKey(key);
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * True if this map contains a value indexed by the given value index in the map for the given
   * key.
   */
  public boolean containsValue(final K key, final V value)
  {
    readLock().lock();
    try
    {
      final Set<V> set = delegate.get(key);
      return set != null && set.contains(value);
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * True if the delegate map contains a non-empty map for the given key.
   */
  public boolean containsValues(final K key)
  {
    readLock().lock();
    try
    {
      return delegate.containsKey(key) && !delegate.get(key).isEmpty();
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * True if no keys exist in the delegate map.
   */
  public boolean isEmpty()
  {
    readLock().lock();
    try
    {
      return delegate.isEmpty();
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * If the key is valid, returns the children associated with the key, otherwise an empty set is
   * returned.
   */
  @SuppressWarnings("unchecked")
  public Set<V> lookupChildren(final K key)
  {
    readLock().lock();
    try
    {
      final Set<V> set = delegate.get(key);
      return set != null ? set : Collections.EMPTY_SET;
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * Returns all keys in the delegate map.
   */
  public Set<K> lookupKeys()
  {
    readLock().lock();
    try
    {
      return delegate.keySet();
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * Returns the size of the children for the given key, or 0 if one does not exist.
   */
  public int lookupSize(final K key)
  {
    readLock().lock();
    try
    {
      final Set<V> set = delegate.get(key);
      return set != null ? set.size() : 0;
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * Inserts the value as a child of the given key. If the child container for the current key does
   * not yet exist, one is created. Returns true if successful.
   */
  public boolean put(final K key, final V value)
  {
    writeLock().lock();
    try
    {
      return putIfAbsent(key).add(value);
    }
    finally
    {
      writeLock().unlock();
    }
  }

  public boolean putAll(final K key, final Set<V> values)
  {
    writeLock().lock();
    try
    {
      return putIfAbsent(key).addAll(values);
    }
    finally
    {
      writeLock().unlock();
    }
  }

  /**
   * If no child container is associated with the given key, a new one is created, associated with
   * the key, and returned. If one already exists, it is returned.
   */
  public Set<V> putKey(final K key)
  {
    writeLock().lock();
    try
    {
      return putIfAbsent(key);
    }
    finally
    {
      writeLock().unlock();
    }
  }

  /**
   * Removes all children of this key, and removes the key from the relationship. Returns the
   * removed children, if any, or null otherwise.
   */
  public Set<V> remove(final K key)
  {
    writeLock().lock();
    try
    {
      return delegate.remove(key);
    }
    finally
    {
      writeLock().unlock();
    }
  }

  /**
   * Removes the value from the children of the given key. Returns true if the value was removed.
   */
  public boolean remove(final K key, final V value)
  {
    writeLock().lock();
    try
    {
      final Set<V> set = delegate.get(key);
      return set != null ? set.remove(value) : false;
    }
    finally
    {
      writeLock().unlock();
    }
  }

  /**
   * Returns the size of the relationship.
   */
  public int size()
  {
    readLock().lock();
    try
    {
      return delegate.size();
    }
    finally
    {
      readLock().unlock();
    }
  }

  protected Set<V> putIfAbsent(final K key)
  {
    Set<V> result = delegate.get(key);
    if (result == null)
    {
      result = TypeTools.newHashSet(cardinality);
      delegate.put(key, result);
    }
    return result;
  }
}