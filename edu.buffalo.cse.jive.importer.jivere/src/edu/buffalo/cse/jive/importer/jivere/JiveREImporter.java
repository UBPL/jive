package edu.buffalo.cse.jive.importer.jivere;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.buffalo.cse.jive.launch.offline.IOfflineImporter;
import edu.buffalo.cse.jive.launch.offline.OfflineImporterException;
import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.lib.XMLTools.XMLEventField;
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

public class JiveREImporter extends DefaultHandler implements IOfflineImporter
{
  private final static int BASE_16 = 16;
  private final static int BLOCK_SIZE = 100;
  private final static String INIT_ESCAPED = "<init>";
  private static final String PROTO_JIVERE = "jivere://";
  private EventDAO event;
  private List<EventDAO> eventList;
  private String fileName;
  private IExecutionModel model;
  private IStaticModelDelegate staticModelDelegate;

  @Override
  public void characters(final char[] ch, final int start, final int length) throws SAXException
  {
    if (event != null && event.currentField() != null)
    {
      event.setCurrentFieldValue(unescape(new String(ch, start, length)));
    }
  }

  public void createEvents()
  {
    // result in trace log order
    final List<IJiveEvent> jiveEvents = TypeTools.newArrayList(eventList.size());
    // keep track of all stacks
    final Map<IThreadValue, Stack<IMethodContour>> stacks = TypeTools.newHashMap();
    for (final EventDAO event : eventList)
    {
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
        // unsupported by JiveRE
        // case EXCEPTION_CATCH:
        // case EXCEPTION_THROW:
        // case FIELD_READ:
        // case LINE_STEP:
        // case METHOD_RETURNED:
        // case THREAD_LOCK:
        // case TYPE_LOAD:
        // case VAR_ASSIGN:
        // case VAR_DELETE:
        // case FIELD_READ:
        // case FIELD_WRITE:
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
                createStaticContours(typeNode, thread, line, jiveEvents);
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
                createStaticContours(returnTypeNode, thread, line, jiveEvents);
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
              jiveEvents.add(eventFactory().createMethodCallEvent(thread, line, caller, target));
            }
            break;
          case METHOD_ENTERED:
            {
              // no details
              jiveEvents.add(eventFactory().createMethodEnteredEvent(thread, line));
            }
            break;
          case METHOD_EXIT:
            {
              // pop the stack
              stacks.get(thread).pop();
              // no details
              jiveEvents.add(eventFactory().createMethodExitEvent(thread, line));
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
              createStaticContours(typeNode, thread, line, jiveEvents);
              // retrieve the object identifier-- may not be unique
              final long objectId = Long.parseLong(event.getFieldValue(XMLEventField.OBJECT),
                  JiveREImporter.BASE_16);
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
              jiveEvents.add(eventFactory().createNewObjectEvent(thread, line, contour));
            }
            break;
          case SYSTEM_END:
            {
              // no details
              jiveEvents.add(eventFactory().createSystemExitEvent());
            }
            break;
          case THREAD_END:
            {
              // no details
              jiveEvents.add(eventFactory().createThreadEndEvent(thread));
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
      if (jiveEvents.size() == JiveREImporter.BLOCK_SIZE)
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
    this.fileName = url.substring(JiveREImporter.PROTO_JIVERE.length());
    this.model = model;
    this.staticModelDelegate = upstream;
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
    System.err.println("Imported the Jive RE trace in " + ((System.nanoTime() - start) / 1000000)
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

  private void createStaticContours(final ITypeNode typeNode, final IThreadValue thread,
      final ILineValue line, final List<IJiveEvent> jiveEvents)
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
        createStaticContours(typeNode.superClass().node(), thread, line, jiveEvents);
      }
      // safety: all parent static contours exist and have corresponding load events
      contour = typeNode.createStaticContour();
      // add the type load event
      jiveEvents.add(eventFactory().createTypeLoadEvent(thread, line, contour));
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
    final int index = string.indexOf(JiveREImporter.INIT_ESCAPED);
    if (index > 0)
    {
      string = string.substring(0, index) + constructorName(string.substring(0, index - 1))
          + string.substring(index + JiveREImporter.INIT_ESCAPED.length());
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
        default:
          this.fields.put(currentField, value);
          break;
      }
    }

    public String threadId()
    {
      return this.threadId;
    }

    @Override
    public String toString()
    {
      final StringBuffer buffer = new StringBuffer("");
      buffer.append("id = ").append(id).append("\n");
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
