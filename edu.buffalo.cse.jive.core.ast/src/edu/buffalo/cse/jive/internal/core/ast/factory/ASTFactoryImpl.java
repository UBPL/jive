package edu.buffalo.cse.jive.internal.core.ast.factory;

import static edu.buffalo.cse.jive.model.IStaticModel.NodeKind.NK_CLASS;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeKind.NK_ENUM;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeKind.NK_INTERFACE;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_ABSTRACT;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_ANONYMOUS;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_ARGUMENT;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_CATCH_VARIABLE;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_COMPILE_TIME_FINAL;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_CONSTRUCTOR;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_ENUM_CONSTANT;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_FIELD_INITIALIZER;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_FINAL;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_LOCAL;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_NATIVE;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_STATIC;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_TRANSIENT;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_TYPE_INITIALIZER;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_VOLATILE;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility.NV_LOCAL;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility.NV_PACKAGE;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility.NV_PRIVATE;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility.NV_PROTECTED;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility.NV_PUBLIC;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

import edu.buffalo.cse.jive.core.ast.ASTFactory.ASTVisitorFactory;
import edu.buffalo.cse.jive.core.ast.ASTFactory.ElementKindFactory;
import edu.buffalo.cse.jive.core.ast.ASTFactory.ElementModifierFactory;
import edu.buffalo.cse.jive.core.ast.ASTFactory.ElementVisibilityFactory;
import edu.buffalo.cse.jive.internal.core.ast.LocalAndAnonymousTypeVisitor;
import edu.buffalo.cse.jive.internal.core.ast.ResolvedFactory;
import edu.buffalo.cse.jive.internal.core.ast.StatementVisitor;
import edu.buffalo.cse.jive.internal.core.ast.StaticModelDelegateForAST;
import edu.buffalo.cse.jive.internal.core.ast.VariableDeclarationVisitor;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IJiveProject;
import edu.buffalo.cse.jive.model.IModelCache;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IMethodDependenceGraph;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory.IStaticModelDelegate;

public class ASTFactoryImpl
{
  private final ASTVisitorFactory astVisitorFactory = new ASTVisitorFactoryImpl();
  private final ElementKindFactory elmentKindFactory = new ElementKindFactoryImpl();
  private final ElementModifierFactory elmentModifierFactory = new ElementModifierFactoryImpl();
  private final ElementVisibilityFactory elmentVisibilityFactory = new ElementVisibilityFactoryImpl();

  public ASTVisitorFactory astVisitorFactory()
  {
    return this.astVisitorFactory;
  }

  public IStaticModelDelegate createStaticModelDelegate(final IExecutionModel model,
      final IJiveProject project, final IModelCache modelCache,
      final IStaticModelDelegate downstream)
  {
    return new StaticModelDelegateForAST(model, project, modelCache, downstream);
  }

  public ElementKindFactory elementKindFactory()
  {
    return this.elmentKindFactory;
  }

  public ElementModifierFactory elementModifierFactory()
  {
    return this.elmentModifierFactory;
  }

  public ElementVisibilityFactory elementVisibilityFactory()
  {
    return this.elmentVisibilityFactory;
  }

  private class ASTVisitorFactoryImpl implements ASTVisitorFactory
  {
    @Override
    public IMethodDependenceGraph createDependenceGraph(final ASTNode astNode,
        final ResolvedFactory resolvedFactory)
    {
      final StatementVisitor v = createStatementVisitor(resolvedFactory);
      astNode.accept(v);
      return v.mdg();
    }

    @Override
    public LocalAndAnonymousTypeVisitor createLocalAndAnonymousTypeCollector()
    {
      return new LocalAndAnonymousTypeVisitor();
    }

    @Override
    public VariableDeclarationVisitor createVariableDeclarationVisitor(
        final ResolvedFactory resolvedFactory)
    {
      return new VariableDeclarationVisitor(resolvedFactory);
    }

    private StatementVisitor createStatementVisitor(final ResolvedFactory resolvedFactory)
    {
      return new StatementVisitor(resolvedFactory);
    }
  }

  private class ElementKindFactoryImpl implements ElementKindFactory
  {
    @Override
    public NodeKind createTypeKind(final ITypeBinding binding)
    {
      return binding.isEnum() ? NK_ENUM : binding.isInterface() ? NK_INTERFACE : NK_CLASS;
    }
  }

  private class ElementModifierFactoryImpl implements ElementModifierFactory
  {
    @Override
    public Set<NodeModifier> createArgumentModifiers(final IVariableBinding binding)
    {
      final int bindingModifiers = binding.getModifiers();
      final Set<NodeModifier> modifiers = new LinkedHashSet<NodeModifier>();
      modifiers.add(NM_ARGUMENT);
      if (Modifier.isFinal(bindingModifiers))
      {
        modifiers.add(NM_FINAL);
      }
      return modifiers;
    }

    @Override
    public Set<NodeModifier> createCatchVariableModifiers(final IVariableBinding binding)
    {
      final int bindingModifiers = binding.getModifiers();
      final Set<NodeModifier> modifiers = new LinkedHashSet<NodeModifier>();
      modifiers.add(NM_CATCH_VARIABLE);
      if (Modifier.isFinal(bindingModifiers))
      {
        modifiers.add(NM_FINAL);
      }
      return modifiers;
    }

    @Override
    public Set<NodeModifier> createEnumConstantModifiers(final IVariableBinding binding)
    {
      final int bindingModifiers = binding.getModifiers();
      final Set<NodeModifier> modifiers = new LinkedHashSet<NodeModifier>();
      modifiers.add(NM_ENUM_CONSTANT);
      if (Modifier.isFinal(bindingModifiers))
      {
        modifiers.add(NM_FINAL);
      }
      if (Modifier.isStatic(bindingModifiers))
      {
        modifiers.add(NM_STATIC);
      }
      if (Modifier.isTransient(bindingModifiers))
      {
        modifiers.add(NM_TRANSIENT);
      }
      if (Modifier.isVolatile(bindingModifiers))
      {
        modifiers.add(NM_VOLATILE);
      }
      return modifiers;
    }

    @Override
    public Set<NodeModifier> createFieldInitializerModifiers(final int bindingModifiers)
    {
      final Set<NodeModifier> modifiers = new LinkedHashSet<NodeModifier>();
      modifiers.add(NM_FIELD_INITIALIZER);
      if (Modifier.isStatic(bindingModifiers))
      {
        modifiers.add(NM_STATIC);
      }
      return modifiers;
    }

    @Override
    public Set<NodeModifier> createFieldModifiers(final IVariableBinding binding)
    {
      final int bindingModifiers = binding.getModifiers();
      final Set<NodeModifier> modifiers = new LinkedHashSet<NodeModifier>();
      if (Modifier.isFinal(bindingModifiers))
      {
        modifiers.add(NM_FINAL);
        if (binding.getConstantValue() != null)
        {
          modifiers.add(NM_COMPILE_TIME_FINAL);
        }
      }
      if (Modifier.isStatic(bindingModifiers))
      {
        modifiers.add(NM_STATIC);
      }
      if (Modifier.isTransient(bindingModifiers))
      {
        modifiers.add(NM_TRANSIENT);
      }
      if (Modifier.isVolatile(bindingModifiers))
      {
        modifiers.add(NM_VOLATILE);
      }
      return modifiers;
    }

    @Override
    public Set<NodeModifier> createLocalVariableModifiers(final IVariableBinding binding)
    {
      final int bindingModifiers = binding.getModifiers();
      final Set<NodeModifier> modifiers = new LinkedHashSet<NodeModifier>();
      if (Modifier.isFinal(bindingModifiers))
      {
        modifiers.add(NM_FINAL);
      }
      return modifiers;
    }

    @Override
    public Set<NodeModifier> createMethodModifiers(final IMethodBinding binding)
    {
      final int bindingModifiers = binding.getModifiers();
      final Set<NodeModifier> modifiers = new LinkedHashSet<NodeModifier>();
      if (Modifier.isAbstract(bindingModifiers))
      {
        modifiers.add(NM_ABSTRACT);
      }
      if (binding.isConstructor())
      {
        modifiers.add(NM_CONSTRUCTOR);
      }
      if (Modifier.isFinal(bindingModifiers))
      {
        modifiers.add(NM_FINAL);
      }
      if (Modifier.isNative(bindingModifiers))
      {
        modifiers.add(NM_NATIVE);
      }
      if (Modifier.isStatic(bindingModifiers))
      {
        modifiers.add(NM_STATIC);
      }
      if (Modifier.isSynchronized(bindingModifiers))
      {
        modifiers.add(NodeModifier.NM_SYNCHRONIZED);
      }
      return modifiers;
    }

    @Override
    public Set<NodeModifier> createTypeInitializerModifiers(final int bindingModifiers)
    {
      final Set<NodeModifier> modifiers = new LinkedHashSet<NodeModifier>();
      modifiers.add(NM_TYPE_INITIALIZER);
      if (Modifier.isStatic(bindingModifiers))
      {
        modifiers.add(NM_STATIC);
      }
      return modifiers;
    }

    @Override
    public Set<NodeModifier> createTypeModifiers(final ITypeBinding binding)
    {
      final int bindingModifiers = binding.getModifiers();
      final Set<NodeModifier> modifiers = new LinkedHashSet<NodeModifier>();
      if (Modifier.isAbstract(bindingModifiers))
      {
        modifiers.add(NM_ABSTRACT);
      }
      if (binding.isAnonymous())
      {
        modifiers.add(NM_ANONYMOUS);
      }
      if (Modifier.isFinal(bindingModifiers))
      {
        modifiers.add(NM_FINAL);
      }
      if (binding.isLocal())
      {
        modifiers.add(NM_LOCAL);
      }
      if (Modifier.isStatic(bindingModifiers))
      {
        modifiers.add(NM_STATIC);
      }
      return modifiers;
    }
  }

  private class ElementVisibilityFactoryImpl implements ElementVisibilityFactory
  {
    @Override
    public NodeVisibility createFieldVisibility(final IVariableBinding binding)
    {
      final int bindingModifiers = binding.getModifiers();
      if (Modifier.isPrivate(bindingModifiers))
      {
        return NV_PRIVATE;
      }
      if (Modifier.isProtected(bindingModifiers))
      {
        return NV_PROTECTED;
      }
      if (Modifier.isPublic(bindingModifiers))
      {
        return NV_PUBLIC;
      }
      return NV_PACKAGE;
    }

    @Override
    public NodeVisibility createInitializerVisibility()
    {
      return NV_LOCAL;
    }

    @Override
    public NodeVisibility createMethodVisibility(final IMethodBinding binding)
    {
      final int bindingModifiers = binding.getModifiers();
      if (Modifier.isPrivate(bindingModifiers))
      {
        return NV_PRIVATE;
      }
      if (Modifier.isProtected(bindingModifiers))
      {
        return NV_PROTECTED;
      }
      if (Modifier.isPublic(bindingModifiers))
      {
        return NV_PUBLIC;
      }
      return NV_PACKAGE;
    }

    @Override
    public NodeVisibility createTypeVisibility(final ITypeBinding binding)
    {
      final int bindingModifiers = binding.getModifiers();
      if (binding.isLocal())
      {
        return NV_LOCAL;
      }
      if (Modifier.isPrivate(bindingModifiers))
      {
        return NV_PRIVATE;
      }
      if (Modifier.isProtected(bindingModifiers))
      {
        return NV_PROTECTED;
      }
      if (Modifier.isPublic(bindingModifiers))
      {
        return NV_PUBLIC;
      }
      return NV_PACKAGE;
    }
  }
}
