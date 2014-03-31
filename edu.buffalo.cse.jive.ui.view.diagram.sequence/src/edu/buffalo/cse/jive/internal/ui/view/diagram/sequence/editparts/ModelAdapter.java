package edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts;

import static edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.Message.MessageKind.MK_DEFAULT;
import static edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.Message.MessageKind.MK_FOUND;
import static edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.Message.MessageKind.MK_LOST;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.Message.InitiatorMessage;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.Message.MessageKind;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.Message.MessageOrientation;
import edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts.Message.TerminatorMessage;
import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IRealTimeEvent;
import edu.buffalo.cse.jive.model.IEventModel.IThreadStartEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;

/**
 * A component to manage the parent-child, source-connection, and target-connection relationships of
 * the sequence model. It traverses the model once to compute these relationships. Accessor methods
 * are provided so that the {@code EditPart}s corresponding to sequence model objects can obtain
 * their relationships.
 */
class ModelAdapter
{
  /**
   * Adapter used to determine when to prune traversals at collapsed executions.
   */
  private final UIAdapter uiAdapter;
  /**
   * A mapping from objects to messages for which it is a source.
   */
  private final Map<Object, List<Message>> sourceConnections;
  /**
   * A mapping from objects to messages for which it is a target.
   */
  private final Map<Object, List<Message>> targetConnections;
  /**
   * Life line model objects, which can be either {@code ContourId}s or {@code ThreadStartEvent}s.
   */
  private final Set<Object> lifelines;
  /**
   * State of the sequence diagram's thread activations.
   */
  private boolean showExpandedLifeLines;
  /**
   * State of the sequence diagram's thread activations.
   */
  private boolean showThreadActivations;
  private final Map<IInitiatorEvent, Message> initiatorCache;
  private final Map<IInitiatorEvent, Message> terminatorCache;
  private boolean isRealTime;
  private long systemStartTime;

  /**
   * Constructs a model manager using the supplied compaction manager.
   */
  ModelAdapter(final UIAdapter uiAdapter)
  {
    this.uiAdapter = uiAdapter;
    this.isRealTime = false;
    this.systemStartTime = -1;
    showThreadActivations = false;
    showExpandedLifeLines = false;
    lifelines = new LinkedHashSet<Object>();
    sourceConnections = new HashMap<Object, List<Message>>();
    targetConnections = new HashMap<Object, List<Message>>();
    initiatorCache = new HashMap<IInitiatorEvent, Message>();
    terminatorCache = new HashMap<IInitiatorEvent, Message>();
  }

  /**
   * Creates an initiator message for the broken in-model call to nestedExecution.
   */
  private void addBrokenInitiatorMessage(final IInitiatorEvent nestedExecution,
      final IInitiatorEvent execution)
  {
    final Message message = createInitiatorMessage(execution, nestedExecution,
        MessageKind.MK_FOUND_BROKEN, MessageOrientation.MO_LEFT_TO_RIGHT);
    // source is the nested execution's eventual in-model parent execution
    addSourceConnection(execution, message);
    // target is the nested execution
    addTargetConnection(nestedExecution, message);
  }

  /**
   * Creates a terminator message for the broken in-model return from nestedExecution.
   */
  private void addBrokenTerminatorMessage(final IInitiatorEvent nestedExecution,
      final IInitiatorEvent execution)
  {
    final Message message = createTerminatorMessage(execution, nestedExecution,
        MessageKind.MK_LOST_BROKEN, MessageOrientation.MO_RIGHT_TO_LEFT);
    // execution is the eventual in-model target for a return arrow from the nested execution
    if (nestedExecution.terminator() != null)
    {
      // source is the nested execution
      addSourceConnection(nestedExecution, message);
      // target is the execution
      addTargetConnection(execution, message);
    }
  }

  /**
   * Creates an initiator message for the found call to in-model nestedExecution.
   */
  private void addFoundInitiatorMessage(final IInitiatorEvent nestedExecution)
  {
    final Message message = createInitiatorMessage(nestedExecution, MK_FOUND);
    // source is the nested executions's life line
    addSourceConnection(showExpandedLifeLines ? nestedExecution.executionContext()
        : nestedExecution.executionContext().concreteContour(), message);
    // target is the nested executions
    addTargetConnection(nestedExecution, message);
  }

  /**
   * Creates an initiator message for the in-model call to nestedExecution.
   */
  private void addInModelInitiatorMessage(final IInitiatorEvent nestedExecution)
  {
    final Message message = createInitiatorMessage(nestedExecution, MK_DEFAULT);
    // source is the nested execution's parent execution
    addSourceConnection(nestedExecution.parent(), message);
    // target is the nested execution
    addTargetConnection(nestedExecution, message);
  }

  /**
   * Creates a terminator message for the in-model return from nestedExecution.
   */
  private void addInModelTerminatorMessage(final IInitiatorEvent nestedExecution)
  {
    final Message message = createTerminatorMessage(nestedExecution, MK_DEFAULT);
    // execution is a target for a return arrow from the nested execution
    if (nestedExecution.terminator() != null)
    {
      // source is the nested execution
      addSourceConnection(nestedExecution, message);
      // target is the execution
      addTargetConnection(nestedExecution.parent(), message);
    }
  }

  /**
   * Creates a terminator message for the lost return from the in-model nestedExecution.
   */
  private void addLostTerminatorMessage(final IInitiatorEvent nestedExecution)
  {
    final Message message = createTerminatorMessage(nestedExecution, MK_LOST);
    // execution is a target for a return arrow from the nested execution
    if (nestedExecution.terminator() != null)
    {
      // source is the nested execution
      addSourceConnection(nestedExecution, message);
      // target is the nested execution's life line
      addTargetConnection(showExpandedLifeLines ? nestedExecution.executionContext()
          : nestedExecution.executionContext().concreteContour(), message);
    }
  }

  private void addSourceConnection(final Object key, final Message message)
  {
    if (!sourceConnections.containsKey(key))
    {
      sourceConnections.put(key, new LinkedList<Message>());
    }
    sourceConnections.get(key).add(message);
  }

  private void addTargetConnection(final Object key, final Message message)
  {
    if (!targetConnections.containsKey(key))
    {
      targetConnections.put(key, new LinkedList<Message>());
    }
    targetConnections.get(key).add(message);
  }

  private Message createInitiatorMessage(final IInitiatorEvent caller,
      final IInitiatorEvent callee, final MessageKind kind, final MessageOrientation orientation)
  {
    if (!initiatorCache.containsKey(callee))
    {
      initiatorCache.put(callee, new InitiatorMessage()
        {
          @Override
          public MessageKind kind()
          {
            return kind;
          }

          @Override
          public MessageOrientation orientation()
          {
            return orientation;
          }

          @Override
          public IInitiatorEvent source()
          {
            return caller;
          }

          @Override
          public IInitiatorEvent target()
          {
            return callee;
          }

          @Override
          public String toString()
          {
            return String.format("Initiator (%s): [from %s, to %s", kind, source(), target());
          }
        });
    }
    return initiatorCache.get(callee);
  }

  private Message createInitiatorMessage(final IInitiatorEvent callee, final MessageKind kind)
  {
    return createInitiatorMessage(callee.parent(), callee, kind,
        MessageOrientation.MO_LEFT_TO_RIGHT);
  }

  private Message createTerminatorMessage(final IInitiatorEvent caller,
      final IInitiatorEvent callee, final MessageKind kind, final MessageOrientation orientation)
  {
    if (!terminatorCache.containsKey(callee))
    {
      terminatorCache.put(callee, new TerminatorMessage()
        {
          @Override
          public MessageKind kind()
          {
            return kind;
          }

          @Override
          public MessageOrientation orientation()
          {
            return orientation;
          }

          @Override
          public IInitiatorEvent source()
          {
            return callee;
          }

          @Override
          public IInitiatorEvent target()
          {
            return caller;
          }

          @Override
          public String toString()
          {
            return String.format("Terminator (%s): [from %s, to %s", kind, source(), target());
          }
        });
    }
    return terminatorCache.get(callee);
  }

  private Message createTerminatorMessage(final IInitiatorEvent callee, final MessageKind kind)
  {
    return createTerminatorMessage(callee.parent(), callee, kind,
        MessageOrientation.MO_RIGHT_TO_LEFT);
  }

  /**
   * Visits the execution and generates the appropriate source/target connections.
   */
  private void visitExecution(final IInitiatorEvent initiator)
  {
    // a life line must exist on which to place the execution.
    if (initiator instanceof IThreadStartEvent)
    {
      if (showThreadActivations)
      {
        lifelines.add(initiator.thread());
      }
    }
    else
    {
      lifelines.add(showExpandedLifeLines ? initiator.executionContext() : initiator
          .executionContext().concreteContour());
    }
    // process nested children only if the execution is not collapsed and it has children
    if (uiAdapter.isCollapsed(initiator) || initiator.nestedInitiators().isEmpty())
    {
      return;
    }
    // no out-of-model calls detected yet
    IInitiatorEvent modelCaller = null;
    IInitiatorEvent modelReturner = null;
    // nested executions are always in-model
    for (final IInitiatorEvent nestedExecution : initiator.nestedInitiators())
    {
      // skip marker nested executions
      if (nestedExecution == null)
      {
        // execution <-- + <-- modelReturner
        if (modelReturner != null)
        {
          // outstanding broken terminator
          addBrokenTerminatorMessage(modelReturner, initiator);
        }
        // caller for the next broken initiator
        modelCaller = initiator;
        modelReturner = null;
        continue;
      }
      // nested execution called from a thread activation that is not visible
      if (initiator instanceof IThreadStartEvent && !showThreadActivations)
      {
        // * --> nestedExecution
        addFoundInitiatorMessage(nestedExecution);
        // * <-- nestedExecution
        addLostTerminatorMessage(nestedExecution);
      }
      // nested execution called from out-of-model
      else if (initiator.eventId() != nestedExecution.parent().eventId())
      {
        // modelCaller --> + --> nestedExecution
        if (modelCaller != null)
        {
          // broken initiator for this nested execution
          addBrokenInitiatorMessage(nestedExecution, initiator);
        }
        else
        {
          // outstanding lost terminator
          addLostTerminatorMessage(modelReturner);
          // found initiator for this nested execution
          addFoundInitiatorMessage(nestedExecution);
        }
        modelCaller = null;
        modelReturner = nestedExecution;
      }
      // nested execution called from the in-model parent execution
      else
      {
        // execution --> nestedExecution
        addInModelInitiatorMessage(nestedExecution);
        // execution <-- nestedExecution
        addInModelTerminatorMessage(nestedExecution);
      }
      // recursively visit the nested execution
      visitExecution(nestedExecution);
    }
    // outstanding lost terminator: execution <-- + <-- modelReturner OR * <-- modelReturner
    if (modelReturner != null)
    {
      // terminator is the last event on the thread: use a simple lost message
      if (modelReturner.terminator() != null
          && initiator.lastChildEvent().eventId() <= modelReturner.terminator().eventId())
      {
        addLostTerminatorMessage(modelReturner);
      }
      // terminator is broken
      else if (modelReturner.terminator() != null)
      {
        addBrokenTerminatorMessage(modelReturner, initiator);
      }
    }
  }

  /**
   * Initiators for which the supplied model element is the source. Model is either an
   * InitiatorEvent representing an execution occurrence (for in-model and broken messages) or a
   * contour identifier representing a life line (for lost/found messages).
   */
  List<Message> getSourceMessages(final Object model)
  {
    if (sourceConnections.containsKey(model))
    {
      return sourceConnections.get(model);
    }
    return Collections.emptyList();
  }

  /**
   * Initiators for which the supplied model element is the target. Model is either an
   * InitiatorEvent representing an execution occurrence (for in-model and broken messages) or a
   * contour identifier representing a life line (for lost/found messages).
   */
  List<Message> getTargetMessages(final Object model)
  {
    if (targetConnections.containsKey(model))
    {
      return targetConnections.get(model);
    }
    return Collections.emptyList();
  }

  boolean isRealTime()
  {
    return this.isRealTime;
  }

  /**
   * Life lines to be placed on the sequence diagram.
   */
  List<Object> lifelines()
  {
    return new ArrayList<Object>(lifelines);
  }

  /**
   * Sets the state of the sequence diagram's lifelines. When lifelines are expanded, there is a
   * lifeline for each context of the object if a method was called on it. Otherwise, all method
   * activations are placed on the context of the object's concrete class.
   */
  void setExpandLifelines(final boolean expanded)
  {
    showExpandedLifeLines = expanded;
  }

  /**
   * Sets whether thread activations should be shown. Hidden thread activations are treated as
   * filtered methods.
   */
  void setShowThreadActivations(final boolean show)
  {
    showThreadActivations = show;
  }

  long systemStartTime()
  {
    return this.systemStartTime;
  }

  /**
   * Update all relationships for the model's objects.
   */
  void update(final IExecutionModel model, final Set<IThreadValue> hiddenThreads)
  {
    lifelines.clear();
    initiatorCache.clear();
    terminatorCache.clear();
    sourceConnections.clear();
    targetConnections.clear();
    isRealTime = model.lookupRoot() instanceof IRealTimeEvent;
    systemStartTime = isRealTime ? ((IRealTimeEvent) model.lookupRoot()).timestamp() : -1;
    /*
     * Traverse all threads in order to update these relationships.
     */
    for (final IThreadStartEvent thread : model.lookupThreads())
    {
      // threads that have no methods called are ignored
      if (thread.nestedInitiators().isEmpty())
      {
        continue;
      }
      // threads that are currently hidden are ignored
      if (hiddenThreads.contains(thread.thread()))
      {
        continue;
      }
      // traverse the execution
      visitExecution(thread);
    }
  }
}