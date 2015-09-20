package edu.buffalo.cse.jive.internal.debug.jdi.model;

import java.util.List;
import java.util.Map;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

/**
 * Mirrors the JDI {@code StackFrame}, exposing (parts) of its state in a safe way.
 */
@SuppressWarnings("restriction")
final class JiveStackFrame implements StackFrame
{
  private final int hashCode;
  private final Method method;
  private final ObjectReference thisObject;
  private final ThreadReference thread;

  JiveStackFrame(final StackFrame frame)
  {
    this.hashCode = frame.hashCode();
    this.thread = frame.thread();
    this.method = frame.location().method();
    this.thisObject = (method.isStatic() || method.isNative()) ? null : frame.thisObject();
  }

  @Override
  public boolean equals(final Object o)
  {
    if (o == this)
    {
      return true;
    }
    if (o instanceof StackFrame)
    {
      final ThreadReference otherThread = ((StackFrame) o).thread();
      final Method otherMethod = ((StackFrame) o).location().method();
      final ObjectReference otherThisObject = (method.isStatic() || method.isNative()) ? null
          : ((StackFrame) o).thisObject();
      if (!thread.equals(otherThread))
      {
        return false;
      }
      if (!method.equals(otherMethod))
      {
        return false;
      }
      if (thisObject == null)
      {
        return otherThisObject == null;
      }
      return thisObject.equals(otherThisObject);
    }
    return false;
  }

  @Override
  public List<Value> getArgumentValues()
  {
    return null;
  }

  @Override
  public Value getValue(final LocalVariable arg0)
  {
    System.err.println("Unexpected call to JiveStackFrame.getValue");
    return null;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public Map getValues(final List arg0)
  {
    System.err.println("Unexpected call to JiveStackFrame.getValues");
    return null;
  }

  @Override
  public int hashCode()
  {
    return this.hashCode;
  }

  @Override
  public Location location()
  {
    return method.location();
  }

  @Override
  public void setValue(final LocalVariable arg0, final Value arg1) throws InvalidTypeException,
      ClassNotLoadedException
  {
        System.err.println("Unexpected call to JiveStackFrame.setValue");
  }

  @Override
  public ObjectReference thisObject()
  {
    return thisObject;
  }

  @Override
  public ThreadReference thread()
  {
    return thread;
  }

  @Override
  public VirtualMachine virtualMachine()
  {
    return null;
  }

  @Override
  public LocalVariable visibleVariableByName(final String arg0) throws AbsentInformationException
  {
    return null;
  }

  @Override
  public List<LocalVariable> visibleVariables() throws AbsentInformationException
  {
    return null;
  }
}