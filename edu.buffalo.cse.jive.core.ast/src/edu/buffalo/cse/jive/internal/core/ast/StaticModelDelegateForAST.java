package edu.buffalo.cse.jive.internal.core.ast;

import static edu.buffalo.cse.jive.model.IStaticModel.NodeOrigin.NO_AST;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.buffalo.cse.jive.lib.TypeTools;
import edu.buffalo.cse.jive.core.ast.ASTFactory;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IJiveProject;
import edu.buffalo.cse.jive.model.IModelCache;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IMethodDependenceGraph;
import edu.buffalo.cse.jive.model.IStaticModel.IFileNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.INode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNodeRef;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory.IStaticModelDelegate;

public class StaticModelDelegateForAST implements IStaticModelDelegate
{
  private final List<String> cache;
  private CompilationUnit cu;
  private final IStaticModelDelegate downstream;
  private final StaticModelFactoryAdapterForAST factoryAdapter;
  private final IStaticModelFactory modelFactory;
  private final IJiveProject project;
  private final ResolvedFactory resolvedFactory;
  private final Map<IFile, ICompilationUnit> units;
  private final IModelCache modelCache;

  public StaticModelDelegateForAST(final IExecutionModel model, final IJiveProject project,
      final IModelCache modelCache, final IStaticModelDelegate downstream)
  {
    this.cache = TypeTools.newArrayList();
    this.downstream = downstream;
    this.modelCache = modelCache;
    this.modelFactory = model.staticModelFactory();
    this.project = project;
    this.units = new HashMap<IFile, ICompilationUnit>();
    this.factoryAdapter = new StaticModelFactoryAdapterForAST(model, this);
    this.resolvedFactory = new ResolvedFactory(factoryAdapter);
    downstream.setUpstream(this);
  }

  /**
   * AST resolves method nodes during type node resolution, therefore, any method that remains
   * unresolved should be resolved by downstream.
   */
  @Override
  public IMethodNode resolveMethod(final ITypeNode type, final Object methodObject,
      final String methodKey)
  {
    return downstream.resolveMethod(type, methodObject, methodKey);
  }

  @Override
  public ITypeNodeRef resolveType(final String typeKey, final String typeName)
  {
    // short-circuit when the type is known
    final ITypeNode result = factoryAdapter.lookupTypeNode(typeKey);
    if (result != null)
    {
      return result;
    }
    // check for an array type-- make sure the component type is created
    if (typeKey.startsWith("["))
    {
      final String componentKey = typeKey.substring(1);
      final String componentName = typeName.substring(0, typeName.length() - 2);
      resolveType(componentKey, componentName);
    }
    // if the AST approach does not succeed, try JDI
    if (resolveAST(typeKey, typeName) == null)
    {
      downstream.resolveType(typeKey, typeName);
    }
    /**
     * By looking up the reference instead of the node, we guarantee a non-null result. That is, if
     * a type node exists by now, it is returned. If not, a reference is created and returned. We
     * note that {@code processType} must always try to create a type node when one does not exist.
     * A reference is returned as a last resource, only when the type cannot be resolved using the
     * AST (i.e., the source is not available) *AND* it has not been loaded by the VM yet. This
     * means that a type can fail to be resolved during the AST processing but can be later resolved
     * using JDI.
     */
    return factoryAdapter.lookupTypeRef(typeKey);
  }

  @Override
  public void setUpstream(final IStaticModelDelegate upstream)
  {
    // no support for upstream delegates
  }

  private IMethodDependenceGraph collectDependences(final ASTNode astNode)
  {
    // create a method dependence graph for this node (method, constructor, type/field initializer)
    return ASTFactory.astVisitorFactory().createDependenceGraph(astNode, resolvedFactory);
  }

  /**
   * A variable node created from a method declaration's arguments found during visitation of a
   * method.
   */
  private void createArgumentNode(final ResolvedVariableDeclaration rvd, final IMethodNode parent)
  {
    // add the argument node to the tree
    factoryAdapter.createArgumentNode(rvd, parent);
    // arguments do not have initializers
  }

  private CompilationUnit createAST(final IFile file)
  {
    if (!units.containsKey(file))
    {
      units.put(file, JavaCore.createCompilationUnitFrom(file));
    }
    final ICompilationUnit icu = units.get(file);
    final ASTParser parser = ASTParser.newParser(AST.JLS8);
    parser.setKind(ASTParser.K_COMPILATION_UNIT);
    parser.setSource(icu);
    parser.setResolveBindings(true);
    return (CompilationUnit) parser.createAST(null);
  }

  /**
   * A variable node created from a catch clause's exception found during visitation of a method or
   * class initializer.
   */
  private void createCatchVariableNode(final ResolvedVariableDeclaration rvd,
      final IMethodNode parent)
  {
    // add the variable node to the tree
    factoryAdapter.createCatchVariableNode(rvd, parent);
    // catch variables do not have initializers
  }

  /**
   * A variable node created from an enhanced for's variable parameter found during visitation of a
   * method or class initializer.
   */
  private void createEnhancedForVariableNode(final ResolvedVariableDeclaration rvd,
      final IMethodNode parent)
  {
    // add the variable node to the tree
    factoryAdapter.createEnhancedForVariableNode(rvd, parent);
    // retrieve the node
    final SingleVariableDeclaration node = (SingleVariableDeclaration) rvd.node();
    // process local and anonymous types in this variable's initializer
    if (node.getInitializer() != null)
    {
      processLocalAndAnonymousTypes(node.getInitializer(), parent);
    }
  }

  /**
   * A field node created from a field declaration found during visitation of a type.
   */
  private INode createFieldNode(final ResolvedVariableDeclaration rvd, final ITypeNode parent)
  {
    // add the field node to the tree
    final INode fd = factoryAdapter.createFieldNode(rvd, parent);
    // retrieve the node
    final VariableDeclarationFragment node = (VariableDeclarationFragment) rvd.node();
    // process local and anonymous types in this field's initializer
    if (node.getInitializer() != null)
    {
      processLocalAndAnonymousTypes(node.getInitializer(), parent);
    }
    return fd;
  }

  /**
   * A method node created from a method declaration found during visitation of a type.
   */
  private IMethodNode createMethodNode(final MethodDeclaration astNode, final int index,
      final ITypeNode typeNode)
  {
    final IMethodNode methodNode = factoryAdapter.createMethodNode(astNode, typeNode);
    // process arguments
    processMethodArguments(astNode, methodNode);
    // process local variables in this method
    processLocalVariableDeclarations(astNode, methodNode);
    // process local and anonymous types in this method
    processLocalAndAnonymousTypes(astNode, methodNode);
    return methodNode;
  }

  /**
   * A method node created from an initializer body found during visitation of a type.
   */
  private IMethodNode createTypeInitializerNode(final Initializer astNode, final int index,
      final ITypeNode parent)
  {
    final IMethodNode methodNode = factoryAdapter.createTypeInitializerNode(astNode, parent);
    // process local and anonymous types in this initializer
    processLocalAndAnonymousTypes(astNode, methodNode);
    // process local variables in this initializer
    processLocalVariableDeclarations(astNode, methodNode);
    return methodNode;
  }

  /**
   * A type node created from a type declaration or anonymous instantiation found during visitation
   * of a file (top-level type), type (member types), method or class initializer body (local type
   * or anonymous type), or field declaration (anonymous class assignment as the field initializer).
   */
  private void createTypeNode(final ASTNode astNode, final INode parent)
  {
    ITypeBinding binding = null;
    if (astNode instanceof AbstractTypeDeclaration)
    {
      final AbstractTypeDeclaration atd = (AbstractTypeDeclaration) astNode;
      binding = atd.resolveBinding();
      if (factoryAdapter.lookupTypeNode(binding) != null)
      {
        return;
      }
    }
    else if (astNode instanceof AnonymousClassDeclaration)
    {
      final AnonymousClassDeclaration acd = (AnonymousClassDeclaration) astNode;
      binding = acd.resolveBinding();
      if (factoryAdapter.lookupTypeNode(binding) != null)
      {
        return;
      }
    }
    else
    {
      throw new IllegalArgumentException(
          "A type node can only be created from one of the following AST nodes: AbstractTypeDeclaration or AnonymousClassDeclaration, however, found: "
              + astNode + ".");
    }
    final String typeName = ASTTools.getDefault().typeName(binding);
    if (modelCache != null && !modelCache.acceptsClass(typeName))
    {
      System.err.println("Omitting out-of-model source type '" + typeName + "' from AST.");
      return;
    }
    // add the type node to the tree
    final ITypeNode typeNode = factoryAdapter.createTypeNode(astNode, binding, parent);
    // collect body declaration
    final List<?> bodyDeclarations = astNode instanceof AnonymousClassDeclaration ? ((AnonymousClassDeclaration) astNode)
        .bodyDeclarations() : ((AbstractTypeDeclaration) astNode).bodyDeclarations();
    // partitions of the body declarations
    final List<FieldDeclaration> fields = new LinkedList<FieldDeclaration>();
    final List<EnumConstantDeclaration> enums = new LinkedList<EnumConstantDeclaration>();
    final Map<ASTNode, Integer> methods = new LinkedHashMap<ASTNode, Integer>();
    final List<AbstractTypeDeclaration> types = new LinkedList<AbstractTypeDeclaration>();
    // collect field declarations, method declarations (and initializers), and type declarations
    partitionBodyDeclarations(bodyDeclarations, fields, methods, types);
    // collect enum constant declarations
    if (astNode instanceof EnumDeclaration)
    {
      final EnumDeclaration ed = (EnumDeclaration) astNode;
      for (final Object ec : ed.enumConstants())
      {
        enums.add((EnumConstantDeclaration) ec);
      }
    }
    // process MDGs after all types declared in the source have been processed
    processBodyDeclarations(enums, fields, methods, types, astNode, typeNode);
    // fix <init> and <clinit> nodes
    factoryAdapter.model().staticAnalysisFactory().postProcessMDGs(typeNode);
  }

  /**
   * A variable node created from a local variable declaration found during visitation of a method
   * or class initializer.
   */
  private void createVariableNode(final ResolvedVariableDeclaration rvd, final IMethodNode parent)
  {
    // add the variable node to the tree
    factoryAdapter.createVariableNode(rvd, parent);
    // retrieve the node
    final VariableDeclarationFragment node = (VariableDeclarationFragment) rvd.node();
    // process local and anonymous types in this variable's initializer
    if (node.getInitializer() != null)
    {
      processLocalAndAnonymousTypes(node.getInitializer(), parent);
    }
  }

  private void partitionBodyDeclarations(final List<?> bodyDeclarations,
      final List<FieldDeclaration> fields, final Map<ASTNode, Integer> methods,
      final List<AbstractTypeDeclaration> types)
  {
    // indexes for methods and initializers
    int index = 0;
    // collect body declarations
    for (final Object o : bodyDeclarations)
    {
      final BodyDeclaration bd = (BodyDeclaration) o;
      if (bd instanceof FieldDeclaration)
      {
        // record field declarations to create field nodes
        fields.add((FieldDeclaration) bd);
        // record initializer expressions in order to create method nodes
        for (final Object f : ((FieldDeclaration) bd).fragments())
        {
          final VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
          if (fragment.getInitializer() != null)
          {
            methods.put(fragment, index);
            index++;
          }
        }
      }
      else if (bd instanceof Initializer)
      {
        methods.put(bd, index);
        index++;
      }
      else if (bd instanceof MethodDeclaration)
      {
        methods.put(bd, index);
        index++;
      }
      else if (bd instanceof TypeDeclaration || bd instanceof EnumDeclaration)
      {
        types.add((AbstractTypeDeclaration) bd);
      }
    }
  }

  private void processBodyDeclarations(final List<EnumConstantDeclaration> enums,
      final List<FieldDeclaration> fields, final Map<ASTNode, Integer> methods,
      final List<AbstractTypeDeclaration> types, final ASTNode astNode, final ITypeNode typeNode)
  {
    // process enum constants
    for (final EnumConstantDeclaration ec : enums)
    {
      processEnumConstantDeclaration(ec, astNode, typeNode);
    }
    // process fields
    for (final FieldDeclaration fd : fields)
    {
      processFieldDeclaration(fd, astNode, typeNode);
    }
    // process types recursively
    for (final AbstractTypeDeclaration atd : types)
    {
      createTypeNode(atd, typeNode);
    }
    // process anonymous declarations in enum constants recursively
    for (final EnumConstantDeclaration ec : enums)
    {
      if (ec.getAnonymousClassDeclaration() != null)
      {
        createTypeNode(ec.getAnonymousClassDeclaration(), typeNode);
      }
    }
    // process methods and initializers in declaration order
    for (final ASTNode md : methods.keySet())
    {
      if (md instanceof Initializer)
      {
        // collect dependences for the type initializer
        final IMethodNode method = createTypeInitializerNode((Initializer) md, methods.get(md),
            typeNode);
        method.setDependenceGraph(collectDependences(md));
      }
      else if (md instanceof VariableDeclarationFragment)
      {
        final VariableDeclarationFragment fragment = (VariableDeclarationFragment) md;
        final IMethodNode method = factoryAdapter.createFieldInitializerNode(
            fragment.getInitializer(), typeNode);
        // collect dependences for the field declaration
        method.setDependenceGraph(collectDependences(md.getParent()));
      }
      else
      {
        final IMethodNode method = createMethodNode((MethodDeclaration) md, methods.get(md),
            typeNode);
        // collect dependences for the method declaration
        method.setDependenceGraph(collectDependences(md));
      }
    }
    // process constructor calls in enum constants
    for (final EnumConstantDeclaration ec : enums)
    {
      final IMethodNode method = factoryAdapter.createEnumInitializerNode(ec, typeNode);
      // collect dependences for the enum constant declaration
      method.setDependenceGraph(collectDependences(ec));
    }
  }

  private void processEnumConstantDeclaration(final EnumConstantDeclaration field,
      final ASTNode astNode, final ITypeNode parent)
  {
    factoryAdapter
        .createEnumConstantNode(new ResolvedVariableDeclaration(field, astNode, parent.lineFrom(),
            parent.lineTo()), parent);
  }

  private void processFieldDeclaration(final FieldDeclaration field, final ASTNode astNode,
      final ITypeNode parent)
  {
    for (final Object f : field.fragments())
    {
      final VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
      final IVariableBinding binding = fragment.resolveBinding();
      if (!binding.isSynthetic())
      {
        createFieldNode(new ResolvedVariableDeclaration(fragment, astNode, parent.lineFrom(),
            parent.lineTo()), parent);
      }
    }
  }

  private void processLocalAndAnonymousTypes(final ASTNode target, final INode parent)
  {
    final LocalAndAnonymousTypeVisitor v = ASTFactory.astVisitorFactory()
        .createLocalAndAnonymousTypeCollector();
    // collect local and anonymous types
    target.accept(v);
    // create anonymous types
    for (final AnonymousClassDeclaration acd : v.anonymousClassDeclarationNodes())
    {
      createTypeNode(acd, parent);
    }
    // create member types
    for (final TypeDeclaration td : v.typeDeclarationNodes())
    {
      createTypeNode(td, parent);
    }
  }

  /**
   * Called by method and type initializer nodes to collect locally declared variables.
   */
  private void processLocalVariableDeclarations(final ASTNode target, final IMethodNode parent)
  {
    final VariableDeclarationVisitor v = ASTFactory.astVisitorFactory()
        .createVariableDeclarationVisitor(resolvedFactory);
    // collect variables
    target.accept(v);
    // traverse variables by index order
    for (final ResolvedVariableDeclaration rvd : v.getVariables())
    {
      // create catch variables
      if (v.catchVariables().contains(rvd))
      {
        createCatchVariableNode(rvd, parent);
      }
      // create enhanced for variables
      else if (v.enhancedForVariables().contains(rvd))
      {
        createEnhancedForVariableNode(rvd, parent);
      }
      // create local variables
      else if (v.variableFragments().contains(rvd))
      {
        createVariableNode(rvd, parent);
      }
    }
  }

  private void processMethodArguments(final MethodDeclaration astNode, final IMethodNode parent)
  {
    for (final Object o : astNode.parameters())
    {
      final SingleVariableDeclaration argument = (SingleVariableDeclaration) o;
      createArgumentNode(new ResolvedVariableDeclaration(argument, astNode, parent.lineFrom(),
          parent.lineTo()), parent);
    }
  }

  private void processTypeNodeFromAST(final IType javaType)
  {
    // the resource file gives us the file name
    final IFile file = (IFile) javaType.getResource();
    // the qualified java name gives us the relative path
    final String javaName = javaType.getFullyQualifiedName().replace('.', '/');
    // if the qualified java name has no relative path, use only the file name
    final String fileName = javaName.indexOf("/") == -1 ? file.getName() : javaName.substring(0,
        javaName.lastIndexOf("/") + 1) + file.getName();
    if (factoryAdapter.lookupFile(fileName) == null)
    {
      // creates an AST and adds all its source elements to the static model
      cu = createAST(file);
      final IFileNode fileNode = modelFactory.createFileNode(fileName, lineNumber(0),
          lineNumber(cu.getLength() - 1), NO_AST);
      // create a type node for every top-level type
      for (final Object topLevelType : cu.types())
      {
        createTypeNode((AbstractTypeDeclaration) topLevelType, fileNode);
      }
    }
  }

  private ITypeNodeRef resolveAST(final String typeKey, final String typeName)
  {
    // obtain the top-level type name based on this type's name
    final String topLevelTypeName = topLevelName(typeName);
    // try to obtain the corresponding java model element for this type
    final IType javaType = project == null ? null : (IType) project.findType(topLevelTypeName);
    // we only create AST nodes from source types
    if (javaType != null && !javaType.isBinary()
        && factoryAdapter.lookupTypeNodeByName(topLevelTypeName) == null)
    {
      cache.add(topLevelTypeName);
      processTypeNodeFromAST(javaType);
    }
    return factoryAdapter.lookupTypeNode(typeKey);
  }

  private String topLevelName(final String typeName)
  {
    // obtain the top-level type name based on this type's name
    return typeName.contains("$") ? typeName.substring(0, typeName.indexOf("$")) : typeName;
  }

  int lineNumber(final int position)
  {
    return cu == null ? -1 : cu.getLineNumber(position);
  }

  ITypeNodeRef resolveInterface(final ITypeBinding intf)
  {
    final String typeName = ASTTools.getDefault().typeName(intf);
    // obtain the top-level type name based on this type's name
    final String topLevelTypeName = topLevelName(typeName);
    // avoid duplicate processing of the top-level interface
    if (cache.contains(topLevelTypeName))
    {
      return null;
    }
    return resolveAST(ASTTools.getDefault().typeKey(intf), typeName);
  }
}