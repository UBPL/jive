package edu.buffalo.cse.jive.model.factory;

import java.util.Set;

import edu.buffalo.cse.jive.model.IStaticModel;

public interface IStaticModelFactory extends IStaticModel
{
  public long KNOWN_TYPE_COUNT = 66;

  public IFileNode createFileNode(String fileName, int lineFrom, int lineTo, NodeOrigin origin);

  /**
   * Manually create a java.lang.Thread type node. For now, this is exclusively used for Fiji.
   */
  public ITypeNode createThreadNode();

  public ITypeNode createTypeNode(String key, String name, INode parent, int lineFrom, int lineTo,
      NodeKind kind, NodeOrigin origin, Set<NodeModifier> modifiers, NodeVisibility visibility,
      ITypeNodeRef superNode, Set<ITypeNodeRef> superInterfaceNodes, IValue defaultValue);

  public IFileNode lookupFileNode(String fileName);

  public ILineValue lookupLine(String fileName, int lineNo);

  public IMethodNode lookupMethodNode(String methodKey);

  /**
   * Returns a non-null reference to a method node that can be resolved with the given key. The
   * reference may be an actual method node.
   */
  public IMethodNodeRef lookupMethodRef(String key);

  public ITypeNode lookupObjectType();

  public IRootNode lookupRoot();

  public ITypeNode lookupRPDLType();

  public IMethodNode lookupSnapshotRunMethod();

  public ITypeNode lookupSnapshotType();

  public ITypeNode lookupTypeNode(String key);

  public ITypeNode lookupTypeNodeByName(String name);

  /**
   * Returns a non-null reference to a type node that can be resolved with the given key. The
   * reference is an actual type node if it can be resolved immediately.
   */
  public ITypeNodeRef lookupTypeRef(String key);

  /**
   * Returns a non-null reference to a type node that can be resolved with the given key. The
   * reference is an actual type node if it can be resolved immediately.
   */
  public ITypeNodeRef lookupTypeRefByName(String name);

  public ITypeNode lookupVoidType();

  /**
   * Convenience method to translate signatures to type names.
   */
  public String signatureToTypeName(String signature);

  /**
   * Convenience method to translate type names to signatures.
   */
  public String typeNameToSignature(String name);

  /**
   * Resolves types and methods using a particular model provider, e.g., AST or JDI.
   */
  public interface IStaticModelDelegate
  {
    public IMethodNode resolveMethod(ITypeNode type, Object methodObject, String methodKey);

    public ITypeNodeRef resolveType(String typeKey, String typeName);

    public void setUpstream(IStaticModelDelegate upstream);
  }
}