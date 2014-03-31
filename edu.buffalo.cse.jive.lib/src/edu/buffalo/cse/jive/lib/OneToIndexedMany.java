package edu.buffalo.cse.jive.lib;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * One key (of type K) can be associated to multiple values (of type V), each of which is indexed by
 * a value index (of type J).
 */
public class OneToIndexedMany<K, J, V> extends ReentrantReadWriteLock
{
  private static final long serialVersionUID = 5814670554309841709L;
  private static final int DEFAULT_CARDINALITY = 10;
  // expected cardinality of the relationship
  private final int cardinality;
  private final Map<K, Map<J, V>> delegate;
  private final boolean isSortedMap;
  private final Map<J, V> emptyMap;

  public OneToIndexedMany()
  {
    this(false);
  }

  public OneToIndexedMany(final boolean isSortedMap)
  {
    this(OneToIndexedMany.DEFAULT_CARDINALITY, isSortedMap);
  }

  public OneToIndexedMany(final int cardinality, final boolean isSortedMap)
  {
    this.cardinality = cardinality;
    this.delegate = TypeTools.newHashMap();
    this.isSortedMap = isSortedMap;
    final Map<J, V> map = TypeTools.newSortedMap();
    this.emptyMap = Collections.unmodifiableMap(map);
  }

  /**
   * Concatenate the and return all values in the delegate map.
   */
  public Collection<V> allValues()
  {
    readLock().lock();
    try
    {
      final List<V> result = TypeTools.newArrayList(delegate.size() * cardinality);
      for (final Map<J, V> map : delegate.values())
      {
        result.addAll(map.values());
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
  public boolean containsValue(final K key, final J valueIndex)
  {
    readLock().lock();
    try
    {
      final Map<J, V> values = delegate.get(key);
      return values != null && values.containsKey(valueIndex);
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
   * If the key is valid, returns the map corresponding to the children associated with the key,
   * otherwise an empty map is returned.
   */
  public Map<J, V> lookupChildren(final K key)
  {
    readLock().lock();
    try
    {
      final Map<J, V> values = delegate.get(key);
      return values != null ? values : emptyMap;
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
   * Returns the value indexed by the given value index in the map for the given key, or null if one
   * does not exist.
   */
  public V lookupValue(final K key, final J valueIndex)
  {
    readLock().lock();
    try
    {
      final Map<J, V> values = delegate.get(key);
      return values != null ? values.get(valueIndex) : null;
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * Inserts the value in a map indexed by the given value index in the map for the given key. If a
   * map for the current key does not yet exist, one is created. Returns the replaced value in the
   * map, if any, or null otherwise.
   */
  public V put(final K key, final J valueIndex, final V value)
  {
    writeLock().lock();
    try
    {
      return putIfAbsent(key).put(valueIndex, value);
    }
    finally
    {
      writeLock().unlock();
    }
  }

  /**
   * If no value map is associated with the given key, a new one is created, associated with the
   * key, and returned. If a map already exists for the key, it is returned.
   */
  public Map<J, V> putKey(final K key)
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
   * Removes all values currently mapped to this key, and removes the key from the relationship.
   * Returns the removed values, if any, or null otherwise.
   */
  public Map<J, V> remove(final K key)
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
   * Removes the value indexed by the given value index in the map for the given key. Returns the
   * removed value, if any, or null otherwise.
   */
  public V remove(final K key, final J valueIndex)
  {
    writeLock().lock();
    try
    {
      final Map<J, V> map = delegate.get(key);
      return map != null ? map.remove(valueIndex) : null;
    }
    finally
    {
      writeLock().unlock();
    }
  }

  /**
   * Returns the size of the delegate map.
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

  /**
   * Returns a collection associated with the key. If no such collection exists, an empty one is
   * returned.
   */
  @SuppressWarnings("unchecked")
  public Collection<V> values(final K key)
  {
    readLock().lock();
    try
    {
      final Map<J, V> values = delegate.get(key);
      return values != null ? values.values() : Collections.EMPTY_LIST;
    }
    finally
    {
      readLock().unlock();
    }
  }

  protected Map<J, V> putIfAbsent(final K key)
  {
    Map<J, V> result = delegate.get(key);
    if (result == null)
    {
      if (isSortedMap)
      {
        result = TypeTools.newSortedMap();
      }
      else
      {
        result = TypeTools.newHashMap();
      }
      delegate.put(key, result);
    }
    return result;
  }
}