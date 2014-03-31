package edu.buffalo.cse.jive.internal.debug.jdi.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.sun.jdi.ClassType;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.ExceptionEvent;

import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.debug.jdi.model.IExecutionState;
import edu.buffalo.cse.jive.debug.jdi.model.IThreadSummary;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IContourModel.IObjectContour;
import edu.buffalo.cse.jive.model.IEventModel.IMethodTerminatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.ITerminatorEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IModel.ILineValue;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IMethodDependenceGraph;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedData;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedLine;
import edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.model.factory.IContourFactory;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory;

final class ExecutionState implements IExecutionState
{
  private static final int IDS_INITIAL_CAPACITY = 2048;
  private final Set<JiveStackFrame> inModelFrames;
  private final Map<Long, IObjectContour> oidToContour;
  private final ConcurrentMap<Object, IContour> keyToContour;
  private final ConcurrentMap<String, ILineValue> accessorKeyToLine;
  private final ConcurrentMap<Long, ThreadSummary> threadSummaries;
  private final ConcurrentMap<Long, ExceptionEvent> threadToException;
  private final Map<Long, ILineValue> threadToLine;
  private final Map<Long, IObjectContour> threadToArrayResult;
  private final ConcurrentMap<Long, Deque<JiveStackFrame>> threadToFrames;
  private final ConcurrentMap<Long, Deque<JiveStackFrame>> threadToInModelFrames;
  private final Map<Long, IMethodTerminatorEvent> threadToPendingMethodReturned;
  private final Map<IContourMember, Object> variableToValue;
  private final Set<IMethodContour> visitedArguments;
  private final Object[] NULL_HOLDER = new Object[] { null };
  private final IExecutionModel model;

  ExecutionState(final IExecutionModel executionModel)
  {
    this.model = executionModel;
    this.inModelFrames = TypeTools.newConcurrentSet();
    this.oidToContour = TypeTools.newConcurrentHashMap();
    this.threadToPendingMethodReturned = TypeTools.newConcurrentHashMap();
    this.keyToContour = TypeTools.newConcurrentHashMap(ExecutionState.IDS_INITIAL_CAPACITY);
    this.accessorKeyToLine = TypeTools.newConcurrentHashMap();
    this.threadToException = TypeTools.newConcurrentHashMap();
    this.threadToLine = TypeTools.newConcurrentHashMap();
    this.threadToArrayResult = TypeTools.newConcurrentHashMap();
    this.threadToFrames = TypeTools.newConcurrentHashMap();
    this.threadToInModelFrames = TypeTools.newConcurrentHashMap();
    this.threadSummaries = TypeTools.newConcurrentHashMap();
    this.variableToValue = TypeTools.newConcurrentHashMap();
    this.visitedArguments = TypeTools.newConcurrentSet();
  }

  @Override
  public boolean containsContour(final StackFrame frame)
  {
    return keyToContour.containsKey(toJiveStackFrame(frame));
  }

  @Override
  public boolean containsException(final long threadId)
  {
    return threadToException.containsKey(threadId);
  }

  @Override
  public boolean containsInModelFrame(final StackFrame frame)
  {
    return inModelFrames.contains(toJiveStackFrame(frame));
  }

  @Override
  public boolean containsThread(final ThreadReference thread)
  {
    return threadToFrames.containsKey(thread.uniqueID());
  }

  @Override
  public boolean containsTopFrameContour(final long threadId)
  {
    return lookupContour(framePeek(threadId)) != null;
  }

  @Override
  public ILineValue currentLine(final IThreadValue thread)
  {
    final ILineValue old = threadToLine.get(thread.id());
    return old == null ? model.valueFactory().createUnavailableLine() : old;
  }

  @Override
  public IObjectContour deleteObject(final long oid)
  {
    return oidToContour.remove(oid);
  }

  @Override
  public int frameCount(final long threadId)
  {
    return lookupFrames(threadId).size();
  }

  @Override
  public StackFrame framePeek(final long threadId)
  {
    return lookupFrames(threadId).peek();
  }

  @Override
  public StackFrame framePeekInModel(final long threadId)
  {
    return lookupInModelFrames(threadId).peek();
  }

  @Override
  public StackFrame framePop(final long threadId)
  {
    final StackFrame f = lookupFrames(threadId).pop();
    if (containsInModelFrame(f))
    {
      inModelFrames.remove(f);
      lookupInModelFrames(threadId).pop();
    }
    variableToValue.remove(f);
    return f;
  }

  @Override
  public StackFrame framePush(final long threadId, final StackFrame frame, final boolean isInModel)
  {
    final JiveStackFrame f = toJiveStackFrame(frame);
    lookupFrames(threadId).push(f);
    if (isInModel)
    {
      inModelFrames.add(f);
      lookupInModelFrames(threadId).push(f);
    }
    return f;
  }

  @Override
  public boolean hasFrames(final long threadId)
  {
    return !lookupFrames(threadId).isEmpty();
  }

  @Override
  public ITerminatorEvent getPendingMethodReturned(final long threadId)
  {
    return threadToPendingMethodReturned.get(threadId);
  }

  @Override
  public boolean hasPendingMethodReturned(final long threadId)
  {
    return threadToPendingMethodReturned.containsKey(threadId);
  }

  @Override
  public IThreadSummary inspectThread(final ThreadReference thread)
  {
    if (threadToFrames.get(thread.uniqueID()) == null)
    {
      return null;
    }
    final ThreadSummary oldSummary = threadSummaries.get(thread.uniqueID());
    final ThreadSummary newSummary = new ThreadSummary(contourFactory(), thread, oldSummary);
    // ignore thread state changes if: inconsistent waits (with no locks) or no state changes, and
    // no locks were released/acquired
    if ((newSummary.inconsistentWait() || !newSummary.isWaiting())
        && newSummary.acquiredLockDescriptions().size() == 0
        && newSummary.acquiredLocks().size() == 0
        && newSummary.releasedLockDescriptions().size() == 0
        && newSummary.releasedLocks().size() == 0)
    {
      return null;
    }
    threadSummaries.putIfAbsent(thread.uniqueID(), newSummary);
    return newSummary;
  }

  @Override
  public boolean isArrayCellWriteLine(final IContourMember var, final ThreadReference thread,
      final boolean checkDef)
  {
    final ILineValue line = threadToLine.get(thread.uniqueID());
    // if there is no line node to process
    if (line == model.valueFactory().createUnavailableLine())
    {
      return false;
    }
    // try to determine from the static information whether this is an assignment
    final IMethodDependenceGraph mdg = ((IMethodNode) var.schema().parent()).getDependenceGraph();
    if (mdg == null)
    {
      return false;
    }
    final IResolvedLine rline = mdg.dependenceMap().get(line.lineNumber());
    // no corresponding line in the mdg
    if (rline == null)
    {
      return false;
    }
    // not an array cell update
    if (rline.kind() != LineKind.LK_ASSIGNMENT_ARRAY_CELL
        && rline.kind() != LineKind.LK_POSTFIX_ARRAY_CELL
        && rline.kind() != LineKind.LK_PREFIX_ARRAY_CELL)
    {
      return false;
    }
    // the resolved line should contain at most one definition
    for (final IResolvedData rd : rline.definitions())
    {
      if (rd.data() == var.schema())
      {
        return !checkDef || (rd.isDef() && rd.isLHS());
      }
    }
    // not a definition of the given member
    return false;
  }

  @Override
  public boolean isArrayCellWriteLine(final IMethodContour method, final IContourMember field,
      final ThreadReference thread)
  {
    final ILineValue line = threadToLine.get(thread.uniqueID());
    // if there is no line node to process
    if (line == model.valueFactory().createUnavailableLine())
    {
      return false;
    }
    // try to determine from the static information whether this is an assignment
    final IMethodDependenceGraph mdg = method.schema().getDependenceGraph();
    if (mdg == null)
    {
      return false;
    }
    final IResolvedLine rline = mdg.dependenceMap().get(line.lineNumber());
    // no corresponding line in the mdg
    if (rline == null || rline.kind() == LineKind.LK_METHOD_DECLARATION)
    {
      return false;
    }
    // not an array cell update
    if (rline.kind() != LineKind.LK_ASSIGNMENT_ARRAY_CELL
        && rline.kind() != LineKind.LK_POSTFIX_ARRAY_CELL
        && rline.kind() != LineKind.LK_PREFIX_ARRAY_CELL)
    {
      return false;
    }
    // the resolved line should contain at most one field definition
    for (final IResolvedData rd : rline.definitions())
    {
      // check the field by name, if the field is given
      if (rd.data().kind() == NodeKind.NK_FIELD
          && (field == null || field.name().equals(rd.data().name())))
      {
        return true;
      }
    }
    // not a definition of the given member
    return false;
  }

  @Override
  public IObjectContour lookupArrayResult(final ThreadReference thread)
  {
    return threadToArrayResult.get(thread.uniqueID());
  }

  @Override
  public IMethodContour lookupContour(final StackFrame frame)
  {
    return (IMethodContour) keyToContour.get(toJiveStackFrame(frame));
  }

  @Override
  public ExceptionEvent lookupException(final long threadID)
  {
    return threadToException.get(threadID);
  }

  @Override
  public IContextContour lookupObject(final long oid)
  {
    return oidToContour.get(oid);
  }

  @Override
  public ILineValue lookupSyntheticAccessorLine(final String methodKey)
  {
    return accessorKeyToLine.get(methodKey);
  }

  @Override
  public String methodKey(final Method method)
  {
    if (method == null)
    {
      return null;
    }
    final ReferenceType type = method.declaringType();
    if (method.isStaticInitializer())
    {
      return type.signature() + ".<clinit>()";
    }
    String signature = method.signature();
    signature = signature.substring(0, signature.indexOf(')') + 1);
    if (method.isConstructor())
    {
      final ClassType declaringType = (ClassType) method.declaringType();
      // this method is a constructor and it's declaring type is a non-static nested type
      // or a method
      if (!declaringType.isStatic() && declaringType.name().contains("$")
          && !declaringType.name().startsWith("$"))
      {
        return type.signature() + "." + constructorName(method.declaringType().name()) + signature;
      }
      return type.signature() + "." + constructorName(method.declaringType().name()) + signature;
    }
    return type.signature() + "." + method.name() + signature;
  }

  @Override
  public String methodName(final Method method)
  {
    if (method == null)
    {
      return null;
    }
    final String attributes;
    if (method.isBridge())
    {
      attributes = " <BRIDGE>";
    }
    else if (method.isSynthetic())
    {
      attributes = " <SYNTHETIC>";
    }
    else
    {
      attributes = "";
    }
    return methodKey(method) + attributes;
    // final ReferenceType type = method.declaringType();
    // if (method.isStaticInitializer()) {
    // return type.name() + ".<clinit>" + attributes;
    // }
    // if (method.isConstructor()) {
    // return type.name() + "." + constructorName(method.declaringType().name()) + attributes;
    // }
    // return type.name() + "." + method.name() + attributes;
  }

  @Override
  public void methodReturnedPending(final long threadId, final IMethodTerminatorEvent terminator)
  {
    if (framePeek(threadId) != null && containsInModelFrame(framePeek(threadId)))
    {
      threadToPendingMethodReturned.put(threadId, terminator);
    }
  }

  @Override
  public IMethodTerminatorEvent methodReturnedProcessed(final long threadId)
  {
    return threadToPendingMethodReturned.remove(threadId);
  }

  @Override
  public ILineValue nextLine(final IThreadValue thread, final ILineValue line)
  {
    final ILineValue old = threadToLine.put(thread.id(), line);
    return old == null ? model.valueFactory().createUnavailableLine() : old;
  }

  @Override
  public void observedArrayResult(final ThreadReference thread, final IObjectContour oc)
  {
    threadToArrayResult.put(thread.uniqueID(), oc);
  }

  @Override
  public void observedContour(final IObjectContour contour, final long oid)
  {
    oidToContour.put(oid, contour);
  }

  @Override
  public boolean observedException(final long threadId, final ExceptionEvent event)
  {
    final Object old = threadToException.putIfAbsent(threadId, event);
    return old == null && event != null;
  }

  /**
   * Observed a method contour.
   */
  @Override
  public boolean observedMethod(final StackFrame frame, final IMethodContour contour)
  {
    final Object old = keyToContour.putIfAbsent(toJiveStackFrame(frame), contour);
    return old == null && contour != null;
  }

  /**
   * Observed a synthetic accessor.
   */
  @Override
  public boolean observedSyntheticAccessor(final String methodKey, final ILineValue line)
  {
    final Object old = accessorKeyToLine.putIfAbsent(methodKey, line);
    return old == null && methodKey != null;
  }

  @Override
  public void observedThread(final IThreadValue thread)
  {
    lookupFrames(thread.id());
  }

  /**
   * NOTE: object equality does not capture situations in which the local variables is assigned the
   * same value it already had, although an actual assignment takes place. On the other hand,
   * standard equality results in too many variable writes since the underlying JVM may return
   * different Value references to a given variable (at different points in execution) even if the
   * variable's value hasn't actually changed.
   */
  @Override
  public boolean observedVariable(final IContourMember var, final Value value,
      final ThreadReference thread, final int lineNumber)
  {
    final Object actual = notNull(value);
    final Object old = variableToValue.put(var, actual);
    // obviously, an assignment
    if (old == null || !old.equals(actual))
    {
      return true;
    }
    // final variables do not change
    if (var.schema().modifiers().contains(NodeModifier.NM_FINAL))
    {
      return false;
    }
    // fields have read and write events that are used for array changes
    if (var.schema().kind() == NodeKind.NK_FIELD)
    {
      return false;
    }
    /**
     * NOTE: this is a more expensive check based on static analysis, when available. It addresses
     * the cases in which an assignment binds a value to a variable identical to the old value. In
     * such occurrences, if static information is available, we can determine if this is an actual
     * assignment.
     */
    final ILineValue line = threadToLine.get(thread.uniqueID());
    // if there is no line node to process
    if (line == model.valueFactory().createUnavailableLine())
    {
      return false;
    }
    // try to determine from the static information whether this is an assignment
    final IMethodDependenceGraph mdg = ((IMethodNode) var.schema().parent()).getDependenceGraph();
    if (mdg == null)
    {
      return false;
    }
    // no corresponding line in the mdg
    final IResolvedLine rline = mdg.dependenceMap().get(line.lineNumber());
    if (rline == null || rline.kind() == LineKind.LK_METHOD_DECLARATION)
    {
      return false;
    }
    // the resolved line should contain at most one definition
    for (final IResolvedData rd : rline.definitions())
    {
      if (rd.data() == var.schema())
      {
        return true;
      }
    }
    /**
     * Static definition not found-- this is imprecise in the case where only JDI information is
     * available and the new and old values of an assignment are identical.
     */
    return false;
  }

  // @Override
  // public Set<IContextContour> reachableObjects()
  // {
  // return new HashSet<IContextContour>(oidToContour.values());
  // }
  @Override
  public void removeArguments(final IMethodContour method)
  {
    visitedArguments.remove(method);
  }

  @Override
  public void removeArrayResult(final ThreadReference thread)
  {
    threadToArrayResult.remove(thread.uniqueID());
  }

  @Override
  public boolean removeException(final long threadId)
  {
    threadToException.remove(threadId);
    return true;
  }

  @Override
  public ILineValue removeSyntheticAccessor(final String methodKey)
  {
    return accessorKeyToLine.remove(methodKey);
  }

  @Override
  public boolean removeVariable(final IContourMember var, final ThreadReference thread)
  {
    /**
     * If the variable is still in scope (based on the static analysis), a variable delete should
     * not be generated.
     */
    final ILineValue line = threadToLine.get(thread.uniqueID());
    if (line != null && var.schema().lineFrom() <= line.lineNumber()
        && line.lineNumber() <= var.schema().lineTo())
    {
      return false;
    }
    return variableToValue.remove(var) != null;
  }

  @Override
  public void reset()
  {
    inModelFrames.clear();
    keyToContour.clear();
    threadSummaries.clear();
    threadToException.clear();
    threadToLine.clear();
    threadToPendingMethodReturned.clear();
    threadToFrames.clear();
    threadToInModelFrames.clear();
    variableToValue.clear();
    visitedArguments.clear();
  }

  @Override
  public IContextContour retrieveContext(final StackFrame frame)
  {
    final StackFrame f = toJiveStackFrame(frame);
    final Method method = f.location().method();
    final ReferenceType type = method.declaringType();
    if (method.isStatic() || method.isNative())
    {
      return contourFactory().retrieveStaticContour(type.name());
    }
    else
    {
      final ObjectReference object = f.thisObject();
      return contourFactory().retrieveInstanceContour(type.name(), object.uniqueID());
    }
  }

  @Override
  public IMethodContour retrieveContour(final StackFrame frame)
  {
    final IMethodContour result = lookupContour(frame);
    if (result == null)
    {
      throw new IllegalArgumentException(String.format("No contour identifier for frame: %s (%s)!",
          frame.location().method().name(), frame.location()));
    }
    return result;
  }

  @Override
  public IMethodContour retrieveTopContour(final long threadId)
  {
    return retrieveContour(framePeek(threadId));
  }

  @Override
  public boolean visitedArguments(final IMethodContour method)
  {
    return !visitedArguments.add(method);
  }

  private String constructorName(final String typeName)
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

  private Deque<JiveStackFrame> lookupFrames(final long threadId)
  {
    Deque<JiveStackFrame> frames = threadToFrames.get(threadId);
    if (frames == null)
    {
      threadToFrames.putIfAbsent(threadId, new ArrayDeque<JiveStackFrame>());
      frames = threadToFrames.get(threadId);
    }
    return frames;
  }

  private Deque<JiveStackFrame> lookupInModelFrames(final long threadId)
  {
    Deque<JiveStackFrame> frames = threadToInModelFrames.get(threadId);
    if (frames == null)
    {
      threadToInModelFrames.putIfAbsent(threadId, new ArrayDeque<JiveStackFrame>());
      frames = threadToInModelFrames.get(threadId);
    }
    return frames;
  }

  /**
   * Adapts a null value for use in a ConcurrentHashMap and also transforms values to strings in
   * order to circumvent JDI issues-- the same actual value of a variable may be represented by JDI
   * using different reference values.
   */
  private Object notNull(final Object value)
  {
    return value == null ? NULL_HOLDER : value.toString();
  }

  private JiveStackFrame toJiveStackFrame(final StackFrame frame)
  {
    return frame instanceof JiveStackFrame ? (JiveStackFrame) frame : new JiveStackFrame(frame);
  }

  protected IContourFactory contourFactory()
  {
    return model.contourFactory();
  }

  protected IStaticModelFactory staticModelFactory()
  {
    return model.staticModelFactory();
  }
}