package edu.buffalo.cse.jive.exporter.trace;

import java.util.List;

import edu.buffalo.cse.jive.lib.XMLTools.XMLEventField;
import edu.buffalo.cse.jive.exporter.IExportModelFilter;
import edu.buffalo.cse.jive.exporter.XML;
import edu.buffalo.cse.jive.model.IEventModel.IAssignEvent;
import edu.buffalo.cse.jive.model.IEventModel.IExceptionCatchEvent;
import edu.buffalo.cse.jive.model.IEventModel.IExceptionThrowEvent;
import edu.buffalo.cse.jive.model.IEventModel.IFieldReadEvent;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.ILockEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodExitEvent;
import edu.buffalo.cse.jive.model.IEventModel.INewObjectEvent;
import edu.buffalo.cse.jive.model.IEventModel.ITypeLoadEvent;
import edu.buffalo.cse.jive.model.IEventModel.IVarDeleteEvent;

public class XMLExporter
{
  public static String export(final List<? extends IJiveEvent> events,
      final IExportModelFilter filter)
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("<events>");
    for (final IJiveEvent event : events)
    {
      if (filter == null || filter.accepts(event))
      {
        sb.append(eventToXML(event));
      }
    }
    sb.append("</events>");
    return sb.toString();
  }

  /**
   * TODO: reduce the cyclomatic complexity.
   */
  private static String eventToXML(final IJiveEvent event)
  {
    final StringBuffer buffer = new StringBuffer(XML.tagOpen("event"));
    EventXML.eventToXML(buffer, event);
    buffer.append(XML.tagOpen("details"));
    if (event instanceof IAssignEvent)
    {
      EventXML.assignToXML(buffer, (IAssignEvent) event);
    }
    else if (event instanceof IExceptionCatchEvent)
    {
      EventXML.exceptionCatchToXML(buffer, (IExceptionCatchEvent) event);
    }
    else if (event instanceof IExceptionThrowEvent)
    {
      EventXML.exceptionThrowToXML(buffer, (IExceptionThrowEvent) event);
    }
    else if (event instanceof IFieldReadEvent)
    {
      EventXML.fieldReadToXML(buffer, (IFieldReadEvent) event);
    }
    else if (event instanceof ILockEvent)
    {
      EventXML.lockToXML(buffer, (ILockEvent) event);
    }
    else if (event instanceof IMethodCallEvent)
    {
      EventXML.methodCallToXML(buffer, (IMethodCallEvent) event);
    }
    else if (event instanceof IMethodExitEvent)
    {
      EventXML.methodExitToXML(buffer, (IMethodExitEvent) event);
    }
    else if (event instanceof INewObjectEvent)
    {
      EventXML.newObjectToXML(buffer, (INewObjectEvent) event);
    }
    else if (event instanceof ITypeLoadEvent)
    {
      EventXML.typeLoadToXML(buffer, (ITypeLoadEvent) event);
    }
    else if (event instanceof IVarDeleteEvent)
    {
      EventXML.varDeleteToXML(buffer, (IVarDeleteEvent) event);
    }
    buffer.append(XML.tagClose("details"));
    buffer.append(XML.tagClose("event"));
    return buffer.toString();
  }

  private static final class EventXML
  {
    public static void assignToXML(final StringBuffer buffer, final IAssignEvent event)
    {
      buffer.append(XML.tagOpen(XMLEventField.CONTEXT.fieldName()));
      buffer.append(XML.CData(event.contour().signature()));
      buffer.append(XML.tagClose(XMLEventField.CONTEXT.fieldName()));
      buffer.append(XML.tagOpen(XMLEventField.VARIABLE.fieldName()));
      buffer.append(XML.CData(event.member().name()));
      buffer.append(XML.tagClose(XMLEventField.VARIABLE.fieldName()));
      buffer.append(XML.tagOpen(XMLEventField.VALUE.fieldName()));
      buffer.append(XML.CData(event.newValue().toString()));
      buffer.append(XML.tagClose(XMLEventField.VALUE.fieldName()));
    }

    public static void exceptionCatchToXML(final StringBuffer buffer,
        final IExceptionCatchEvent event)
    {
      buffer.append(XML.tagOpen(XMLEventField.EXCEPTION.fieldName()));
      buffer.append(XML.CData(event.exception().toString()));
      buffer.append(XML.tagClose(XMLEventField.EXCEPTION.fieldName()));
      buffer.append(XML.tagOpen(XMLEventField.CATCHER.fieldName()));
      if (event.contour() != null)
      {
        buffer.append(XML.CData(event.contour().signature()));
      }
      buffer.append(XML.tagClose(XML.tagOpen(XMLEventField.CATCHER.fieldName())));
      buffer.append(XML.tagOpen(XML.tagOpen(XMLEventField.VARIABLE.fieldName())));
      if (event.member() != null)
      {
        buffer.append(XML.CData(event.member().schema().name()));
      }
      buffer.append(XML.tagClose(XMLEventField.VARIABLE.fieldName()));
    }

    public static void exceptionThrowToXML(final StringBuffer buffer,
        final IExceptionThrowEvent event)
    {
      buffer.append(XML.tagOpen(XMLEventField.EXCEPTION.fieldName()));
      buffer.append(XML.CData(event.exception().toString()));
      buffer.append(XML.tagClose(XMLEventField.EXCEPTION.fieldName()));
      buffer.append(XML.tagOpen(XMLEventField.THROWER.fieldName()));
      buffer.append(XML.CData(event.thrower().toString()));
      buffer.append(XML.tagClose(XMLEventField.THROWER.fieldName()));
      buffer.append(XML.tagOpen(XMLEventField.FRAME_POPPED.fieldName()));
      buffer.append(XML.CData(String.valueOf(event.framePopped())));
      buffer.append(XML.tagClose(XMLEventField.FRAME_POPPED.fieldName()));
    }

    public static void fieldReadToXML(final StringBuffer buffer, final IFieldReadEvent event)
    {
      buffer.append(XML.tagOpen(XMLEventField.CONTEXT.fieldName()));
      buffer.append(XML.CData(event.contour().signature()));
      buffer.append(XML.tagClose(XMLEventField.CONTEXT.fieldName()));
      buffer.append(XML.tagOpen(XMLEventField.VARIABLE.fieldName()));
      buffer.append(XML.CData(event.member().schema().name()));
      buffer.append(XML.tagClose(XMLEventField.VARIABLE.fieldName()));
    }

    public static void lockToXML(final StringBuffer buffer, final ILockEvent event)
    {
      buffer.append(XML.tagOpen(XMLEventField.OPERATION.fieldName()));
      buffer.append(XML.CData(event.lockOperation().toString()));
      buffer.append(XML.tagClose(XMLEventField.OPERATION.fieldName()));
      buffer.append(XML.tagOpen(XMLEventField.LOCK.fieldName()));
      buffer.append(XML.CData(event.lockDescription()));
      buffer.append(XML.tagClose(XMLEventField.LOCK.fieldName()));
    }

    public static void methodCallToXML(final StringBuffer buffer, final IMethodCallEvent event)
    {
      buffer.append(XML.tagOpen(XMLEventField.CALLER.fieldName()));
      buffer.append(XML.CData(event.caller().toString()));
      buffer.append(XML.tagClose(XMLEventField.CALLER.fieldName()));
      buffer.append(XML.tagOpen(XMLEventField.TARGET.fieldName()));
      buffer.append(XML.CData(event.target().toString()));
      buffer.append(XML.tagClose(XMLEventField.TARGET.fieldName()));
    }

    public static void methodExitToXML(final StringBuffer buffer, final IMethodExitEvent event)
    {
      buffer.append(XML.tagOpen(XMLEventField.RETURNER.fieldName()));
      buffer.append(XML.CData(event.returnContext().toString()));
      buffer.append(XML.tagClose(XMLEventField.RETURNER.fieldName()));
      buffer.append(XML.tagOpen(XMLEventField.VALUE.fieldName()));
      if (event.returnValue() != null && !event.returnValue().isUninitialized())
      {
        buffer.append(XML.CData(event.returnValue().toString()));
      }
      buffer.append(XML.tagClose(XMLEventField.VALUE.fieldName()));
    }

    public static void newObjectToXML(final StringBuffer buffer, final INewObjectEvent event)
    {
      buffer.append(XML.tagOpen(XMLEventField.OBJECT.fieldName()));
      buffer.append(XML.CData(event.newContour().signature()));
      buffer.append(XML.tagClose(XMLEventField.OBJECT.fieldName()));
    }

    public static void typeLoadToXML(final StringBuffer buffer, final ITypeLoadEvent event)
    {
      buffer.append(XML.tagOpen(XMLEventField.TYPE.fieldName()));
      buffer.append(XML.CData(event.newContour().signature()));
      buffer.append(XML.tagClose(XMLEventField.TYPE.fieldName()));
    }

    public static void varDeleteToXML(final StringBuffer buffer, final IVarDeleteEvent event)
    {
      buffer.append(XML.tagOpen(XMLEventField.CONTEXT.fieldName()));
      buffer.append(XML.CData(event.contour().signature()));
      buffer.append(XML.tagClose(XMLEventField.CONTEXT.fieldName()));
      buffer.append(XML.tagOpen(XMLEventField.VARIABLE.fieldName()));
      buffer.append(XML.CData(event.member().schema().name()));
      buffer.append(XML.tagClose(XMLEventField.VARIABLE.fieldName()));
    }

    private static void eventToXML(final StringBuffer buffer, final IJiveEvent event)
    {
      buffer.append(XML.tagOpen(XMLEventField.ID.fieldName()));
      buffer.append(XML.PCData(event.eventId()));
      buffer.append(XML.tagClose(XMLEventField.ID.fieldName()));
      buffer.append(XML.tagOpen(XMLEventField.THREAD.fieldName()));
      buffer.append(XML.CData(event.thread().name()));
      buffer.append(XML.tagClose(XMLEventField.THREAD.fieldName()));
      buffer.append(XML.tagOpen(XMLEventField.KIND.fieldName()));
      buffer.append(XML.CData(event.kind().toString()));
      buffer.append(XML.tagClose(XMLEventField.KIND.fieldName()));
      buffer.append(XML.tagOpen(XMLEventField.FILE.fieldName()));
      buffer.append(XML.CData(event.line().file().name()));
      buffer.append(XML.tagClose(XMLEventField.FILE.fieldName()));
      buffer.append(XML.tagOpen(XMLEventField.LINE.fieldName()));
      buffer.append(XML.PCData(event.line().lineNumber()));
      buffer.append(XML.tagClose(XMLEventField.LINE.fieldName()));
    }
  }
}
