package edu.buffalo.cse.jive.internal.debug.jdi.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadGroupReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

class Synthetics
{
  /**
   * Our synthetic thread appears as an instance of java.lang.Thread.
   */
  private static ClassType findVMThread(final VirtualMachine vm)
  {
    ClassType thread = null;
    for (final Object c : vm.classesByName("java.lang.Thread"))
    {
      thread = (ClassType) c;
      break;
    }
    return thread;
  }

  /**
   * Our synthetic thread group appears as an instance of java.lang.Thread.
   */
  private static ClassType findVMThreadGroup(final VirtualMachine vm)
  {
    ClassType thread = null;
    for (final Object c : vm.classesByName("java.lang.ThreadGroup"))
    {
      thread = (ClassType) c;
      break;
    }
    return thread;
  };

  static StackFrame createSyntheticFrame(final VirtualMachine vm)
  {
    return new SyntheticStackFrame(new SyntheticThread(vm, new SyntheticThreadGroup(vm)));
  }

  private final static class SyntheticStackFrame implements StackFrame
  {
    private final SyntheticThread thread;

    private SyntheticStackFrame(final SyntheticThread thread)
    {
      this.thread = thread;
      thread.frames.add(this);
    }

    @Override
    public List getArgumentValues()
    {
      return Collections.emptyList();
    }

    @Override
    public Value getValue(final LocalVariable arg0)
    {
      // no variables
      return null;
    }

    @Override
    public Map getValues(final List arg0)
    {
      return Collections.emptyMap();
    }

    @Override
    public Location location()
    {
      return null;
    }

    @Override
    public void setValue(final LocalVariable arg0, final Value arg1) throws InvalidTypeException,
        ClassNotLoadedException
    {
      // cowardly ignore
    }

    @Override
    public ObjectReference thisObject()
    {
      return null;
    }

    @Override
    public ThreadReference thread()
    {
      return thread;
    }

    @Override
    public VirtualMachine virtualMachine()
    {
      return thread.virtualMachine();
    }

    @Override
    public LocalVariable visibleVariableByName(final String arg0) throws AbsentInformationException
    {
      // no visible variables
      return null;
    }

    @Override
    public List visibleVariables() throws AbsentInformationException
    {
      return Collections.emptyList();
    }
  }

  private final static class SyntheticThread implements ThreadReference
  {
    private final VirtualMachine vm;
    private final SyntheticThreadGroup group;
    private final List<SyntheticStackFrame> frames = new LinkedList<SyntheticStackFrame>();

    private SyntheticThread(final VirtualMachine vm, final SyntheticThreadGroup group)
    {
      this.vm = vm;
      this.group = group;
      group.threads.add(this);
    }

    @Override
    public ObjectReference currentContendedMonitor() throws IncompatibleThreadStateException
    {
      // no monitors
      return null;
    }

    @Override
    public void disableCollection()
    {
      // cowardly ignore
    }

    @Override
    public void enableCollection()
    {
      // cowardly ignore
    }

    @Override
    public int entryCount() throws IncompatibleThreadStateException
    {
      return 0;
    }

    @Override
    public void forceEarlyReturn(final Value arg0) throws InvalidTypeException,
        ClassNotLoadedException, IncompatibleThreadStateException
    {
      // cowardly ignore
    }

    @Override
    public StackFrame frame(final int index) throws IncompatibleThreadStateException
    {
      return frames.get(index);
    }

    @Override
    public int frameCount() throws IncompatibleThreadStateException
    {
      return frames.size();
    }

    @Override
    public List frames() throws IncompatibleThreadStateException
    {
      return frames;
    }

    @Override
    public List frames(final int fromIndex, final int toIndex)
        throws IncompatibleThreadStateException
    {
      return frames.subList(fromIndex, toIndex);
    }

    @Override
    public Value getValue(final Field arg0)
    {
      // no fields
      return null;
    }

    @Override
    public Map getValues(final List arg0)
    {
      return Collections.emptyMap();
    }

    @Override
    public void interrupt()
    {
      // cowardly ignore
    }

    @Override
    public Value invokeMethod(final ThreadReference arg0, final Method arg1, final List arg2,
        final int arg3) throws InvalidTypeException, ClassNotLoadedException,
        IncompatibleThreadStateException, InvocationException
    {
      // cowardly ignore
      return null;
    }

    @Override
    public boolean isAtBreakpoint()
    {
      return false;
    }

    @Override
    public boolean isCollected()
    {
      return false;
    }

    @Override
    public boolean isSuspended()
    {
      return false;
    }

    @Override
    public String name()
    {
      return "JIVE Snapshot";
    }

    @Override
    public List ownedMonitors() throws IncompatibleThreadStateException
    {
      return Collections.emptyList();
    }

    @Override
    public List ownedMonitorsAndFrames() throws IncompatibleThreadStateException
    {
      return frames;
    }

    @Override
    public ThreadReference owningThread() throws IncompatibleThreadStateException
    {
      // no owning thread
      return null;
    }

    @Override
    public void popFrames(final StackFrame arg0) throws IncompatibleThreadStateException
    {
      // cowardly ignore
    }

    @Override
    public ReferenceType referenceType()
    {
      return Synthetics.findVMThread(vm);
    }

    @Override
    public List referringObjects(final long arg0)
    {
      return Collections.emptyList();
    }

    @Override
    public void resume()
    {
      // cowardly ignore
    }

    @Override
    public void setValue(final Field arg0, final Value arg1) throws InvalidTypeException,
        ClassNotLoadedException
    {
      // cowardly ignore
    }

    @Override
    public int status()
    {
      return ThreadReference.THREAD_STATUS_RUNNING;
    }

    @Override
    public void stop(final ObjectReference arg0) throws InvalidTypeException
    {
      // cowardly ignore
    }

    @Override
    public void suspend()
    {
      // cowardly ignore
    }

    @Override
    public int suspendCount()
    {
      return 0;
    }

    @Override
    public ThreadGroupReference threadGroup()
    {
      return group;
    }

    @Override
    public Type type()
    {
      return vm.allThreads().get(0).type();
    }

    @Override
    public long uniqueID()
    {
      return -1;
    }

    @Override
    public VirtualMachine virtualMachine()
    {
      return vm;
    }

    @Override
    public List waitingThreads() throws IncompatibleThreadStateException
    {
      return Collections.emptyList();
    }
  }

  private final static class SyntheticThreadGroup implements ThreadGroupReference
  {
    private final VirtualMachine vm;
    private final List<ThreadReference> threads = new LinkedList<ThreadReference>();

    private SyntheticThreadGroup(final VirtualMachine vm)
    {
      this.vm = vm;
    }

    @Override
    public void disableCollection()
    {
      // cowardly ignore
    }

    @Override
    public void enableCollection()
    {
      // cowardly ignore
    }

    @Override
    public int entryCount() throws IncompatibleThreadStateException
    {
      return 0;
    }

    @Override
    public Value getValue(final Field arg0)
    {
      return null;
    }

    @Override
    public Map getValues(final List arg0)
    {
      return Collections.emptyMap();
    }

    @Override
    public Value invokeMethod(final ThreadReference arg0, final Method arg1, final List arg2,
        final int arg3) throws InvalidTypeException, ClassNotLoadedException,
        IncompatibleThreadStateException, InvocationException
    {
      return null;
    }

    @Override
    public boolean isCollected()
    {
      return false;
    }

    @Override
    public String name()
    {
      return "Snapshot Group";
    }

    @Override
    public ThreadReference owningThread() throws IncompatibleThreadStateException
    {
      return null;
    }

    @Override
    public ThreadGroupReference parent()
    {
      return null;
    }

    @Override
    public ReferenceType referenceType()
    {
      return Synthetics.findVMThreadGroup(vm);
    }

    @Override
    public List referringObjects(final long arg0)
    {
      return Collections.emptyList();
    }

    @Override
    public void resume()
    {
      // cowardly ignore
    }

    @Override
    public void setValue(final Field arg0, final Value arg1) throws InvalidTypeException,
        ClassNotLoadedException
    {
      // cowardly ignore
    }

    @Override
    public void suspend()
    {
      // cowardly ignore
    }

    @Override
    public List threadGroups()
    {
      return Collections.emptyList();
    }

    @Override
    public List threads()
    {
      return threads;
    }

    @Override
    public Type type()
    {
      return vm.allThreads().get(0).threadGroup().type();
    }

    @Override
    public long uniqueID()
    {
      return -2;
    }

    @Override
    public VirtualMachine virtualMachine()
    {
      return vm;
    }

    @Override
    public List waitingThreads() throws IncompatibleThreadStateException
    {
      return Collections.emptyList();
    }
  }
  // private final static class SyntheticThreadObject implements ObjectReference {
  //
  // private final SyntheticThread thread;
  //
  // SyntheticThreadObject(final SyntheticThread thread) {
  //
  // this.thread = thread;
  // }
  //
  // @Override
  // public void disableCollection() {
  //
  // // cowardly ignore
  // }
  //
  // @Override
  // public void enableCollection() {
  //
  // // cowardly ignore
  // }
  //
  // @Override
  // public int entryCount() throws IncompatibleThreadStateException {
  //
  // return 0;
  // }
  //
  // @Override
  // public Value getValue(final Field arg0) {
  //
  // return null;
  // }
  //
  // @Override
  // public Map getValues(final List arg0) {
  //
  // return Collections.emptyMap();
  // }
  //
  // @Override
  // public Value invokeMethod(final ThreadReference arg0, final Method arg1, final List arg2,
  // final int arg3) throws InvalidTypeException, ClassNotLoadedException,
  // IncompatibleThreadStateException, InvocationException {
  //
  // return null;
  // }
  //
  // @Override
  // public boolean isCollected() {
  //
  // return false;
  // }
  //
  // @Override
  // public ThreadReference owningThread() throws IncompatibleThreadStateException {
  //
  // return thread;
  // }
  //
  // @Override
  // public ReferenceType referenceType() {
  //
  // return thread.referenceType();
  // }
  //
  // @Override
  // public List referringObjects(final long arg0) {
  //
  // return Collections.emptyList();
  // }
  //
  // @Override
  // public void setValue(final Field arg0, final Value arg1) throws InvalidTypeException,
  // ClassNotLoadedException {
  //
  // // cowardly ignore
  // }
  //
  // @Override
  // public Type type() {
  //
  // return thread.type();
  // }
  //
  // @Override
  // public long uniqueID() {
  //
  // return -3;
  // }
  //
  // @Override
  // public VirtualMachine virtualMachine() {
  //
  // return thread.virtualMachine();
  // }
  //
  // @Override
  // public List waitingThreads() throws IncompatibleThreadStateException {
  //
  // return Collections.emptyList();
  // }
  // }
}
