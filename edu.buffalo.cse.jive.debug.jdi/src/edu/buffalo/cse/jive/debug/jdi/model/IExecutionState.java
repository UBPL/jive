package edu.buffalo.cse.jive.debug.jdi.model;

import com.sun.jdi.Method;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.ExceptionEvent;

import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IContourModel.IObjectContour;
import edu.buffalo.cse.jive.model.IEventModel.IMethodTerminatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.ITerminatorEvent;
import edu.buffalo.cse.jive.model.IModel.ILineValue;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;

/**
 * This manager maintains the state necessary for generating Jive events. This includes model
 * aspects such as elements that have been seen, modified, etc. The class keeps both JDI model
 * information as well as a map from JDI model elements to Jive model elements.
 */
@SuppressWarnings("restriction")
public interface IExecutionState
{
  /**
   * Only method activation contours are returned.
   */
  public boolean containsContour(StackFrame frame);

  public boolean containsException(long threadId);

  public boolean containsInModelFrame(StackFrame frame);

  public boolean containsThread(ThreadReference thread);

  /**
   * Checks whether the top frame in this thread has a corresponding contour Id.
   */
  public boolean containsTopFrameContour(long threadId);

  public ILineValue currentLine(IThreadValue threadId);

  public IObjectContour deleteObject(long oid);

  public int frameCount(long threadId);

  public StackFrame framePeek(long threadId);

  public StackFrame framePeekInModel(long threadId);

  public StackFrame framePop(long threadId);

  public StackFrame framePush(long threadId, StackFrame frame, boolean isInModel);

  public ITerminatorEvent getPendingMethodReturned(long threadId);

  public boolean hasFrames(long threadId);

  public boolean hasPendingMethodReturned(long threadId);

  public IThreadSummary inspectThread(ThreadReference thread);

  /**
   * Determines if the current line has an array modification using the given local variable.
   */
  public boolean isArrayCellWriteLine(IContourMember var, ThreadReference thread, boolean checkDef);

  /**
   * Determines if the current line has some array modification using a field.
   */
  public boolean isArrayCellWriteLine(IMethodContour method, IContourMember field,
      ThreadReference thread);

  public IObjectContour lookupArrayResult(ThreadReference thread);

  /**
   * Only method activation contours are returned.
   */
  public IMethodContour lookupContour(StackFrame frame);

  public ExceptionEvent lookupException(long threadId);

  public IContextContour lookupObject(long oid);

  public ILineValue lookupSyntheticAccessorLine(String methodKey);

  public String methodKey(Method method);

  public String methodName(Method method);

  /**
   * A method returned pending is recorded for every method exit event that exists to an in-model
   * caller. The first event in the frame corresponding to the caller of the terminated method is
   * responsible for processing this method.
   */
  public void methodReturnedPending(long threadId, final IMethodTerminatorEvent terminator);

  public IMethodTerminatorEvent methodReturnedProcessed(long threadId);

  /**
   * Records the next current line for the given thread and returns the previous current line.
   */
  public ILineValue nextLine(IThreadValue threadId, ILineValue line);

  public void observedArrayResult(ThreadReference thread, IObjectContour oc);

  public void observedContour(IObjectContour contour, long oid);

  public boolean observedException(long threadId, ExceptionEvent event);

  public boolean observedMethod(StackFrame frame, IMethodContour contour);

  public boolean observedSyntheticAccessor(String methodKey, ILineValue line);

  public void observedThread(IThreadValue thread);

  /**
   * Returns true if this variable was not previously observed or its value has changed since last
   * observation.
   */
  public boolean observedVariable(IContourMember var, Value value, ThreadReference thread, int lineNumber);

  // public Set<IContextContour> reachableObjects();
  public void removeArguments(IMethodContour method);

  public void removeArrayResult(ThreadReference thread);

  public boolean removeException(long threadId);

  public ILineValue removeSyntheticAccessor(String methodKey);

  public boolean removeVariable(IContourMember var, final ThreadReference thread);

  public void reset();

  public IContextContour retrieveContext(StackFrame frame);

  public IMethodContour retrieveContour(StackFrame frame);

  public IMethodContour retrieveTopContour(long threadId);

  /**
   * Returns true if the arguments for the method have already been visited.
   */
  public boolean visitedArguments(IMethodContour method);
}