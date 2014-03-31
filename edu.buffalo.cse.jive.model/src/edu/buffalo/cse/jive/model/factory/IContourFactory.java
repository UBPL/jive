package edu.buffalo.cse.jive.model.factory;

import edu.buffalo.cse.jive.model.IContourModel;

public interface IContourFactory extends IContourModel
{
  public IObjectContour lookupInstanceContour(String typeName, long oid);

  public IContextContour lookupStaticContour(String typeName);

  public IObjectContour retrieveInstanceContour(String typeName, long oid);

  public IContextContour retrieveStaticContour(String typeName);
}