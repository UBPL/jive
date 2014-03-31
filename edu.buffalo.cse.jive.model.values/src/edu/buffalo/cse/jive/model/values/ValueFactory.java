package edu.buffalo.cse.jive.model.values;

import edu.buffalo.cse.jive.lib.StringTools;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IContourModel.IObjectContour;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.factory.IValueFactory;

public final class ValueFactory implements IValueFactory
{
  private static final long SYSTEM_THREAD_ID = -1000;
  private final IExecutionModel model;
  private final IThreadValue SYSTEM_THREAD;
  private final IFileValue UNAVAILABLE_FILE;
  private final ILineValue UNAVAILABLE_LINE;
  private final IValue VALUE_NULL;
  private final IValue VALUE_SYSTEM;
  private final IValue VALUE_UNINITIALIZED;

  public ValueFactory(final IExecutionModel model)
  {
    this.model = model;
    VALUE_NULL = new NullValue();
    VALUE_SYSTEM = new SystemCallerValue();
    VALUE_UNINITIALIZED = new UninitializedValue();
    SYSTEM_THREAD = createThread(ValueFactory.SYSTEM_THREAD_ID, "SYSTEM");
    UNAVAILABLE_FILE = createFile("unavailable");
    UNAVAILABLE_LINE = createLine(UNAVAILABLE_FILE.name(), -1);
  }

  @Override
  public ILineValue createLine(final String fileName, final int lineNumber)
  {
    if (model.store().lookupLineValue(fileName, lineNumber) == null)
    {
      model.store().storeLine(fileName, new LineValue(createFile(fileName), lineNumber));
    }
    return model.store().lookupLineValue(fileName, lineNumber);
  }

  @Override
  public IValue createNullValue()
  {
    return VALUE_NULL;
  }

  @Override
  public IValue createOutOfModelMethodKeyReference(final String description, final String methodKey)
  {
    // index by method signature
    if (model.store().lookupValue(methodKey) == null)
    {
      model.store().indexValue(methodKey, new OutOfModelMethodKeyReference(description, methodKey));
    }
    return model.store().lookupValue(methodKey);
  }

  @Override
  public IValue createOutOfModelMethodReference(final String description,
      final IMethodContour topInModelMethod)
  {
    return new OutOfModelMethodReference(description, topInModelMethod);
  }

  @Override
  public IValue createOutOfModelValue(final String description)
  {
    // index by description
    if (model.store().lookupValue(description) == null)
    {
      model.store().indexValue(description, new OutOfModelValue(description));
    }
    return model.store().lookupValue(description);
  }

  @Override
  public IValue createPrimitiveValue(final String value)
  {
    if (model.store().lookupValue(value) == null)
    {
      model.store().indexValue(value, new PrimitiveValue(value));
    }
    return model.store().lookupValue(value);
  }

  @Override
  public IValue createReference(final IContour contour)
  {
    if (model.store().lookupValue(contour) == null)
    {
      model.store().indexValue(
          contour,
          contour instanceof IMethodContour ? new MethodContourReference((IMethodContour) contour)
              : new ContourReference(contour));
    }
    return model.store().lookupValue(contour);
  }

  @Override
  public IValue createResolvedValue(final String value, final String typeName)
  {
    if (model.store().lookupValue(value) == null)
    {
      model.store().indexValue(value, new ResolvedValue(value, typeName));
    }
    return model.store().lookupValue(value);
  }

  @Override
  public IValue createSystemCaller()
  {
    return VALUE_SYSTEM;
  }

  @Override
  public IThreadValue createThread(final long uniqueId, final String name)
  {
    if (model.store().lookupThread(uniqueId) == null)
    {
      model.store().storeThread(uniqueId, new ThreadValue(uniqueId, name));
    }
    return model.store().lookupThread(uniqueId);
  }

  @Override
  public ILineValue createUnavailableLine()
  {
    return UNAVAILABLE_LINE;
  }

  @Override
  public IValue createUninitializedValue()
  {
    return VALUE_UNINITIALIZED;
  }

  @Override
  public IExecutionModel model()
  {
    return model;
  }

  private IFileValue createFile(final String fileName)
  {
    if (model.store().lookupFileValue(fileName) == null)
    {
      model.store().storeFileValue(fileName, new FileValue(fileName));
    }
    return model.store().lookupFileValue(fileName);
  }

  @Override
  public IThreadValue createSystemThread()
  {
    return SYSTEM_THREAD;
  }

  private abstract class AbstractValue
  {
    private final long id;

    private AbstractValue()
    {
      this.id = createId();
    }

    @Override
    public boolean equals(final Object other)
    {
      return (other instanceof AbstractValue) && ((AbstractValue) other).id == id;
    }

    public long id()
    {
      return this.id;
    }

    public boolean isContourReference()
    {
      return this instanceof ContourReference;
    }

    public boolean isGarbageCollected(final long eventId)
    {
      return false;
    }

    public boolean isInModel()
    {
      return this instanceof InModelValue;
    }

    public boolean isMethodContourReference()
    {
      return this instanceof MethodContourReference;
    }

    public boolean isNull()
    {
      return this == VALUE_NULL;
    }

    public boolean isOutOfModel()
    {
      return this instanceof OutOfModelValue;
    }

    public boolean isOutOfModelMethodKeyReference()
    {
      return this instanceof OutOfModelMethodKeyReference;
    }

    public boolean isOutOfModelMethodReference()
    {
      return this instanceof OutOfModelMethodReference;
    }

    public boolean isPrimitive()
    {
      return this instanceof PrimitiveValue;
    }

    public boolean isResolved()
    {
      return this instanceof ResolvedValue;
    }

    public boolean isUninitialized()
    {
      return this == VALUE_UNINITIALIZED;
    }

    public IExecutionModel model()
    {
      return model;
    }

    protected abstract long createId();
  }

  /**
   * Standard implementation of a reference value which checks for equality based on the string
   * representation of the reference value.
   */
  private class ContourReference extends AbstractValue implements IContourReference
  {
    private final IContour contour;

    private ContourReference(final IContour contour)
    {
      super();
      assert contour != null : "Cannot instantiate a ReferenceValue with a null identifier.";
      this.contour = contour;
    }

    @Override
    public IContour contour()
    {
      return contour;
    }

    @Override
    public boolean isGarbageCollected(final long eventId)
    {
      return contour instanceof IObjectContour
          && model.store().isGarbageCollected(((IObjectContour) contour).oid(), eventId);
    }

    @Override
    public ValueKind kind()
    {
      return ValueKind.IM_CONTOUR_REFERENCE;
    }

    @Override
    public String toString()
    {
      return contour.signature();
    }

    @Override
    public String value()
    {
      return contour.signature();
    }

    @Override
    protected long createId()
    {
      return model.store().storeValue(this);
    }
  }

  private final class FileValue extends AbstractValue implements IFileValue
  {
    private final String fileName;

    private FileValue(final String fileName)
    {
      this.fileName = fileName;
    }

    @Override
    public boolean equals(final Object other)
    {
      return (other instanceof FileValue) && ((FileValue) other).id() == super.id;
    }

    @Override
    public String name()
    {
      return fileName;
    }

    @Override
    public String toString()
    {
      return this.fileName;
    }

    @Override
    protected long createId()
    {
      return model.store().nextCount(IFileValue.class);
    }
  }

  /**
   * Standard implementation of a literal value which checks for equality based on the literal
   * representation of the value.
   */
  private abstract class InModelValue extends Value implements IInModelValue
  {
    protected InModelValue(final String value)
    {
      super(value);
    }
  }

  private final class LineValue extends AbstractValue implements ILineValue
  {
    private final IFileValue file;
    private final int lineNumber;

    private LineValue(final IFileValue file, final int lineNumber)
    {
      this.file = file;
      this.lineNumber = lineNumber;
    }

    @Override
    public boolean equals(final Object other)
    {
      return (other instanceof LineValue) && ((LineValue) other).id() == super.id;
    }

    @Override
    public IFileValue file()
    {
      return file;
    }

    @Override
    public int lineNumber()
    {
      return lineNumber;
    }

    @Override
    public String toString()
    {
      return StringTools.lineToString(this);
    }

    @Override
    protected long createId()
    {
      return model.store().nextCount(ILineValue.class);
    }
  }

  /**
   * Standard implementation of an in-model reference value that represents the target of a call.
   */
  private final class MethodContourReference extends ContourReference implements
      IMethodContourReference
  {
    MethodContourReference(final IMethodContour method)
    {
      super(method);
    }

    @Override
    public IMethodContour contour()
    {
      return (IMethodContour) super.contour();
    }

    @Override
    public ValueKind kind()
    {
      return ValueKind.IM_METHOD_CONTOUR_REFERENCE;
    }
  }

  private final class NullValue extends Value
  {
    private NullValue()
    {
      super("null");
    }

    @Override
    public ValueKind kind()
    {
      return ValueKind.NULL;
    }
  }

  /**
   * Standard implementation of an out-of-model target value that represents the target of a call.
   * The value is a method signature representing the out-of-model call.
   */
  private final class OutOfModelMethodKeyReference extends OutOfModelValue implements
      IOutOfModelMethodKeyReference
  {
    final String methodKey;

    OutOfModelMethodKeyReference(final String description, final String methodKey)
    {
      super(description);
      this.methodKey = methodKey;
    }

    @Override
    public String key()
    {
      return this.methodKey;
    }

    @Override
    public ValueKind kind()
    {
      return ValueKind.OM_METHOD_KEY_REFERENCE;
    }
  }

  /**
   * Standard implementation of an out-of-model target value that represents the target of a call.
   */
  private final class OutOfModelMethodReference extends OutOfModelValue implements
      IOutOfModelMethodReference
  {
    private final IMethodContour method;

    OutOfModelMethodReference(final String description, final IMethodContour method)
    {
      super(description);
      this.method = method;
    }

    @Override
    public ValueKind kind()
    {
      return ValueKind.OM_METHOD_REFERENCE;
    }

    @Override
    public IMethodContour method()
    {
      return this.method;
    }
  }

  /**
   * Standard implementation of an encoded value which checks for equality based on the encoding of
   * the value.
   */
  private class OutOfModelValue extends Value implements IOutOfModelValue
  {
    private OutOfModelValue(final String value)
    {
      super(value);
    }

    @Override
    public ValueKind kind()
    {
      return ValueKind.OUT_OF_MODEL;
    }
  }

  /**
   * Standard implementation of a primitive value.
   */
  private final class PrimitiveValue extends InModelValue
  {
    protected PrimitiveValue(final String value)
    {
      super(value);
    }

    @Override
    public ValueKind kind()
    {
      return ValueKind.IM_PRIMITIVE;
    }
  }

  /**
   * Standard implementation of an encoded value which checks for equality based on the encoding of
   * the value.
   */
  private final class ResolvedValue extends OutOfModelValue implements IResolvedValue
  {
    private final String typeName;

    private ResolvedValue(final String value, final String typeName)
    {
      super(value);
      this.typeName = typeName;
    }

    @Override
    public ValueKind kind()
    {
      return ValueKind.OM_RESOLVED;
    }

    @Override
    public String toString()
    {
      return typeName != null && typeName.length() > 0 ? String
          .format("%s (%s)", value(), typeName) : super.toString();
    }

    @Override
    public String typeName()
    {
      return this.typeName;
    }
  }

  private final class SystemCallerValue extends OutOfModelValue
  {
    private SystemCallerValue()
    {
      super("SYSTEM");
    }

    @Override
    public ValueKind kind()
    {
      return ValueKind.SYSTEM_CALLER;
    }
  }

  /**
   * Standard implementation of an encoded value which checks for equality based on the string
   * representation of the encoded value.
   */
  private final class ThreadValue implements IThreadValue
  {
    private final String name;
    private final long uniqueId;

    private ThreadValue(final long uniqueId, final String name)
    {
      assert name != null : "Cannot instantiate a ThreadId with a null name.";
      this.name = name;
      this.uniqueId = uniqueId;
    }

    @Override
    public boolean equals(final Object other)
    {
      return (other instanceof ThreadValue) && ((ThreadValue) other).id() == uniqueId;
    }

    @Override
    public long id()
    {
      return this.uniqueId;
    }

    @Override
    public IExecutionModel model()
    {
      return model;
    }

    @Override
    public String name()
    {
      return name;
    }

    @Override
    public String toString()
    {
      return String.format("%s (%d)", name, id());
    }
  }

  private final class UninitializedValue extends Value
  {
    private UninitializedValue()
    {
      super("<?>");
    }

    @Override
    public ValueKind kind()
    {
      return ValueKind.UNINITIALIZED;
    }
  }

  private abstract class Value extends AbstractValue implements IValue
  {
    private final String value;

    private Value(final String value)
    {
      assert value != null : "Cannot instantiate an IValue with a null value.";
      this.value = value;
    }

    @Override
    public String toString()
    {
      return this.value;
    }

    @Override
    public String value()
    {
      return this.value;
    }

    @Override
    protected long createId()
    {
      return model.store().storeValue(this);
    }
  }
}