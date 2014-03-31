package edu.buffalo.cse.jive.ui;

public interface IStepActionFactory
{
  public IUpdatableStepAction createPauseAction();

  public IUpdatableStepAction createRunBackwardAction();

  public IUpdatableStepAction createRunForwardAction();

  public IUpdatableStepAction createStepBackwardAction();

  public IUpdatableStepAction createStepForwardAction();
}