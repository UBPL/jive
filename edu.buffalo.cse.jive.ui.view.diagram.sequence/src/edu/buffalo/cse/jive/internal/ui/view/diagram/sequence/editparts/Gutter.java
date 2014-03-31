package edu.buffalo.cse.jive.internal.ui.view.diagram.sequence.editparts;

import edu.buffalo.cse.jive.model.IEventModel.ISystemStartEvent;

/**
 * This is a dummy model element that contains a {@code SystemStartEvent} and is used to create a
 * gutter life line in the sequence diagram. This special life line is not visible and serves to
 * guarantee that the diagram has the proper height and, therefore, can always display the temporal
 * context line.
 */
public class Gutter
{
  private final ISystemStartEvent model;

  Gutter(final ISystemStartEvent model)
  {
    this.model = model;
  }

  ISystemStartEvent getSystemStart()
  {
    return model;
  }
}