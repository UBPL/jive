package edu.buffalo.cse.jive.internal.core.ast;

import static edu.buffalo.cse.jive.model.IStaticModel.NodeOrigin.NO_AST;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility.NV_LOCAL;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.core.ast.ASTFactory;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IModel;
import edu.buffalo.cse.jive.model.IStaticModel.IDataNode;
import edu.buffalo.cse.jive.model.IStaticModel.IFileNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.INode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNodeRef;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory;

/**
 * Factory that adapts the AST model to Jive's StaticModel model.
 */
class StaticModelFactoryAdapterForAST implements IModel
{
  private final StaticModelDelegateForAST delegate;
  private final IExecutionModel model;
  /**
   * Maps AST field and variable node ids to their declaration order within their declaring type or
   * method, respectively.
   */
  private final Map<IVariableBinding, Integer> varMapping;

  StaticModelFactoryAdapterForAST(final IExecutionModel model,
      final StaticModelDelegateForAST delegate)
  {
    this.model = model;
    this.delegate = delegate;
    this.varMapping = TypeTools.newHashMap();
  }

  @Override
  public IExecutionModel model()
  {
    return model;
  }

  private Set<ITypeNodeRef> createSuperInterfaces(final ITypeBinding binding)
  {
    final Set<ITypeNodeRef> superInterfaceNodes = new LinkedHashSet<ITypeNodeRef>();
    // process implemented interfaces
    for (final ITypeBinding superBinding : binding.getInterfaces())
    {
      superInterfaceNodes.add(lookupTypeRef(superBinding));
    }
    return superInterfaceNodes;
  }

  private int lineFromNumber(final ASTNode node)
  {
    int nodeStart = node.getStartPosition();
    /**
     * Skip the java documentation in body declaration nodes.
     */
    if (node instanceof AbstractTypeDeclaration)
    {
      nodeStart = ((AbstractTypeDeclaration) node).getName().getStartPosition();
    }
    else if (node instanceof EnumConstantDeclaration)
    {
      nodeStart = ((EnumConstantDeclaration) node).getName().getStartPosition();
    }
    else if (node instanceof Initializer)
    {
      nodeStart = ((Initializer) node).getBody().getStartPosition();
    }
    else if (node instanceof MethodDeclaration)
    {
      nodeStart = ((MethodDeclaration) node).getName().getStartPosition();
    }
    return ASTTools.lineNumber(node, nodeStart);
  }

  private int lineToNumber(final ASTNode node)
  {
    return ASTTools.lineNumber(node, node.getStartPosition() + node.getLength() - 1);
  }

  private IStaticModelFactory modelFactory()
  {
    return model.staticModelFactory();
  }

  private void putBinding(final IVariableBinding var, final int index)
  {
    varMapping.put(var.getVariableDeclaration(), index);
  }

  void createArgumentNode(final ResolvedVariableDeclaration rvd, final IMethodNode parent)
  {
    final SingleVariableDeclaration node = (SingleVariableDeclaration) rvd.node();
    final IVariableBinding binding = node.resolveBinding();
    // add the variable node to the tree
    final IDataNode data = parent.addDataMember(binding.getName(), parent.lineFrom(), parent
        .lineTo(), resolveTypeRef(binding.getType()), NO_AST, ASTFactory.elementModifierFactory()
        .createArgumentModifiers(binding), NV_LOCAL, model.valueFactory()
        .createUninitializedValue());
    putBinding(binding, data.index());
  }

  IDataNode createCatchVariableNode(final ResolvedVariableDeclaration rvd, final IMethodNode parent)
  {
    final SingleVariableDeclaration node = (SingleVariableDeclaration) rvd.node();
    final int lineFrom = rvd.lineFrom();
    final int lineTo = rvd.lineTo();
    final IVariableBinding binding = node.resolveBinding();
    // add the variable node to the tree
    final IDataNode data = parent.addDataMember(binding.getName(), lineFrom, lineTo,
        resolveTypeRef(binding.getType()), NO_AST, ASTFactory.elementModifierFactory()
            .createCatchVariableModifiers(binding), NV_LOCAL, model.valueFactory()
            .createUninitializedValue());
    putBinding(binding, data.index());
    return data;
  }

  IDataNode createEnhancedForVariableNode(final ResolvedVariableDeclaration rvd,
      final IMethodNode parent)
  {
    final SingleVariableDeclaration node = (SingleVariableDeclaration) rvd.node();
    final int lineFrom = rvd.lineFrom();
    final int lineTo = rvd.lineTo();
    final IVariableBinding binding = node.resolveBinding();
    // add the variable node to the tree
    final IDataNode data = parent.addDataMember(binding.getName(), lineFrom, lineTo,
        resolveTypeRef(binding.getType()), NO_AST, ASTFactory.elementModifierFactory()
            .createLocalVariableModifiers(binding), NV_LOCAL, model.valueFactory()
            .createUninitializedValue());
    putBinding(binding, data.index());
    return data;
  }

  INode createEnumConstantNode(final ResolvedVariableDeclaration rvd, final ITypeNode parent)
  {
    final EnumConstantDeclaration node = (EnumConstantDeclaration) rvd.node();
    final int lineFrom = rvd.lineFrom();
    final int lineTo = rvd.lineTo();
    final IVariableBinding binding = node.resolveVariable();
    // add the field node to the tree
    final IDataNode data = parent.addDataMember(binding.getName(), lineFrom, lineTo,
        resolveTypeRef(binding.getType()), NO_AST, ASTFactory.elementModifierFactory()
            .createEnumConstantModifiers(binding), ASTFactory.elementVisibilityFactory()
            .createFieldVisibility(binding), model.valueFactory().createNullValue());
    return data;
  }

  IMethodNode createEnumInitializerNode(final EnumConstantDeclaration node, final ITypeNode parent)
  {
    final int lineFrom = lineFromNumber(node);
    final int lineTo = lineToNumber(node);
    final int modifiers = node.resolveVariable().getModifiers();
    final String name = ASTTools.getDefault().methodName(node);
    final String key = ASTTools.getDefault().methodKey(node);
    // add the method node to the tree
    return parent.addMethodMember(key, name, lineFrom, lineTo, model.staticModelFactory()
        .lookupVoidType(), NO_AST, ASTFactory.elementModifierFactory()
        .createFieldInitializerModifiers(modifiers), ASTFactory.elementVisibilityFactory()
        .createInitializerVisibility(), Collections.<ITypeNodeRef> emptySet());
  }

  IMethodNode createFieldInitializerNode(final ASTNode node, final ITypeNode parent)
  {
    final VariableDeclarationFragment fragment = ASTTools.enclosingVariableDeclaration(node);
    final int lineFrom = lineFromNumber(node);
    final int lineTo = lineToNumber(node);
    final int modifiers = fragment.resolveBinding().getModifiers();
    final String name = ASTTools.getDefault().methodName(node);
    final String key = ASTTools.getDefault().methodKey(node);
    // add the method node to the tree
    return parent.addMethodMember(key, name, lineFrom, lineTo, model.staticModelFactory()
        .lookupVoidType(), NO_AST, ASTFactory.elementModifierFactory()
        .createFieldInitializerModifiers(modifiers), ASTFactory.elementVisibilityFactory()
        .createInitializerVisibility(), Collections.<ITypeNodeRef> emptySet());
  }

  INode createFieldNode(final ResolvedVariableDeclaration rvd, final ITypeNode parent)
  {
    final VariableDeclarationFragment node = (VariableDeclarationFragment) rvd.node();
    final int lineFrom = rvd.lineFrom();
    final int lineTo = rvd.lineTo();
    final IVariableBinding binding = node.resolveBinding();
    final Object constant = binding.getConstantValue();
    final IValue defaultValue;
    if (binding.getType().isPrimitive())
    {
      if (constant == null)
      {
        final ITypeNodeRef type = resolveType(binding.getType());
        defaultValue = type.node().defaultValue();
      }
      else
      {
        defaultValue = model.valueFactory().createPrimitiveValue(constant.toString());
      }
    }
    else if (constant instanceof String)
    {
      defaultValue = model.valueFactory()
          .createResolvedValue(String.format("\"%s\"", constant), "");
    }
    else
    {
      defaultValue = model.valueFactory().createNullValue();
    }
    // else {
    // defaultValue = model.valueFactory().createResolvedValue(constant.toString(), "");
    // }
    // add the field node to the tree
    final IDataNode data = parent.addDataMember(binding.getName(), lineFrom, lineTo,
        resolveTypeRef(binding.getType()), NO_AST, ASTFactory.elementModifierFactory()
            .createFieldModifiers(binding), ASTFactory.elementVisibilityFactory()
            .createFieldVisibility(binding), defaultValue);
    return data;
  }

  IMethodNode createMethodNode(final MethodDeclaration astNode, final ITypeNode parent)
  {
    final int lineFrom = lineFromNumber(astNode);
    final int lineTo = lineToNumber(astNode);
    final IMethodBinding binding = astNode.resolveBinding();
    final ITypeNodeRef methodReturnType = astNode.getReturnType2() == null ? null
        : resolveTypeRef(astNode.getReturnType2().resolveBinding());
    final Set<NodeModifier> modifiers = ASTFactory.elementModifierFactory().createMethodModifiers(
        binding);
    final String name = modifiers.contains(NodeModifier.NM_CONSTRUCTOR) ? parent.name() : ASTTools
        .getDefault().methodName(binding);
    final String key = ASTTools.getDefault().methodKey(binding);
    final Set<ITypeNodeRef> exceptions = TypeTools.newHashSet();
    for (final ITypeBinding excp : binding.getExceptionTypes())
    {
      final String typeKey = ASTTools.getDefault().typeKey(excp);
      exceptions.add(modelFactory().lookupTypeRef(typeKey));
    }
    // add the method node to the tree
    return parent.addMethodMember(key, name, lineFrom, lineTo, methodReturnType == null ? model
        .staticModelFactory().lookupVoidType() : methodReturnType, NO_AST, modifiers, ASTFactory
        .elementVisibilityFactory().createMethodVisibility(binding), exceptions);
  }

  IMethodNode createTypeInitializerNode(final Initializer astNode, final ITypeNode parent)
  {
    final int lineFrom = lineFromNumber(astNode);
    final int lineTo = lineToNumber(astNode);
    final String name = ASTTools.getDefault().methodName(astNode);
    final String key = ASTTools.getDefault().methodKey(astNode);
    // add the method node to the tree
    return parent.addMethodMember(key, name, lineFrom, lineTo, model.staticModelFactory()
        .lookupVoidType(), NO_AST, ASTFactory.elementModifierFactory()
        .createTypeInitializerModifiers(astNode.getModifiers()), ASTFactory
        .elementVisibilityFactory().createInitializerVisibility(), Collections
        .<ITypeNodeRef> emptySet());
  }

  ITypeNode createTypeNode(final ASTNode astNode, final ITypeBinding binding, final INode parent)
  {
    final int lineFrom = lineFromNumber(astNode);
    final int lineTo = lineToNumber(astNode);
    // process superclass
    final ITypeNodeRef superNode = resolveType(binding.getSuperclass());
    // process superinterfaces
    final Set<ITypeNodeRef> superInterfaceNodes = createSuperInterfaces(binding);
    final String name = ASTTools.getDefault().typeName(binding);
    final String key = ASTTools.getDefault().typeKey(binding);
    // add the type node to the tree
    return modelFactory().createTypeNode(key, name, parent, lineFrom, lineTo,
        ASTFactory.elementKindFactory().createTypeKind(binding), NO_AST,
        ASTFactory.elementModifierFactory().createTypeModifiers(binding),
        ASTFactory.elementVisibilityFactory().createTypeVisibility(binding), superNode,
        superInterfaceNodes, model.valueFactory().createNullValue());
  }

  IDataNode createVariableNode(final ResolvedVariableDeclaration rvd, final IMethodNode parent)
  {
    final VariableDeclarationFragment node = (VariableDeclarationFragment) rvd.node();
    final int lineFrom = rvd.lineFrom();
    final int lineTo = rvd.lineTo();
    final IVariableBinding binding = node.resolveBinding();
    // add the variable node to the tree
    final IDataNode data = parent.addDataMember(binding.getName(), lineFrom, lineTo,
        resolveTypeRef(binding.getType()), NO_AST, ASTFactory.elementModifierFactory()
            .createLocalVariableModifiers(binding), NV_LOCAL, model.valueFactory()
            .createUninitializedValue());
    putBinding(binding, data.index());
    return data;
  }

  Integer getBindingIndex(final IVariableBinding var)
  {
    return varMapping.get(var.getVariableDeclaration());
  }

  IFileNode lookupFile(final String name)
  {
    return modelFactory().lookupFileNode(name);
  }

  ITypeNode lookupTypeNode(final ITypeBinding binding)
  {
    return binding == null ? null : modelFactory().lookupTypeNode(
        ASTTools.getDefault().typeKey(binding));
  }

  ITypeNode lookupTypeNode(final String key)
  {
    return modelFactory().lookupTypeNode(key);
  }

  ITypeNode lookupTypeNodeByName(final String name)
  {
    return modelFactory().lookupTypeNodeByName(name);
  }

  ITypeNodeRef lookupTypeRef(final ITypeBinding binding)
  {
    return binding == null ? null : modelFactory().lookupTypeRef(
        ASTTools.getDefault().typeKey(binding));
  }

  ITypeNodeRef lookupTypeRef(final String typeKey)
  {
    return modelFactory().lookupTypeRef(typeKey);
  }

  ITypeNodeRef resolveType(final ITypeBinding binding)
  {
    if (binding == null)
    {
      return null;
    }
    final String typeKey = ASTTools.getDefault().typeKey(binding);
    final ITypeNode result = modelFactory().lookupTypeNode(typeKey);
    if (result == null)
    {
      delegate.resolveType(typeKey, ASTTools.getDefault().typeName(binding));
    }
    return modelFactory().lookupTypeRef(typeKey);
  }

  ITypeNodeRef resolveTypeRef(final ITypeBinding binding)
  {
    if (binding == null)
    {
      return null;
    }
    final String typeKey = ASTTools.getDefault().typeKey(binding);
    ITypeNodeRef result = modelFactory().lookupTypeNode(typeKey);
    if (result == null && binding.isInterface())
    {
      result = delegate.resolveInterface(binding);
    }
    if (result == null)
    {
      result = modelFactory().lookupTypeRef(typeKey);
    }
    return result;
  }
}