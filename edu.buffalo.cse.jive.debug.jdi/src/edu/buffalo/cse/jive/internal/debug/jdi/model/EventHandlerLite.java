package edu.buffalo.cse.jive.internal.debug.jdi.model;

import java.util.Stack;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayType;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.event.AccessWatchpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.ModificationWatchpointEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;

import edu.buffalo.cse.jive.debug.jdi.model.IJDIEventHandler;
import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.lib.TypeTools;

/**
 * 1) Create the memory mapped to back the event store.
 * 2) Create a flyweight object that maps to a position in the event store and exposed its data.
 * 3) Support get/set methods that read/write events to the store.
 * 
 * @author demian
 *
 */
public class EventHandlerLite implements IJDIEventHandler
{
  private final static String ENCODED_FIELD_READ = "FR....: %016x-%016x, oId.....: %016x, fId...: %016x\n";
  private final static String ENCODED_FIELD_WRITE = "FW....: %016x-%016x, oId.....: %016x, fId...: %016x, vId...: %016x\n";
  private final static String ENCODED_LINE_STEP = "STP...: %016x-%016x\n";
  private final static String ENCODED_METHOD_ENTERED = "MEN...: %016x-%016x, pfrId...: %016x, oId...: %016x\n";
  private final static String ENCODED_METHOD_EXITING = "MEX...: %016x-%016x, nfrId...: %016x\n";
  private final static String ENCODED_METHOD_RETURNED = "MRT...: %016x-%016x\n";
  private final static String ENCODED_OBJECT_NEW = "ON....: %016x-%016x, tId...: %016x, oId...: %016x\n";
  private final static String ENCODED_TYPE_LOAD = "TL....: %016x-%016x, tId...: %016x\n";
  //
  private final static long KIND_FIELD_READ = 1L;
  private final static long KIND_FIELD_WRITE = 2L;
  private final static long KIND_LINE_STEP = 3L;
  private final static long KIND_METHOD_CALL = 4L;
  private final static long KIND_METHOD_ENTERED = 5L;
  private final static long KIND_METHOD_EXITING = 6L;
  private final static long KIND_METHOD_RETURNED = 7L;
  private final static long KIND_OBJECT_NEW = 8L;
  private final static long KIND_TYPE_LOAD = 9L;
  //
  private final static long ID_NONE = -1L;
  // bits per element, LONG #1
  private static final byte BITS_TYPE_ID = 14; // 16K maximum type definitions per program
  private static final byte BITS_METHOD_ID = 20; // 1M maximum method definitions per program
  private static final byte BITS_SOURCE_ID = 14; // 16K maximum source files per program
  private static final byte BITS_LINE_NO = 16; // 65K maximum lines per file
  // bits per element, LONG #2
  private static final byte BITS_KIND = 5; // 32 maximum event kinds
  private static final byte BITS_THREAD_ID = 14; // 16K maximum threads per execution
  private static final byte BITS_STACK_DEPTH = 15; // 32K maximum stack depth per stack
  private static final byte BITS_STACK_ID = 30; // 1B maximum stack frames per execution
  // bits per element, LONG #3
  private static final long BITS_OBJECT_ID = 32; // 4.3B maximum objects
  // mask per element, LONG #1
  private static final long MASK_TYPE_ID = 0x0000000000003FFF; // 14 bits ~ 16K
  private static final long MASK_METHOD_ID = 0x00000000000FFFFF; // 20 bits ~ 1M
  private static final long MASK_SOURCE_ID = 0x0000000000003FFF; // 14 bits ~ 16K
  private static final long MASK_LINE_NO = 0x000000000000FFFF; // 16 bits ~ 65K
  // mask per element, LONG #2
  private static final long MASK_KIND = 0x000000000000001F; // 5 bits ~ 32
  private static final long MASK_THREAD_ID = 0x0000000000003FFF; // 14 bits ~ 16K
  private static final long MASK_STACK_DEPTH = 0x0000000000007FFF; // 15 bits ~ 32K
  private static final long MASK_STACK_ID = 0x000000003FFFFFFF; // 30 bits ~ 1B
  // mask per element, LONG #3
  private static final long MASK_OBJECT_ID = 0x00000000FFFFFFFF; // 32 bits ~ 4.3B
  //
  private final Registry registry;
  private LocatableEvent currentEvent;
  private ThreadReference currentThread;

  public EventHandlerLite(final IJiveDebugTarget owner)
  {
    this.registry = new Registry();
    this.currentEvent = null;
  }

  @Override
  public void createSnapshot(final Object jvm)
  {
    // TODO Auto-generated method stub
  }

  /**
   * Notification of a field access in the target VM. Field modifications are not considered field
   * accesses.
   * 
   * @param event
   * 
   * @see <a href="">http://docs.oracle.com/javase/7/docs/jdk/api/jpda/jdi/index.html</a>
   */
  @Override
  public void jdiAccessWatchpoint(final AccessWatchpointEvent event)
  {
    final Method method = event.location().method();
    if (method.isSynthetic() || method.isBridge() || method.name().contains("$"))
    {
      return;
    }
    final Field field = event.field();
    if (field.isSynthetic() || field.name().contains("$"))
    {
      return;
    }
    //
    this.currentEvent = event;
    this.currentThread = event.thread();
    //
    final long fId = registry.getFieldId(event.field());
    //
    final long oId = registry.getObjectId(event.object());
    //
    final long[] eventId = registry.encode(EventHandlerLite.KIND_FIELD_READ);
    //
    System.out.format(EventHandlerLite.ENCODED_FIELD_READ, eventId[0], eventId[1], oId, fId);
    //
    System.out.println(registry.decode(eventId));
    System.out.print(registry.decodeObject(oId));
    System.out.print(registry.decodeField(fId));
    //
    this.currentEvent = null;
    this.currentThread = null;
  }

  /**
   * Notification of a class prepare in the target VM. See the JVM specification for a definition of
   * class preparation. Class prepare events are not generated for primitive classes (for example,
   * java.lang.Integer.TYPE).
   * 
   * @param event
   * 
   * @see <a href="">http://docs.oracle.com/javase/7/docs/jdk/api/jpda/jdi/index.html</a>
   */
  @Override
  public void jdiClassPrepare(final ClassPrepareEvent event)
  {
    //
    this.currentEvent = null;
    this.currentThread = event.thread();
    //
    jdiTypeLoad(event.referenceType());
    //
    //
    this.currentEvent = null;
    this.currentThread = null;
  }

  /**
   * Notification of an exception in the target VM. When an exception is thrown which satisfies a
   * currently enabled exception request, an event set containing an instance of this class will be
   * added to the VM's event queue. If the exception is thrown from a non-native method, the
   * exception event is generated at the location where the exception is thrown. If the exception is
   * thrown from a native method, the exception event is generated at the first non-native location
   * reached after the exception is thrown.
   * 
   * @param event
   * 
   * @see <a href="">http://docs.oracle.com/javase/7/docs/jdk/api/jpda/jdi/index.html</a>
   */
  @Override
  public void jdiExceptionThrown(final ExceptionEvent event)
  {
    //
    //
    this.currentEvent = event;
    this.currentThread = event.thread();
    //
    //
    this.currentEvent = null;
    this.currentThread = null;
  }

  /**
   * Notification of a method invocation in the target VM. This event occurs after entry into the
   * invoked method and before any code has executed. Method entry events are generated for both
   * native and non-native methods.
   * 
   * @param event
   * 
   * @see <a href="">http://docs.oracle.com/javase/7/docs/jdk/api/jpda/jdi/index.html</a>
   */
  @Override
  public void jdiMethodEntry(final MethodEntryEvent event)
  {
    final Method method = event.location().method();
    if (method.isSynthetic() || method.isBridge() || method.name().contains("$"))
    {
      return;
    }
    //
    this.currentEvent = event;
    this.currentThread = event.thread();
    //
    final long thId = registry.getThreadId(event.thread());
    //
    final long pfrId = registry.methodEntered(thId);
    //
    final long[] eventId = registry.encode(EventHandlerLite.KIND_METHOD_ENTERED);
    //
    System.out.format(EventHandlerLite.ENCODED_METHOD_ENTERED, eventId[0], eventId[1], pfrId,
        eventId[2]);
    //
    System.out.println(registry.decode(eventId));
    //
    this.currentEvent = null;
    this.currentThread = null;
  }

  /**
   * Notification of a method return in the target VM. This event is generated after all code in the
   * method has executed, but the location of this event is the last executed location in the
   * method. Method exit events are generated for both native and non-native methods. Method exit
   * events are not generated if the method terminates with a thrown exception.
   * 
   * @param event
   * 
   * @see <a href="">http://docs.oracle.com/javase/7/docs/jdk/api/jpda/jdi/index.html</a>
   */
  @Override
  public void jdiMethodExit(final MethodExitEvent event)
  {
    if (event.method().isSynthetic() || event.method().isBridge()
        || event.method().name().contains("$"))
    {
      return;
    }
    //
    this.currentEvent = event;
    this.currentThread = event.thread();
    //
    final long thId = registry.getThreadId(event.thread());
    //
    final long[] eventId = registry.encode(EventHandlerLite.KIND_METHOD_EXITING);
    //
    final long nfrId = registry.methodExiting(thId);
    //
    System.out.format(EventHandlerLite.ENCODED_METHOD_EXITING, eventId[0], eventId[1], nfrId);
    //
    System.out.println(registry.decode(eventId));
    //
    this.currentEvent = null;
    this.currentThread = null;
  }

  /**
   * Notification of a field modification in the target VM.
   * 
   * @param event
   * 
   * @see <a href="">http://docs.oracle.com/javase/7/docs/jdk/api/jpda/jdi/index.html</a>
   */
  @Override
  public void jdiModificationWatchpoint(final ModificationWatchpointEvent event)
  {
    final Method method = event.location().method();
    if (method.isSynthetic() || method.isBridge() || method.name().contains("$"))
    {
      return;
    }
    final Field field = event.field();
    if (field.isSynthetic() || field.name().contains("$"))
    {
      return;
    }
    //
    this.currentEvent = event;
    this.currentThread = event.thread();
    //
    final long fId = registry.getFieldId(field);
    //
    final long oId = registry.getObjectId(event.object());
    //
    final long vId = registry.getValueId(event.valueToBe());
    //
    final long[] eventId = registry.encode(EventHandlerLite.KIND_FIELD_WRITE);
    //
    System.out.format(EventHandlerLite.ENCODED_FIELD_WRITE, eventId[0], eventId[1], oId, fId, vId);
    //
    System.out.print(registry.decode(eventId));
    System.out.print(registry.decodeObject(oId));
    System.out.print(registry.decodeField(fId));
    System.out.println(registry.decodeValue(vId));
    //
    this.currentEvent = null;
    this.currentThread = null;
  }

  public void jdiObjectNew(final ObjectReference object)
  {
    if (currentEvent == null)
    {
      return;
    }
    //
    final long[] eventId = registry.encode(EventHandlerLite.KIND_OBJECT_NEW);
    //
    final long tId = registry.getTypeId(object.referenceType()) & EventHandlerLite.MASK_OBJECT_ID;
    //
    final long oId = registry.getObjectId(object) & EventHandlerLite.MASK_OBJECT_ID;
    //
    System.out.format(EventHandlerLite.ENCODED_OBJECT_NEW, eventId[0], eventId[1], tId, oId);
    //
    System.out.print(registry.decode(eventId));
    System.out.print(registry.decodeType(tId));
    System.out.println(registry.decodeObject(oId));
  }

  @Override
  public void jdiStep(final StepEvent event)
  {
    final Method method = event.location().method();
    if (method.isSynthetic() || method.isBridge() || method.name().contains("$"))
    {
      return;
    }
    //
    this.currentEvent = event;
    this.currentThread = event.thread();
    //
    final long[] eventId = registry.encode(EventHandlerLite.KIND_LINE_STEP);
    //
    System.out.format(EventHandlerLite.ENCODED_LINE_STEP, eventId[0], eventId[1]);
    //
    System.out.println(registry.decode(eventId));
    //
    this.currentEvent = null;
    this.currentThread = null;
  }

  @Override
  public void jdiThreadDeath(final ThreadDeathEvent event)
  {
    //
    this.currentEvent = null;
    this.currentThread = event.thread();
    //
    this.currentEvent = null;
    this.currentThread = null;
  }

  @Override
  public void jdiThreadStart(final ThreadStartEvent event)
  {
    //
    this.currentEvent = null;
    this.currentThread = event.thread();
    //
    this.currentEvent = null;
    this.currentThread = null;
  }

  public void jdiTypeLoad(final ReferenceType type)
  {
    if (registry.contains(type))
    {
      return;
    }
    if (type instanceof ClassType)
    {
      jdiTypeLoadClass((ClassType) type);
    }
    else if (type instanceof InterfaceType)
    {
      jdiTypeLoadInterface((InterfaceType) type);
    }
    else if (type instanceof ArrayType)
    {
      jdiTypeLoadArray((ArrayType) type);
    }
  }

  public void jdiTypeLoadArray(final ArrayType type)
  {
    try
    {
      final Type componentType = type.componentType();
      if (componentType instanceof ReferenceType)
      {
        jdiTypeLoad(((ReferenceType) componentType));
      }
    }
    catch (final ClassNotLoadedException e)
    {
      System.err.println("Error: type not loaded for array '" + type.signature() + "'");
      //
      String componentTypeName = type.signature();
      componentTypeName = componentTypeName.substring(componentTypeName.lastIndexOf('[') + 1);
      //
      registry.getTypeId(componentTypeName);
    }
    jdiTypeLoadComplete(type);
  }

  public void jdiTypeLoadClass(final ClassType type)
  {
    final ClassType superClass = type.superclass();
    if (superClass != null)
    {
      jdiTypeLoad(superClass);
    }
    for (final InterfaceType superInterface : type.interfaces())
    {
      jdiTypeLoad(superInterface);
    }
    jdiTypeLoadComplete(type);
  }

  public void jdiTypeLoadComplete(final ReferenceType type)
  {
    final long[] eventId = registry.encode(EventHandlerLite.KIND_TYPE_LOAD);
    //
    final long tId = registry.getTypeId(type) & EventHandlerLite.MASK_OBJECT_ID;
    //
    System.out.format(EventHandlerLite.ENCODED_TYPE_LOAD, eventId[0], eventId[1], tId);
    //
    System.out.print(registry.decode(eventId));
    System.out.println(registry.decodeType(tId));
  }

  public void jdiTypeLoadInterface(final InterfaceType type)
  {
    for (final InterfaceType superInterface : type.superinterfaces())
    {
      jdiTypeLoad(superInterface);
    }
    // only interested in interfaces with data
    if (type.fields().size() > 0)
    {
      jdiTypeLoadComplete(type);
    }
  }

  @Override
  public void jdiVMDeath(final VMDeathEvent event)
  {
    //
    this.currentEvent = null;
    this.currentThread = null;
    //
    this.currentEvent = null;
    this.currentThread = null;
  }

  @Override
  public void jdiVMDisconnect(final VMDisconnectEvent event)
  {
    //
    this.currentEvent = null;
    this.currentThread = null;
    //
    this.currentEvent = null;
    this.currentThread = null;
  }

  @Override
  public void jdiVMStart(final VMStartEvent event)
  {
    //
    this.currentEvent = null;
    this.currentThread = null;
    //
    this.currentEvent = null;
    this.currentThread = null;
  }

  @Override
  public void reset()
  {
    // ignored
  }

  private final class Registry
  {
    private final AtomicInteger FIELD_ID;
    private final AtomicInteger METHOD_ID;
    private final AtomicInteger OBJECT_ID;
    private final AtomicInteger SOURCE_ID;
    private final AtomicInteger THREAD_ID;
    private final AtomicInteger TYPE_ID;
    private final AtomicInteger VALUE_ID;
    private final AtomicInteger VARIABLE_ID;
    private final ConcurrentMap<String, String> index;
    private final ConcurrentMap<Long, StackMirror> stacks;
    private final ConcurrentMap<String, Long> store;

    public Registry()
    {
      this.FIELD_ID = new AtomicInteger(0);
      this.METHOD_ID = new AtomicInteger(0);
      this.OBJECT_ID = new AtomicInteger(0);
      this.SOURCE_ID = new AtomicInteger(0);
      this.THREAD_ID = new AtomicInteger(0);
      this.TYPE_ID = new AtomicInteger(0);
      this.VALUE_ID = new AtomicInteger(0);
      this.VARIABLE_ID = new AtomicInteger(0);
      this.index = TypeTools.newConcurrentHashMap();
      this.stacks = TypeTools.newConcurrentHashMap();
      this.store = TypeTools.newConcurrentHashMap();
    }

    public boolean contains(final ReferenceType type)
    {
      return store.containsKey(Signatures.getPrefixed(type));
    }

    public String decode(final long[] eventId)
    {
      final StringBuilder result = new StringBuilder("");
      //
      final long tId = (eventId[0] >> (EventHandlerLite.BITS_METHOD_ID
          + EventHandlerLite.BITS_SOURCE_ID + EventHandlerLite.BITS_LINE_NO))
          & EventHandlerLite.MASK_TYPE_ID;
      result.append("typeId.......: ");
      result.append(indexLookup(Signatures.PREFIX_TYPE, tId));
      result.append("\n");
      final long mId = (eventId[0] >> (EventHandlerLite.BITS_SOURCE_ID + EventHandlerLite.BITS_LINE_NO))
          & EventHandlerLite.MASK_METHOD_ID;
      result.append("methodId.....: ");
      result.append(indexLookup(Signatures.PREFIX_METHOD, mId));
      result.append("\n");
      final long srcId = (eventId[0] >> EventHandlerLite.BITS_LINE_NO)
          & EventHandlerLite.MASK_SOURCE_ID;
      final long lineNo = (eventId[0] & EventHandlerLite.MASK_LINE_NO);
      result.append("sourceId.....: ");
      result.append(indexLookup(Signatures.PREFIX_SOURCE, srcId));
      result.append("@");
      result.append(lineNo);
      result.append("\n");
      //
      final long kind = (eventId[1] >> (EventHandlerLite.BITS_STACK_DEPTH
          + EventHandlerLite.BITS_STACK_ID + EventHandlerLite.BITS_THREAD_ID))
          & EventHandlerLite.MASK_KIND;
      result.append("kind.........: ");
      result.append(kind);
      result.append("\n");
      final long thId = (eventId[1] >> (EventHandlerLite.BITS_STACK_DEPTH + EventHandlerLite.BITS_STACK_ID))
          & EventHandlerLite.MASK_THREAD_ID;
      result.append("threadId.....: ");
      result.append(indexLookup(Signatures.PREFIX_THREAD, thId));
      result.append("\n");
      final long depth = (eventId[1] >> (EventHandlerLite.BITS_STACK_ID))
          & EventHandlerLite.MASK_STACK_DEPTH;
      result.append("depth........: ");
      result.append(depth);
      result.append("\n");
      final long frId = eventId[1] & EventHandlerLite.MASK_STACK_ID;
      result.append("frameId......: ");
      result.append(frId);
      result.append("\n");
      //
      return result.toString();
    }

    public String decodeField(final long fId)
    {
      final StringBuilder result = new StringBuilder("");
      //
      result.append("fieldId......: ");
      result.append(indexLookup(Signatures.PREFIX_FIELD, fId));
      result.append("\n");
      //
      return result.toString();
    }

    public String decodeObject(final long oId)
    {
      final StringBuilder result = new StringBuilder("");
      //
      result.append("objectId.....: ");
      result.append(indexLookup(Signatures.PREFIX_OBJECT, oId));
      result.append("\n");
      //
      return result.toString();
    }

    public String decodeType(final long tId)
    {
      final StringBuilder result = new StringBuilder("");
      //
      result.append("typeId.......: ");
      result.append(indexLookup(Signatures.PREFIX_TYPE, tId));
      result.append("\n");
      //
      return result.toString();
    }

    public String decodeValue(final long vId)
    {
      final StringBuilder result = new StringBuilder("");
      //
      result.append("valueId......: ");
      result.append(indexLookup(Signatures.PREFIX_VALUE, vId));
      result.append("\n");
      //
      return result.toString();
    }

    public long[] encode(final long kind)
    {
      final long[] eventId = new long[3];
      final Location location = currentEvent != null ? currentEvent.location() : null;
      final ThreadReference th = currentEvent != null ? currentEvent.thread() : currentThread;
      //
      StackFrame fr = null;
      try
      {
        fr = th != null && th.frameCount() > 0 ? th.frame(0) : null;
      }
      catch (final IncompatibleThreadStateException e)
      {
        System.err.println(e.getMessage());
      }
      //
      final ObjectReference object = fr != null ? fr.thisObject() : null;
      //
      final Method method = location != null ? location.method() : (fr != null
          && fr.location() != null ? fr.location().method() : null);
      //
      // static: type
      final long tId = getTypeId(method != null ? method.declaringType() : null);
      //
      // static: method
      final long mId = getMethodId(method);
      //
      // runtime: object execution context
      final long oId = getObjectId(object);
      //
      // static: source + line number
      final long locId = getLocationId(location);
      //
      // runtime: thread
      final long thId = getThreadId(th);
      //
      // runtime: frame
      final long frId = getCurrentFrameId(thId);
      //
      // runtime: stack depth
      long depth = EventHandlerLite.ID_NONE;
      try
      {
        depth = th.frameCount();
      }
      catch (final IncompatibleThreadStateException e)
      {
        System.err.println(e.getMessage());
      }
      // encode the static part
      eventId[0] = ((tId & EventHandlerLite.MASK_TYPE_ID) << (EventHandlerLite.BITS_METHOD_ID
          + EventHandlerLite.BITS_SOURCE_ID + EventHandlerLite.BITS_LINE_NO))
          | ((mId & EventHandlerLite.MASK_METHOD_ID) << (EventHandlerLite.BITS_SOURCE_ID + EventHandlerLite.BITS_LINE_NO))
          | locId;
      // encode the runtime part (event kind and frame information)
      eventId[1] = ((kind & EventHandlerLite.MASK_KIND) << (EventHandlerLite.BITS_STACK_DEPTH
          + EventHandlerLite.BITS_STACK_ID + EventHandlerLite.BITS_THREAD_ID))
          | ((thId & EventHandlerLite.MASK_THREAD_ID) << (EventHandlerLite.BITS_STACK_DEPTH + EventHandlerLite.BITS_STACK_ID))
          | ((depth & EventHandlerLite.MASK_STACK_DEPTH) << (EventHandlerLite.BITS_STACK_ID))
          | (frId & EventHandlerLite.MASK_STACK_ID);
      // encode the runtime part (frame object)
      eventId[2] = (oId & EventHandlerLite.MASK_OBJECT_ID);
      // return the encoded event prefix
      return eventId;
    }

    public long getCurrentFrameId(final long thId)
    {
      StackMirror sm = stacks.get(thId);
      if (sm == null)
      {
        sm = new StackMirror();
        stacks.put(thId, sm);
      }
      return sm.getCurrentFrameId();
    }

    public Long getFieldId(final Field field)
    {
      final String sig = Signatures.getPrefixed(field);
      final Long id = store.get(sig);
      if (id != null)
      {
        return id;
      }
      return register(Signatures.PREFIX_FIELD, sig, FIELD_ID.incrementAndGet());
    }

    public long getLocationId(final Location location)
    {
      final String sig = Signatures.getPrefixed(location);
      Long sourceId = store.get(sig);
      if (sourceId == null)
      {
        sourceId = register(Signatures.PREFIX_SOURCE, sig, SOURCE_ID.incrementAndGet());
      }
      final long lineId = location != null ? location.lineNumber() : EventHandlerLite.ID_NONE;
      return ((EventHandlerLite.MASK_SOURCE_ID & sourceId) << EventHandlerLite.BITS_LINE_NO)
          | (EventHandlerLite.MASK_LINE_NO & lineId);
    }

    public Long getMethodId(final Method method)
    {
      final String sig = Signatures.getPrefixed(method);
      Long id = store.get(sig);
      if (id != null)
      {
        return id;
      }
      id = register(Signatures.PREFIX_METHOD, sig, METHOD_ID.incrementAndGet());
      try
      {
        if (method != null)
        {
          for (final LocalVariable v : method.variables())
          {
            getVariableId(v);
          }
        }
      }
      catch (final AbsentInformationException ignored)
      {
      }
      return id;
    }

    public long getObjectId(final ObjectReference object)
    {
      final String sig = Signatures.getPrefixed(object);
      Long id = store.get(sig);
      if (id != null)
      {
        return id;
      }
      id = register(Signatures.PREFIX_OBJECT, sig, OBJECT_ID.incrementAndGet());
      if (object != null)
      {
        jdiObjectNew(object);
      }
      return id;
    }

    public long getThreadId(final ThreadReference thread)
    {
      final String sig = Signatures.getPrefixed(thread);
      final Long id = store.get(sig);
      if (id != null)
      {
        return id;
      }
      return register(Signatures.PREFIX_THREAD, sig, THREAD_ID.incrementAndGet());
    }

    public long getTypeId(final ReferenceType type)
    {
      final String sig = Signatures.getPrefixed(type);
      Long id = store.get(sig);
      if (id != null)
      {
        return id;
      }
      id = register(Signatures.PREFIX_TYPE, sig, TYPE_ID.incrementAndGet());
      if (type != null)
      {
        jdiTypeLoad(type);
        for (final Method m : type.allMethods())
        {
          getMethodId(m);
        }
        for (final Field f : type.allFields())
        {
          getFieldId(f);
        }
      }
      return id;
    }

    public long getTypeId(final String typeName)
    {
      final String sig = Signatures.PREFIX_TYPE + typeName;
      final Long id = store.get(sig);
      if (id != null)
      {
        return id;
      }
      return register(Signatures.PREFIX_TYPE, sig, TYPE_ID.incrementAndGet());
    }

    public long getValueId(final Value value)
    {
      final String sig = Signatures.getPrefixed(value);
      final Long id = store.get(sig);
      if (id != null)
      {
        return id;
      }
      return register(Signatures.PREFIX_VALUE, sig, VALUE_ID.incrementAndGet());
    }

    public Long getVariableId(final LocalVariable variable)
    {
      final String sig = Signatures.getPrefixed(variable);
      final Long id = store.get(sig);
      if (id != null)
      {
        return id;
      }
      return register(Signatures.PREFIX_VARIABLE, sig, VARIABLE_ID.incrementAndGet());
    }

    public String indexLookup(final String prefix, final long id)
    {
      final String result = index.get(prefix + id);
      return result.substring(prefix.length());
    }

    public long methodEntered(final long thId)
    {
      StackMirror sm = stacks.get(thId);
      if (sm == null)
      {
        sm = new StackMirror();
        stacks.put(thId, sm);
      }
      return sm.methodEntered();
    }

    public long methodExiting(final long thId)
    {
      StackMirror sm = stacks.get(thId);
      if (sm == null)
      {
        sm = new StackMirror();
        stacks.put(thId, sm);
      }
      return sm.methodExiting();
    }

    public Long register(final String prefix, final String element, final long id)
    {
      if (!store.containsKey(element))
      {
        store.putIfAbsent(element, id);
        index.putIfAbsent(prefix + id, element);
      }
      return store.get(element);
    }
  }

  private static class Signatures
  {
    private static final String NULL = "null";
    private static final String PREFIX_FIELD = "field: ";
    private static final String PREFIX_SOURCE = "source: ";
    private static final String PREFIX_METHOD = "method: ";
    private static final String PREFIX_OBJECT = "object: ";
    private static final String PREFIX_THREAD = "thread: ";
    private static final String PREFIX_TYPE = "type: ";
    private static final String PREFIX_VALUE = "value: ";
    private static final String PREFIX_VARIABLE = "variable: ";
    private static final String SIGNATURE_NONE = "";

    public static String get(final Field field)
    {
      return field == null ? Signatures.NULL : field.declaringType().signature() + '/'
          + field.name() + '/' + field.modifiers();
    }

    public static String get(final LocalVariable variable)
    {
      return variable == null ? Signatures.NULL : variable.name() + ": " + variable.typeName();
    }

    public static String get(final Location location)
    {
      try
      {
        return location == null ? Signatures.NULL : location.sourcePath();
      }
      catch (final AbsentInformationException ignored)
      {
      }
      return Signatures.SIGNATURE_NONE;
    }

    public static String get(final Method method)
    {
      if (method == null)
      {
        return Signatures.NULL;
      }
      if (method.isStaticInitializer())
      {
        return "<clinit>()";
      }
      String signature = method.signature();
      signature = signature.substring(0, signature.indexOf(')') + 1);
      if (method.isConstructor())
      {
        return Signatures.constructorName(method.declaringType().name()) + signature;
      }
      return method.name() + signature;
    }

    public static String get(final ObjectReference object)
    {
      return (object == null ? Signatures.NULL : object.type().name() + " (id=" + object.uniqueID()
          + ")");
    }

    public static String get(final ReferenceType type)
    {
      return type == null ? Signatures.NULL : type.signature();
    }

    public static String get(final ThreadReference thread)
    {
      return thread == null ? Signatures.NULL : "" + thread.uniqueID();
    }

    public static String get(final Value value)
    {
      return value == null ? Signatures.NULL : value.toString();
    }

    public static String getPrefixed(final Field field)
    {
      return Signatures.PREFIX_FIELD + Signatures.get(field);
    }

    public static String getPrefixed(final LocalVariable variable)
    {
      return Signatures.PREFIX_VARIABLE + Signatures.get(variable);
    }

    public static String getPrefixed(final Location location)
    {
      return Signatures.PREFIX_SOURCE + Signatures.get(location);
    }

    public static String getPrefixed(final Method method)
    {
      return Signatures.PREFIX_METHOD + Signatures.get(method);
    }

    public static String getPrefixed(final ObjectReference object)
    {
      return Signatures.PREFIX_OBJECT + Signatures.get(object);
    }

    public static String getPrefixed(final ReferenceType type)
    {
      return Signatures.PREFIX_TYPE + Signatures.get(type);
    }

    public static String getPrefixed(final ThreadReference thread)
    {
      return Signatures.PREFIX_THREAD + Signatures.get(thread);
    }

    public static String getPrefixed(final Value value)
    {
      return Signatures.PREFIX_VALUE + Signatures.get(value);
    }

    private static String constructorName(final String typeName)
    {
      String lastName = typeName;
      if (typeName.indexOf("$") != -1)
      {
        lastName = typeName.substring(typeName.lastIndexOf("$") + 1);
        try
        {
          Integer.parseInt(lastName);
          lastName = "<init>";
        }
        catch (final NumberFormatException nfe)
        {
          // not a number, so last name is an actual name
        }
      }
      else if (typeName.indexOf(".") != -1)
      {
        lastName = typeName.substring(typeName.lastIndexOf(".") + 1);
      }
      return lastName;
    }
  }

  private static final class StackMirror
  {
    private final AtomicInteger FRAME_ID;
    private final Stack<Integer> stack;

    public StackMirror()
    {
      this.FRAME_ID = new AtomicInteger(0);
      this.stack = new Stack<Integer>();
    }

    public long getCurrentFrameId()
    {
      return stack.isEmpty() ? EventHandlerLite.ID_NONE : stack.peek();
    }

    public long methodEntered()
    {
      final long result = stack.isEmpty() ? EventHandlerLite.ID_NONE : stack.peek();
      stack.push(FRAME_ID.incrementAndGet());
      return result;
    }

    public long methodExiting()
    {
      stack.pop();
      return stack.isEmpty() ? EventHandlerLite.ID_NONE : stack.peek();
    }
  }
}
