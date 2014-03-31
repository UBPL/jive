package edu.buffalo.cse.jive.ui;

import org.eclipse.gef.EditPartViewer;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;

public interface UICoreFactory
{
  public IJumpMenuManager createJumpMenuManager(EditPartViewer viewer);

  public IStepAction createRunToEventAction(IJiveDebugTarget target, IJiveEvent event);

  public ISliceMenuManager createSliceMenuManager();

  public IStepActionFactory createStepActionFactory(IJiveView view);

  public IDiagramOutputActionFactory diagramOutputFactory();

  public void dispose();

  public ISourceLookupFacility sourceLookupFacility();

  public IStepManager stepManager();

  public IThreadColorManager threadColorManager();
}
