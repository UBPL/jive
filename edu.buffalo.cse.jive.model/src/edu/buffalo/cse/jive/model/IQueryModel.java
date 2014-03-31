package edu.buffalo.cse.jive.model;

import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;

public interface IQueryModel extends IModel
{
  /**
   * Predicate based query supporting trace event queries.
   */
  public interface EventQuery
  {
    /**
     * Must be called explicitly if the query is canceled or interrupted.
     */
    public void close();

    /**
     * The result of the last event processing, which is either {@code null} or an actual event,
     * depending on whether the event satisfied the predicate, i.e., if it was a match for the
     * predicate.
     */
    public IJiveEvent match();

    /**
     * Must be called prior to (re-)executing a query.
     */
    public void open();

    /**
     * Returns true only if one event is processed.
     */
    public boolean processEvent();
    
    /**
     * Returns true if the specified event satisfies the predicate.
     */
    public boolean satisfies(final IJiveEvent event);
  }

  public interface ExceptionQueryParams
  {
    /**
     * An optional fully-qualified class name in which the exception is being checked.
     */
    public String classText();

    /**
     * An optional fully-qualified class name specifying the exception that should be checked.
     */
    public String exceptionText();

    /**
     * An optional instance number specifying the instance in which the exception should be checked.
     */
    public String instanceText();

    /**
     * An optional method name specifying the method context in which the exception should be
     * checked.
     */
    public String methodText();
  }

  public interface InvariantViolatedQueryParams
  {
    /**
     * A fully-qualified class name in which the invariant is being checked.
     */
    public String classText();

    /**
     * An optional instance number specifying what instance of the class should be checked.
     */
    public String instanceText();

    /**
     * A variable of the class used on the left side of the operator.
     */
    public String leftVariableText();

    /**
     * An optional method number specifying what method of the class should be checked.
     */
    public String methodText();

    /**
     * A relational operator selector.
     */
    public RelationalOperator operator();

    /**
     * A relational operator selector.
     */
    public String operatorText();

    /**
     * A variable of the class used on the right side of the operator.
     */
    public String rightVariableText();
  }

  public interface LineExecutedQueryParams
  {
    /**
     * The line number of interest.
     */
    public String lineNumberText();

    /**
     * A relative path of the Java source file.
     */
    public String sourcePathText();
  }

  public interface MethodQueryParams
  {
    /**
     * A fully-qualified class name in which the method is called.
     */
    public String classText();

    /**
     * An optional instance number specifying what instance of the class should be checked.
     */
    public String instanceText();

    /**
     * A method name specifying what method on the class should be checked.
     */
    public String methodText();
  }

  public interface MethodReturnedQueryParams extends MethodQueryParams
  {
    /**
     * A relational operator selector.
     */
    public RelationalOperator operator();

    /**
     * A relational operator selector.
     */
    public String operatorText();

    /**
     * A value to be used on the right side of the operator.
     */
    public String returnValueText();
  }

  public interface ObjectCreatedQueryParams
  {
    /**
     * A fully-qualified class of the object created.
     */
    public String classText();

    /**
     * An optional instance number specifying what instance of the class should be checked.
     */
    public String instanceText();
  }

  public interface SlicingQueryParams
  {
    /**
     * A string representing the event number of an assign event in the execution trace.
     */
    public String eventText();
  }

  public interface VariableChangedQueryParams
  {
    /**
     * A fully-qualified class name in which the condition is being checked.
     */
    public String classText();

    /**
     * An optional instance number specifying what instance of the class should be checked.
     */
    public String instanceText();

    /**
     * A method name specifying in which method the variable resides.
     */
    public String methodText();

    /**
     * A relational operator selector.
     */
    public RelationalOperator operator();

    /**
     * A relational operator selector.
     */
    public String operatorText();

    /**
     * A value to be used on the right side of the operator.
     */
    public String valueText();

    /**
     * A variable (either field or local variable) that should be checked.
     */
    public String variableText();
  }
}