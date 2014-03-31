package edu.buffalo.cse.jive.internal.debug.jdi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.internal.debug.core.IJDIEventListener;
import org.eclipse.jdt.internal.debug.core.model.JDIDebugTarget;

import com.sun.jdi.Field;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.event.AccessWatchpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.ModificationWatchpointEvent;
import com.sun.jdi.request.AccessWatchpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.ModificationWatchpointRequest;

import edu.buffalo.cse.jive.debug.JiveDebugPlugin;
import edu.buffalo.cse.jive.debug.jdi.model.IEventHandlerFactory;

final class EventHandlerFactory implements IEventHandlerFactory
{
  private final JiveDebugTarget owner;
  private final AccessWatchpointHandler fieldReadHandler;
  private final ModificationWatchpointHandler fieldWriteHandler;
  private final ClassPrepareHandler classPrepareHandler;
  private final MethodEntryHandler methodEntryHandler;
  private final MethodExitHandler methodExitHandler;
  private final ExceptionHandler exceptionHandler;
  private final Set<ReferenceType> classes;

  EventHandlerFactory(final JiveDebugTarget owner)
  {
    this.owner = owner;
    this.fieldReadHandler = new AccessWatchpointHandler();
    this.fieldWriteHandler = new ModificationWatchpointHandler();
    this.classPrepareHandler = new ClassPrepareHandler();
    this.methodEntryHandler = new MethodEntryHandler();
    this.methodExitHandler = new MethodExitHandler();
    this.exceptionHandler = new ExceptionHandler();
    this.classes = new HashSet<ReferenceType>();
    if (owner.isActive())
    {
      createRequests();
    }
  }

  @Override
  public void createRequests()
  {
    for (final ReferenceType refType : classes)
    {
      classPrepareHandler.createFieldRequests(owner, refType);
    }
    methodEntryHandler.createRequest();
    methodExitHandler.createRequest();
    exceptionHandler.createRequest();
  }

  @Override
  public void removeRequests()
  {
    classPrepareHandler.removeFieldRequests(owner);
    methodEntryHandler.removeRequest();
    methodExitHandler.removeRequest();
    exceptionHandler.removeRequest();
  }

  private class AccessWatchpointHandler implements IJDIEventListener
  {
    protected AccessWatchpointHandler()
    {
    }

    @Override
    public void eventSetComplete(final Event event, final JDIDebugTarget target,
        final boolean suspend, final EventSet eventSet)
    {
      // TODO: add support for Eclipse 3.5 event handling
    }

    @Override
    public boolean handleEvent(final Event event, final JDIDebugTarget target,
        final boolean suspendVote, final EventSet eventSet)
    {
      if (owner.isActive())
      {
        owner.jdiHandler().jdiAccessWatchpoint((AccessWatchpointEvent) event);
      }
      return true;
    }
  }

  private class ClassPrepareHandler implements IJDIEventListener
  {
    final List<EventRequest> fieldRequests = new ArrayList<EventRequest>();

    protected ClassPrepareHandler()
    {
      createRequest();
    }

    @Override
    public void eventSetComplete(final Event event, final JDIDebugTarget target,
        final boolean suspend, final EventSet eventSet)
    {
      // TODO: add support for Eclipse 3.5 event handling
    }

    @Override
    public boolean handleEvent(final Event event, final JDIDebugTarget target,
        final boolean suspendVote, final EventSet eventSet)
    {
      classes.add(((ClassPrepareEvent) event).referenceType());
      if (!owner.isActive())
      {
        return true;
      }
      createFieldRequests(target, ((ClassPrepareEvent) event).referenceType());
      owner.jdiHandler().jdiClassPrepare((ClassPrepareEvent) event);
      return true;
    }

    protected void createFieldRequests(final JDIDebugTarget target, final ReferenceType refType)
    {
      final EventRequestManager manager = target.getEventRequestManager();
      if (manager != null)
      {
        try
        {
          // monitor all non-synthetic fields of the prepared classes for reads/writes
          for (final Object o : refType.fields())
          {
            final Field f = (Field) o;
            // Ignore compiler generated fields
            if (!f.isSynthetic() && f.name().indexOf("$") == -1)
            {
              // monitor field reads
              final AccessWatchpointRequest readRequest = manager.createAccessWatchpointRequest(f);
              readRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
              readRequest.enable();
              fieldRequests.add(readRequest);
              target.addJDIEventListener(fieldReadHandler, readRequest);
              // monitor field writes
              final ModificationWatchpointRequest writeRequest = manager
                  .createModificationWatchpointRequest(f);
              writeRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
              writeRequest.enable();
              fieldRequests.add(writeRequest);
              target.addJDIEventListener(fieldWriteHandler, writeRequest);
            }
          }
        }
        catch (final RuntimeException e)
        {
          JiveDebugPlugin.log(e);
        }
      }
    }

    protected void createRequest()
    {
      final EventRequestManager manager = owner.getEventRequestManager();
      if (manager != null)
      {
        try
        {
          final ClassPrepareRequest request = manager.createClassPrepareRequest();
          owner.jdiManager().modelFilter().filter(request);
          request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
          request.enable();
          owner.addJDIEventListener(this, request);
        }
        catch (final RuntimeException e)
        {
          JiveDebugPlugin.log(e);
        }
      }
    }

    protected void removeFieldRequests(final JDIDebugTarget target)
    {
      final EventRequestManager manager = target.getEventRequestManager();
      if (manager != null && !fieldRequests.isEmpty())
      {
        try
        {
          manager.deleteEventRequests(fieldRequests);
          for (final EventRequest request : fieldRequests)
          {
            target.removeJDIEventListener(
                request instanceof AccessWatchpointRequest ? fieldReadHandler : fieldWriteHandler,
                request);
          }
        }
        catch (final RuntimeException e)
        {
          JiveDebugPlugin.log(e);
        }
      }
    }
  }

  private class ExceptionHandler implements IJDIEventListener
  {
    private ExceptionRequest request;

    // private List<ExceptionRequest> originalRequests = new ArrayList<ExceptionRequest>();
    protected ExceptionHandler()
    {
    }

    @Override
    public void eventSetComplete(final Event event, final JDIDebugTarget target,
        final boolean suspend, final EventSet eventSet)
    {
      // TODO: add support for Eclipse 3.5 event handling
    }

    @Override
    public boolean handleEvent(final Event event, final JDIDebugTarget target,
        final boolean suspendVote, final EventSet eventSet)
    {
      if (owner.isActive())
      {
        owner.jdiHandler().jdiExceptionThrown((ExceptionEvent) event);
      }
      return true;
    }

    protected void createRequest()
    {
      final EventRequestManager manager = owner.getEventRequestManager();
      if (manager != null)
      {
        try
        {
          /**
           * NOTE: (@2012-10-11, dlessa): In Juno, the list of exception requests is implemented as
           * an UnmodifiableCollection, therefore, we cannot circumvent the processing order of the
           * exception handlers any longer... Hopefully, this will not affect the semantics of
           * exception handling in Jive.
           */
          // Don't filter ExceptionEvents as they are needed to adjust the call stack
          if (request != null)
          {
            removeRequest();
          }
          request = manager.createExceptionRequest(null, true, true);
          request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
          request.enable();
          owner.addJDIEventListener(this, request);
        }
        catch (final RuntimeException e)
        {
          JiveDebugPlugin.log(e);
        }
      }
    }

    protected void removeRequest()
    {
      final EventRequestManager manager = owner.getEventRequestManager();
      if (manager != null && request != null)
      {
        try
        {
          owner.removeJDIEventListener(this, request);
          manager.deleteEventRequest(request);
          request = null;
        }
        catch (final RuntimeException e)
        {
          JiveDebugPlugin.log(e);
        }
      }
    }
  }

  private class MethodEntryHandler implements IJDIEventListener
  {
    private MethodEntryRequest request;

    protected MethodEntryHandler()
    {
    }

    @Override
    public void eventSetComplete(final Event event, final JDIDebugTarget target,
        final boolean suspend, final EventSet eventSet)
    {
      // TODO: add support for Eclipse 3.5 event handling
    }

    @Override
    public boolean handleEvent(final Event event, final JDIDebugTarget target,
        final boolean suspendVote, final EventSet eventSet)
    {
      if (owner.isActive())
      {
        owner.jdiHandler().jdiMethodEntry((MethodEntryEvent) event);
      }
      return true;
    }

    protected void createRequest()
    {
      final EventRequestManager manager = owner.getEventRequestManager();
      if (manager != null)
      {
        try
        {
          if (request != null)
          {
            removeRequest();
          }
          request = manager.createMethodEntryRequest();
          owner.jdiManager().modelFilter().filter(request);
          request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
          request.enable();
          owner.addJDIEventListener(this, request);
        }
        catch (final RuntimeException e)
        {
          JiveDebugPlugin.log(e);
        }
      }
    }

    protected void removeRequest()
    {
      final EventRequestManager manager = owner.getEventRequestManager();
      if (manager != null && request != null)
      {
        try
        {
          owner.removeJDIEventListener(this, request);
          manager.deleteEventRequest(request);
          request = null;
        }
        catch (final RuntimeException e)
        {
          JiveDebugPlugin.log(e);
        }
      }
    }
  }

  private class MethodExitHandler implements IJDIEventListener
  {
    private MethodExitRequest request;

    protected MethodExitHandler()
    {
    }

    @Override
    public void eventSetComplete(final Event event, final JDIDebugTarget target,
        final boolean suspend, final EventSet eventSet)
    {
      // TODO: add support for Eclipse 3.5 event handling
    }

    @Override
    public boolean handleEvent(final Event event, final JDIDebugTarget target,
        final boolean suspendVote, final EventSet eventSet)
    {
      if (owner.isActive())
      {
        owner.jdiHandler().jdiMethodExit((MethodExitEvent) event);
      }
      return true;
    }

    protected void createRequest()
    {
      final EventRequestManager manager = owner.getEventRequestManager();
      if (manager != null)
      {
        try
        {
          if (request != null)
          {
            removeRequest();
          }
          request = manager.createMethodExitRequest();
          owner.jdiManager().modelFilter().filter(request);
          request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
          request.enable();
          owner.addJDIEventListener(this, request);
        }
        catch (final RuntimeException e)
        {
          JiveDebugPlugin.log(e);
        }
      }
    }

    protected void removeRequest()
    {
      final EventRequestManager manager = owner.getEventRequestManager();
      if (manager != null && request != null)
      {
        try
        {
          owner.removeJDIEventListener(this, request);
          manager.deleteEventRequest(request);
          request = null;
        }
        catch (final RuntimeException e)
        {
          JiveDebugPlugin.log(e);
        }
      }
    }
  }

  private class ModificationWatchpointHandler implements IJDIEventListener
  {
    protected ModificationWatchpointHandler()
    {
    }

    @Override
    public void eventSetComplete(final Event event, final JDIDebugTarget target,
        final boolean suspend, final EventSet eventSet)
    {
      // TODO: add support for Eclipse 3.5 event handling
    }

    @Override
    public boolean handleEvent(final Event event, final JDIDebugTarget target,
        final boolean suspendVote, final EventSet eventSet)
    {
      if (owner.isActive())
      {
        owner.jdiHandler().jdiModificationWatchpoint((ModificationWatchpointEvent) event);
      }
      return true;
    }
  }
}