package edu.buffalo.cse.jive.model.factory;

import edu.buffalo.cse.jive.model.IStaticAnalysis;
import edu.buffalo.cse.jive.model.IStaticModel.IDataNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNodeRef;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;

public interface IStaticAnalysisFactory extends IStaticAnalysis
{
  /**
   * Determines whether the bridge key represents a method signature that matches the target key,
   * representing the static signature of the bridged method.
   */
  public boolean bridges(String bridgeKey, String targetKey);

  public IMethodDependenceGraph newMethodDependenceGraph(IResolvedLine parent);

  /**
   * Direct method reference. Method calls may be resolved during static analysis but may need to be
   * resolved later at run-time, if the type on which the method is declared has not been resolved
   * statically yet. In the latter case, a method key suffices to resolve the method dynamically.
   */
  public IResolvedCall newResolvedCall(IMethodNodeRef ref, int index, IResolvedNode qualifierOf,
      boolean isLHS, boolean isActual);

  /**
   * Direct reference to a local variable or field. Local variables are *always* available during
   * static analysis since they must be defined in the method body being analyzed when the resolved
   * data element is created. On the other hand, fields may need to be resolved later at run-time
   * for the same reason as method calls. The index represents the data node's index in the source
   * file in question. Since a method body is fully defined in a single file, no two data nodes in a
   * file can have the same index.
   */
  public IResolvedData newResolvedData(IDataNode data, int index, IResolvedNode qualifierOf,
      boolean isLHS, boolean isActual, boolean isDef);

  /**
   * Indirect field reference. Fields may be resolved but may need to be resolved dynamically if the
   * type on which the field is declared is not resolved at the time static analysis is performed. A
   * type key and field name suffice to resolve the field dynamically, since field names are unique
   * within a type.
   */
  public IResolvedData newResolvedData(String typeKey, String fieldName, int index,
      IResolvedNode qualifierOf, boolean isLHS, boolean isActual, boolean isDef);

  public IResolvedLine newResolvedLine(LineKind kind, int lineNumber, IResolvedLine parent);

  public IResolvedThis newResolvedThis(ITypeNode type, int index, IResolvedNode qualifierOf,
      boolean isLHS, boolean isActual);

  /**
   * Determines whether the method represented by the type and method nodes overrides the method
   * represented by the resolved call node.
   */
  public boolean overrides(ITypeNode typeNode, IMethodNode methodNode, IResolvedCall callNode);

  /**
   * Allows the type to post-process initializer mdgs so that they can be used from JDI. This method
   * performs two main tasks: merges all static initializers <clinit@...> into a single <clinit>
   * method. Then, it adds the mdg of each <init@...> method to every constructor node.
   */
  public void postProcessMDGs(ITypeNode type);
}