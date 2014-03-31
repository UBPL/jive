package edu.buffalo.cse.jive.model.slicing;

import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_CATCH;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_METHOD_DECLARATION;
import static edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind.LK_RETURN_IN_TRY;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IEventModel.IAssignEvent;
import edu.buffalo.cse.jive.model.IEventModel.IExceptionThrowEvent;
import edu.buffalo.cse.jive.model.IEventModel.IFieldAssignEvent;
import edu.buffalo.cse.jive.model.IEventModel.IFieldReadEvent;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.ILineStepEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodReturnedEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodTerminatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IVarAssignEvent;
import edu.buffalo.cse.jive.model.IEventModel.IVarDeleteEvent;
import edu.buffalo.cse.jive.model.IModel.IContourReference;
import edu.buffalo.cse.jive.model.IModel.IMethodContourReference;
import edu.buffalo.cse.jive.model.IModel.IOutOfModelMethodKeyReference;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IMethodDependenceGraph;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedCall;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedData;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedLine;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedNode;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedThis;
import edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind;
import edu.buffalo.cse.jive.model.IStaticModel.IDataNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;

/**
 * Maintains a single resolved line for a method slice. As trace events are passed to the line
 * resolver for processing, the resolved line is automatically updated to reference the line of the
 * most recently processed event. Upon processing each trace event, the resolved returns a resolved
 * node, if the trace event is associated to such an event.
 * 
 * TODO: reduce the cyclomatic complexity across and nested block depth.
 */
final class LineSlice
{
  /**
   * Marker object to flag nodes that are argument irrelevant.
   */
  private static final Object NODE_ARG_IRRELEVANT = new Object()
    {
      @Override
      public String toString()
      {
        return "<irrelevant argument>";
      }
    };
  private static final IResolvedNode NODE_RESULT = new IResolvedNode()
    {
      @Override
      public int compareTo(final IResolvedNode other)
      {
        return other == this ? 0 : -1;
      }

      @Override
      public long id()
      {
        return -1;
      }

      @Override
      public boolean isActual()
      {
        return false;
      }

      @Override
      public boolean isLHS()
      {
        return false;
      }

      @Override
      public IResolvedNode qualifierOf()
      {
        return null;
      }

      @Override
      public int sourceIndex()
      {
        return -1;
      }

      @Override
      public String toString()
      {
        return "<result node>";
      }
    };
  /**
   * List that indicates which resolved nodes are relevant to this line's assignment.
   */
  private final List<Boolean> defRelevant;
  private BitSet hasToString;
  /**
   * A line is catch relevant if it is the first line executed prior to control flow being
   * transferred to a catch clause. In other words, it is the line that causes the control flow to
   * be eventually transferred to the catch block and where the exception is effectively (although
   * possibly indirectly) thrown.
   */
  private boolean isCatchRelevant;
  /**
   * A line is def relevant if it modifies a local variable that is in the method slice's chase
   * variable set or a field that is in the program slice's chase field set.
   */
  private boolean isDefRelevant;
  /**
   * A line is entry point relevant when it is the first in a method slice which has been added to
   * the computation by virtue of being a caller method of a relevant slice, by being the point of
   * an exception throw or catch, or by being a relevant constructor or class initializer.
   * 
   * Some of these method slices are incomplete in the sense that their computation starts at any
   * point in the trace, not from the method terminator event. The consequence is that the trace may
   * be in a position corresponding to the middle of a source statement. Regardless, all data events
   * are ignored, as they cannot affect the slice (if the line is a def, the def itself has already
   * happened; no reads can affect the slice).
   */
  private boolean isEntryPoint;
  private boolean isFirstEntryPointEvent;
  /**
   * This flag determines whether static nodes are mapped to the run-time nodes flexibly. This is
   * necessary in order to deal with a number of static-to-dynamic issues. Three are most important:
   * 
   * <pre>
   *   a) When a slice completes, it returns to a caller slice. The statement in the caller slice
   *      may not have been fully processed by the slicing algorithm, for instance, because the
   *      assignment serving as slicing criterion is made on a method called on this line. Thus,
   *      not all static nodes will be resolved in this statement. Example:
   *      
   *      F = G + foo(X,Y,Z) + H + bar(V,W) + J;
   *      
   *      If the assignment serving for the slicing criterion is in the execution of foo, then
   *      this statement should not process H, V, W, J, or bar.
   *      
   *   b) Boolean expressions in general may "confuse" the static-to-dynamic mapping due to short
   *      circuiting. For example, when a statement corresponds to a control predicate, a short
   *      circuit may cause part of the static nodes in the line not to have a dynamic counterpart. 
   *      Example:
   *      
   *      if ((G || foo(X,Y,Z)) && H && (bar(V,W) || J)) { ... }
   *      
   *      In the line above, if G is true, foo is not called; if G is false and foo returns false,
   *      H and J are never read, bar is never called. And so on.
   *      
   *   c) When the statement corresponds to a conditional expression, only one of the branches of
   *      the conditional are ever executed. However, the static analysis will typically associate
   *      all nodes to the same line (because conditionals are often written as one expression on 
   *      a single line). Thus, it is not possible to tell whether the node being resolved is a
   *      if-branch node or an else-branch node. Additionally, the conditional expression itself
   *      is a boolean expression and also suffers from the problems described in (b). Example:
   *      
   *      F = X > Y ? G + foo(X,Y,Z) : H + bar(V,W) + J;
   * </pre>
   */
  private boolean isFlexibleRHS;
  /**
   * A line is field def relevant if it is def relevant and either the LHS references a field in the
   * program's chase field set or the RHS of the assignment uses a field in the program's field.
   * Additionally, if the line executes a method that is field def relevant, then it is also field
   * def * relevant.
   */
  private boolean isFieldDef;
  /**
   * Indicates whether this line contains an assignment involving the LHS variable implicitly, e.g.,
   * X += 1;
   */
  private boolean isSelfWrite;
  /**
   * A local variable assignment, used to track the assignment to a catch variable on the same line.
   */
  private IVarAssignEvent lastVarAssign;
  /**
   * Line cursor identifies the resolved source line being currently processed. Multiple dynamic
   * lines may map to the same static line, such as in the case where a statement spans multiple
   * source lines.
   */
  private IResolvedLine line;
  /**
   * Method slice parent of this resolver.
   */
  private final MethodSlice parent;
  /**
   * Whenever a field write event is processed, and it's part of a line containing a self write, we
   * expect a field read without a resolution argument next.
   */
  private boolean pendingSelfWriteFieldRead;
  /**
   * Out-of-model calls are relevant if the line is def relevant. When this happens, their qualifier
   * fields and/or actual arguments might be relevant.
   */
  final Set<IResolvedNode> relevantToOutOfModel;
  /**
   * Source lines currently relevant to the slice. As each line in the set is visited, it is removed
   * from the set.
   */
  private final BitSet relevantLines;
  /**
   * Ordered list in which the statically resolved nodes must be resolved dynamically.
   */
  private final List<IResolvedNode> resolutionList;
  /**
   * Ordered list in which the special formal : actual variable writes must be resolved dynamically.
   */
  private final List<IResolvedNode> resolutionArgs;
  /**
   * Mapping of node resolution positions to dynamic node resolutions.
   */
  private final Map<Integer, Object> resolutionMap;
  /**
   * Current position into the resolver.
   */
  private Integer resolutionCursor;
  /**
   * When a catch line is processed with a relevant completed method, the completed method slice
   * processing is delayed to its call site.
   */
  private MethodSlice catchCompleted;

  LineSlice(final MethodSlice parent)
  {
    this.defRelevant = TypeTools.newArrayList();
    this.hasToString = new BitSet();
    this.isCatchRelevant = false;
    this.isDefRelevant = false;
    this.isEntryPoint = false;
    this.isFirstEntryPointEvent = false;
    this.isFlexibleRHS = false;
    this.isFieldDef = false;
    // this.isLastEventArrayCellWrite = false;
    this.isSelfWrite = false;
    this.lastVarAssign = null;
    // this.lastEvent = null;
    this.line = null;
    this.parent = parent;
    this.pendingSelfWriteFieldRead = false;
    this.relevantLines = new BitSet();
    this.relevantToOutOfModel = TypeTools.newHashSet();
    this.resolutionCursor = -1;
    this.resolutionList = TypeTools.newArrayList();
    this.resolutionArgs = TypeTools.newArrayList();
    this.resolutionMap = TypeTools.newHashMap();
  }

  /**
   * Checks that the current resolution cursor matches the call and the call has been determined to
   * be relevant.
   */
  public boolean isArgOutRelevant(final IMethodCallEvent call)
  {
    if (resolutionCursor >= 0 && resolutionCursor < resolutionList.size()
        && resolutionList.get(resolutionCursor) instanceof IResolvedCall
        && defRelevant.get(resolutionCursor) && call.target().isContourReference()
    /**
     * && resolutionList.get( resolutionCursor ).isActual()
     */
    )
    {
      final IResolvedCall rc = (IResolvedCall) resolutionList.get(resolutionCursor);
      final IMethodContourReference target = (IMethodContourReference) call.target();
      final IMethodNode method = target.contour().schema();
      return method == rc.call().node()
          || call
              .model()
              .staticAnalysisFactory()
              .overrides(target.contour().parent().concreteContour().schema(),
                  target.contour().schema(), rc);
    }
    return false;
  }

  @Override
  public String toString()
  {
    final StringBuffer buffer = new StringBuffer("");
    buffer.append("\n").append(line).append("\n");
    buffer.append("  isCatchLine.........: ").append(isCatchLine()).append("\n");
    buffer.append("  isCatchRelevant.....: ").append(isCatchRelevant).append("\n");
    buffer.append("  isDefRelevant.......: ").append(isDefRelevant).append("\n");
    buffer.append("  isEntryPoint........: ").append(isEntryPoint).append("\n");
    buffer.append("  isFieldDef..........: ").append(isFieldDef).append("\n");
    buffer.append("  isFirstEntryPoint...: ").append(isFirstEntryPointEvent).append("\n");
    buffer.append("  isFlexible..........: ").append(isFlexibleRHS).append("\n");
    buffer.append("  isSelfWrite.........: ").append(isSelfWrite).append("\n");
    buffer.append("  relevant lines......: ").append(relevantLines.toString()).append("\n");
    buffer.append("  def relevant map....: ").append(defRelevant.toString()).append("\n");
    buffer.append("  pending field read..: ").append(pendingSelfWriteFieldRead).append("\n");
    buffer.append("  resolution cursor...: ").append(resolutionCursor).append("\n");
    buffer.append("  resolution list.....: ").append(resolutionList.toString()).append("\n");
    buffer.append("  resolution args.....: ").append(resolutionArgs).append("\n");
    return buffer.toString();
  }

  private void check(final String message, final IJiveEvent event)
  {
    if (message != null)
    {
      throw new IllegalArgumentException(message + "\nat event:\n" + event.toString()
          + "\nat line:\n" + this.toString() + "\nat method:\n" + parent.method().toString());
    }
  }

  /**
   * Returns the resolved node at the current cursor position.
   */
  private IResolvedNode currentResolvedNode()
  {
    // find the key to resolve
    final IResolvedNode key = resolutionList.get(resolutionCursor);
    if (key == null)
    {
      throw new IllegalArgumentException("Invalid node resolution: null.");
    }
    // return the node to be resolved dynamically
    return key;
  }

  private IResolvedNode currentResolvedNodeArg()
  {
    return resolutionArgs.remove(resolutionArgs.size() - 1);
  }

  private boolean isOutOfModelCall(final IResolvedNode key, final IMethodCallEvent event,
      final boolean process)
  {
    if (key instanceof IResolvedCall)
    {
      final IResolvedCall rcall = (IResolvedCall) key;
      // the key/event pair is a match
      if (event != null && validateMethodCall(key, event) == null)
      {
        return false;
      }
      // method node reference is not associated with an actual method node
      if (rcall.call().node() == null)
      {
        // System.err.println("OUT_OF_MODEL_METHOD::NO_NODE[" + key + "]");
        if (process)
        {
          processOutOfModelCall(rcall);
        }
        return true;
      }
      // method node reference is associated with a method declaration (i.e., Interface method)
      // and the event is not a method call
      if (rcall.call().node().parent().kind() == NodeKind.NK_INTERFACE && event == null)
      {
        // either the current event is not a method call or the method call does not match the
        // interface method
        // System.err.println("POTENTIAL_OUT_OF_MODEL_METHOD::INTERFACE_METHOD[" + key + "]");
        if (process)
        {
          processOutOfModelCall(rcall);
        }
        return true;
      }
    }
    return false;
  }

  // spurious field read on the LHS of an array cell assignment
  private boolean isSpuriousFieldReadOnArrayCellAssignment(final IFieldReadEvent event)
  {
    if (line.kind() == LineKind.LK_ASSIGNMENT_ARRAY_CELL && resolutionCursor == -1
        && resolutionList.get(0) instanceof IResolvedData
        && ((IResolvedData) resolutionList.get(0)).isDef()
        && event.member().schema().type().node().kind() == NodeKind.NK_ARRAY
        && ((IResolvedData) resolutionList.get(0)).data().type().node().kind() == NodeKind.NK_ARRAY)
    {
      // System.err.println("SPURIOUS_FIELD_READ_ON_ARRAY_CELL_ASSIGNMENT");
      return true;
    }
    return false;
  }

  // spurious field write on the LHS of an array cell assignment
  private boolean isSpuriousFieldWriteOnArrayCellAssignment(final IFieldAssignEvent event)
  {
    if (line.definitions().size() > 0
        && resolutionCursor == -1
        && resolutionList.get(0) instanceof IResolvedData
        && ((IResolvedData) resolutionList.get(0)).isDef()
        && (line.kind() == LineKind.LK_ASSIGNMENT
            || line.kind() == LineKind.LK_VARIABLE_DECLARATION || line.kind() == LineKind.LK_FIELD_DECLARATION)
        && event.contour().schema().node().kind() == NodeKind.NK_ARRAY
        && ((IResolvedData) resolutionList.get(0)).data().type().node().kind() == NodeKind.NK_ARRAY)
    {
      System.err.println("SPURIOUS_FIELD_WRITE_ON_ARRAY");
      return true;
    }
    return false;
  }

  /**
   * Called when the source line being processed by the slicer changes, that is, when the first
   * event on the new line is detected by the slicer. The line cursor points to the new line, which
   * is guaranteed to be non-null.
   */
  private void lineChanged(final IJiveEvent event, final IResolvedLine oldLine)
  {
    // all types of relevance are cleared
    isDefRelevant = false;
    isFlexibleRHS = false;
    isFieldDef = false;
    // isLastEventArrayCellWrite = false;
    isSelfWrite = false;
    pendingSelfWriteFieldRead = false;
    // all resolved elements for the previous line are cleared
    defRelevant.clear();
    hasToString.clear();
    relevantToOutOfModel.clear();
    resolutionArgs.clear();
    resolutionList.clear();
    resolutionMap.clear();
    resolutionCursor = -1;
    // no variable assignments detected on this line yet
    lastVarAssign = null;
    // no line to process
    if (line == null)
    {
      return;
    }
    isFlexibleRHS = isEntryPoint || line.isControl() || line.hasConditional()
        || (catchCompleted != null && oldLine != null && oldLine.kind() == LK_CATCH);
    // a catch relevant line or relevant entry point or an exception relevant parent
    if (isCatchRelevant || (isFirstEntryPointEvent && parent.isExceptionRelevant()))
    {
      relevantLines.set(line.lineNumber());
    }
    // this line returns a value
    final boolean hasResult = line.kind().isReturn()
        && parent.method().lookupResultMember() != null;
    // a line returning a value has an implicit variable definition (but no resolved node in keys)
    if (hasResult)
    {
      resolutionList.add(LineSlice.NODE_RESULT);
      defRelevant.add(false);
    }
    // keys for the resolved nodes of the new line
    final List<IResolvedNode> defs = TypeTools.newArrayList();
    // all resolved argument (method declaration) definitions
    if (line.kind() == LK_METHOD_DECLARATION && !parent.isClinit())
    {
      resolutionArgs.addAll(parent.methodDeclLine().definitions());
    }
    // all resolved assignments
    defs.addAll(line.definitions());
    IResolvedNode defNode = null;
    boolean hasAssignment = false;
    // only definitions at this point
    for (int index = defs.size() - 1; index >= 0; index--)
    {
      defNode = defs.get(index);
      resolutionList.add(defNode);
      defRelevant.add(false);
      hasAssignment = true;
      // if (defNode instanceof IResolvedData && ((IResolvedData) defNode).data() != null
      // && ((IResolvedData) defNode).data().type().node() != null
      // && ((IResolvedData) defNode).data().type().node().kind() == NodeKind.NK_ARRAY)
      // {
      // resolutionList.add(defNode);
      // }
    }
    // process uses separately since they are given in source order
    final List<IResolvedNode> uses = TypeTools.newArrayList();
    // add all resolved data uses (at any nesting level) in source order
    uses.addAll(line.uses());
    // index the nodes in the order in which they will be resolved using the trace
    for (int index = uses.size() - 1; index >= 0; index--)
    {
      final IResolvedNode use = uses.get(index);
      if (defNode != null && use instanceof IResolvedData && ((IResolvedData) use).data() != null
          && defNode.equals(use))
      {
        isSelfWrite = true;
        continue;
      }
      else
      {
        resolutionList.add(use);
      }
      if (use instanceof IResolvedCall
          && ((IResolvedCall) use).call().key().equals("Ljava/lang/Object;.toString()"))
      {
        hasToString.set(index);
      }
      /**
       * Mark this line as flexible if we find a potential out-of-model call in the line.
       */
      if (isOutOfModelCall(use, null, false))
      {
        isFlexibleRHS = true;
        final IResolvedCall call = (IResolvedCall) use;
        for (int i = 0; i < call.size(); i++)
        {
          // conservative assumption: fields/variables in any argument position of an out-of-model
          // call are relevant
          for (final IResolvedNode rv : call.uses(i))
          {
            if (rv instanceof IResolvedData)
            {
              relevantToOutOfModel.add(rv);
            }
          }
        }
      }
      if (use instanceof IResolvedData)
      {
        final IResolvedData node = (IResolvedData) use;
        if (node instanceof IResolvedThis
            || (!node.isDef() && !node.isLHS() && node.data() != null && node.data().kind() == NodeKind.NK_VARIABLE))
        {
          isFlexibleRHS = true;
        }
        // qualifier of a potential out-of-model call
        if (isOutOfModelCall(node.qualifierOf(), null, false))
        {
          relevantToOutOfModel.add(node);
        }
        // LHS of an assignment is not def relevant (should only happen when isSelfWrite is true)
        if (node.isDef())
        {
          defRelevant.add(false);
          System.err.println("Unexpected use: node is a def! " + node.toString());
          continue;
        }
        // an array field read is not def relevant
        else if (node.data() != null && node.data().kind() == NodeKind.NK_FIELD
            && node.data().parent().kind() == NodeKind.NK_ARRAY)
        {
          defRelevant.add(false);
          continue;
        }
      }
      // relevant if the node is a 'root' for an assignment or result expression; that is, it's NOT
      // on the LHS and NOT in an argument position of the RHS; these are inferred as we process the
      // line using the events
      defRelevant.add((hasAssignment || hasResult) && !use.isActual() && !use.isLHS()
          && use.qualifierOf() == null);
    }
    // initialize the line resolution cursor
    nextResolutionPosition();
    // process a pending exception throwing method
    if (catchCompleted != null && oldLine != null && oldLine.kind() == LK_CATCH)
    {
      final IResolvedNode node = resolveNode((IMethodCallEvent) catchCompleted.initialEvent()
          .parent());
      updateQualifiers(node);
      catchCompleted = null;
    }
  }

  /**
   * Method dependence graph to use as static look up for this slice.
   */
  private final IMethodDependenceGraph mdg()
  {
    return parent.method() == null ? null : parent.method().schema().getDependenceGraph();
  }

  private void nextResolutionPosition()
  {
    // update to the next position
    resolutionCursor++;
    // resolved reads
    processStatic(false);
  }

  private boolean preprocessCallCompleted(final IMethodCallEvent event, final MethodSlice completed)
  {
    // the completed call assigned to a relevant field or created a relevant object
    if (completed != null && (completed.isFieldDefRelevant() || completed.isConstructorRelevant()))
    {
      setRelevantLine();
      // if the completed slice is globally relevant, so is this line
      isFieldDef = completed.isFieldDefRelevant() || isFieldDef;
    }
    if (line == null
        || (resolutionCursor == -1 && !isDefRelevant && !isUseRelevant() && !isFieldDef && !isEntryPoint))
    {
      return false;
    }
    // implicit synthetic calls made at run-time
    if (line.kind() == LineKind.LK_FIELD_DECLARATION && parent.isConstructor())
    {
      // System.err.println("field initializer filter: " + "\nat event:\n" + event.toString()
      // + "\nat line:\n" + (line != null ? line.toString() : "null") + "\nat method:\n"
      // + parent.method().toString());
      return false;
    }
    else if (line.kind() == LK_METHOD_DECLARATION)
    {
      if (!(event.target() instanceof IMethodContourReference))
      {
        // System.err.println("out-of-model filter: " + "\nat event:\n" + event.toString()
        // + "\nat line:\n" + (line != null ? line.toString() : "null") + "\nat method:\n"
        // + parent.method().toString());
        return false;
      }
      final IMethodNode method = ((IMethodContourReference) event.target()).contour().schema();
      if (method.modifiers().contains(NodeModifier.NM_SYNTHETIC))
      {
        // System.err.println("synthetic filter: " + "\nat event:\n" + event.toString()
        // + "\nat line:\n" + (line != null ? line.toString() : "null") + "\nat method:\n"
        // + parent.method().toString());
        return false;
      }
    }
    // out-of-model target is synthetic
    else if (!event.inModel() && event.target().toString().contains("SYNTHETIC"))
    {
      System.err.println("OUT_OF_MODEL_CALL_TO_SYNTHETIC");
      return completed != null && completed.isRelevant();
    }
    // in-model call to toString() from an out-of-model caller valueOf(L/java/lang/Object;)
    else if (!event.parent().inModel()
        && event.caller().toString().equals("Ljava/lang/String;.valueOf(Ljava/lang/Object;)")
        && event.inModel() && event.execution().schema().key().endsWith(".toString()"))
    {
      System.err.println("OUT_OF_MODEL_CALL_TO_IN_MODEL_TO_STRING");
      return completed != null && completed.isRelevant();
    }
    if (resolutionList.isEmpty() || line.kind() == LK_CATCH || completed.isClinit()
        || (!isCatchRelevant && event.terminator() instanceof IExceptionThrowEvent))
    {
      processStatic(false);
      // the method call should be processed at the call site, which hasn't been computed yet
      if (line.kind() == LK_CATCH && completed != null && completed.isRelevant()
          && event.terminator() instanceof IExceptionThrowEvent)
      {
        catchCompleted = completed;
      }
      return false;
    }
    return true;
  }

  private void processInModelCall(final IResolvedCall call, final BitSet argRelevant)
  {
    // process each argument position of the call
    for (int i = 0; i < call.size(); i++)
    {
      // resolved nodes used in this argument position
      final List<IResolvedNode> uses = call.uses(i);
      // all nodes in the same argument position have the same relevance
      for (final IResolvedNode use : uses)
      {
        final int useIndex = resolutionList.indexOf(use);
        // relevance has been determined by the completed method slice
        defRelevant.set(useIndex, argRelevant.get(i));
        // local variables in relevant argument positions are processed at this point
        if (use instanceof IResolvedData && argRelevant.get(i))
        {
          final IDataNode node = ((IResolvedData) use).data();
          if (node != null && node.kind() == NodeKind.NK_VARIABLE)
          {
            // contour member matching the variable
            final IContourMember member = parent.method().lookupMember(node);
            // map the key node to the relevant contour member
            resolutionMap.put(useIndex, member);
            // set as relevant the qualifiers of this use
            updateQualifiers(use);
            // mark the variable as relevant
            parent.handleVariableUse(member);
          }
          else if (node != null && node.kind() == NodeKind.NK_FIELD
              && node.modifiers().contains(NodeModifier.NM_COMPILE_TIME_FINAL)
              && node.parent() != null && node.type().node() != null
              && (node.type().node().isPrimitive() || node.type().node().isString()))
          {
            final IContextContour context = parent.initialEvent().model().contourFactory()
                .lookupStaticContour(node.parent().name());
            if (context != null)
            {
              // contour member matching the variable
              final IContourMember member = context.lookupMember(node);
              // map the key node to the relevant contour member
              resolutionMap.put(useIndex, member);
              // mark the field as relevant
              parent.program().handleFieldUse(context, member);
            }
          }
          else
          {
            // map the key node to a irrelevant value
            resolutionMap.put(useIndex, LineSlice.NODE_ARG_IRRELEVANT);
          }
        }
      }
    }
  }

  private void processOutOfModelCall(final IResolvedCall call)
  {
    // System.err.println("PROCESSING_OUT_OF_MODEL_CALL[" + call + "]");
    // process each argument position of the call
    for (int i = 0; i < call.size(); i++)
    {
      // resolved nodes used in this argument position
      final List<IResolvedNode> uses = call.uses(i);
      // all nodes in the same argument position have the same relevance
      for (final IResolvedNode use : uses)
      {
        final int useIndex = resolutionList.indexOf(use);
        // def-relevant out-of-model call arguments might be relevant
        defRelevant.set(useIndex,
            isUseRelevant() || isDefRelevant() || relevantLines.get(line.lineNumber()));
        // local variables in relevant argument positions are processed at this point
        if (defRelevant.get(useIndex) && use instanceof IResolvedData)
        {
          final IDataNode node = ((IResolvedData) use).data();
          if (node != null && node.kind() == NodeKind.NK_VARIABLE)
          {
            // contour member matching the variable
            final IContourMember member = parent.method().lookupMember(node);
            // map the key node to the relevant contour member
            resolutionMap.put(useIndex, member);
            // set as relevant the qualifiers of this use
            updateQualifiers(use);
            // mark the variable as relevant
            parent.handleVariableUse(member);
          }
          else
          {
            // map the key node to an irrelevant value
            resolutionMap.put(useIndex, LineSlice.NODE_ARG_IRRELEVANT);
          }
        }
      }
    }
  }

  private void processStatic(final boolean lineChanging)
  {
    final boolean processArguments = resolutionCursor > 0
        && (resolutionList.get(resolutionCursor - 1) instanceof IResolvedCall);
    while (resolutionCursor != -1 && resolutionCursor < resolutionList.size())
    {
      final IResolvedNode key = resolutionList.get(resolutionCursor);
      // no events are generated for "this" expressions
      if (key instanceof IResolvedThis)
      {
        // System.err.println("'this' node: " + key.toString());
        resolutionCursor++;
        continue;
      }
      // during a relevant line change, process all pending out-of-model calls
      if (lineChanging && (isUseRelevant() || isDefRelevant || isEntryPoint)
          && isOutOfModelCall(resolutionList.get(resolutionCursor), null, true))
      {
        resolutionCursor++;
        continue;
      }
      // some data nodes do not have a matching run-time event
      if (key instanceof IResolvedData && (processArguments || !((IResolvedData) key).isActual()))
      {
        final IResolvedData node = (IResolvedData) key;
        final IDataNode data = node.data();
        // actual arguments have been mapped and handled
        if (processArguments && node.isActual() && data != null
            && data.kind() == NodeKind.NK_VARIABLE)
        {
          // relevant variable use in an assignment, control, or flexible line
          if (defRelevant.get(resolutionCursor)
              && resolutionMap.get(resolutionCursor) != LineSlice.NODE_ARG_IRRELEVANT)
          {
            final IContourMember member = (IContourMember) resolutionMap.get(resolutionCursor);
            parent.handleVariableUse(member);
            if (parent.isNormalFlowRelevant()
                && member.schema().modifiers().contains(NodeModifier.NM_ARGUMENT))
            {
              // mark this argument as relevant
              parent.handleArgumentUse(member);
            }
            // System.err.println("statically handled relevant argument: " + key.toString());
          }
          resolutionCursor++;
          continue;
        }
        // out-of-model node (e.g., System.err)
        if (data == null)
        {
          // System.err.println("Out-of-model node: " + key.toString());
          resolutionCursor++;
          continue;
        }
        // current index points to a resolved local variable read
        if (!node.isDef() && data.kind() == NodeKind.NK_VARIABLE)
        {
          // contour member matching the variable
          final IContourMember member = parent.method().lookupMember(data);
          // map the key node to the relevant contour member
          resolutionMap.put(resolutionCursor, member);
          // relevant variable use in an assignment, control, or flexible line
          if (relevantLines.get(line.lineNumber())
              && (isDefRelevant() || isUseRelevant() || (!node.isLHS() && isFlexibleRHS)))
          {
            if (member == null)
            {
              System.err.println("NULL_MEMBER[" + member + "]");
            }
            parent.handleVariableUse(member);
          }
          if (parent.isNormalFlowRelevant()
              && member.schema().modifiers().contains(NodeModifier.NM_ARGUMENT))
          {
            // mark this argument as relevant
            parent.handleArgumentUse(member);
          }
          resolutionCursor++;
          continue;
        }
        // current index points to an array cell write-- field or local variable
        // if (node.isDef()
        // && line.kind().isArrayCellAssignment()
        // && ((data.type().node() != null && data.type().node().kind() == NodeKind.NK_ARRAY) ||
        // (data
        // .type().node() == null && data.type().key().contains("[")))) {
        // resolutionCursor++;
        // continue;
        // }
        /**
         * Current index points to a final field of a primitive or string type whose value has been
         * inlined at compile time.
         */
        if (data.kind() == NodeKind.NK_FIELD
            && data.modifiers().contains(NodeModifier.NM_COMPILE_TIME_FINAL)
            && data.type().node() != null
            && (data.type().node().isPrimitive() || data.type().node().isString()))
        {
          resolutionCursor++;
          // compile time final instance fields cannot be resolved-- don't know which
          // instance to resolve to...
          if (data.parent() != null && data.modifiers().contains(NodeModifier.NM_STATIC))
          {
            final IContextContour context = parent.initialEvent().model().contourFactory()
                .lookupStaticContour(data.parent().name());
            if (context != null)
            {
              // contour member matching the variable
              final IContourMember member = context.lookupMember(data);
              // map the key node to the relevant contour member
              resolutionMap.put(resolutionCursor, member);
              // if use relevant, add to the program
              if (isUseRelevant())
              {
                // mark the field as relevant
                parent.program().handleFieldUse(context, member);
              }
            }
          }
          continue;
        }
      }
      /**
       * Could not process this node-- ignore and move to the next if appropriate.
       */
      else if (isFlexibleRHS && lineChanging && !key.isLHS())
      {
        resolutionCursor++;
        continue;
      }
      break;
    }
    // check whether resolutions for the current line are complete
    if (resolutionCursor >= resolutionList.size())
    {
      resolutionCursor = -1;
    }
  }

  private void relevantLineCompleted(final IResolvedLine oldLine)
  {
    /**
     * Check if the current line is the first one in a catch block following an old line not in the
     * same catch block.
     */
    if (!relevantLines.get(line.lineNumber()))
    {
      final IResolvedLine catchBlock = line.parent() != null && line.parent().kind() == LK_CATCH ? line
          : null;
      if (catchBlock != null)
      {
        IResolvedLine oldCatchBlock = oldLine;
        while (oldCatchBlock != null && oldCatchBlock.kind() != LK_CATCH)
        {
          oldCatchBlock = oldCatchBlock.parent();
        }
        if (catchBlock != oldCatchBlock)
        {
          relevantLines.set(line.lineNumber());
        }
      }
    }
    // // the new line is a continue following a relevant pass on its loop controller
    // if (line.kind() == LineKind.LK_CONTINUE && oldLine.isLoopControl()
    // && line.lineNumber() > oldLine.lineNumber()) {
    // isContinueRelevant = true;
    // }
    // inherited control dependences-- we traverse them all because, for some dependences,
    // the compiler might skip the line altogether, as in certain switch/case statements
    IResolvedLine p = oldLine.parent();
    while (p != null)
    {
      relevantLines.set(p.lineNumber());
      p = p.parent();
    }
    // propagated control dependences (unstructured constructs)
    for (final IResolvedLine node : oldLine.jumpDependences())
    {
      // add the jump dependence
      relevantLines.set(node.lineNumber());
      // add its parent dependence
      if (node.parent() != null)
      {
        relevantLines.set(node.parent().lineNumber());
      }
    }
    // the first line to execute before a catch caused the exception and must be relevant
    isCatchRelevant = oldLine.kind() == LK_CATCH && relevantLines.get(oldLine.lineNumber());
    // if (isCatchRelevant) {
    // System.err.println("catch relevant: " + line.toString());
    // }
    // clear this line from the chased lines
    relevantLines.clear(oldLine.lineNumber());
    // this line is not entry point relevant any longer
    isEntryPoint = false;
  }

  /**
   * Matches a field read event to the closest node in the resolution list.
   */
  private IResolvedData resolveFlexibleNode(final IFieldReadEvent event)
  {
    skipOutOfModelCalls(null);
    if (resolutionCursor == -1)
    {
      System.err.println("FLEXIBLE_RESOLUTION_EOF: " + event.toString());
      return null;
    }
    // try the current node
    IResolvedNode key = resolutionList.get(resolutionCursor);
    // find the first key that can be associated with **the given** field read
    while (resolutionCursor != -1 && resolutionCursor < resolutionList.size()
        && validateFieldRead(key, event) != null)
    {
      // update to the next position
      resolutionCursor++;
      // check whether resolutions for the current line are complete
      if (resolutionCursor < resolutionList.size())
      {
        // try the current node
        key = resolutionList.get(resolutionCursor);
      }
      else
      {
        key = null;
        resolutionCursor = -1;
      }
    }
    // check for a "spurious" field read for an array field cell write
    if (isSpuriousFieldReadOnArrayCellAssignment(event))
    {
      return null;
    }
    // make sure the key can be associated with **the given** field read
    check(validateFieldRead(key, event), event);
    // map the resolved node
    resolutionMap.put(resolutionList.indexOf(key), event);
    // update to the next position
    resolutionCursor++;
    // check whether resolutions for the current line are complete
    if (resolutionCursor >= resolutionList.size())
    {
      resolutionCursor = -1;
    }
    // return the resolved node
    return (IResolvedData) key;
  }

  /**
   * Matches a method call event to the closest node in the resolution list.
   */
  private IResolvedCall resolveMethodNode(final IMethodCallEvent event)
  {
    // try the current node
    IResolvedNode key = resolutionList.get(resolutionCursor);
    // find the first key that can be associated with **the given** method call-- skips
    // non-matching calls, both in-model and out-of-model
    while (resolutionCursor != -1 && resolutionCursor < resolutionList.size()
        && (isOutOfModelCall(key, event, true) || validateMethodCall(key, event) != null))
    {
      // update to the next position
      resolutionCursor++;
      // check whether resolutions for the current line are complete
      if (resolutionCursor < resolutionList.size())
      {
        // try the current node
        key = resolutionList.get(resolutionCursor);
      }
      else
      {
        key = null;
        resolutionCursor = -1;
      }
    }
    // make sure the key can be associated with **the given** method call
    check(validateMethodCall(key, event), event);
    // map the resolved node
    resolutionMap.put(resolutionList.indexOf(key), event);
    // do not update if the method is toString()-- the line is flexible so the next
    // node should resolve correctly, whether it's another out-of-model call to toString
    // or the actual next node on the line
    if (((IResolvedCall) key).call().key().equals("Ljava/lang/Object;.toString()"))
    {
      // return the resolved node
      return (IResolvedCall) key;
    }
    // update to the next position
    resolutionCursor++;
    // check whether resolutions for the current line are complete
    if (resolutionCursor >= resolutionList.size())
    {
      resolutionCursor = -1;
    }
    // return the resolved node
    return (IResolvedCall) key;
  }

  /**
   * Guarantees that the current resolved node matches a field assignment event.
   * 
   * No need for flexible resolution because assignments occur as the very last event on a
   * statement. There are exceptions to this rule, but those will not be supported (e.g., using an
   * assignment as part of an expression).
   */
  private IResolvedNode resolveNode(final IFieldAssignEvent event)
  {
    skipOutOfModelCalls(null);
    final IResolvedNode key = currentResolvedNode();
    final String validFieldAssign = validateFieldAssign(key);
    if (validFieldAssign == null)
    {
      // map the resolved node
      resolutionMap.put(resolutionList.indexOf(key), event);
      // mark the line as relevant
      relevantLines.set(line.lineNumber());
      // update the resolution cursor
      nextResolutionPosition();
      // return the resolved node
      return key;
    }
    // check for an inferred array member write
    else if (event.contour().schema().kind() == NodeKind.NK_ARRAY)
    {
      // make sure the key can be associated with a field assign
      check(validateVarAssign(key), event);
      // variable assign node for an array write-- wait for a variable write event
      return key;
    }
    else
    {
      // force exception
      check(null, event);
      // unreachable
      return null;
    }
  }

  /**
   * Guarantees that the current resolved node matches a field read event.
   */
  private IResolvedNode resolveNode(final IFieldReadEvent event)
  {
    skipOutOfModelCalls(null);
    if (isFlexibleRHS)
    {
      if (resolutionList.isEmpty())
      {
        System.err.println("SPURIOUS_FIELD_READ_ON_EMPTY_RESOLUTION_LIST");
        return null;
      }
      // check for a "spurious" field read for an array field cell write
      if (resolutionCursor == -1 && isSpuriousFieldReadOnArrayCellAssignment(event))
      {
        System.err.println("FIELD_READ_FOR_ARRAY_FIELD_CELL_WRITE[" + event + "]");
        return null;
      }
      return resolveFlexibleNode(event);
    }
    // check for a "spurious" field read for an array field cell write
    if (resolutionCursor == -1 && isSpuriousFieldReadOnArrayCellAssignment(event))
    {
      System.err.println("FIELD_READ_FOR_ARRAY_FIELD_CELL_WRITE[" + event + "]");
      return null;
    }
    final IResolvedNode key = currentResolvedNode();
    // check for a "spurious" field read for an array field cell write
    if (isSpuriousFieldReadOnArrayCellAssignment(event))
    {
      return null;
    }
    // make sure the key can be associated with a field read
    check(validateFieldRead(key), event);
    // map the resolved node
    resolutionMap.put(resolutionList.indexOf(key), event);
    // update the resolution cursor
    nextResolutionPosition();
    // return the resolved node
    return key;
  }

  /**
   * Guarantees that the current resolved node matches a method call event. The target of this
   * method call must match the call node at the current cursor position.
   */
  private IResolvedCall resolveNode(final IMethodCallEvent event)
  {
    if (isFlexibleRHS)
    {
      if (resolutionList.isEmpty())
      {
        return null;
      }
      // implicit synthetic calls made at run-time
      if (line.kind() == LK_METHOD_DECLARATION)
      {
        if (!(event.target() instanceof IMethodContourReference))
        {
          return null;
        }
        final IMethodNode method = ((IMethodContourReference) event.target()).contour().schema();
        if (method.modifiers().contains(NodeModifier.NM_SYNTHETIC))
        {
          return null;
        }
      }
      skipOutOfModelCalls(event);
      if (resolutionCursor == -1)
      {
        return null;
      }
      return resolveMethodNode(event);
    }
    skipOutOfModelCalls(event);
    /**
     * nested method called at a return statement nested within a try; this is the second pass-- the
     * result variable assignment happened on the first pass so skip the result assignment
     */
    if (line.kind() == LK_RETURN_IN_TRY && resolutionCursor == 0)
    {
      resolutionCursor++;
    }
    final IResolvedNode key = currentResolvedNode();
    // make sure the key can be associated with a method call
    check(validateMethodCall(key), event);
    // map the resolved node
    resolutionMap.put(resolutionList.indexOf(key), event);
    // update to the next position
    resolutionCursor++;
    // check whether resolutions for the current line are complete
    if (resolutionCursor >= resolutionList.size())
    {
      resolutionCursor = -1;
    }
    // return the resolved node
    return (IResolvedCall) key;
  }

  /**
   * Guarantees that the current resolved node matches a variable assignment event.
   * 
   * No need for flexible resolution because assignments occur as the very last event on a
   * statement. There are exceptions to this rule, but those will not be supported (e.g., using an
   * assignment as part of an expression).
   */
  private IResolvedNode resolveNode(final IVarAssignEvent event)
  {
    skipOutOfModelCalls(null);
    final IResolvedNode key = currentResolvedNode();
    // make sure the key can be associated with a variable assign
    check(validateVarAssign(key), event);
    // map the resolved node
    resolutionMap.put(resolutionList.indexOf(key), event);
    // mark the line as relevant
    relevantLines.set(line.lineNumber());
    // update the resolution cursor
    nextResolutionPosition();
    // return the resolved node
    return key;
  }

  /**
   * Guarantees that the current resolved node matches a variable assignment event.
   * 
   * No need for flexible resolution because arguments are resolved at the method declaration line
   * and, therefore, no flexibility can ever arise.
   */
  private IResolvedNode resolveNodeArg(final IVarAssignEvent event)
  {
    final IResolvedNode key = currentResolvedNodeArg();
    // make sure the key can be associated with a variable assign
    check(validateArgAssign(key), event);
    // return the resolved node
    return key;
  }

  private void setRelevantLine()
  {
    if (line != null && line.lineNumber() > 0)
    {
      relevantLines.set(line.lineNumber());
    }
  }

  /**
   * Skips out-of-model calls in the line until an exact method call or a potential non-method call
   * node for the event is found.
   */
  private void skipOutOfModelCalls(final IMethodCallEvent event)
  {
    if (resolutionCursor == -1)
    {
      return;
    }
    // try the current node
    IResolvedNode key = resolutionList.get(resolutionCursor);
    // find the first key that can be associated with **the given** method call
    while (resolutionCursor != -1 && resolutionCursor < resolutionList.size()
        && isOutOfModelCall(key, event, true))
    {
      // System.err.println("SKIP_OUT_OF_MODEL_CALL[" + key + "]");
      // update to the next position
      resolutionCursor++;
      // check whether resolutions for the current line are complete
      if (resolutionCursor < resolutionList.size())
      {
        // try the current node
        key = resolutionList.get(resolutionCursor);
      }
      else
      {
        key = null;
        resolutionCursor = -1;
      }
    }
  }

  private void updateQualifiers(final IResolvedNode qualified)
  {
    IResolvedNode head = qualified;
    while (head != null)
    {
      final IResolvedNode headDup = head;
      head = null;
      // find the immediate qualifier of the head
      for (int i = 0; i < resolutionList.size(); i++)
      {
        final IResolvedNode node = resolutionList.get(i);
        // if a qualifier is found, make it relevant and update the head
        if (node.qualifierOf() == headDup)
        {
          defRelevant.set(i, true);
          // update this qualifier in the resolution map, if possible
          if (node instanceof IResolvedData && ((IResolvedData) node).data() != null
              && ((IResolvedData) node).data().kind() == NodeKind.NK_VARIABLE)
          {
            // contour member matching the variable
            final IContourMember member = parent.method().lookupMember(
                ((IResolvedData) node).data());
            resolutionMap.put(i, member);
            // mark the qualifier as relevant
            parent.handleVariableUse(member);
          }
          head = node;
          break;
        }
      }
    }
  }

  private String validateArgAssign(final IResolvedNode key)
  {
    // validate the key as a data node
    if (!(key instanceof IResolvedData))
    {
      return "Expected a data node but found '" + (key == null ? "null" : key.toString())
          + "' instead.";
    }
    // validate the key as a definition node
    final IResolvedData node = (IResolvedData) key;
    // validate the key as a variable data node
    final IDataNode data = node.data();
    if (data != null
        && (data.kind() == NodeKind.NK_FIELD || !data.modifiers()
            .contains(NodeModifier.NM_ARGUMENT)))
    {
      return "Expected a variable definition node but found '" + key.toString() + "' instead.";
    }
    return null;
  }

  private String validateFieldAssign(final IResolvedNode key)
  {
    // validate the key as a data node
    if (!(key instanceof IResolvedData))
    {
      return "Expected a data node but found '" + (key == null ? "null" : key.toString())
          + "' instead.";
    }
    // validate the key as a definition node
    final IResolvedData node = (IResolvedData) key;
    if (!node.isDef())
    {
      return "Expected a field definition node but found: " + node.toString();
    }
    // validate the key as a field data node
    final IDataNode data = node.data();
    if (data != null && data.kind() == NodeKind.NK_VARIABLE)
    {
      return "Expected a field definition node node but found '" + key.toString() + "' instead.";
    }
    return null;
  }

  private String validateFieldRead(final IResolvedNode key)
  {
    // validate the key as a data node
    if (!(key instanceof IResolvedData))
    {
      return "Expected a field data node but found '" + (key == null ? "null" : key.toString())
          + "' instead.";
    }
    // validate the key as a field data node
    final IDataNode data = ((IResolvedData) key).data();
    if (data != null && data.kind() == NodeKind.NK_VARIABLE)
    {
      return "Expected a field reference node but found '" + key.toString() + "' instead.";
    }
    return null;
  }

  private String validateFieldRead(final IResolvedNode key, final IFieldReadEvent event)
  {
    // check if the key is a field read
    final String result = validateFieldRead(key);
    if (result != null)
    {
      return result;
    }
    // check if the key is a field read node compatible with the event's field
    final IDataNode data = ((IResolvedData) key).data();
    final IDataNode schema = event.member().schema();
    if (data == null)
    {
      return "Invalid field resolution-- trying to match an out-of-model data node from "
          + key.toString() + " with event " + event.toString();
    }
    if (data != schema)
    {
      return "Invalid field resolution-- trying to match in-model field node from "
          + key.toString() + " with event " + event.toString();
    }
    return null;
  }

  private String validateMethodCall(final IResolvedNode key)
  {
    // check if the key is a call node
    if (!(key instanceof IResolvedCall))
    {
      return "Expected a call node but found '" + (key == null ? "null" : key.toString())
          + "' instead.";
    }
    return null;
  }

  private String validateMethodCall(final IResolvedNode key, final IMethodCallEvent event)
  {
    // check if the key is a call node
    final String result = validateMethodCall(key);
    if (result != null)
    {
      return result;
    }
    final IResolvedCall node = (IResolvedCall) key;
    final IMethodContour target = event.execution();
    // check if the call node is compatible with the event's target
    if (event.inModel() && !event.caller().toString().contains("<BRIDGE>"))
    {
      if (node.call().node() == null
          && !event.model().staticAnalysisFactory()
              .overrides(target.parent().concreteContour().schema(), target.schema(), node))
      {
        return "Invalid call resolution-- trying to match an out-of-model method call from "
            + key.toString() + " with call target " + target.toString();
      }
      if (node.call().node() != target.schema()
          && !event.model().staticAnalysisFactory()
              .overrides(target.parent().concreteContour().schema(), target.schema(), node))
      {
        return "Invalid call resolution-- trying to match in-model method call from "
            + key.toString() + " with call target " + target.toString();
      }
      return null;
    }
    /**
     * Resolve bridge methods-- the keys may represent different methods altogether or two methods
     * that are related via generics. For example, one may be a {@code compareTo(T)} in
     * {@code Comparable<T>} and the other an implementation with a concrete instantiation of
     * {@code T}, such as in {@code String extends Comparable<String>}.
     */
    if (event.inModel() && event.caller().toString().contains("<BRIDGE>"))
    {
      final String bridgeKey;
      if (event.caller() instanceof IOutOfModelMethodKeyReference)
      {
        bridgeKey = ((IOutOfModelMethodKeyReference) event.caller()).value();
      }
      else
      {
        bridgeKey = event.caller().toString()
            .substring(0, event.caller().toString().indexOf("<") - 1);
        // bridgeKey = ((IMethodContourReference) event.target()).contour().schema().key();
      }
      if (!event.model().staticAnalysisFactory().bridges(bridgeKey, node.call().key()))
      {
        return "Invalid bridge resolution-- trying to match bridged method call from "
            + key.toString() + " with call target " + target.toString();
      }
      return null;
    }
    // resolve other out-of-model methods-- target
    String targetKey = event.target().toString();
    if (targetKey.contains("."))
    {
      targetKey = targetKey.substring(targetKey.lastIndexOf(".") + 1);
    }
    targetKey = targetKey.substring(0, targetKey.indexOf(")"));
    // resolve other out-of-model methods-- static method call node
    String nodeKey = node.call().key();
    if (nodeKey.contains("."))
    {
      nodeKey = nodeKey.substring(nodeKey.indexOf(".") + 1);
    }
    nodeKey = nodeKey.substring(0, nodeKey.indexOf(")"));
    /**
     * Match by name/argument list: target key is the out-of-model call, so all its arguments must
     * be in the static method call node signature. A typical example is the synthetic constructor
     * of a non-static member class. The out-of-model (synthetic) call has the enclosing class and
     * the constructor's class in the argument list; the node call has only the enclosing class.
     */
    if (targetKey.contains(nodeKey))
    {
      System.err.println("OUT_OF_MODEL_TARGET_MAPPED_TO_CALL_NODE[" + targetKey + "]");
      return null;
    }
    //
    // TODO: should we process any other cases?
    // TODO: perhaps be more restrictive and include the method's type, then see about less
    // restrictive cases?
    // TODO: should we be less restrictive and simply match by type/name?
    //
    // it was not possible to map the event to the current static method call node
    return "Invalid call resolution-- trying to match out-of-model method call from "
        + node.toString() + " with call target " + event.target().toString();
  }

  private String validateVarAssign(final IResolvedNode key)
  {
    // special result local variable
    if (key == LineSlice.NODE_RESULT)
    {
      return null;
    }
    // validate the key as a data node
    if (!(key instanceof IResolvedData))
    {
      return "Expected a field data node but found '" + key.toString() + "' instead.";
    }
    // validate the key as a definition node
    final IResolvedData node = (IResolvedData) key;
    if (!node.isDef())
    {
      return "Expected a variable definition node but found: " + node.toString();
    }
    // validate the key as a variable data node
    final IDataNode data = node.data();
    if (data != null && data.kind() == NodeKind.NK_FIELD)
    {
      return "Expected a variable definition node but found '" + key.toString() + "' instead.";
    }
    return null;
  }

  /**
   * Determines to which source line this event is associated and updates the resolved map if
   * necessary. If this line does not match the line cursor, this method updates the line cursor.
   * This method should be called once for each event processed by the slice.
   */
  boolean handleEvent(final IJiveEvent event)
  {
    if (event instanceof IFieldReadEvent
        // enum types read their enum constants after initialization and are never relevant
        && ((IFieldReadEvent) event).member().schema().modifiers()
            .contains(NodeModifier.NM_ENUM_CONSTANT)
        && parent.program().handleEnumConstant(((IFieldReadEvent) event).member().schema()))
    {
      System.err.println(String.format("Ignoring read of enum constant '%s' at %s.",
          ((IFieldReadEvent) event).member().schema().name(), event.line().toString()));
      return false;
    }
    // line mismatch (should be fine)
    if (line != null && event.line().lineNumber() != line.lineNumber())
    {
      if (MethodSlice.DEBUG_MODE)
      {
        System.err
            .println(String
                .format(
                    "Handling lines with mismatching numbers.\n(Normal: overflow lines and off-by-one line step at the end of a block, for which there is no static node.)\nEvent '%s (%s)'\nLine %s.",
                    event.toString(), event.line().toString(), line));
      }
    }
    // a pending field read for a self-write is relevant only if the line is def-relevant
    if (pendingSelfWriteFieldRead)
    {
      if (event instanceof IFieldReadEvent)
      {
        pendingSelfWriteFieldRead = false;
        return this.isDefRelevant;
      }
      System.err.println(String.format(
          "Expecting a Field Read after a Self Field Write.)\nEvent '%s (%s)'\nLine %s.",
          event.toString(), event.line().toString(), line));
    }
    // mark the line as relevant if it's the first ever
    if (parent.program().initialEvent() == event)
    {
      relevantLines.set(line.lineNumber());
    }
    /**
     * For constructors and class initializers, entry-point relevance is bound to the entry point
     * event. For other methods, entry-point relevance is bound to the line.
     */
    final boolean isEntryRelevant = (!parent.isClinit() && !parent.isConstructor() && isFirstEntryPointEvent)
        || (parent.program().initialEvent() == event);
    isFirstEntryPointEvent = false;
    /**
     * This local relevance flag indicates whether the input event should be in the slice.
     */
    boolean isRelevant = isUseRelevant() || isCatchRelevant || isDefRelevant || isFieldDef
        || isEntryRelevant;
    //
    // cannot resolve node for this event, but will conservatively process events
    //
    if (mdg() == null || line == null)
    {
      if (event instanceof IFieldAssignEvent)
      {
        final boolean isSpuriousArrayCellWrite = isSpuriousFieldWriteOnArrayCellAssignment((IFieldAssignEvent) event);
        final boolean isSupuriousArrayFieldWrite = isSpuriousArrayCellWrite
            && parent.program().isRelevantContext(((IFieldAssignEvent) event).contour());
        if (isSupuriousArrayFieldWrite || parent.program().hasChaseField((IFieldAssignEvent) event))
        {
          parent.program().handleFieldDef((IFieldAssignEvent) event);
          isDefRelevant = true;
          isFieldDef = true;
          // avoid duplicating events on the method and program slices
          return false;
        }
      }
      //
      // relevant variable assignment
      //
      else if (event instanceof IVarAssignEvent)
      {
        final IContourMember member = ((IVarAssignEvent) event).member();
        if (parent.hasChaseVariable(member) /* || isLastEventArrayCellWrite */)
        {
          parent.handleVariableDef((IVarAssignEvent) event);
          isDefRelevant = true;
          // let the method slice collect the event
          return true;
        }
      }
      if (event instanceof IAssignEvent)
      {
        // might need to chain value
        parent.program().chainValueDef((IAssignEvent) event);
      }
      return isRelevant;
    }
    //
    // the current line has no further nodes to resolve
    //
    if (line.kind() == LK_METHOD_DECLARATION && (parent.isClinit() || resolutionArgs.isEmpty()))
    {
      // relevant field assignment
      //
      if (event instanceof IFieldAssignEvent)
      {
        final boolean isSpuriousArrayCellWrite = isSpuriousFieldWriteOnArrayCellAssignment((IFieldAssignEvent) event);
        final boolean isSupuriousArrayFieldWrite = isSpuriousArrayCellWrite
            && parent.program().isRelevantContext(((IFieldAssignEvent) event).contour());
        if (isSupuriousArrayFieldWrite || parent.program().hasChaseField((IFieldAssignEvent) event))
        {
          parent.program().handleFieldDef((IFieldAssignEvent) event);
          return true;
        }
        // might need to chain value
        parent.program().chainValueDef((IFieldAssignEvent) event);
      }
      return isRelevant;
    }
    //
    // advance the resolution cursor if an event is irrelevant to the slice
    //
    boolean advanceCursor = false;
    //
    // only arguments can be processed now
    //
    if (line.kind() == LK_METHOD_DECLARATION && !parent.isClinit() && !resolutionArgs.isEmpty())
    {
      // try to resolve as an argument chased by the line
      //
      if (event instanceof IVarAssignEvent)
      {
        // process argument
        resolveNodeArg((IVarAssignEvent) event);
        // contour
        final IContourMember member = ((IVarAssignEvent) event).member();
        // corner case-- synthetic argument assignment
        if (parent.program().initialEvent() == event)
        {
          if (((IVarAssignEvent) event).newValue().isContourReference())
          {
            final IContourReference cr = (IContourReference) ((IVarAssignEvent) event).newValue();
            parent.program().addContext((IContextContour) cr.contour());
          }
          // defines an argument --> ARG_OUT
          isDefRelevant = true;
        }
        if (parent.hasChaseVariable(member))
        {
          parent.handleVariableDef((IVarAssignEvent) event);
          // mark this argument as relevant
          parent.handleArgumentUse(member);
          // if this argument is a reference and the method slice is relevant
          if (parent.isRelevant() && ((IVarAssignEvent) event).newValue().isContourReference())
          {
            final IContourReference cr = (IContourReference) ((IVarAssignEvent) event).newValue();
            parent.program().addContext((IContextContour) cr.contour());
          }
          // isLastEventArrayCellWrite = false;
          // let the method slice collect the event
          return true;
        }
        // might need to chain value
        parent.program().chainValueDef((IAssignEvent) event);
      }
      // ignore the event
      return false;
    }
    //
    // line step updates the line relevance
    //
    if (event instanceof ILineStepEvent)
    {
      return isRelevant && relevantLines.get(((ILineStepEvent) event).line().lineNumber());
    }
    //
    // resolve the field definition
    //
    else if (event instanceof IFieldAssignEvent)
    {
      final IFieldAssignEvent fae = (IFieldAssignEvent) event;
      //
      // self-writes are followed by field reads generated by the compiler
      //
      if (isSelfWrite)
      {
        pendingSelfWriteFieldRead = true;
      }
      final boolean isSpuriousArrayCellWrite = isSpuriousFieldWriteOnArrayCellAssignment(fae);
      final boolean isSupuriousArrayFieldWrite = false && isSpuriousArrayCellWrite
          && parent.program().isRelevantContext(fae.contour());
      //
      // try to resolve as a field chased by the program slice
      //
      if (isSupuriousArrayFieldWrite || parent.program().hasChaseField(fae))
      {
        parent.program().handleFieldDef(fae);
        isDefRelevant = true;
        isFieldDef = true;
        if (!isSupuriousArrayFieldWrite)
        {
          final IResolvedNode node = resolveNode(fae);
          // updates the relevance of the nodes that qualify this node
          updateQualifiers(node);
          // handle implicit use of the def variable
          if (isSelfWrite)
          {
            parent.program().handleFieldUse((IFieldAssignEvent) event);
          }
          final IResolvedData data = (IResolvedData) node;
          if ((line.kind() == LineKind.LK_ASSIGNMENT_ARRAY_CELL
              || line.kind() == LineKind.LK_POSTFIX_ARRAY_CELL || line.kind() == LineKind.LK_PREFIX_ARRAY_CELL)
              && data.isLHS()
              && data.isDef()
              && data.data() != null
              && data.data().kind() == NodeKind.NK_VARIABLE
              && data.data().type().node() != null
              && data.data().type().node().kind() == NodeKind.NK_ARRAY
              && fae.contour().schema().kind() == NodeKind.NK_ARRAY)
          {
            // the local variable used to reference the array whose cell was written;
            // no variable write will show up on the trace, hence we process the local
            // on here; furthermore, the line must have a def for the local variable,
            // so we also update the resolution cursor here
            parent.handleVariableUse(parent.method().lookupMember(data.data()));
            // update to the next position
            resolutionCursor++;
            // check whether resolutions for the current line are complete
            if (resolutionCursor >= resolutionList.size())
            {
              resolutionCursor = -1;
            }
          }
        }
        // avoid duplicating events on the method and program slices
        return false;
      }
      // might need to chain value
      parent.program().chainValueDef(fae);
      // reset the last assignment
      lastVarAssign = null;
      // advance only if the next event is not a var write for the array cell write
      advanceCursor = true;
    }
    //
    // resolve the field use
    //
    else if (event instanceof IFieldReadEvent)
    {
      // make sure we are at the correct node-- necessary for flexible resolution
      final IResolvedNode node = resolveNode((IFieldReadEvent) event);
      // this must be a field read associated with an array field cell write
      if (node == null)
      {
        System.err.println("FIELD_READ_FOR_ARRAY_FIELD_CELL_WRITE[" + event + "]");
        return false;
      }
      final int nodeIndex = resolutionList.indexOf(node);
      final int qualifierIx = node.qualifierOf() != null ? resolutionList.indexOf(node
          .qualifierOf()) : -1;
      //
      // relevant field read in an assignment
      //
      if (isDefRelevant)
      {
        if (defRelevant.get(nodeIndex) || (qualifierIx != -1 && defRelevant.get(qualifierIx)))
        {
          if (qualifierIx != -1 && isOutOfModelCall(resolutionList.get(qualifierIx), null, false))
          {
            // updates the relevance of the nodes that qualify the out-of-model call
            updateQualifiers(resolutionList.get(qualifierIx));
          }
          else
          {
            // updates the relevance of the nodes that qualify this node
            updateQualifiers(node);
          }
        }
        // a relevant field use in the assignment
        if (defRelevant.get(nodeIndex))
        {
          parent.program().handleFieldUse((IFieldReadEvent) event);
        }
        // avoid duplicating events on the method and program slices
        return false;
      }
      //
      // relevant field use in a control line
      //
      else if (isUseRelevant())
      {
        // updates the relevance of the nodes that qualify this node
        updateQualifiers(node);
        // the field's value determined a relevant branch of execution
        parent.program().handleFieldUse((IFieldReadEvent) event);
        // avoid duplicating events on the method and program slices
        return false;
      }
      // advance the cursor position
      // advanceCursor = true;
    }
    //
    // resolve the variable definition
    //
    else if (event instanceof IVarAssignEvent)
    {
      final IVarAssignEvent vae = (IVarAssignEvent) event;
      // track last variable assignment unconditionally-- for tracking catch variables on the same
      // line
      lastVarAssign = (IVarAssignEvent) event;
      // if (vae.member().schema().type().node() != null
      // && vae.member().schema().type().node().kind() == NodeKind.NK_ARRAY)
      // {
      // lastVarAssign = (IVarAssignEvent) event;
      // }
      // else
      // {
      // lastVarAssign = null;
      // }
      final IContourMember member = vae.member();
      /**
       * Local variable being chased or exception in a relevant catch line
       */
      if (parent.hasChaseVariable(member) /* || isLastEventArrayCellWrite */
          || (line.kind() == LK_CATCH && relevantLines.get(line.lineNumber())))
      {
        parent.handleVariableDef(vae);
        isDefRelevant = true;
        resolveNode(vae);
        // handle implicit use of the def variable
        if (isSelfWrite)
        {
          parent.handleVariableUse(member);
        }
        // keep track of the exception globally to track the throw-catch chain
        if (line.kind() == LK_CATCH && relevantLines.get(line.lineNumber())
            && lastVarAssign != null)
        {
          parent.program().addException(lastVarAssign.newValue());
          lastVarAssign = null;
        }
        // let the method slice collect the event
        return true;
      }
      /**
       * A normal flow relevant method slice modifies an argument as part of its computation.
       */
      if (parent.isNormalFlowRelevant()
          && member.schema().modifiers().contains(NodeModifier.NM_ARGUMENT))
      {
        // mark this argument as relevant
        parent.handleArgumentUse(member);
      }
      // might need to chain value
      parent.program().chainValueDef(vae);
      // advance the cursor
      advanceCursor = true;
    }
    else if (event instanceof IVarDeleteEvent)
    {
      final IVarDeleteEvent vde = (IVarDeleteEvent) event;
      // if the variable is currently in use, we should add the delete event to the slice
      // although this changes the value of the variable as far as the user is concerned,
      // this is just a means to express that the variable is (temporarily) out-of-scope
      if (parent.hasPendingDelete(vde))
      {
        parent.handleVariableDel(vde);
        // // let the method slice collect the event
        return true;
      }
      // do not collect the event
      return false;
    }
    /**
     * FieldAssignEvent, FieldReadEvent, and VarAssignEvent should be ignored if they do not match
     * nodes that must be resolved.
     */
    if (advanceCursor)
    {
      nextResolutionPosition();
      return false;
    }
    // add the event to the slice if the line is relevant
    return isRelevant;
  }

  boolean isCatchLine()
  {
    return line != null && line.kind() == LK_CATCH;
  }

  /**
   * Determines if a method slice is relevant for a field/variable assignment in this source line.
   */
  boolean isDefRelevant()
  {
    return this.isDefRelevant && (resolutionCursor >= 0 && defRelevant.get(resolutionCursor));
  }

  boolean isEntryPoint()
  {
    return isEntryPoint;
  }

  // private boolean isCatchRelevant() {
  //
  // return isCatchRelevant;
  // }
  boolean isFieldDefRelevant()
  {
    return isFieldDef;
  }

  /**
   * The current node is relevant as part of the qualifier (path) expression of a relevant node.
   */
  boolean isQualifierRelevant()
  {
    return resolutionCursor > 0 && defRelevant.get(resolutionCursor)
        && resolutionList.get(resolutionCursor).qualifierOf() != null;
  }

  boolean isUseRelevant()
  {
    return line != null && line.isControl() && relevantLines.get(line.lineNumber());
  }

  IResolvedLine line()
  {
    return line;
  }

  /**
   * A method call event is processed exclusively through this method. The completed slice
   * corresponds to an in-model method call and is mapped to the corresponding resolved node. It
   * also uses the slice relevant arguments of the method slice to update the set of assignment
   * relevant members for the current line.
   */
  void nestedCallCompleted(final IMethodCallEvent event, final MethodSlice completed)
  {
    /**
     * A snapshot may capture an execution at any point. This means that a method call may be
     * captured and correspond to a line that has an arbitrary number of control dependences. Some
     * line step events may also be missing, etc. Thus, a call may return to a point in the source
     * that is far *before* the call site, skipping over any previous instructions on the method
     * body. The check performed here tries to deal with this difficulty, namely, by not processing
     * the completed method call.
     * 
     * TODO: use the last event
     */
    // if (event.model().hasSnapshot()) {
    // int lineNo = -1;
    // // the key set must be traversed in increasing line number
    // for (final Integer key : mdg().dependenceMap().keySet()) {
    // if (key == event.line().lineNumber()) {
    // lineNo = key;
    // break;
    // }
    // // find the largest line number smaller or equal to the event's line number
    // if (lineNo <= key && key <= event.line().lineNumber()) {
    // lineNo = key;
    // }
    // }
    //
    // // determine the resolved line associated with the call event
    // IResolvedLine callLine = lineNo > 0 ? mdg().dependenceMap().get(lineNo) : null;
    // if (callLine == null) {
    // if (event instanceof IMethodReturnedEvent) {
    // callLine = line;
    // }
    // else if (!((IMethodCallEvent) event.parent()).target().isOutOfModel()) {
    // callLine = parent.methodDeclLine();
    // }
    // }
    //
    // // there is a discrepancy-- assume it's due to the snapshot
    // if (callLine != line) {
    // // if the completed slice is globally relevant, propagate to the parent method slice
    // if (completed.isFieldDefRelevant()) {
    // parent.setFieldDefRelevant();
    // }
    // return;
    // }
    // }
    if (!preprocessCallCompleted(event, completed))
    {
      return;
    }
    //
    // mark this line as relevant if it calls a slice relevant method
    //
    if (completed.isRelevant())
    {
      setRelevantLine();
    }
    // implicit call to super constructor
    if (line.kind() != LineKind.LK_SUPER_CONSTRUCTOR && parent.isSuperConstructor(completed))
    {
      System.err.println("IMPLICIT_CALL_TO_SUPER_CONSTRUCTOR");
      // nothing to do-- no arguments, no call site, no nothing
      return;
    }
    // when this line does not have a toString() call and this method is a toString()
    // we resolve the node in-place, that is, without matching an actual node
    if (hasToString.isEmpty() && !event.parent().inModel()
        && event.caller().toString().equals("Ljava/lang/String;.valueOf(Ljava/lang/Object;)")
        && event.inModel() && event.execution().schema().key().endsWith(".toString()"))
    {
      System.err.println("NO_EXPLICIT_TO_STRING_CALL");
      // nothing to do-- no arguments, no call site, no nothing
      // break-- this call is not on the source line, so we cannot process it
      // furthermore, we should not advance the cursor
      return;
    }
    // retrieve the call node associated with the event and the completed slice
    final IResolvedCall call = resolveNode(event);
    // updates the relevance of the nodes that qualify this method call
    if (completed.isRelevant())
    {
      // System.err.println("RELEVANT_CALL_COMPLETED[" + call + "]");
      updateQualifiers(call);
      // relevant in-model call
      if (event.inModel() && (completed.arguments().size() > 0 || completed.isRelevant()))
      {
        isDefRelevant = completed.arguments().size() > 0 || completed.isFieldDefRelevant()
            || completed.isResultRelevant();
        // collect the relevant positions
        final BitSet argRelevant = new BitSet();
        // determine the relevant argument positions in the completed slice
        for (final IContourMember arg : completed.arguments())
        {
          argRelevant.set(arg.schema().index());
        }
        // process the in-model call arguments
        processInModelCall(call, argRelevant);
      }
      // relevant out-of-model call
      else if (!event.inModel()
          && (completed.isConstructorRelevant() || completed.isFieldDefRelevant()))
      {
        System.err.println("RELEVANT_OUT_OF_MODEL_CALL_COMPLETED[" + call + "]");
        isDefRelevant = completed.isFieldDefRelevant();
        processOutOfModelCall(call);
      }
    }
    // process all local variable arguments
    processStatic(false);
  }

  /**
   * The completed slice was called from out-of-model after another in-model call was made on the
   * same out-of-model interaction. This corresponds to an activation with a standard "found" call
   * arrow.
   */
  void nestedOutOfModelCallCompleted(final MethodSlice completed)
  {
    // the completed call assigned to a relevant field or created a relevant object
    if (completed != null && (completed.isFieldDefRelevant() || completed.isConstructorRelevant()))
    {
      setRelevantLine();
      // if the completed slice is globally relevant, so is this line
      isFieldDef = completed.isFieldDefRelevant() || isFieldDef;
    }
  }

  void setEntryPoint()
  {
    isEntryPoint = true;
    isFirstEntryPointEvent = true;
  }

  /**
   * Makes sure that the resolved line is correctly associated with the event. Returns the old line,
   * if a line change occurred, or the current line.
   */
  IResolvedLine updateLine(final IJiveEvent event)
  {
    // cannot resolve node for this event
    if (mdg() == null)
    {
      // System.err.println(String.format(
      // "Out-of-model MDG for method '%s' and event '%s' (line: '%s').", parent.method()
      // .signature(), event.toString(), event.line().toString()));
      line = null;
      resolutionCursor = -1;
      return line;
    }
    else if (line != null && event.line().lineNumber() == line.lineNumber())
    {
      return line;
    }
    else if (line != null && event.parent() instanceof IMethodCallEvent
        && ((IMethodCallEvent) event.parent()).target().toString().contains("access$")
        && ((IMethodCallEvent) event.parent()).target().toString().contains("SYNTHETIC")
        && ((IMethodCallEvent) event.parent()).parent() instanceof IMethodCallEvent)
    {
      System.err.println("EVENT_IN_SYNTHETIC_ACCESSOR");
      return line;
    }
    int lineNo = -1;
    if (event.line().lineNumber() != -1)
    {
      // the key set must be traversed in increasing line number
      for (final Integer key : mdg().dependenceMap().keySet())
      {
        if (key == event.line().lineNumber())
        {
          lineNo = key;
          break;
        }
        // find the largest line number smaller or equal to the event's line number
        if (lineNo <= key && key <= event.line().lineNumber())
        {
          lineNo = key;
        }
      }
    }
    // TODO: method call and method returned from <clinit> should be null?
    // determine the resolved line associated with the event
    IResolvedLine newLine = lineNo > 0 ? mdg().dependenceMap().get(lineNo) : null;
    if (newLine == null)
    {
      /**
       * Process special cases of the method returned event-- the Java compiler sometimes introduces
       * synthetic and/or bridge calls to implement parts of its functionality.
       */
      if (event instanceof IMethodReturnedEvent)
      {
        final IMethodTerminatorEvent terminator = ((IMethodReturnedEvent) event).terminator();
        if (terminator == null
            || !(terminator.parent().caller() instanceof IMethodContourReference))
        {
          return line;
        }
        if (((IMethodContourReference) terminator.parent().caller()).contour() != parent.method())
        {
          return line;
        }
        if (newLine == null)
        {
          // System.err.println(String.format(
          // "OVERRIDE_METHOD_RETURN_FOR_NULL_LINE[%s]-- using current line.", event.toString()));
          return line;
        }
      }
      else if (!((IMethodCallEvent) event.parent()).target().isOutOfModel())
      {
        // System.err.println(String.format("Unmapped in-model line '%s' in event '%s'.",
        // event.line().toString(), event.toString()));
        newLine = parent.methodDeclLine();
      }
    }
    // boundary condition for step events
    if (event instanceof ILineStepEvent && newLine.lineNumber() != event.line().lineNumber())
    {
      // stepping at the NO-OP last line of this method
      if (parent.method().schema().lineTo() == event.line().lineNumber())
      {
        // System.err.println(String.format("Line step at the end of a method.\nEvent '%s (%s)'.",
        // event.toString(), event.line().toString()));
        line = null;
        resolutionCursor = -1;
        return line;
      }
    }
    // determine whether the line cursor changed
    if (newLine != line)
    {
      final IResolvedLine oldLine = line;
      // update outstanding local variable uses
      if (resolutionCursor != -1 && newLine.kind() != LK_CATCH)
      {
        processStatic(true);
      }
      // change the line
      line = newLine;
      // clear the catch flag early since this flag is dependent on the old line
      isCatchRelevant = false;
      // use the old line to update the set of chase lines
      if (line != null && oldLine != null && relevantLines.get(oldLine.lineNumber()))
      {
        relevantLineCompleted(oldLine);
      }
      // the program keeps track of the enum declaration
      if (line != null && line.kind() == LineKind.LK_ENUM_CONSTANT_DECLARATION)
      {
        parent.program().handleEnumDeclaration(
            (ITypeNode) line.definitions().get(0).data().parent());
      }
      // initialize the new line
      lineChanged(event, oldLine);
      // return the old line
      return oldLine;
    }
    // old and new line are the same
    return line;
  }
}