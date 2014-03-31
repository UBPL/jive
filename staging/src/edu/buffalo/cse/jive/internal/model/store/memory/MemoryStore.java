package edu.buffalo.cse.jive.internal.model.store.memory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import edu.buffalo.cse.jive.model.IMemoryStore;

/**
 * Transient store backed by an auto-growing memory-mapped file.
 */
public class MemoryStore extends ReentrantReadWriteLock implements IMemoryStore
{
  private static final long serialVersionUID = 3273082451422530792L;
  private static final int INTEGER_SIZE = 4;
  private static final int BUFFER_SIZE = 1 << 28; // 256MB
  private final List<MappedByteBuffer> mbbs;
  private final RandomAccessFile raf;
  private final File temp;
  //
  private long eof;

  public MemoryStore()
  {
    this.mbbs = new ArrayList<MappedByteBuffer>();
    this.eof = 0;
    File localTemp = null;
    try
    {
      localTemp = new File("/opt/tmp/" + File.createTempFile("__mm", ".jive").getName());
    }
    catch (final IOException e)
    {
      e.printStackTrace();
    }
    RandomAccessFile localRaf = null;
    try
    {
      if (localTemp != null)
      {
        localRaf = new RandomAccessFile(localTemp.getAbsolutePath(), "rw");
      }
    }
    catch (final FileNotFoundException e)
    {
      e.printStackTrace();
    }
    this.temp = localTemp;
    this.raf = localRaf;
  }

  @Override
  public byte[] getBytes(final IReference ref)
  {
    readLock().lock();
    try
    {
      final long position = ((Reference) ref).id();
      // buffer and offset
      final int buffer = (int) (position / MemoryStore.BUFFER_SIZE);
      final int offset = (int) (position % MemoryStore.BUFFER_SIZE);
      // buffer to read
      final MappedByteBuffer mbb = mbbs.get(buffer);
      // offset to read
      mbb.position(offset);
      // read the length in bytes
      final int len = mbb.getInt();
      // allocate the bytes for the object
      final byte[] bytes = new byte[len];
      // read the bytes
      mbb.get(bytes);
      // return
      return bytes;
    }
    finally
    {
      readLock().unlock();
    }
  }

  @Override
  public String getString(final IReference ref)
  {
    return new String(getBytes(ref));
  }

  @Override
  public void close() throws IOException
  {
    for (final MappedByteBuffer mbb : mbbs)
    {
      mbb.clear();
    }
    raf.close();
    temp.delete();
  }

  @Override
  public IReference putBytes(final byte[] data)
  {
    writeLock().lock();
    try
    {
      // the data we need to store: size and string
      final int payload = data.length + MemoryStore.INTEGER_SIZE;
      // check if the payload fits in the current buffer
      if ((eof + payload) > MemoryStore.BUFFER_SIZE * mbbs.size())
      {
        try
        {
          grow();
        }
        catch (final IOException e)
        {
          e.printStackTrace();
        }
      }
      // position where we store the string
      final long refId = eof;
      // buffer/offset coordinates
      final int buffer = (int) (eof / MemoryStore.BUFFER_SIZE);
      final int offset = (int) (eof % MemoryStore.BUFFER_SIZE);
      // buffer to store
      final MappedByteBuffer mbb = mbbs.get(buffer);
      // offset to append the string
      mbb.position(offset);
      // length and bytes
      mbb.putInt(data.length);
      mbb.put(data);
      // update the eof cursor
      eof += payload;
      // return the string reference
      return new Reference(refId);
    }
    finally
    {
      writeLock().unlock();
    }
  }

  @Override
  public IReference putStorable(final IStorable storable)
  {
    if (storable == null)
    {
      return new Reference(-1);
    }
    return putBytes(storable.bytes());
  }

  @Override
  public IReference putString(final String string)
  {
    if (string == null)
    {
      return Reference.NULL;
    }
    return putBytes(string.getBytes());
  }

  @Override
  public long size()
  {
    return eof;
  }

  private void grow() throws IOException
  {
    final long offset = mbbs.size() * MemoryStore.BUFFER_SIZE;
    mbbs.add(raf.getChannel().map(FileChannel.MapMode.READ_WRITE, offset, MemoryStore.BUFFER_SIZE));
    // move the EOF to the beginning of the new buffer
    eof = (mbbs.size() - 1) * MemoryStore.BUFFER_SIZE;
  }

  private final static class Reference implements IReference
  {
    public final static IReference NULL = new Reference(-1);
    
    private final long id;

    Reference(final long id)
    {
      this.id = id;
    }

    @Override
    public boolean equals(Object other)
    {
      return other instanceof Reference && ((Reference) other).id == id;
    }

    long id()
    {
      return this.id;
    }
  }
}
