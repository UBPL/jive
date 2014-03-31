package edu.buffalo.cse.jive.model.factory;

import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IModel;

public interface IValueFactory extends IModel
{
  public ILineValue createLine(String fileName, int lineNumber);

  public IValue createNullValue();

  public IValue createOutOfModelMethodKeyReference(String description, String methodKey);

  public IValue createOutOfModelMethodReference(String description, IMethodContour topInModelMethod);

  public IValue createOutOfModelValue(String description);

  public IValue createPrimitiveValue(String value);

  public IValue createReference(IContour contour);

  public IValue createResolvedValue(String value, String typeName);

  public IValue createSystemCaller();

  public IThreadValue createSystemThread();

  public IThreadValue createThread(long uniqueId, String name);

  public ILineValue createUnavailableLine();

  public IValue createUninitializedValue();
}