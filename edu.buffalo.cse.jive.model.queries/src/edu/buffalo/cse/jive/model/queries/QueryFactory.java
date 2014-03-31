package edu.buffalo.cse.jive.model.queries;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourTokens;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IEventModel.EventKind;
import edu.buffalo.cse.jive.model.IEventModel.IAssignEvent;
import edu.buffalo.cse.jive.model.IEventModel.IExceptionCatchEvent;
import edu.buffalo.cse.jive.model.IEventModel.IExceptionThrowEvent;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.ILineStepEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodExitEvent;
import edu.buffalo.cse.jive.model.IEventModel.INewObjectEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IExecutionModel.IProgramSlice;
import edu.buffalo.cse.jive.model.RelationalOperator;
import edu.buffalo.cse.jive.model.factory.IQueryFactory;

public class QueryFactory implements IQueryFactory
{
  private final IExecutionModel model;

  public QueryFactory(final IExecutionModel model)
  {
    this.model = model;
  }

  @Override
  public EventQuery createExceptionCaughtQuery(final ExceptionQueryParams params)
  {
    return new ExceptionCaughtQuery(params);
  }

  @Override
  public EventQuery createExceptionThrownQuery(final ExceptionQueryParams params)
  {
    return new ExceptionThrownQuery(params);
  }

  @Override
  public EventQuery createInvariantViolatedQuery(final InvariantViolatedQueryParams params)
  {
    return new InvariantViolatedQuery(params);
  }

  @Override
  public EventQuery createLineExecutedQuery(final LineExecutedQueryParams params)
  {
    return new LineExecutedQuery(params);
  }

  @Override
  public EventQuery createMethodCalledQuery(final MethodQueryParams params)
  {
    return new MethodCalledQuery(params);
  }

  @Override
  public EventQuery createMethodReturnedQuery(final MethodReturnedQueryParams params)
  {
    return new MethodReturnedQuery(params);
  }

  @Override
  public EventQuery createObjectCreatedQuery(final ObjectCreatedQueryParams params)
  {
    return new ObjectCreatedQuery(params);
  }

  @Override
  public EventQuery createSlicingQuery(final SlicingQueryParams params)
  {
    return new SlicingQuery(params);
  }

  @Override
  public EventQuery createVariableChangedQuery(final VariableChangedQueryParams params)
  {
    return new VariableChangedQuery(params);
  }

  @Override
  public IExecutionModel model()
  {
    return model;
  }

  /**
   * A query used to check whether an exception was caught. The query is capable of finding caught
   * exceptions of a particular type or caught by a particular class, instance, or in a certain
   * method.
   */
  private class ExceptionCaughtQuery extends ExceptionQuery
  {
    private ExceptionCaughtQuery(final ExceptionQueryParams params)
    {
      super(params);
    }

    @Override
    public boolean satisfies(final IJiveEvent event)
    {
      if (event.kind() != EventKind.EXCEPTION_CATCH)
      {
        return false;
      }
      final IExceptionCatchEvent catchEvent = (IExceptionCatchEvent) event;
      final IContourTokens tokenizer = catchEvent.contour().tokenize();
      final String className = tokenizer.className();
      final String instanceNumber = tokenizer.instanceNumber();
      final String methodName = tokenizer.methodName();
      final String exceptionName = catchEvent.exception().toString();
      return satisfies(className, instanceNumber, methodName, exceptionName);
    }
  }

  private abstract class ExceptionQuery extends JiveQuery
  {
    private ExceptionQuery(final ExceptionQueryParams params)
    {
      super(params);
    }

    @Override
    protected ExceptionQueryParams params()
    {
      return (ExceptionQueryParams) super.params();
    }

    protected boolean satisfies(final String className, final String instanceNumber,
        final String methodName, final String exceptionName)
    {
      if (rejectString(params().classText(), className))
      {
        return false;
      }
      if (rejectString(params().instanceText(), instanceNumber))
      {
        return false;
      }
      if (rejectString(params().methodText(), methodName))
      {
        return false;
      }
      if (rejectStringSuffix(params().exceptionText(), exceptionName))
      {
        return false;
      }
      return true;
    }
  }

  /**
   * A query to check whether an exception was caught. The query is capable of finding caught
   * exceptions of a particular type or caught by a particular class, instance, or in a certain
   * method.
   */
  private class ExceptionThrownQuery extends ExceptionQuery
  {
    private ExceptionThrownQuery(final ExceptionQueryParams params)
    {
      super(params);
    }

    @Override
    public boolean satisfies(final IJiveEvent event)
    {
      if (event.kind() != EventKind.EXCEPTION_THROW)
      {
        return false;
      }
      final IExceptionThrowEvent throwEvent = (IExceptionThrowEvent) event;
      if (!(throwEvent.thrower() instanceof IContourReference))
      {
        return false;
      }
      final IContour id = ((IContourReference) throwEvent.thrower()).contour();
      final IContourTokens tokenizer = id.tokenize();
      final String className = tokenizer.className();
      final String instanceNumber = tokenizer.instanceNumber();
      final String methodName = tokenizer.methodName();
      final String exceptionName = throwEvent.exception().toString();
      return satisfies(className, instanceNumber, methodName, exceptionName);
    }
  }

  private class InvariantViolatedQuery extends JiveQuery
  {
    /**
     * Assignment events satisfying the left operand criteria. This map guarantees that we keep only
     * the most current assignment of the variable.
     */
    private final Set<IAssignEvent> leftOperands = new LinkedHashSet<IAssignEvent>();
    /**
     * Assignment events satisfying the right operand criteria. This map guarantees that we keep
     * only the most current assignment of the variable.
     */
    private final Set<IAssignEvent> rightOperands = new LinkedHashSet<IAssignEvent>();

    private InvariantViolatedQuery(final InvariantViolatedQueryParams params)
    {
      super(params);
    }

    @Override
    public void close()
    {
      super.close();
      leftOperands.clear();
      rightOperands.clear();
    }

    @Override
    public boolean satisfies(final IJiveEvent event)
    {
      if (event.kind() == EventKind.FIELD_WRITE)
      {
        final IAssignEvent assignEvent = (IAssignEvent) event;
        final IContourTokens tokenizer = assignEvent.contour().tokenize();
        // not the class, instance, method, or variables we are looking for
        if (rejectEvent(tokenizer, assignEvent))
        {
          return false;
        }
        // checking for a violation
        boolean violation = false;
        /**
         * If the invariant query involves the same variable name on both the lhs and rhs, we do not
         * add the variable to the left operands. Note that, in this case, we are effectively
         * checking a temporal condition, i.e.: n < n ==> n(t) < n(t-1). The latest value of the
         * variable is in the event and the relevant operands are only the right operands.
         */
        if (params().leftVariableText().equalsIgnoreCase(params().rightVariableText()))
        {
          // if this satisfies the left operand it also satisfies the right
          if (!rejectLeftOperand(tokenizer, assignEvent))
          {
            // check for the violation using old right operand values
            violation = checkLeftViolation(assignEvent, rightOperands);
            // add the event to the right operands so that later violations can be caught
            rightOperands.add(assignEvent);
          }
        }
        /**
         * If the invariant query involves two variable names, then we check the violation in one
         * direction-- the assign event may satisfy the left or right variable, but not both.
         */
        else
        {
          if (!rejectLeftOperand(tokenizer, assignEvent))
          {
            violation = checkLeftViolation(assignEvent, rightOperands);
            // add the event to the left operands so that later violations can be caught
            leftOperands.add(assignEvent);
          }
          else if (!rejectRightOperand(tokenizer, assignEvent))
          {
            violation = checkRightViolation(assignEvent, leftOperands);
            // add the event to the right operands so that later violations can be caught
            rightOperands.add(assignEvent);
          }
        }
        return violation;
      }
      return false;
    }

    /**
     * Local assignments are not visible beyond their scopes. An older variable assignment can only
     * trigger an invariant violation if the event against which it is checked happens in the same
     * execution context.
     */
    private boolean checkLeftViolation(final IAssignEvent event,
        final Collection<IAssignEvent> operands)
    {
      for (final IAssignEvent operand : operands)
      {
        // if there is a violation, short-circuit the evaluation
        if (event != operand && checkViolation(event, operand))
        {
          return true;
        }
      }
      // no violation is expensive
      return false;
    }

    /**
     * Local assignments are not visible beyond their scopes. An older variable assignment can only
     * trigger an invariant violation if the event against which it is checked happens in the same
     * execution context.
     */
    private boolean checkRightViolation(final IAssignEvent event,
        final Collection<IAssignEvent> operands)
    {
      for (final IAssignEvent operand : operands)
      {
        // if there is a violation, short-circuit the evaluation
        if (event != operand && checkViolation(operand, event))
        {
          return true;
        }
      }
      // no violation is expensive
      return false;
    }

    private boolean checkViolation(final IAssignEvent left, final IAssignEvent right)
    {
      // // some event pairs must happen in the same context
      // if (sameContext && !left.contourId().equals(right.contourId())) {
      // return false;
      // }
      // return true if there is a violation
      return !params().operator().evaluate(left.newValue(), right.newValue());
    }

    // private String key(final ContourIdTokenizer parser, final AssignEvent event) {
    //
    // String key = "";
    // if (params().classText().length() != 0) {
    // key += (parser.className() == null ? "" : parser.className());
    // }
    // if (params().instanceText().length() != 0) {
    // key += (parser.instanceNumber() == null ? "" : ":" + parser.instanceNumber());
    // }
    // if (params().methodText().length() != 0) {
    // key += (parser.methodName() == null ? "" : "#" + parser.methodName());
    // }
    // key += "." + event.variableId().toString();
    // return key;
    // }
    private boolean rejectEvent(final IContourTokens tokenizer, final IAssignEvent event)
    {
      if (rejectString(params().classText(), tokenizer.className()))
      {
        return true;
      }
      if (rejectString(params().instanceText(), tokenizer.instanceNumber()))
      {
        return true;
      }
      if (rejectString(params().methodText(), tokenizer.methodName()))
      {
        return true;
      }
      if (rejectLeftOperand(tokenizer, event) && rejectRightOperand(tokenizer, event))
      {
        return true;
      }
      return false;
    }

    private boolean rejectLeftOperand(final IContourTokens tokenizer, final IAssignEvent event)
    {
      if (rejectString(params().leftVariableText(), event.member().schema().name()))
      {
        return true;
      }
      return false;
    }

    private boolean rejectRightOperand(final IContourTokens tokenizer, final IAssignEvent event)
    {
      if (rejectString(params().rightVariableText(), event.member().schema().name()))
      {
        return true;
      }
      return false;
    }

    // @Override
    // public boolean satisfies(final DataEvent event) {
    //
    // if (event.kind() == EventKind.FIELD_ASSIGN || event.kind() == EventKind.VAR_ASSIGN) {
    // final AssignEvent assignEvent = (AssignEvent) event;
    // final ContourIdTokenizer parser = assignEvent.contourId().tokenize();
    // // not the class, instance, or variables we are looking for
    // if (rejectEvent(parser, assignEvent)) {
    // return false;
    // }
    // // checking for a violation
    // boolean violation = false;
    // /**
    // * If the invariant query involves the same variable name on both the lhs and rhs, we do not
    // * add the variable to the left operands. Note that, in this case, we are effectively
    // * checking a temporal condition, i.e.: n < n ==> n(t) < n(t-1). The latest value of the
    // * variable is in the event and the relevant operands are only the right operands.
    // */
    // if (params().leftVariableText().equalsIgnoreCase(params().rightVariableText())) {
    // // if this satisfies the left operand it also satisfies the right
    // if (!rejectLeftOperand(parser, assignEvent)) {
    // // check the violation if the method is not rejected
    // if (!rejectString(params().methodText(), parser.methodName())) {
    // // check for the violation using old right operand values
    // violation = checkLeftViolation(assignEvent, rightOperands.values());
    // }
    // // add the event to the right operands so that later violations can be caught
    // rightOperands.put(key(parser, assignEvent), assignEvent);
    // }
    // }
    // /**
    // * If the invariant query involves two variable names, then we check the violation in one
    // * direction-- the assign event may satisfy the left or right variable, but not both.
    // */
    // else {
    // if (!rejectLeftOperand(parser, assignEvent)) {
    // // check the violation if the method is not rejected
    // if (!rejectString(params().methodText(), parser.methodName())) {
    // violation = checkLeftViolation(assignEvent, rightOperands.values());
    // }
    // // add the event to the left operands so that later violations can be caught
    // leftOperands.put(key(parser, assignEvent), assignEvent);
    // }
    // else if (!rejectRightOperand(parser, assignEvent)) {
    // // check the violation if the method is not rejected
    // if (!rejectString(params().methodText(), parser.methodName())) {
    // violation = checkRightViolation(assignEvent, leftOperands.values());
    // }
    // // add the event to the right operands so that later violations can be caught
    // rightOperands.put(key(parser, assignEvent), assignEvent);
    // }
    // }
    // return violation;
    // }
    // return false;
    // }
    @Override
    protected InvariantViolatedQueryParams params()
    {
      return (InvariantViolatedQueryParams) super.params();
    }
  }

  /**
   * Common ancestor for all event predicate queries. It lays out the template for query evaluation
   * as follows:
   * 
   * <pre>
   * {@code
   * // create the query
   * query = new ...
   * // open the query
   * query.open()
   * try {
   *   // process the next event
   *   while (query.processEvent()) {
   *     // check if the event satisfied the predicate
   *     if (query.current() != null) {
   *       ...
   *     }
   *   }
   * }
   * finally {
   *   // release the query
   *   query.close();
   * }
   * </pre>
   */
  private abstract class JiveQuery implements EventQuery
  {
    private Iterator<? extends IJiveEvent> iterator;
    private IJiveEvent match;
    private final Object params;

    private JiveQuery(final Object params)
    {
      this.params = params;
    }

    @Override
    public void close()
    {
      if (iterator != null)
      {
        match = null;
        iterator = null;
        model.readUnlock();
      }
    }

    /**
     * Returns the iterator over the trace events for this query.
     */
    protected Iterator<? extends IJiveEvent> createIterator()
    {
      return model.traceView().events().iterator();
    }

    @Override
    public IJiveEvent match()
    {
      return this.match;
    }

    @Override
    public void open()
    {
      if (iterator == null)
      {
        model.readLock();
        iterator = createIterator();
      }
    }

    @Override
    public boolean processEvent()
    {
      if (iterator != null)
      {
        final boolean result = iterator.hasNext();
        if (!result)
        {
          close();
          return false;
        }
        final IJiveEvent event = iterator.next();
        match = satisfies(event) ? event : null;
        return true;
      }
      return false;
    }

    protected Object params()
    {
      return this.params;
    }

    /**
     * Rejects only if the form string is a non-null, non-empty string which is not equal to the
     * event string.
     */
    protected boolean rejectString(final String formString, final String eventString)
    {
      return formString.length() != 0 && !formString.equals(eventString == null ? "" : eventString);
    }

    /**
     * Rejects only if the form string is a non-null, non-empty string which is not equal to a
     * suffix of the event string.
     */
    protected boolean rejectStringSuffix(final String formString, final String eventString)
    {
      return formString.length() != 0
          && !(eventString == null ? "" : eventString).endsWith(formString);
    }
  }

  /**
   * A query to check whether a line is executed. A relative path of a Java source file and a line
   * number are required as input. The query returns a match for each time the line is executed.
   */
  private class LineExecutedQuery extends JiveQuery
  {
    private LineExecutedQuery(final LineExecutedQueryParams params)
    {
      super(params);
    }

    @Override
    public boolean satisfies(final IJiveEvent event)
    {
      if (event.kind() != EventKind.LINE_STEP)
      {
        return false;
      }
      final ILineStepEvent stepEvent = (ILineStepEvent) event;
      if (rejectString(params().sourcePathText(), stepEvent.line().file().name()))
      {
        return false;
      }
      if (rejectString(params().lineNumberText(), String.valueOf(stepEvent.line().lineNumber())))
      {
        return false;
      }
      return true;
    }

    @Override
    protected LineExecutedQueryParams params()
    {
      return (LineExecutedQueryParams) super.params();
    }
  }

  /**
   * A query to check whether a method was called. The query is capable of checking for method calls
   * on a single instance or over all instances of a class (if an instance number is not provided).
   */
  private class MethodCalledQuery extends MethodQuery
  {
    private MethodCalledQuery(final MethodQueryParams params)
    {
      super(params);
    }

    @Override
    public boolean satisfies(final IJiveEvent event)
    {
      if (event.kind() != EventKind.METHOD_CALL)
      {
        return false;
      }
      final IMethodCallEvent callEvent = (IMethodCallEvent) event;
      if (!callEvent.inModel())
      {
        return false;
      }
      final IContour id = ((IMethodContourReference) callEvent.target()).contour();
      final IContourTokens tokenizer = id.tokenize();
      final String className = tokenizer.className();
      final String instanceNumber = tokenizer.instanceNumber();
      final String methodName = tokenizer.methodName();
      return satisfies(className, instanceNumber, methodName);
    }
  }

  /**
   * Common ancestor for method predicate queries.
   */
  private abstract class MethodQuery extends JiveQuery
  {
    private MethodQuery(final MethodQueryParams params)
    {
      super(params);
    }

    @Override
    protected MethodQueryParams params()
    {
      return (MethodQueryParams) super.params();
    }

    protected boolean satisfies(final String className, final String instanceNumber,
        final String methodName)
    {
      if (rejectString(params().classText(), className))
      {
        return false;
      }
      if (rejectString(params().instanceText(), instanceNumber))
      {
        return false;
      }
      if (rejectString(params().methodText(), methodName))
      {
        return false;
      }
      return true;
    }
  }

  /**
   * A query to check method returns and optionally conditions on return values. The query is
   * capable of checking for method returns on a single instance or over all instances of a class
   * (if an instance number is not provided).
   */
  private class MethodReturnedQuery extends MethodQuery
  {
    private MethodReturnedQuery(final MethodReturnedQueryParams params)
    {
      super(params);
    }

    @Override
    public boolean satisfies(final IJiveEvent event)
    {
      if (event.kind() != EventKind.METHOD_EXIT)
      {
        return false;
      }
      final IMethodExitEvent returnEvent = (IMethodExitEvent) event;
      if (!returnEvent.parent().inModel())
      {
        return false;
      }
      final IMethodContour id = returnEvent.parent().execution();
      final IContourTokens tokenizer = id.tokenize();
      final String className = tokenizer.className();
      final String instanceNumber = tokenizer.instanceNumber();
      final String methodName = tokenizer.methodName();
      if (!satisfies(className, instanceNumber, methodName))
      {
        return false;
      }
      if (params().operator() != RelationalOperator.NONE
          && !params().operator().evaluate(returnEvent.returnValue(), params().returnValueText()))
      {
        return false;
      }
      return true;
    }

    @Override
    protected MethodReturnedQueryParams params()
    {
      return (MethodReturnedQueryParams) super.params();
    }
  }

  /**
   * A query to search for object creations. The query is capable of searching for a single instance
   * creation or for instance creations of a class (if an instance number is not provided).
   */
  private class ObjectCreatedQuery extends JiveQuery
  {
    private ObjectCreatedQuery(final ObjectCreatedQueryParams params)
    {
      super(params);
    }

    @Override
    public boolean satisfies(final IJiveEvent event)
    {
      if (event.kind() != EventKind.OBJECT_NEW)
      {
        return false;
      }
      final INewObjectEvent newEvent = (INewObjectEvent) event;
      IContour newContour = newEvent.newContour();
      while (newContour != null)
      {
        final IContourTokens tokenizer = newContour.tokenize();
        if (params().classText().equals(tokenizer.className()))
        {
          if (!rejectString(params().instanceText(), tokenizer.instanceNumber()))
          {
            return true;
          }
        }
        newContour = newContour.parent();
      }
      return false;
    }

    @Override
    protected ObjectCreatedQueryParams params()
    {
      return (ObjectCreatedQueryParams) super.params();
    }
  }

  private class SlicingQuery extends JiveQuery
  {
    private SlicingQuery(final SlicingQueryParams params)
    {
      super(params);
    }

    @Override
    public Iterator<IJiveEvent> createIterator()
    {
      final IProgramSlice slice = model.sliceView().computeSlice(
          Integer.parseInt(params().eventText()));
      return slice.events().iterator();
    }

    @Override
    protected SlicingQueryParams params()
    {
      return (SlicingQueryParams) super.params();
    }

    @Override
    public boolean satisfies(final IJiveEvent event)
    {
      return true;
    }
  }

  /**
   * A query to check where a variable has changed and also when a condition on the new value holds.
   * The query is capable of checking for variable changes on a single instance or over all
   * instances of a class (if an instance number is not provided).
   * 
   * TODO: (demian) Method names are always null because only assign events are used by this method.
   * We need to figure how to enable queries by method name.
   */
  private class VariableChangedQuery extends JiveQuery
  {
    private VariableChangedQuery(final VariableChangedQueryParams params)
    {
      super(params);
    }

    /**
     * TODO: reduce the cyclomatic complexity.
     */
    @Override
    public boolean satisfies(final IJiveEvent event)
    {
      if (event.kind() != EventKind.FIELD_WRITE && event.kind() != EventKind.VAR_ASSIGN)
      {
        return false;
      }
      final IAssignEvent assignEvent = (IAssignEvent) event;
      final IContourTokens tokenizer1 = assignEvent.contour().tokenize();
      final String className = tokenizer1.className();
      final String instanceNumber = tokenizer1.instanceNumber();
      final String variableName = assignEvent.member().schema().name();
      if (rejectString(params().classText(), className))
      {
        return false;
      }
      if (rejectString(params().instanceText(), instanceNumber))
      {
        return false;
      }
      if (rejectString(params().variableText(), variableName))
      {
        return false;
      }
      if (params().operator() != RelationalOperator.NONE
          && !params().operator().evaluate(assignEvent.newValue(), params().valueText()))
      {
        return false;
      }
      // if a method name is provided, it must match the event's method name
      if (params().methodText().length() > 0)
      {
        final IMethodCallEvent caller = assignEvent.parent();
        // in-model method
        if (caller.target() instanceof IMethodContourReference)
        {
          final IMethodContourReference imtv = (IMethodContourReference) caller.target();
          final IContourTokens tokenizer2 = imtv.contour().tokenize();
          final String methodName = tokenizer2.methodName();
          if (rejectString(params().methodText(), methodName))
          {
            return false;
          }
        }
        else
        {
          // reject out-of-model method
          return false;
        }
      }
      return true;
    }

    @Override
    protected VariableChangedQueryParams params()
    {
      return (VariableChangedQueryParams) super.params();
    }
  }
}