package edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts;

import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;

/**
 * A message is an element in a Unified Modeling Language (UML) diagram that defines a specific kind
 * of communication between instances in an interaction. A message conveys information from one
 * instance, which is represented by a life line, to another instance in an interaction.
 * 
 * In UML, a lost message is a message that has a known sender but the receiver is not known. A
 * found message is a message that does not have a known sender but has a receiver. In Jive terms,
 * this amounts to having an out-of-model sender or receiver, respectively.
 * 
 * All messages in Jive's sequence diagram are asynchronous.
 */
public interface Message
{
  public MessageKind kind();

  public MessageOrientation orientation();

  public IInitiatorEvent source();

  public IInitiatorEvent target();

  /**
   * Message that connects an initiator event to the first data event in the initiated execution.
   */
  public interface InitiatorMessage extends Message
  {
  }

  public enum MessageKind
  {
    MK_DEFAULT("default"),
    MK_FOUND("found"),
    MK_FOUND_BROKEN("broken found"),
    MK_LOST("lost"),
    MK_LOST_BROKEN("broken lost");
    private final String messageName;

    private MessageKind(final String messageName)
    {
      this.messageName = messageName;
    }

    @Override
    public String toString()
    {
      return messageName;
    }
  }

  public enum MessageOrientation
  {
    MO_LEFT_TO_RIGHT,
    MO_RIGHT_TO_LEFT;
  }

  /**
   * Message that connects a terminator event to the first data event in the parent execution after
   * termination.
   */
  public interface TerminatorMessage extends Message
  {
  }
}