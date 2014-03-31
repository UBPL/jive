package edu.buffalo.cse.jive.internal.ui;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.gef.EditPartViewer;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.ui.IDiagramOutputActionFactory;
import edu.buffalo.cse.jive.ui.IJiveView;
import edu.buffalo.cse.jive.ui.IJumpMenuManager;
import edu.buffalo.cse.jive.ui.ISliceMenuManager;
import edu.buffalo.cse.jive.ui.ISourceLookupFacility;
import edu.buffalo.cse.jive.ui.IStepAction;
import edu.buffalo.cse.jive.ui.IStepActionFactory;
import edu.buffalo.cse.jive.ui.IStepManager;
import edu.buffalo.cse.jive.ui.IThreadColorManager;
import edu.buffalo.cse.jive.ui.UICoreFactory;

public final class UICoreFactoryImpl implements UICoreFactory
{
  private DiagramOutputActionFactory diagramOutputActionFactory;
  private SourceLookupFacility sourceLookupFacility;
  private StepManager stepManager;
  private ThreadColorManager threadColorManager;

  public UICoreFactoryImpl()
  {
    this.stepManager = new StepManager();
    this.sourceLookupFacility = new SourceLookupFacility();
    launchManager().addLaunchListener(this.sourceLookupFacility);
    stepManager().addStepListener(this.sourceLookupFacility);
  }

  @Override
  public IJumpMenuManager createJumpMenuManager(final EditPartViewer viewer)
  {
    return new JumpMenuManager(viewer);
  }

  @Override
  public IStepAction createRunToEventAction(final IJiveDebugTarget target, final IJiveEvent event)
  {
    return new RunToEventAction(target, event);
  }

  @Override
  public ISliceMenuManager createSliceMenuManager()
  {
    return new SliceMenuManager();
  }

  @Override
  public IStepActionFactory createStepActionFactory(final IJiveView view)
  {
    return new StepActionFactory(view);
  }

  @Override
  public IDiagramOutputActionFactory diagramOutputFactory()
  {
    if (this.diagramOutputActionFactory == null)
    {
      this.diagramOutputActionFactory = new DiagramOutputActionFactory();
    }
    return this.diagramOutputActionFactory;
  }

  @Override
  public void dispose()
  {
    // release the diagram output factory
    diagramOutputActionFactory = null;
    // clean up and release the source lookup facility
    if (this.sourceLookupFacility != null)
    {
      launchManager().removeLaunchListener(this.sourceLookupFacility);
      stepManager().removeStepListener(this.sourceLookupFacility);
      sourceLookupFacility = null;
    }
    // clean up and release the step manager
    if (this.stepManager != null)
    {
      stepManager.dispose();
      stepManager = null;
    }
    // clean up and release the thread color manager
    if (this.threadColorManager != null)
    {
      threadColorManager.dispose();
      threadColorManager = null;
    }
  }

  @Override
  public ISourceLookupFacility sourceLookupFacility()
  {
    return this.sourceLookupFacility;
  }

  @Override
  public IStepManager stepManager()
  {
    return this.stepManager;
  }

  @Override
  public IThreadColorManager threadColorManager()
  {
    if (this.threadColorManager == null)
    {
      this.threadColorManager = new ThreadColorManager();
    }
    return this.threadColorManager;
  }

  private ILaunchManager launchManager()
  {
    return DebugPlugin.getDefault().getLaunchManager();
  }
}