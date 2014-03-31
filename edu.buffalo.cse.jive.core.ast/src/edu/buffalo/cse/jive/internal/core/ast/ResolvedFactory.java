package edu.buffalo.cse.jive.internal.core.ast;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.buffalo.cse.jive.model.IStaticAnalysis.IMethodDependenceGraph;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedCall;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedData;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedLine;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedNode;
import edu.buffalo.cse.jive.model.IStaticAnalysis.LineKind;
import edu.buffalo.cse.jive.model.IStaticModel.IDataNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNodeRef;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.model.factory.IStaticAnalysisFactory;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory;

public final class ResolvedFactory
{
  private final StaticModelFactoryAdapterForAST factoryAdapter;

  ResolvedFactory(final StaticModelFactoryAdapterForAST factoryAdapter)
  {
    this.factoryAdapter = factoryAdapter;
  }

  public IMethodDependenceGraph newMethodDependenceGraph(final IResolvedLine parent)
  {
    return analysisFactory().newMethodDependenceGraph(parent);
  }

  public IResolvedCall newResolvedCall(final ASTNode node, final Object method,
      final int startPosition, final IResolvedNode qualifierOf, final boolean isLHS,
      final boolean isActual)
  {
    // make sure the return type has an id lower than the method id
    final String returnTypeKey = ASTTools.getDefault().returnTypeKey(method);
    modelFactory().lookupTypeRef(returnTypeKey);
    final String methodKey = ASTTools.getDefault().methodKey(method);
    final IMethodNodeRef methodNode = modelFactory().lookupMethodRef(methodKey);
    return analysisFactory().newResolvedCall(methodNode, startPosition, qualifierOf, isLHS,
        isActual);
  }

  public IResolvedLine newResolvedDependence(final LineKind kind, final int lineNumber,
      final IResolvedLine parent)
  {
    return analysisFactory().newResolvedLine(kind, lineNumber, parent);
  }

  public IResolvedNode newResolvedThisVariable(final ThisExpression node,
      final AbstractTypeDeclaration enclosing, final IResolvedNode qualifierOf,
      final boolean isLHS, final boolean isActual)
  {
    final ITypeBinding type = enclosing.resolveBinding();
    final String key = ASTTools.getDefault().typeKey(type);
    final ITypeNode typeNode = modelFactory().lookupTypeNode(key);
    return analysisFactory().newResolvedThis(typeNode, node.getStartPosition(), qualifierOf, isLHS,
        isActual);
  }

  public IResolvedNode newResolvedThisVariable(final ThisExpression node,
      final AnonymousClassDeclaration enclosing, final IResolvedNode qualifierOf,
      final boolean isLHS, final boolean isActual)
  {
    final ITypeBinding type = enclosing.resolveBinding();
    final String key = ASTTools.getDefault().typeKey(type);
    final ITypeNode typeNode = modelFactory().lookupTypeNode(key);
    return analysisFactory().newResolvedThis(typeNode, node.getStartPosition(), qualifierOf, isLHS,
        isActual);
  }

  /**
   * 
   * TODO: reduce the cyclomatic complexity.
   */
  public IResolvedData newResolvedVariable(final ASTNode node, final ASTNode parentNode,
      final IResolvedNode qualifierOf, final boolean isLHS, final boolean isActual,
      final boolean isDef)
  {
    // return new ResolvedVariable(node, parentNode);
    final IVariableBinding var = ASTTools.resolveVariableBinding(node);
    if (var.isField())
    {
      // the field must be resolved w.r.t. its declaring class, not the parentNode
      final String key;
      final ITypeBinding type = var.getDeclaringClass();
      if (type != null)
      {
        key = ASTTools.getDefault().typeKey(type);
      }
      // this node must be an array field
      else
      {
        if (node /* .getParent() */instanceof FieldAccess)
        {
          final FieldAccess exp = (FieldAccess) node; // .getParent();
          key = ASTTools.getDefault().typeKey(exp.getExpression().resolveTypeBinding());
        }
        else if (node /* .getParent() */instanceof SuperFieldAccess)
        {
          final SuperFieldAccess exp = (SuperFieldAccess) node; // .getParent();
          key = ASTTools.getDefault().typeKey(exp.getQualifier().resolveTypeBinding());
        }
        else
        {
          if (!(node.getParent() instanceof QualifiedName))
          {
            throw new IllegalArgumentException(
                "Array fields should have a field or qualified name parent.");
          }
          final QualifiedName exp = (QualifiedName) node.getParent();
          key = ASTTools.getDefault().typeKey(exp.getQualifier().resolveTypeBinding());
        }
      }
      final ITypeNode typeNode = modelFactory().lookupTypeNode(key);
      // type node not statically processed yet
      if (typeNode == null)
      {
        // lazy resolved variable reference-- the type key and field name resolve the field
        return analysisFactory().newResolvedData(key, var.getName(), node.getStartPosition(),
            qualifierOf, isLHS, isActual, isDef);
      }
      else
      {
        // if type == null, this is the only array data member, namely, length
        final IDataNode dataNode = type == null ? typeNode.dataMembers().get(-1) : typeNode
            .lookupDataMember(var.getName());
        // due to circular or forward references, the type is resolved but the field is not
        if (dataNode == null)
        {
          return analysisFactory().newResolvedData(key, var.getName(), node.getStartPosition(),
              qualifierOf, isLHS, isActual, isDef);
        }
        return analysisFactory().newResolvedData(dataNode, node.getStartPosition(), qualifierOf,
            isLHS, isActual, isDef);
      }
    }
    // variable declared within a method, type initializer, or field/variable initializer expression
    final String key = ASTTools.getDefault().methodKey(parentNode);
    IMethodNode methodNode = modelFactory().lookupMethodNode(key);
    // a local variable can only be uncovered during the static processing of a method
    if (methodNode == null)
    {
      throw new IllegalStateException(
          "All method nodes must be resolved when processing local variables!");
    }
    final Integer bindingIndex = factoryAdapter.getBindingIndex(var);
    IDataNode dataNode = methodNode.dataMembers().get(bindingIndex);
    // supports final local variables declared by a method and used by a local class
    while (dataNode == null && methodNode != null
        && methodNode.parent().modifiers().contains(NodeModifier.NM_LOCAL))
    {
      methodNode = (IMethodNode) methodNode.parent().parent();
      dataNode = methodNode.dataMembers().get(bindingIndex);
    }
    return analysisFactory().newResolvedData(dataNode, node.getStartPosition(), qualifierOf, isLHS,
        isActual, isDef);
  }

  public ResolvedVariableDeclaration newVariableDeclaration(
      final SingleVariableDeclaration exception, final CatchClause node)
  {
    return new ResolvedVariableDeclaration(exception, node);
  }

  public ResolvedVariableDeclaration newVariableDeclaration(
      final SingleVariableDeclaration parameter, final EnhancedForStatement node)
  {
    return new ResolvedVariableDeclaration(parameter, node);
  }

  public ResolvedVariableDeclaration newVariableDeclaration(
      final VariableDeclarationFragment fragment, final ASTNode parent)
  {
    return new ResolvedVariableDeclaration(fragment, parent);
  }

  /**
   * Always called from the statement visitor, therefore, all variables are top-level.
   */
  public IResolvedData resolveExpression(final Expression expression, final ASTNode node,
      final IResolvedNode qualifierOf, final boolean isLHS, final boolean isDef)
  {
    IResolvedData rv = null;
    if (expression instanceof Name)
    {
      final IBinding binding = ((Name) expression).resolveBinding();
      if (binding instanceof IVariableBinding)
      {
        rv = newResolvedVariable(expression, ASTTools.enclosingScope(node), qualifierOf, isLHS,
            false, isDef);
      }
    }
    else if (expression instanceof FieldAccess)
    {
      rv = newResolvedVariable(expression, ASTTools.enclosingScope(node), qualifierOf, isLHS,
          false, isDef);
    }
    else if (expression instanceof SuperFieldAccess)
    {
      rv = newResolvedVariable(expression, ASTTools.enclosingScope(node), qualifierOf, isLHS,
          false, isDef);
    }
    return rv;
  }

  private IStaticAnalysisFactory analysisFactory()
  {
    return factoryAdapter.model().staticAnalysisFactory();
  }

  private IStaticModelFactory modelFactory()
  {
    return factoryAdapter.model().staticModelFactory();
  }
}