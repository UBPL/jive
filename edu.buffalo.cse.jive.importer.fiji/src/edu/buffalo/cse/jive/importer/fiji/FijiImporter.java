package edu.buffalo.cse.jive.importer.fiji;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.lib.XMLTools.XMLEventField;
import edu.buffalo.cse.jive.launch.offline.IOfflineImporter;
import edu.buffalo.cse.jive.launch.offline.OfflineImporterException;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IContourModel.IObjectContour;
import edu.buffalo.cse.jive.model.IEventModel.EventKind;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IModel.ILineValue;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;
import edu.buffalo.cse.jive.model.IModel.IValue;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNodeRef;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.model.IStaticModel.NodeOrigin;
import edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility;
import edu.buffalo.cse.jive.model.factory.IContourFactory;
import edu.buffalo.cse.jive.model.factory.IEventFactory;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory.IStaticModelDelegate;
import edu.buffalo.cse.jive.model.factory.IValueFactory;

public class FijiImporter extends DefaultHandler implements IOfflineImporter
{
  private final static int BASE_16 = 16;
  private final static int BLOCK_SIZE = 100;
  private final static String INIT_ESCAPED = "<init>";
  private static final String PROTO_FIJI = "fiji://";
  private EventDAO event;
  private List<EventDAO> eventList;
  private String fileName;
  private IExecutionModel model;
  private IStaticModelDelegate staticModelDelegate;
  private Map<Long, String> threadToScope;
  private Map<Long, LinkedList<String>> threadToScopeStack;
  private Map<Long, String> objectIdToScope;
  private Map<String, Set<IObjectContour>> scopeToContours;

  // public static void main(final String args[]) throws SAXException, IOException {
  //
  // final FijiImporter importer = new FijiImporter(
  // "/home/demian/Workspaces/jive-2.0/jive-fork/fiji-12-05-31.xml", new ExecutionModel());
  // importer.process();
  // }
  @Override
  public void characters(final char[] ch, final int start, final int length) throws SAXException
  {
    if (event != null && event.currentField() != null)
    {
      event.setCurrentFieldValue(unescape(new String(ch, start, length)));
    }
  }

  /**
   * Observations:
   * 
   * -- In details, use CDATA for details that would otherwise be escaped.
   * 
   * -- THREAD PRIORITY: is the target always the same as the target?
   * 
   * -- THREAD NEW: thread class signature, otherwise must be java.lang.Thread.
   * 
   * -- THREAD START: id = 2 has no NEW prior to the start (newthread == 3 was actually 2?).
   * 
   * -- Inferred type load events-- hierarchy?
   * 
   * -- Thread #2 was never created; Thread #3 was created but no events ever happened in its
   * context.
   * 
   * -- Ignoring the target since callers/targets have no numbering scheme to identify the contours.
   * 
   */
  public void createEvents()
  {
    // result in trace log order
    final List<IJiveEvent> jiveEvents = TypeTools.newArrayList(eventList.size());
    // keep track of all stacks
    final Map<IThreadValue, Stack<IMethodContour>> stacks = TypeTools.newHashMap();
    for (final EventDAO event : eventList)
    {
      // all events reference a timestamp
      final long timestamp = event.timestamp();
      // all events reference a thread value
      final IThreadValue thread = createThread(event);
      // most events reference a line value
      final ILineValue line = valueFactory().createUnavailableLine();
      // specialized processing based on the event kind
      if (event.kind() == null)
      {
        // SCJ T0, SCJ PEH Deadline, SCJ Cycle Start, SCJ PEH Release
        System.err.println("SKIPPING EVENT" + event.fields);
        continue;
      }
      try
      {
        switch (event.kind())
        {
        // unsupported by Fiji
        // case EXCEPTION_CATCH:
        // case EXCEPTION_THROW:
        // case FIELD_READ:
        // case LINE_STEP:
        // case METHOD_RETURNED:
        // case THREAD_LOCK:
        // case TYPE_LOAD:
        // case VAR_ASSIGN:
        // case VAR_DELETE:
          case FIELD_WRITE:
            {
              // . <target>00000000f5d142f0</target>
              // . <value>00000000f5d1423c</value>
              // . <type>javax.safetycritical.SingleMissionSequencer</type>
              // . <field>_mission</field>
              final String typeName = event.getFieldValue(XMLEventField.TYPE);
              // find the type node or create a new one
              final ITypeNode typeNode = resolveType(staticFactory().typeNameToSignature(typeName));
              // contour type?
              if (typeNode.kind() == NodeKind.NK_ARRAY || typeNode.kind() == NodeKind.NK_CLASS)
              {
                // no target-- static field
                if (event.getFieldValue(XMLEventField.TARGET) == null)
                {
                  continue;
                }
                final long lhs = Long.parseLong(event.getFieldValue(XMLEventField.TARGET),
                    FijiImporter.BASE_16);
                final String scopeLHS = objectIdToScope.get(lhs);
                // field value from the given scope
                final long rhs = Long.parseLong(event.getFieldValue(XMLEventField.VALUE),
                    FijiImporter.BASE_16);
                final String scopeRHS = objectIdToScope.get(rhs);
                final LinkedList<String> stack = threadToScopeStack.get(thread.id());
                final int indexLHS = stack == null ? -1 : stack.indexOf(scopeLHS);
                final int indexRHS = stack == null ? -1 : stack.indexOf(scopeRHS);
                jiveEvents.add(eventFactory().createScopeAssignEvent(timestamp, thread, line,
                    scopeLHS, indexLHS, lhs, scopeRHS, indexRHS, rhs));
                System.out.println("SCOPE ASSIGN " + scopeLHS + " <-- " + scopeRHS + " ("
                    + indexLHS + ", " + indexRHS + ")");
              }
            }
            break;
          case METHOD_CALL:
            {
              // <caller><![CDATA[java.lang.Thread#currentThread]]></caller>
              // <target><![CDATA[java.lang.Object#wait]]></target>
              // <signature><![CDATA[Ljava/lang/Object;/wait()V]]></signature>
              final String scaller = event.getFieldValue(XMLEventField.CALLER);
              final IValue caller;
              if ("SYSTEM".equals(scaller))
              {
                // system
                caller = valueFactory().createSystemCaller();
              }
              else
              {
                // top of the respective thread
                final IMethodContour top = stacks.get(thread).peek();
                caller = valueFactory().createReference(top);
              }
              final String starget = event.getFieldValue(XMLEventField.TARGET);
              final IValue target;
              if ("SYSTEM".equals(starget))
              {
                // system
                target = valueFactory().createSystemCaller();
              }
              else
              {
                // signature of the target (i.e., type/method/args/return)
                final String key = event.getFieldValue(XMLEventField.SIGNATURE).replace(";/", ";.");
                // type part of the target's signature
                final String typeKey = key.substring(0, key.indexOf(';') + 1);
                // find the type node or create a new one
                final ITypeNode typeNode = resolveType(typeKey);
                // create the necessary type contours
                createStaticContours(typeNode, timestamp, thread, line, jiveEvents);
                // the static contour must exist now
                final IContextContour callerContour = contourFactory().lookupStaticContour(
                    typeNode.name());
                // signature of the return type
                String returnTypeKey = key.indexOf(')') == -1 ? "V" : key.substring(
                    key.indexOf(')') + 1, key.length());
                if (!returnTypeKey.startsWith("L") && returnTypeKey.endsWith(";"))
                {
                  returnTypeKey = returnTypeKey.substring(0, returnTypeKey.length() - 1);
                }
                // find the return type node or create a new one
                ITypeNode returnTypeNode = resolveType(returnTypeKey);
                returnTypeNode = returnTypeNode == null ? staticFactory().lookupVoidType()
                    : returnTypeNode;
                // create the necessary type contours
                createStaticContours(returnTypeNode, timestamp, thread, line, jiveEvents);
                final String targetName = starget.substring(starget.indexOf('#') + 1);
                // target's schema
                final String methodKey = key.substring(0, key.indexOf(')') + 1);
                IMethodNode methodNode = staticFactory().lookupMethodNode(methodKey);
                if (methodNode == null)
                {
                  methodNode = typeNode.addMethodMember(key.substring(0, key.indexOf(')') + 1),
                      targetName, -1, -1, returnTypeNode, NodeOrigin.NO_JIVE,
                      Collections.<NodeModifier> emptySet(), NodeVisibility.NV_PUBLIC,
                      Collections.<ITypeNodeRef> emptySet());
                }
                // create the method contour
                final IMethodContour method = callerContour.createMethodContour(methodNode, thread);
                // push it onto the stack
                stacks.get(thread).push(method);
                // create an in-model reference to the method contour
                target = valueFactory().createReference(method);
              }
              jiveEvents.add(eventFactory().createRTMethodCallEvent(timestamp, thread, line,
                  caller, target));
            }
            break;
          case METHOD_ENTERED:
            {
              // no details
              jiveEvents.add(eventFactory().createRTMethodEnteredEvent(timestamp, thread, line));
            }
            break;
          case METHOD_EXIT:
            {
              // pop the stack
              stacks.get(thread).pop();
              // no details
              jiveEvents.add(eventFactory().createRTMethodExitEvent(timestamp, thread, line));
            }
            break;
          case MONITOR_LOCK_BEGIN:
            {
              // all monitor events have the same detail: the hexadecimal monitor value
              final String monitor = event.getFieldValue(XMLEventField.MONITOR);
              jiveEvents.add(eventFactory().createMonitorLockBeginEvent(timestamp, thread, line,
                  monitor));
            }
            break;
          case MONITOR_LOCK_END:
            {
              // all monitor events have the same detail: the hexadecimal monitor value
              final String monitor = event.getFieldValue(XMLEventField.MONITOR);
              jiveEvents.add(eventFactory().createMonitorLockEndEvent(timestamp, thread, line,
                  monitor));
            }
            break;
          case MONITOR_LOCK_FAST:
            {
              // all monitor events have the same detail: the hexadecimal monitor value
              final String monitor = event.getFieldValue(XMLEventField.MONITOR);
              jiveEvents.add(eventFactory().createMonitorLockFastEvent(timestamp, thread, line,
                  monitor));
            }
            break;
          case MONITOR_RELOCK:
            {
              // all monitor events have the same detail: the hexadecimal monitor value
              final String monitor = event.getFieldValue(XMLEventField.MONITOR);
              jiveEvents.add(eventFactory().createMonitorRelockEvent(timestamp, thread, line,
                  monitor));
            }
            break;
          case MONITOR_UNLOCK_BEGIN:
            {
              // all monitor events have the same detail: the hexadecimal monitor value
              final String monitor = event.getFieldValue(XMLEventField.MONITOR);
              jiveEvents.add(eventFactory().createMonitorUnlockBeginEvent(timestamp, thread, line,
                  monitor));
            }
            break;
          case MONITOR_UNLOCK_COMPLETE:
            {
              // all monitor events have the same detail: the hexadecimal monitor value
              final String monitor = event.getFieldValue(XMLEventField.MONITOR);
              jiveEvents.add(eventFactory().createMonitorUnlockCompleteEvent(timestamp, thread,
                  line, monitor));
            }
            break;
          case MONITOR_UNLOCK_END:
            {
              // all monitor events have the same detail: the hexadecimal monitor value
              final String monitor = event.getFieldValue(XMLEventField.MONITOR);
              jiveEvents.add(eventFactory().createMonitorUnlockEndEvent(timestamp, thread, line,
                  monitor));
            }
            break;
          case MONITOR_UNLOCK_FAST:
            {
              // all monitor events have the same detail: the hexadecimal monitor value
              final String monitor = event.getFieldValue(XMLEventField.MONITOR);
              jiveEvents.add(eventFactory().createMonitorUnlockFastEvent(timestamp, thread, line,
                  monitor));
            }
            break;
          case OBJECT_NEW:
            {
              // . <type>edu.purdue.scj.BackingStoreID</type>
              // . <size>24</size>
              // . <object>00000000f5836050</object>
              final String typeName = event.getFieldValue(XMLEventField.TYPE);
              // find the type node or create a new one
              final ITypeNode typeNode = resolveType(staticFactory().typeNameToSignature(typeName));
              // create the necessary type contours
              createStaticContours(typeNode, timestamp, thread, line, jiveEvents);
              // retrieve the object identifier-- may not be unique
              final long objectId = Long.parseLong(event.getFieldValue(XMLEventField.OBJECT),
                  FijiImporter.BASE_16);
              long oid = objectId;
              IContextContour existing = contourFactory().lookupInstanceContour(typeName, oid);
              // if the oid is not unique, find an unused one deterministically
              while (existing != null)
              {
                oid = oid * 7 + (oid % 19);
                existing = contourFactory().lookupInstanceContour(typeName, oid);
              }
              int length = -1;
              if (event.hasAttribute(XMLEventField.ELEMENTS))
              {
                length = Integer.parseInt(event.getFieldValue(XMLEventField.ELEMENTS));
              }
              // create the instance contour
              final IObjectContour contour = length == -1 ? typeNode.createInstanceContour(oid)
                  : typeNode.createArrayContour(oid, length);
              // find the contour's allocation scope
              final String scope = threadToScope.get(thread.id());
              // final String scope = threadToScopeStack.get(thread.id()).peek();
              jiveEvents.add(eventFactory().createRTNewObjectEvent(timestamp, thread, line,
                  contour, scope));
              // push the contour onto the current scope (1:N)
              scopeToContours.get(scope).add(contour);
              // map the object identifier on the log to the allocation scope scope (1:1)
              objectIdToScope.put(objectId, scope);
              System.out.println("ALLOC IN " + scope + "[" + contour.signature() + "]");
            }
            break;
          case SCOPE_ALLOC:
            {
              final String scope = event.getFieldValue(XMLEventField.SCOPE);
              final int size = Integer.parseInt(event.getFieldValue(XMLEventField.SIZE));
              final boolean immortal = event.hasAttribute(XMLEventField.IMMORTAL);
              final IJiveEvent allocEvent = eventFactory().createScopeAllocEvent(timestamp, thread,
                  line, scope, size, immortal);
              jiveEvents.add(allocEvent);
              // create a home for the scope allocated objects
              scopeToContours.put(scope, TypeTools.<IObjectContour> newHashSet());
              System.out.println("ALLOCATED SCOPE " + scope + " FOR THREAD " + thread.id());
            }
            break;
          case SCOPE_BACKING_ALLOC:
            {
              final int size = Integer.parseInt(event.getFieldValue(XMLEventField.SIZE));
              jiveEvents.add(eventFactory().createScopeBackingAllocEvent(timestamp, thread, line,
                  size));
            }
            break;
          case SCOPE_BACKING_FREE:
            {
              jiveEvents.add(eventFactory().createScopeBackingFreeEvent(timestamp, thread, line));
            }
            break;
          case SCOPE_ENTER:
            {
              final String scope = event.getFieldValue(XMLEventField.SCOPE);
              jiveEvents.add(eventFactory().createScopeEnterEvent(timestamp, thread, line, scope));
              // mark this thread's allocation scope
              threadToScope.put(thread.id(), scope);
            }
            break;
          case SCOPE_EXIT:
            {
              final String scope = event.getFieldValue(XMLEventField.SCOPE);
              jiveEvents.add(eventFactory().createScopeExitEvent(timestamp, thread, line, scope));
            }
            break;
          case SCOPE_FREE:
            {
              final String scope = event.getFieldValue(XMLEventField.SCOPE);
              jiveEvents.add(eventFactory().createScopeFreeEvent(timestamp, thread, line, scope));
              System.out.println("FREED SCOPE " + scope + " FOR THREAD " + thread.id());
              // create a home for the scope allocated objects
              final Set<IObjectContour> contours = scopeToContours.remove(scope);
              for (final IObjectContour contour : contours)
              {
                jiveEvents.add(eventFactory()
                    .createRTDestroyEvent(timestamp, thread, line, contour));
                System.out.println("FREE FROM " + scope + "[" + contour.signature() + "]");
              }
            }
            break;
          case SCOPE_POP:
            {
              final String scope = event.getFieldValue(XMLEventField.SCOPE);
              jiveEvents.add(eventFactory().createScopePopEvent(timestamp, thread, line, scope));
              // pop the scope from the thread scope stack
              threadToScopeStack.get(thread.id()).pop();
            }
            break;
          case SCOPE_PUSH:
            {
              if (!threadToScopeStack.containsKey(thread.id()))
              {
                System.out.println("ALLOCATING STACK FOR THREAD: " + thread.id());
                threadToScopeStack.put(thread.id(), new LinkedList<String>());
              }
              final String scope = event.getFieldValue(XMLEventField.SCOPE);
              jiveEvents.add(eventFactory().createScopePushEvent(timestamp, thread, line, scope));
              // push the scope onto the thread scope stack
              threadToScopeStack.get(thread.id()).push(scope);
            }
            break;
          case SYSTEM_END:
            {
              // no details
              jiveEvents.add(eventFactory().createRTSystemExitEvent(timestamp));
            }
            break;
          case SYSTEM_START:
            {
              // no details
              jiveEvents.add(eventFactory().createRTSystemStartEvent(timestamp));
            }
            break;
          case THREAD_CREATE:
            {
              // <newthread>3</newthread>
              IObjectContour threadContour = contourFactory().lookupInstanceContour(
                  "java.lang.Thread", Long.valueOf(event.getFieldValue(XMLEventField.NEWTHREAD)));
              // BUG: duplicate thread create in the event log
              if (threadContour != null)
              {
                break;
              }
              // find the type node or create a new one
              final ITypeNode threadNode = resolveType("Ljava/lang/Thread;");
              // create the necessary type contours
              createStaticContours(threadNode, timestamp, thread, line, jiveEvents);
              // create the thread's instance contour
              threadContour = threadNode.createInstanceContour(Integer.valueOf(event
                  .getFieldValue(XMLEventField.NEWTHREAD)));
              // create the new stack
              stacks.put(createThread(Long.valueOf(event.getFieldValue(XMLEventField.NEWTHREAD))),
                  new Stack<IMethodContour>());
              // create the new thread object
              jiveEvents.add(eventFactory().createRTThreadNewEvent(timestamp, thread, line,
                  threadContour, Long.valueOf(event.getFieldValue(XMLEventField.NEWTHREAD))));
              // infer the caller of the thread creation
              final IValue caller;
              if (stacks.get(thread) == null || stacks.get(thread).isEmpty())
              {
                // system
                caller = valueFactory().createSystemCaller();
              }
              else
              {
                // top of the respective thread
                final IMethodContour top = stacks.get(thread).peek();
                caller = valueFactory().createReference(top);
              }
              // the target of the thread creation is its constructor
              final IValue target = valueFactory().createReference(
                  threadContour.createMethodContour(threadNode.methodMembers().get(0), thread));
              // create the constructor call
              jiveEvents.add(eventFactory().createRTMethodCallEvent(timestamp, thread, line,
                  caller, target));
              // create the constructor enter
              jiveEvents.add(eventFactory().createRTMethodEnteredEvent(timestamp, thread, line));
              // update the thread contour's id
              jiveEvents.add(eventFactory()
                  .createRTFieldWriteEvent(
                      timestamp,
                      thread,
                      line,
                      threadContour,
                      valueFactory().createPrimitiveValue(
                          event.getFieldValue(XMLEventField.NEWTHREAD)),
                      threadContour.lookupMember("id")));
              // create the constructor exit
              jiveEvents.add(eventFactory().createRTMethodExitEvent(timestamp, thread, line));
            }
            break;
          case THREAD_END:
            {
              // no details
              jiveEvents.add(eventFactory().createRTThreadEndEvent(timestamp, thread));
            }
            break;
          case THREAD_PRIORITY:
            {
              // <target>2</target>
              // <scheduler>Java</scheduler>
              // <priority>5</priority>
              final String scheduler = event.getFieldValue(XMLEventField.SCHEDULER);
              final int priority = Integer.valueOf(event.getFieldValue(XMLEventField.PRIORITY));
              jiveEvents.add(eventFactory().createRTThreadPriorityEvent(timestamp, thread,
                  scheduler, priority));
              // look up the thread's instance contour
              final IContextContour threadContour = contourFactory().lookupInstanceContour(
                  "java.lang.Thread", Long.valueOf(event.getFieldValue(XMLEventField.TARGET)));
              // the thread must exist if the log is consistent
              assert threadContour != null : "Inconsistent fiji log-- no thread created for thread id "
                  + thread.id();
              // update the thread contour's scheduler
              jiveEvents.add(eventFactory().createRTFieldWriteEvent(
                  timestamp,
                  thread,
                  line,
                  threadContour,
                  valueFactory().createResolvedValue(event.getFieldValue(XMLEventField.SCHEDULER),
                      ""), threadContour.lookupMember("scheduler")));
              // update the thread contour's priority
              jiveEvents.add(eventFactory().createRTFieldWriteEvent(timestamp, thread, line,
                  threadContour,
                  valueFactory().createPrimitiveValue(event.getFieldValue(XMLEventField.PRIORITY)),
                  threadContour.lookupMember("priority")));
            }
            break;
          case THREAD_SLEEP:
            {
              // <waketime>18446744072348045267</waketime>
              final long waketime = Long.valueOf(event.getFieldValue(XMLEventField.WAKETIME));
              jiveEvents.add(eventFactory().createRTThreadSleepEvent(timestamp, thread, waketime));
            }
            break;
          case THREAD_START:
            {
              // <scheduler>Normal</scheduler>
              // <priority>0</priority>
              final String scheduler = event.getFieldValue(XMLEventField.SCHEDULER);
              final int priority = Integer.valueOf(event.getFieldValue(XMLEventField.PRIORITY));
              jiveEvents.add(eventFactory().createRTThreadStartEvent(timestamp, thread, scheduler,
                  priority));
            }
            break;
          case THREAD_WAKE:
            {
              // <waketime>1338497917180685121</waketime>
              final long waketime = Long.valueOf(event.getFieldValue(XMLEventField.WAKETIME));
              jiveEvents.add(eventFactory().createRTThreadWakeEvent(timestamp, thread, waketime));
            }
            break;
          case THREAD_YIELD:
            {
              // <waketime>1338497917180685121</waketime>
              final long waketime = Long.valueOf(event.getFieldValue(XMLEventField.WAKETIME));
              jiveEvents.add(eventFactory().createRTThreadYieldEvent(timestamp, thread, waketime));
            }
            break;
          default:
            {
            }
        }
      }
      catch (final RuntimeException e)
      {
        System.err.println(event);
        e.printStackTrace();
        throw e;
      }
      if (jiveEvents.size() == FijiImporter.BLOCK_SIZE)
      {
        model.eventOccurred(null, jiveEvents);
        jiveEvents.clear();
      }
    }
    // process any remaining events
    if (jiveEvents.size() > 0)
    {
      model.eventOccurred(null, jiveEvents);
      jiveEvents.clear();
    }
  }

  @Override
  public void endElement(final String uri, final String localName, final String qName)
      throws SAXException
  {
    // closing event tag
    if (checkEvent(localName))
    {
      // System.out.print(event.toString());
      eventList.add(event);
      event = null;
    }
    else if (event != null && event.currentField() != null
        && localName.equalsIgnoreCase(event.currentField().fieldName()))
    {
      event.setCurrentField(null);
    }
  }

  @Override
  public void process(final String url, final IExecutionModel model,
      final IStaticModelDelegate upstream) throws OfflineImporterException
  {
    final long start = System.nanoTime();
    this.event = null;
    this.eventList = TypeTools.newArrayList();
    this.fileName = url.substring(FijiImporter.PROTO_FIJI.length());
    this.model = model;
    this.staticModelDelegate = upstream;
    this.threadToScope = TypeTools.newHashMap();
    this.threadToScopeStack = TypeTools.newHashMap();
    this.objectIdToScope = TypeTools.newHashMap();
    this.scopeToContours = TypeTools.newHashMap();
    try
    {
      // Get SAX Parser Factory
      final SAXParserFactory factory = SAXParserFactory.newInstance();
      // Turn on validation, and turn off namespaces
      factory.setValidating(true);
      factory.setNamespaceAware(true);
      final SAXParser parser = factory.newSAXParser();
      parser.parse(fileName, this);
      createEvents();
    }
    catch (final ParserConfigurationException e)
    {
      throw new OfflineImporterException(
          "The underlying parser does not support the requested features.", e);
    }
    catch (final FactoryConfigurationError e)
    {
      throw new OfflineImporterException("Error occurred obtaining SAX Parser Factory.", e);
    }
    catch (final Exception e)
    {
      e.printStackTrace();
      throw new OfflineImporterException(e.getMessage());
    }
    System.err.println("Imported the Fiji trace in " + ((System.nanoTime() - start) / 1000000)
        + "ms.");
  }

  @Override
  public void startElement(final String uri, final String localName, final String name,
      final Attributes atts) throws SAXException
  {
    // skip the root tag
    if (checkRoot(localName))
    {
      event = null;
      return;
    }
    // skip the event tag
    if (checkEvent(localName))
    {
      event = new EventDAO();
      return;
    }
    // skip the details tag
    if (checkDetails(localName))
    {
      event.setCurrentField(null);
      return;
    }
    // try to resolve the tag to a known XML event field tag
    event.setCurrentField(checkEventField(localName));
  }

  private boolean checkDetails(final String tagName)
  {
    return "details".equalsIgnoreCase(tagName);
  }

  private boolean checkEvent(final String tagName)
  {
    return "event".equalsIgnoreCase(tagName);
  }

  private XMLEventField checkEventField(final String tagName)
  {
    try
    {
      return XMLEventField.valueOf(tagName.toUpperCase());
    }
    catch (final IllegalArgumentException e)
    {
    }
    return null;
  }

  private boolean checkRoot(final String tagName)
  {
    return "events".equalsIgnoreCase(tagName);
  }

  private String constructorName(final String signature)
  {
    String result = staticFactory().signatureToTypeName(signature);
    result = result.substring(result.lastIndexOf('.') + 1);
    result = result.substring(result.lastIndexOf('$') + 1);
    return result;
  }

  private IContourFactory contourFactory()
  {
    return model.contourFactory();
  }

  private void createStaticContours(final ITypeNode typeNode, final long timestamp,
      final IThreadValue thread, final ILineValue line, final List<IJiveEvent> jiveEvents)
  {
    // supported static contours: classes
    if (typeNode == null || typeNode.kind() != NodeKind.NK_CLASS)
    {
      return;
    }
    // static contour of the loaded type
    IContextContour contour = contourFactory().lookupStaticContour(typeNode.name());
    // create the static contour if necessary
    if (contour == null)
    {
      // create a new static contour for the super class
      if (typeNode.superClass() != null && typeNode.superClass().node() != null)
      {
        createStaticContours(typeNode.superClass().node(), timestamp, thread, line, jiveEvents);
      }
      // safety: all parent static contours exist and have corresponding load events
      contour = typeNode.createStaticContour();
      // add the type load event
      jiveEvents.add(eventFactory().createRTTypeLoadEvent(timestamp, thread, line, contour));
    }
  }

  private IThreadValue createThread(final EventDAO event)
  {
    return "SYSTEM".equals(event.threadId()) ? createThread(-1000) : createThread(Integer
        .valueOf(event.threadId()));
  }

  private IThreadValue createThread(final long threadId)
  {
    return threadId == -1000 ? valueFactory().createThread(-1000, "SYSTEM") : valueFactory()
        .createThread(threadId, "Thread-" + threadId);
  }

  private IEventFactory eventFactory()
  {
    return model.eventFactory();
  }

  private ITypeNode resolveType(final String key)
  {
    // primitive types
    if (!key.startsWith("L") && !key.startsWith("["))
    {
      return staticFactory().lookupTypeNode(
          key.indexOf(';') >= 0 ? key.substring(0, key.indexOf(';')) : key);
    }
    // find the type node
    ITypeNode typeNode = staticFactory().lookupTypeNode(key);
    if (typeNode == null)
    {
      if ("Ljava/lang/Thread;".equals(key))
      {
        typeNode = staticFactory().createThreadNode();
      }
      else
      {
        typeNode = staticModelDelegate.resolveType(key, staticFactory().signatureToTypeName(key))
            .node();
      }
    }
    return typeNode;
  }

  private IStaticModelFactory staticFactory()
  {
    return model.staticModelFactory();
  }

  private String unescape(final String value)
  {
    String string = value;
    // int index = string.indexOf(CLINIT_ESCAPED);
    // if (index > 0) {
    // string = string.replace(CLINIT_ESCAPED, CLINIT_UNESCAPED);
    // }
    // else {
    final int index = string.indexOf(FijiImporter.INIT_ESCAPED);
    if (index > 0)
    {
      string = string.substring(0, index) + constructorName(string.substring(0, index - 1))
          + string.substring(index + FijiImporter.INIT_ESCAPED.length());
    }
    // }
    return string;
  }

  private IValueFactory valueFactory()
  {
    return model.valueFactory();
  }

  private static class EventDAO
  {
    private XMLEventField currentField;
    private final Map<XMLEventField, String> fields;
    private final Map<String, EventKind> kindLookup;
    private String file;
    private Long id;
    private EventKind kind;
    private Integer line;
    private String threadId;
    private Long timestamp;

    EventDAO()
    {
      this.fields = TypeTools.newHashMap();
      this.kindLookup = TypeTools.newHashMap();
      for (final EventKind kind : EventKind.values())
      {
        kindLookup.put(kind.eventName(), kind);
      }
    }

    public XMLEventField currentField()
    {
      return this.currentField;
    }

    public String getFieldValue(final XMLEventField field)
    {
      return fields.get(field);
    }

    public boolean hasAttribute(final XMLEventField field)
    {
      return fields.containsKey(field);
    }

    public EventKind kind()
    {
      return this.kind;
    }

    public void setCurrentField(final XMLEventField value)
    {
      this.currentField = value;
      this.fields.put(currentField, null);
    }

    public void setCurrentFieldValue(final String value)
    {
      if (currentField == null)
      {
        throw new IllegalArgumentException("Cannot set the value of a null event field.");
      }
      switch (currentField)
      {
        case ID:
          this.id = Long.valueOf(value);
          break;
        case KIND:
          this.kind = kindLookup.get(value);
          break;
        case FILE:
          this.file = value;
          break;
        case LINE:
          this.line = Integer.valueOf(value);
          break;
        case THREAD:
          this.threadId = value;
          break;
        case TIMESTAMP:
          this.timestamp = Long.valueOf(value);
          break;
        default:
          this.fields.put(currentField, value);
          break;
      }
    }

    public String threadId()
    {
      return this.threadId;
    }

    public long timestamp()
    {
      return this.timestamp;
    }

    @Override
    public String toString()
    {
      final StringBuffer buffer = new StringBuffer("");
      buffer.append("id = ").append(id).append("\n");
      buffer.append("timestamp = ").append(timestamp).append("\n");
      buffer.append("kind = ").append(kind.toString()).append("\n");
      buffer.append("file = ").append(file).append("\n");
      buffer.append("line = ").append(line).append("\n");
      buffer.append("threadId = ").append(threadId).append("\n");
      for (final XMLEventField field : fields.keySet())
      {
        if (field == null)
        {
          continue;
        }
        buffer.append(field.fieldName()).append(" = ").append(fields.get(field)).append("\n");
      }
      return buffer.toString();
    }
  }
}
