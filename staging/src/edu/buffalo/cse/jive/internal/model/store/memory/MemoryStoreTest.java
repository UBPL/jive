package edu.buffalo.cse.jive.internal.model.store.memory;

import java.io.FileNotFoundException;
import java.io.IOException;

import edu.buffalo.cse.jive.model.IMemoryStore.IReference;

public class MemoryStoreTest
{
  final private static int SIZE = 10 * 1000 * 1000;

  public static void main(final String args[]) throws FileNotFoundException, IOException
  {
    MemoryStoreTest.testMMF();
    // testAL();
    //
    System.gc();
    //
    System.err.format("Heap[%,d MB]\n", MemoryStoreTest.usedMemory() / 1024 / 1024);
  }

  // pre-allocate array list with capacity of 10M
  // create 10M strings
  // add 10M strings to array list (no re-allocation)
  public static void testAL()
  {
    final String[] strings = new String[MemoryStoreTest.SIZE];
    //
    long start = System.nanoTime();
    //
    for (int i = 0; i < MemoryStoreTest.SIZE; i++)
    {
      strings[i] = "XX_" + i + "_UU_" + (1000000 - i) + "_ZZ";
    }
    //
    long time = System.nanoTime() - start;
    //
    System.gc();
    //
    System.err.format("Time[%4.2f sec]\n", (time / 1000 / 1000 / 1000.0));
    System.err.format("Heap[%,d MB]\n", MemoryStoreTest.usedMemory() / 1024 / 1024);
    System.err.format("Throughput[%4.2fM objects/sec]\n", MemoryStoreTest.SIZE / (time / 1000.0));
    // if (used == 0)
    // {
    // System.err.println("You need to use -XX:-UsedTLAB to see small changes in memory usage.");
    // }
    //
    //
    start = System.nanoTime();
    //
    for (int i = 0; i < MemoryStoreTest.SIZE; i++)
    {
      if (i % 100000 == 0)
      {
        System.err.println(strings[i]);
      }
    }
    //
    time = System.nanoTime() - start;
    //
    // System.gc();
    //
    System.err.format("Time[%4.2f sec]\n", (time / 1000 / 1000 / 1000.0));
    System.err.format("Heap[%,d MB]\n", MemoryStoreTest.usedMemory() / 1024 / 1024);
  }

  // pre-allocate array list with capacity of 10M
  // store 10M strings
  // add 10M string references to array list (no re-allocation)
  public static void testMMF() throws FileNotFoundException, IOException
  {
    final MemoryStore s = new MemoryStore();
    try
    {
      final IReference[] strings = new IReference[MemoryStoreTest.SIZE];
      //
      long start = System.nanoTime();
      //
      for (int i = 0; i < MemoryStoreTest.SIZE; i++)
      {
        strings[i] = s.putString("XX_" + i + "_UU_" + (1000000 - i) + "_ZZ");
      }
      //
      long time = System.nanoTime() - start;
      //
      System.gc();
      //
      System.err.format("Time[%4.2f sec]\n", (time / 1000 / 1000 / 1000.0));
      System.err.format("Size[%,d MB]\n", s.size() / 1024 / 1024);
      System.err.format("Heap[%,d MB]\n", MemoryStoreTest.usedMemory() / 1024 / 1024);
      System.err.format("Throughput[%4.2fM objects/sec]\n", MemoryStoreTest.SIZE / (time / 1000.0));
      // if (used == 0)
      // {
      // System.err.println("You need to use -XX:-UsedTLAB to see small changes in memory usage.");
      // }
      //
      start = System.nanoTime();
      //
      for (int i = 0; i < MemoryStoreTest.SIZE; i++)
      {
        if (i % 100000 == 0)
        {
          System.err.println(s.getString(strings[i]));
        }
      }
      //
      time = System.nanoTime() - start;
      //
      // System.gc();
      //
      System.err.format("Time[%4.2f sec]\n", (time / 1000 / 1000 / 1000.0));
      System.err.format("Heap[%,d MB]\n", MemoryStoreTest.usedMemory() / 1024 / 1024);
    }
    finally
    {
      s.close();
    }
    // for (final IString is : strings)
    // {
    // System.out.println(s.getString(is));
    // }
  }

  private static long usedMemory()
  {
    return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
  }
}