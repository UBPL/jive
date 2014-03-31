package edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts;

import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.ISystemStartEvent;
import edu.buffalo.cse.jive.model.IEventModel.IThreadStartEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;

/**
 * Class providing support for all collapse/expand actions and maintaining information about
 * executions that are collapsed and hidden as a result of such collapsing.
 */
class UIAdapter
{
  private final BitSet hiddenInitiators;
  private final BitSet collapsedRoots;
  private IExecutionModel model;

  UIAdapter()
  {
    this.hiddenInitiators = new BitSet();
    this.collapsedRoots = new BitSet();
  }

  public void setModel(final IExecutionModel model)
  {
    this.model = model;
  }

  private Set<IInitiatorEvent> collectPreserves(final IThreadValue threadId,
      final List<IInitiatorEvent> executions)
  {
    final Set<IInitiatorEvent> preserve = new HashSet<IInitiatorEvent>();
    for (final IInitiatorEvent initiator : executions)
    {
      if (!initiator.thread().equals(threadId))
      {
        throw new IllegalArgumentException(
            "Cannot collapse between executions from different threads.");
      }
      IInitiatorEvent parent = initiator;
      while (parent != null && !(parent instanceof IThreadStartEvent) && !preserve.contains(parent))
      {
        preserve.add(parent);
        parent = parent.parent();
      }
    }
    return preserve;
  }

  /**
   * Recursively hides all initiators nested under this execution.
   */
  private void hideNestedInitiators(final IInitiatorEvent execution)
  {
    for (final IInitiatorEvent initiator : execution.nestedInitiators())
    {
      // marker initiator
      if (initiator == null)
      {
        continue;
      }
      hiddenInitiators.set((int) initiator.eventId());
      if (!isCollapsed(initiator))
      {
        hideNestedInitiators(initiator);
      }
    }
  }

  /**
   * Recursively shows all initiators nested under this execution. Preserves previously collapsed
   * substructure.
   */
  private void showNestedInitiators(final IInitiatorEvent execution)
  {
    for (final IInitiatorEvent initiator : execution.nestedInitiators())
    {
      // marker initiator
      if (initiator == null)
      {
        continue;
      }
      hiddenInitiators.clear((int) initiator.eventId());
      if (!isCollapsed(initiator))
      {
        showNestedInitiators(initiator);
      }
    }
  }

  /**
   * Conceptual FoldBetween algorithm.
   */
  void collapseBetween(final List<IInitiatorEvent> executions)
  {
    if (executions == null || executions.isEmpty())
    {
      return;
    }
    final IThreadValue threadId = executions.get(0).thread();
    final Set<IInitiatorEvent> preserve = collectPreserves(threadId, executions);
    final IExecutionModel model = executions.get(0).model();
    model.readLock();
    try
    {
      final Stack<IInitiatorEvent> roots = new Stack<IInitiatorEvent>();
      roots.push(model.lookupThread(threadId));
      while (!roots.isEmpty())
      {
        for (final IInitiatorEvent initiator : roots.pop().nestedInitiators())
        {
          // marker initiator
          if (initiator == null)
          {
            continue;
          }
          // no collapsing
          if (isCollapsed(initiator))
          {
            continue;
          }
          // collapse
          if (preserve.contains(initiator))
          {
            boolean collapseHere = true;
            for (final IInitiatorEvent nested : initiator.nestedInitiators())
            {
              if (preserve.contains(nested))
              {
                collapseHere = false;
                break;
              }
            }
            // no nested initiator must be preserved
            if (collapseHere)
            {
              collapseChildren(initiator);
            }
            // some nested initiator must be preserved
            else
            {
              roots.push(initiator);
            }
          }
          // no need to preserve this initiator
          else if (initiator.terminator() != null)
          {
            collapseExecution(initiator);
          }
        }
      }
    }
    finally
    {
      model.readUnlock();
    }
  }

  void collapseByTime(final IInitiatorEvent execution, final boolean isBefore)
  {
    execution.model().readLock();
    try
    {
      // this execution is not complete, so there is nothing to collapse after
      if (!isBefore && execution.terminator() == null)
      {
        return;
      }
      IInitiatorEvent root = execution.model().lookupThread(execution.thread());
      IInitiatorEvent nextRoot = null;
      while (root != null)
      {
        for (final IInitiatorEvent initiator : root.nestedInitiators())
        {
          // marker initiator
          if (initiator == null)
          {
            continue;
          }
          // BEFORE: call started and completed before this execution started
          if (isBefore && initiator.terminator() != null
              && initiator.terminator().compareTo(execution) < 0)
          {
            collapseExecution(initiator);
          }
          // AFTER: call started and completed after this execution completed
          else if (!isBefore && initiator.terminator() != null
              && initiator.compareTo(execution.terminator()) > 0)
          {
            collapseExecution(initiator);
          }
          // ancestor call
          else if (!isCollapsed(initiator) && initiator.compareTo(execution) < 0
              && initiator.lastChildEvent() != null
              && initiator.lastChildEvent().compareTo(execution) > 0)
          {
            nextRoot = initiator;
          }
        }
        root = nextRoot;
        nextRoot = null;
      }
    }
    finally
    {
      execution.model().readUnlock();
    }
  }

  /**
   * Conceptual FoldChildren algorithm.
   */
  void collapseChildren(final IInitiatorEvent execution)
  {
    execution.model().readLock();
    try
    {
      // this execution is collapsed-- cannot collapse children
      if (isCollapsed(execution))
      {
        return;
      }
      // this execution is in a collapsed path-- cannot collapse children
      if (isCollapsedPath(execution.parent()))
      {
        return;
      }
      for (final IInitiatorEvent initiator : execution.nestedInitiators())
      {
        // marker initiator
        if (initiator == null)
        {
          continue;
        }
        collapseExecution(initiator);
      }
    }
    finally
    {
      execution.model().readUnlock();
    }
  }

  void collapseExecution(final IInitiatorEvent execution)
  {
    // this execution has no nested initiators
    if (!execution.hasChildren())
    {
      return;
    }
    // this execution is not expanded-- nothing to expand
    if (isCollapsed(execution))
    {
      return;
    }
    // this execution is in a collapsed path-- nothing to collapse
    if (isCollapsedPath(execution.parent()))
    {
      return;
    }
    execution.model().readLock();
    try
    {
      collapsedRoots.set((int) execution.eventId());
      hideNestedInitiators(execution);
    }
    finally
    {
      execution.model().readUnlock();
    }
  }

  void expandAll(final List<IInitiatorEvent> executions)
  {
    model.readLock();
    try
    {
      for (final IInitiatorEvent initiator : executions)
      {
        expandExecution(initiator);
      }
    }
    finally
    {
      model.readUnlock();
    }
  }

  void expandByTime(final IInitiatorEvent execution, final boolean isBefore)
  {
    execution.model().readLock();
    try
    {
      // this execution is not complete, so there is nothing to expand after
      if (!isBefore && execution.terminator() == null)
      {
        return;
      }
      IInitiatorEvent root = execution.model().lookupThread(execution.thread());
      IInitiatorEvent nextRoot = null;
      while (root != null)
      {
        for (final IInitiatorEvent initiator : root.nestedInitiators())
        {
          // marker initiator
          if (initiator == null)
          {
            continue;
          }
          // BEFORE: call started and completed before this execution started
          if (isBefore && initiator.terminator() != null
              && initiator.terminator().compareTo(execution) < 0)
          {
            expandExecution(initiator);
          }
          // AFTER: call started and completed after this execution completed
          else if (!isBefore && initiator.terminator() != null
              && initiator.compareTo(execution.terminator()) > 0)
          {
            expandExecution(initiator);
          }
          // ancestor call
          else if (!isCollapsed(initiator) && initiator.compareTo(execution) < 0
              && initiator.lastChildEvent() != null
              && initiator.lastChildEvent().compareTo(execution) > 0)
          {
            nextRoot = initiator;
          }
        }
        root = nextRoot;
        nextRoot = null;
      }
    }
    finally
    {
      execution.model().readUnlock();
    }
  }

  void expandChildren(final IInitiatorEvent execution)
  {
    execution.model().readLock();
    try
    {
      // this execution is collapsed-- cannot expand children
      if (isCollapsed(execution))
      {
        return;
      }
      // this execution is in a collapsed path-- cannot expand children
      if (isCollapsedPath(execution.parent()))
      {
        return;
      }
      for (final IInitiatorEvent initiator : execution.nestedInitiators())
      {
        // marker initiator
        if (initiator == null)
        {
          continue;
        }
        expandExecution(initiator);
      }
    }
    finally
    {
      execution.model().readUnlock();
    }
  }

  void expandExecution(final IInitiatorEvent execution)
  {
    // this execution is not collapsed-- nothing to expand
    if (!isCollapsed(execution))
    {
      return;
    }
    // this execution is in a collapsed path-- nothing to expand
    if (isCollapsedPath(execution.parent()))
    {
      return;
    }
    execution.model().readLock();
    try
    {
      collapsedRoots.clear((int) execution.eventId());
      showNestedInitiators(execution);
    }
    finally
    {
      execution.model().readUnlock();
    }
  }

  void expandPath(final IInitiatorEvent execution)
  {
    if (execution == null)
    {
      return;
    }
    // expand the path leading to this execution before
    if (execution.parent() != null)
    {
      expandPath(execution.parent());
    }
    // expand the execution
    expandExecution(execution);
  }

  boolean isCollapsed(final IInitiatorEvent execution)
  {
    return collapsedRoots.get((int) execution.eventId());
  }

  /**
   * true if if any execution in the path leading to this execution is collapsed
   */
  boolean isCollapsedPath(final IInitiatorEvent execution)
  {
    if (execution != null)
    {
      if (execution instanceof ISystemStartEvent)
      {
        return false;
      }
      return isCollapsed(execution) || isCollapsedPath(execution.parent());
    }
    return false;
  }

  List<? extends IInitiatorEvent> visibleInitiators(final IThreadValue thread)
  {
    final IExecutionModel model = thread.model();
    final List<IInitiatorEvent> threadActivations = new LinkedList<IInitiatorEvent>();
    threadActivations.add(model.lookupThread(thread));
    return visibleInitiators(threadActivations);
  }

  List<? extends IInitiatorEvent> visibleInitiators(final List<? extends IInitiatorEvent> initiators)
  {
    final List<IInitiatorEvent> result = new LinkedList<IInitiatorEvent>();
    for (final IInitiatorEvent initiator : initiators)
    {
      // marker initiator
      if (initiator == null)
      {
        continue;
      }
      if (!hiddenInitiators.get((int) initiator.eventId()))
      {
        result.add(initiator);
      }
    }
    return result;
  }
}