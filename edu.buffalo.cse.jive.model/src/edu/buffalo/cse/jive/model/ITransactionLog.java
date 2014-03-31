package edu.buffalo.cse.jive.model;

import java.util.List;

import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IModel.IValue;

/**
 * Implementation detail-- push down to more specialized package.
 */
public interface ITransactionLog
{
  public void atomicEmpty(IJiveEvent event);

  public void atomicMethodEnter(IJiveEvent event, IValue caller, IMethodContour contour);

  public void atomicMethodExit(IJiveEvent event, IMethodContour contour);

  public void atomicObjectDestroy(IJiveEvent event, IContextContour contour);

  public void atomicObjectNew(IJiveEvent event, IContextContour newContour);

  public void atomicRemoveContour(IJiveEvent event, IMethodContour contour);

  public void atomicTypeLoad(IJiveEvent event, IContextContour newContour);

  public void atomicValueSet(IJiveEvent event, IContour contour, IContourMember member,
      IValue newValue);

  public List<IContour> getChildren(IContour abstractContour);
}
