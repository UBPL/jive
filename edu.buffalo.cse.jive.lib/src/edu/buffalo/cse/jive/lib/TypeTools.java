package edu.buffalo.cse.jive.lib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class TypeTools
{
  public static <T> ArrayList<T> newArrayList()
  {
    return new ArrayList<T>();
  }

  public static <T> List<T> newArrayList(final int capacity)
  {
    return new ArrayList<T>(capacity);
  }

  public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap()
  {
    return new ConcurrentHashMap<K, V>();
  }

  public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap(final int size)
  {
    return new ConcurrentHashMap<K, V>(size);
  }

  public static <K> Set<K> newConcurrentSet()
  {
    return Collections.newSetFromMap(new ConcurrentHashMap<K, Boolean>());
  }

  public static <K> Set<K> newConcurrentSet(final int size)
  {
    return Collections.newSetFromMap(new ConcurrentHashMap<K, Boolean>(size));
  }

  public static <K, V> HashMap<K, V> newHashMap()
  {
    return new HashMap<K, V>();
  }

  public static <K, V> HashMap<K, V> newHashMap(final int size)
  {
    return new HashMap<K, V>(size);
  }

  public static <T> Set<T> newHashSet()
  {
    return Collections.newSetFromMap(new HashMap<T, Boolean>());
  }

  public static <T> Set<T> newHashSet(final int size)
  {
    return Collections.newSetFromMap(new HashMap<T, Boolean>(size));
  }

  public static <K, V> LinkedHashMap<K, V> newLinkedHashMap()
  {
    return new LinkedHashMap<K, V>();
  }

  public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(final int size)
  {
    return new LinkedHashMap<K, V>(size);
  }

  // public static <T> LinkedList<T> newLinkedList() {
  //
  // return new LinkedList<T>();
  // }
  public static <T> Set<T> newLinkedHashSet()
  {
    return new LinkedHashSet<T>();
  }

  public static <K, V> SortedMap<K, V> newSortedMap()
  {
    return new TreeMap<K, V>();
  }

  public static <M> Set<M> newSortedSet()
  {
    return new TreeSet<M>();
  }

  public static <T> List<T> newUnmodifiableList(final Collection<? extends T> source)
  {
    final List<T> list = TypeTools.newArrayList();
    list.addAll(source);
    return Collections.unmodifiableList(list);
  }
}
