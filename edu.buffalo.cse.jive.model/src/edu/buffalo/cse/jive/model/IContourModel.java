package edu.buffalo.cse.jive.model;

import java.util.Collection;
import java.util.List;

import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IStaticModel.IDataNode;
import edu.buffalo.cse.jive.model.IStaticModel.IEnvironmentNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;

public interface IContourModel extends IModel
{
  public enum ContourKind
  {
    CK_INSTANCE,
    CK_INSTANCE_VIRTUAL,
    CK_METHOD,
    CK_STATIC;
  }

  public interface IContextContour extends IContour
  {
    /**
     * Lookup method (TODO: push to store).
     * 
     * Returns the concrete contour associated with this contour. For static contours this is always
     * the contour itself (no actual creation); for instance contours this is the most specific
     * contour-- i.e., the one for which the program actually invoked {@code new ...}.
     */
    public IContextContour concreteContour();

    /**
     * Factory method (TODO: push to store).
     * 
     */
    public IMethodContour createMethodContour(IMethodNode node, IThreadValue threadId);

    /**
     * Query method.
     */
    public boolean isStatic();

    /**
     * Query method indicating if this is a virtual contour-- true only if this contour represents
     * the innermost contour of an instance.
     * 
     * TODO: push to store
     */
    public boolean isVirtual();

    /**
     * Lookup method (TODO: push to store).
     * 
     * Events that initiated executions within the context of this contour.
     */
    public List<IMethodCallEvent> nestedInitiators();

    /**
     * Data method.
     */
    @Override
    public IContextContour parent();

    /**
     * Description of this contour's structure.
     */
    @Override
    public ITypeNode schema();
  }

  /**
   * Represents the run-time state of a program environment. It may contain members, each of which
   * has a state of its own. It may also be nested within an enclosing environment.
   * <p>
   * The possible types of contours, and their type designation, are:
   * <ul>
   * <li>virtual instance (instance)
   * <li>concrete instance (instance)
   * <li>static (static)
   * <li>static method (method)
   * <li>instance method (method)
   * <li>static inner virtual instance (instance)
   * <li>static inner concrete instance (instance)
   * <li>static inner (static)
   * <li>inner virtual instance (instance)
   * <li>inner concrete instance (instance)
   * </ul>
   * Whether a contour is "inner" or not is revealed by its placement in the contour model.
   */
  public interface IContour extends IModel
  {
    /**
     * Lookup method (TODO: push to store).
     * 
     * Contours nested within this contour. This is a dynamic property in that it may change
     * depending on the state of the execution model.
     */
    public List<IContour> children();

    /**
     * Data method.
     * 
     * Unique contour identifier.
     */
    public long id();

    /**
     * Data method.
     * 
     * Kind of this contour.
     */
    public ContourKind kind();

    /**
     * Lookup method (TODO: push to store).
     * 
     * Looks up a member with the given schema within this contour and its enclosing contours. This
     * method can be used for retrieving members from any non-array static and instance contours.
     * For array contours, use the look up based on the member's index.
     */
    public IContourMember lookupMember(IDataNode schema);

    /**
     * Lookup method (TODO: push to store).
     * 
     * Looks up a member with the given index within this contour. This method should be used only
     * for retrieving an array cell member from a particular array contour.
     */
    public IContourMember lookupMember(int index);

    /**
     * Lookup method (TODO: push to store).
     * 
     * Looks up a member with the given name within this contour and its enclosing contours. This
     * method should be used only for retrieving a field member from an instance or static contour.
     * If the contour structure includes shadowed fields with the given name, the field introduced
     * last (i.e., in the most specific contour) is returned.
     */
    public IContourMember lookupMember(String name);

    /**
     * Lookup method (TODO: push to store).
     * 
     * Members of this contour.
     */
    public Collection<IContourMember> members();

    /**
     * Data method.
     * 
     * Ordinal identifier of this contour. All contours of a given schema have unique ordinal
     * identifiers.
     */
    public long ordinalId();

    /**
     * Data method.
     * 
     * Contour that contains this contour. For static and instance contours, this is a reference to
     * the immediate ancestor of this contour. In the case of top-level contours (e.g., an object
     * instance contour), its parent is null. For method contours, this is the execution context
     * associated with the method.
     */
    public IContour parent();

    /**
     * Data method.
     * 
     * Description of this contour's structure.
     */
    public IEnvironmentNode schema();

    /**
     * Derived data method (TODO: push to store).
     * 
     * String signature of this contour, unique within the execution model.
     */
    public String signature();

    /**
     * Business action method (TODO: push to store).
     */
    public IContourTokens tokenize();
  }

  /**
   * Run-time realization of a contour member. A member has a schema that minimally provides the
   * name and type of the member. It also has an associated run-time value which, by design, cannot
   * be null. If the actual run-time value is null, {@code value()} should return a NullValue
   * instance; if it is unknown (for instance, because it has not been initialized), then value
   * should return an UninitializedValue instance.
   * 
   * Variable members occur within method contours while field members occur within context
   * contours. These members are associated with the corresponding variable or field schema.
   */
  public interface IContourMember
  {
    /**
     * Data method.
     */
    public String name();

    /**
     * Data method.
     */
    public IDataNode schema();

    /**
     * Data method.
     */
    public IValue value();
  }

  /**
   * Helper class.
   * 
   * Collects information about a contour.
   */
  public interface IContourTokens
  {
    /**
     * Data method.
     * 
     * Call number of the {@code #methodName} associated with the contour Id.
     */
    public String callNumber();

    /**
     * Data method.
     * 
     * Fully-qualified class name associated with the contour id.
     */
    public String className();

    /**
     * Data method.
     * 
     * Instance number associated with the contour Id. This number is unique per class. Returns
     * <code>null</code> if the contour Id references a static contour or a static method contour.
     */
    public String instanceNumber();

    /**
     * Data method.
     * 
     * Method name associated with the {@code ContourID}, or <code>null</code> if the contour Id
     * references a static or an instance contour.
     */
    public String methodName();
  }

  /**
   * A method contour, representing a method's activation.
   */
  public interface IMethodContour extends IContour
  {
    /**
     * Lookup method (TODO: push to store).
     * 
     * Lookups a variable instance within the members of this contour.
     */
    @Override
    public IContourMember lookupMember(int index);

    /**
     * Lookup method (TODO: push to store).
     * 
     * Lookups a variable instance within the members of this contour.
     */
    public IContourMember lookupMember(final String varName, final int lineNumber);

    /**
     * Lookup method (TODO: push to store).
     */
    public IContourMember lookupResultMember();

    /**
     * Lookup method (TODO: push to store).
     */
    public IContourMember lookupRPDLMember();

    /**
     * Data method.
     */
    @Override
    public IContextContour parent();

    /**
     * Data method.
     */
    @Override
    public IMethodNode schema();

    /**
     * Data method.
     * 
     * Thread in which the activation occurred.
     */
    public IThreadValue thread();
  }

  public interface IObjectContour extends IContextContour
  {
    /**
     * Temporal property that determines whether the contour is garbage collected at the time
     * corresponding to the given eventId.
     */
    // public boolean isGarbageCollected(long eventId);
    /**
     * Data method.
     * 
     * Object contours have a unique object identifier-- more importantly, all contours that make up
     * an object share the same object identifier.
     */
    public long oid();
  }
}
