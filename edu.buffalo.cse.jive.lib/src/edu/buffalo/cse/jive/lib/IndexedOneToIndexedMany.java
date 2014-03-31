package edu.buffalo.cse.jive.lib;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * One key (of type K) can be associated to multiple values (of type V), each of which is indexed by
 * a value index (of type J). Each key is indexed by a key index (of type I).
 */
public class IndexedOneToIndexedMany<I, K, J, V>
{
  private final OneToIndexedMany<K, J, V> delegate;
  private final Map<I, K> indexToKey;

  public IndexedOneToIndexedMany()
  {
    this(false);
  }

  public IndexedOneToIndexedMany(final boolean isSortedMap)
  {
    this.delegate = new OneToIndexedMany<K, J, V>(isSortedMap);
    this.indexToKey = TypeTools.newHashMap();
  }

  /**
   * Return all values in the delegate.
   */
  public Collection<V> allValues()
  {
    return delegate.allValues();
  }

  /**
   * Clears the delegate and the key index.
   */
  public void clear()
  {
    writeLock().lock();
    try
    {
      delegate.clear();
      indexToKey.clear();
    }
    finally
    {
      writeLock().unlock();
    }
  }

  /**
   * True if the key index maps to a valid key and the delegate contains an entry for this key.
   */
  public boolean containsKey(final I keyIndex)
  {
    readLock().lock();
    try
    {
      final K key = indexToKey.get(keyIndex);
      return key != null && delegate.containsKey(key);
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * True if the key index maps to a valid key and the delegate contains a value for the key and
   * value index pair.
   */
  public boolean containsValue(final I keyIndex, final J valueIndex)
  {
    readLock().lock();
    try
    {
      final K key = indexToKey.get(keyIndex);
      return key != null && delegate.containsValue(key, valueIndex);
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * True if the key index maps to a valid key and the delegate contains a value for the key.
   */
  public boolean containsValues(final I keyIndex)
  {
    readLock().lock();
    try
    {
      final K key = indexToKey.get(keyIndex);
      return key != null && delegate.containsValues(key);
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * True if the delegate is empty.
   */
  public boolean isEmpty()
  {
    return delegate.isEmpty();
  }

  /**
   * If the key index maps to a valid key, the returns the map corresponding to the children
   * associated with the key, otherwise an empty map is returned.
   */
  @SuppressWarnings("unchecked")
  public Map<J, V> lookupChildren(final I keyIndex)
  {
    readLock().lock();
    try
    {
      final K key = indexToKey.get(keyIndex);
      return key != null ? delegate.lookupChildren(key) : Collections.EMPTY_MAP;
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * Returns the key value associated with the given key index.
   */
  public K lookupKey(final I keyIndex)
  {
    readLock().lock();
    try
    {
      return indexToKey.get(keyIndex);
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * Returns all keys in the delegate.
   */
  public Set<K> lookupKeys()
  {
    return delegate.lookupKeys();
  }

  /**
   * If the key index maps to a valid key, the delegate returns the value associated with the key
   * and value index pair, or null if one does not exist.
   */
  public V lookupValue(final I keyIndex, final J valueIndex)
  {
    readLock().lock();
    try
    {
      final K key = indexToKey.get(keyIndex);
      return delegate.lookupValue(key, valueIndex);
    }
    finally
    {
      readLock().unlock();
    }
  }

  /**
   * Indexes the key and then delegates the insertion.
   */
  public V put(final I keyIndex, final J valueIndex, final V value)
  {
    writeLock().lock();
    try
    {
      final K key = indexToKey.get(keyIndex);
      return delegate.put(key, valueIndex, value);
    }
    finally
    {
      writeLock().unlock();
    }
  }

  public Map<J, V> putKey(final I keyIndex, final K key)
  {
    writeLock().lock();
    try
    {
      indexToKey.put(keyIndex, key);
      return delegate.putKey(key);
    }
    finally
    {
      writeLock().unlock();
    }
  }

  /**
   * If the key index maps to a valid key, all values associated with the key are removed from the
   * delegate and the key index is removed. Returns true if a removal occurs.
   */
  public Map<J, V> remove(final I keyIndex)
  {
    writeLock().lock();
    try
    {
      final K key = indexToKey.remove(keyIndex);
      return key != null ? delegate.remove(key) : null;
    }
    finally
    {
      writeLock().unlock();
    }
  }

  /**
   * If the key index maps to a valid key, the delegate removes the value associated with the key
   * and value index pair. Returns the removed value, if any, or null otherwise.
   */
  public V remove(final I keyIndex, final J valueIndex)
  {
    writeLock().lock();
    try
    {
      final K key = indexToKey.get(keyIndex);
      return key != null ? delegate.remove(key, valueIndex) : null;
    }
    finally
    {
      writeLock().unlock();
    }
  }

  /**
   * Returns the size of the delegate.
   */
  public int size()
  {
    return delegate.size();
  }

  /**
   * If the key index maps to a valid key, the delegate returns a collection of values associated
   * with the key, otherwise an empty collection is returned.
   */
  @SuppressWarnings("unchecked")
  public Collection<V> values(final I keyIndex)
  {
    readLock().lock();
    try
    {
      final K key = indexToKey.get(keyIndex);
      return key != null ? delegate.values(key) : Collections.EMPTY_LIST;
    }
    finally
    {
      readLock().unlock();
    }
  }

  private ReadLock readLock()
  {
    return delegate.readLock();
  }

  private WriteLock writeLock()
  {
    return delegate.writeLock();
  }
}