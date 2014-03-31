package edu.buffalo.cse.jive.lib;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * A replacement for an ArrayList that inserts in constant time, not amortized constant time. No
 * resizing is necessary because inserts are always directed to the last inserted page or a newly
 * allocated page if necessary. Each page is backed by an efficient list, such as an ArrayList, and
 * the pages are kept in a hash map for constant time access to any page by page index.
 * 
 * Informal tests with large number of elements (in the millions) give the paged array list a
 * significant advantage over the array list counterpart (about 20% in insertion time).
 * 
 * This list is append-only, therefore, {@code add(E)} and {@code addAll(Collection<? extends E>}
 * are the only mutator operations supported.
 */
public class PagedArrayList<E> implements List<E>
{
  public final static int INITIAL_PAGE_COUNT = 128;
  public final static int DEFAULT_PAGE_SIZE = 8 * 1024;
  private final Map<Integer, List<E>> pages;
  private final int pageSize;

  public PagedArrayList()
  {
    this(PagedArrayList.DEFAULT_PAGE_SIZE);
  }

  public PagedArrayList(final int pageSize)
  {
    this.pages = TypeTools.newHashMap(PagedArrayList.INITIAL_PAGE_COUNT);
    this.pageSize = pageSize;
  }

  @Override
  public boolean add(final E e)
  {
    if (pages.isEmpty() || pages.get(pages.size() - 1).size() == pageSize)
    {
      final List<E> page = TypeTools.newArrayList(pageSize);
      pages.put(pages.size(), page);
    }
    return pages.get(pages.size() - 1).add(e);
  }

  @Override
  public void add(final int index, final E element)
  {
    throw new UnsupportedOperationException(
        "Insertion at a particular index is not supported by a PagedList instance.");
  }

  @Override
  public boolean addAll(final Collection<? extends E> c)
  {
    boolean result = false;
    for (final E element : c)
    {
      result = add(element) || result;
    }
    return result;
  }

  @Override
  public boolean addAll(final int index, final Collection<? extends E> c)
  {
    throw new UnsupportedOperationException(
        "Multiple insertions are only supported at the end of a PagedList instance.");
  }

  @Override
  public void clear()
  {
    for (final List<E> page : pages.values())
    {
      page.clear();
    }
    pages.clear();
  }

  @Override
  public boolean contains(final Object o)
  {
    for (final List<E> page : pages.values())
    {
      if (page.contains(o))
      {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsAll(final Collection<?> c)
  {
    for (final List<E> page : pages.values())
    {
      for (final Object item : c)
      {
        if (!page.contains(item))
        {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public E get(final int index)
  {
    return pages.get(index / pageSize).get(index % pageSize);
  }

  @Override
  public int indexOf(final Object o)
  {
    int index = 0;
    int pageIndex = 0;
    for (final List<E> page : pages.values())
    {
      index = page.indexOf(o);
      if (index != -1)
      {
        return pageIndex * pageSize + index;
      }
      pageIndex++;
    }
    return -1;
  }

  @Override
  public boolean isEmpty()
  {
    return pages.isEmpty();
  }

  @Override
  public Iterator<E> iterator()
  {
    return new PagedListIterator();
  }

  @Override
  public int lastIndexOf(final Object o)
  {
    int index = -1;
    for (int pageIndex = pages.size(); pageIndex >= 0; pageIndex--)
    {
      index = pages.get(pageIndex).lastIndexOf(o);
      if (index != -1)
      {
        return pageIndex * pageSize + index;
      }
    }
    return -1;
  }

  @Override
  public ListIterator<E> listIterator()
  {
    return new PagedListIterator();
  }

  @Override
  public ListIterator<E> listIterator(final int index)
  {
    return new PagedListIterator(index);
  }

  @Override
  public E remove(final int index)
  {
    throw new UnsupportedOperationException(
        "Cannot retain or remove elements from a PagedList instance.");
  }

  @Override
  public boolean remove(final Object o)
  {
    throw new UnsupportedOperationException(
        "Cannot retain or remove elements from a PagedList instance.");
  }

  @Override
  public boolean removeAll(final Collection<?> c)
  {
    throw new UnsupportedOperationException(
        "Cannot retain or remove elements from a PagedList instance.");
  }

  @Override
  public boolean retainAll(final Collection<?> c)
  {
    throw new UnsupportedOperationException(
        "Cannot retain or remove elements from a PagedList instance.");
  }

  @Override
  public E set(final int index, final E element)
  {
    return pages.get(index / pageSize).set(index % pageSize, element);
  }

  @Override
  public int size()
  {
    return pages.isEmpty() ? 0 : (pages.size() - 1) * pageSize + pages.get(pages.size() - 1).size();
  }

  @Override
  public List<E> subList(final int fromIndex, final int toIndex)
  {
    throw new UnsupportedOperationException("Cannot extract a sublist from a PagedList instance.");
  }

  @Override
  public Object[] toArray()
  {
    final Object[] result = new Object[size()];
    for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++)
    {
      final List<E> page = pages.get(pageIndex);
      System.arraycopy(page.toArray(), 0, result, pageSize * pageIndex, page.size());
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] a)
  {
    if (a.length < size())
    {
      a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size());
    }
    for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++)
    {
      final List<E> page = pages.get(pageIndex);
      System
          .arraycopy(page.toArray((T[]) new Object[] {}), 0, a, pageSize * pageIndex, page.size());
    }
    return a;
  }

  // public static void main(String[] args) {
  //
  // // testArrayList(64 * M);
  // // testPagedList(64 * M);
  // }
  //
  // private final static int M = 1000 * 1000;
  //
  // // 15775ms (64M)
  // private static void testArrayList(int size) {
  //
  // final List<Integer> list = Tools.newArrayList();
  // loop(list, size);
  // }
  //
  // // ~13s (64M/64K)
  // private static void testPagedList(int size) {
  //
  // final List<Integer> list = new PagedArrayList<Integer>();
  // loop(list, size);
  // }
  //
  // private static void loop(List<Integer> list, int size) {
  //
  // final long start = System.nanoTime();
  // for (int i = size; i >= 0; i--) {
  // list.add(-i);
  // }
  // final long end = System.nanoTime();
  // System.err.println((end - start) / 1000000 + " ms");
  // }
  final class PagedListIterator implements ListIterator<E>
  {
    private ListIterator<E> current;
    private int pageIndex;

    PagedListIterator()
    {
      this(0);
    }

    PagedListIterator(final int index)
    {
      this.pageIndex = index / pageSize;
      current = pages.get(pageIndex).listIterator(index % pageSize);
    }

    @Override
    public void add(final E e)
    {
      throw new UnsupportedOperationException(
          "Modifications not supported in a PagedListIterator instance.");
    }

    @Override
    public boolean hasNext()
    {
      return current.hasNext() || pageIndex < (pages.size() - 1);
    }

    @Override
    public boolean hasPrevious()
    {
      return current.hasPrevious() || pageIndex > 0;
    }

    @Override
    public E next()
    {
      if (!current.hasNext())
      {
        pageIndex++;
        if (pageIndex == pages.size())
        {
          throw new IllegalAccessError("No next element in the PagedListIterator instance.");
        }
        current = pages.get(pageIndex).listIterator();
      }
      return current.next();
    }

    @Override
    public int nextIndex()
    {
      return pageIndex * pageSize + current.nextIndex();
    }

    @Override
    public E previous()
    {
      if (!current.hasPrevious())
      {
        pageIndex--;
        if (pageIndex == -1)
        {
          throw new IllegalAccessError("No previous element in the PagedListIterator instance.");
        }
        final List<E> page = pages.get(pageIndex);
        current = page.listIterator(page.size());
      }
      return current.previous();
    }

    @Override
    public int previousIndex()
    {
      return pageIndex * pageSize + current.previousIndex();
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException(
          "Modifications not supported in a PagedListIterator instance.");
    }

    @Override
    public void set(final E e)
    {
      throw new UnsupportedOperationException(
          "Modifications not supported in a PagedListIterator instance.");
    }
  }
}
