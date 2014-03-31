package edu.buffalo.cse.jive.core.ast;

import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import edu.buffalo.cse.jive.internal.core.ast.LocalAndAnonymousTypeVisitor;
import edu.buffalo.cse.jive.internal.core.ast.ResolvedFactory;
import edu.buffalo.cse.jive.internal.core.ast.VariableDeclarationVisitor;
import edu.buffalo.cse.jive.internal.core.ast.factory.ASTFactoryImpl;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IJiveProject;
import edu.buffalo.cse.jive.model.IModelCache;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IMethodDependenceGraph;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory.IStaticModelDelegate;

public enum ASTFactory
{
  INSTANCE;
  public static ASTVisitorFactory astVisitorFactory()
  {
    return INSTANCE.factory.astVisitorFactory();
  }

  public static IStaticModelDelegate createStaticModelDelegate(final IExecutionModel model,
      final IJiveProject project, final IModelCache modelCache,
      final IStaticModelDelegate downstream)
  {
    return INSTANCE.factory.createStaticModelDelegate(model, project, modelCache, downstream);
  }

  public static ElementKindFactory elementKindFactory()
  {
    return INSTANCE.factory.elementKindFactory();
  }

  public static ElementModifierFactory elementModifierFactory()
  {
    return INSTANCE.factory.elementModifierFactory();
  }

  public static ElementVisibilityFactory elementVisibilityFactory()
  {
    return INSTANCE.factory.elementVisibilityFactory();
  }

  private final ASTFactoryImpl factory = new ASTFactoryImpl();

  public interface ASTVisitorFactory
  {
    public IMethodDependenceGraph createDependenceGraph(ASTNode astNode,
        ResolvedFactory resolvedFactory);

    public LocalAndAnonymousTypeVisitor createLocalAndAnonymousTypeCollector();

    public VariableDeclarationVisitor createVariableDeclarationVisitor(
        final ResolvedFactory resolvedFactory);
  }

  public interface ElementKindFactory
  {
    public NodeKind createTypeKind(ITypeBinding binding);
  }

  public interface ElementModifierFactory
  {
    public Set<NodeModifier> createArgumentModifiers(final IVariableBinding binding);

    public Set<NodeModifier> createCatchVariableModifiers(final IVariableBinding binding);

    public Set<NodeModifier> createEnumConstantModifiers(final IVariableBinding binding);

    public Set<NodeModifier> createFieldInitializerModifiers(final int bindingModifiers);

    public Set<NodeModifier> createFieldModifiers(final IVariableBinding binding);

    public Set<NodeModifier> createLocalVariableModifiers(final IVariableBinding binding);

    public Set<NodeModifier> createMethodModifiers(final IMethodBinding binding);

    public Set<NodeModifier> createTypeInitializerModifiers(final int bindingModifiers);

    public Set<NodeModifier> createTypeModifiers(final ITypeBinding binding);
  }

  public interface ElementVisibilityFactory
  {
    public NodeVisibility createFieldVisibility(final IVariableBinding binding);

    public NodeVisibility createInitializerVisibility();

    public NodeVisibility createMethodVisibility(final IMethodBinding binding);

    public NodeVisibility createTypeVisibility(final ITypeBinding binding);
  }
}
