package edu.buffalo.cse.jive.internal.core.ast;

import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;

import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedCall;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedData;

enum ASTTools
{
  INSTANCE;
  private static boolean isVariableScope(final ASTNode node)
  {
    // lexical scope is a loop
    if (ASTTools.isEnclosingLoopScope(node))
    {
      return true;
    }
    // lexical scope is a conditional statement or expression
    if (node instanceof ConditionalExpression || node instanceof IfStatement
        || node instanceof SwitchCase)
    {
      return true;
    }
    // lexical scope is a block, body declaration, or catch clause
    if (node instanceof Block || node instanceof BodyDeclaration || node instanceof CatchClause)
    {
      return true;
    }
    return false;
  }

  private static ITypeBinding typeBinding(final ASTNode node)
  {
    // a type node
    if (node instanceof AnonymousClassDeclaration)
    {
      return ((AnonymousClassDeclaration) node).resolveBinding().getErasure();
    }
    else if (node instanceof TypeDeclaration)
    {
      return ((TypeDeclaration) node).resolveBinding().getErasure();
    }
    else if (node instanceof EnumDeclaration)
    {
      return ((EnumDeclaration) node).resolveBinding().getErasure();
    }
    return null;
  }

  protected static AnonymousClassDeclaration anonymousClassScope(final ASTNode node)
  {
    return (AnonymousClassDeclaration) node;
  }

  protected static EnumDeclaration enumScope(final ASTNode node)
  {
    return (EnumDeclaration) node;
  }

  protected static boolean isAnonymousClassDeclaration(final ASTNode node)
  {
    return node instanceof AnonymousClassDeclaration;
  }

  protected static boolean isClassDeclaration(final ASTNode node)
  {
    return ASTTools.isTypeDeclaration(node) && !ASTTools.typeScope(node).isInterface();
  }

  protected static boolean isConstructor(final ASTNode node)
  {
    return ASTTools.isMethodDeclaration(node) && ASTTools.methodScope(node).isConstructor();
  }

  protected static boolean isControlScope(final ASTNode node)
  {
    // loop parent
    if (ASTTools.isEnclosingLoopScope(node))
    {
      return true;
    }
    // method node
    if (ASTTools.isMethodScope(node))
    {
      return true;
    }
    // type initializer or field initializer expression
    if (ASTTools.isInitializerScope(node))
    {
      return true;
    }
    // other control node
    if (node instanceof CatchClause || node instanceof IfStatement
        || node instanceof SwitchStatement)
    {
      return true;
    }
    return false;
  }

  protected static boolean isEnclosingScope(final ASTNode node)
  {
    // a type, method, or initializer node
    return ASTTools.isTypeScope(node) || ASTTools.isMethodScope(node)
        || ASTTools.isInitializerScope(node);
  }

  protected static boolean isEnumDeclaration(final ASTNode node)
  {
    return node instanceof EnumDeclaration;
  }

  protected static boolean isExpression(final ASTNode node)
  {
    return node instanceof Expression;
  }

  protected static boolean isInitializer(final ASTNode node)
  {
    return node instanceof Initializer;
  }

  protected static boolean isMethodDeclaration(final ASTNode node)
  {
    return node instanceof MethodDeclaration;
  }

  protected static boolean isTypeDeclaration(final ASTNode node)
  {
    return node instanceof TypeDeclaration;
  }

  protected static MethodDeclaration methodScope(final ASTNode node)
  {
    return (MethodDeclaration) node;
  }

  protected static TypeDeclaration typeScope(final ASTNode node)
  {
    return (TypeDeclaration) node;
  }

  static String declaringFieldContext(final IVariableBinding binding)
  {
    if (binding.isField())
    {
      return ASTTools.getDefault().typeName(binding.getDeclaringClass());
    }
    return null;
  }

  static String declaringFieldContextType(final IVariableBinding binding)
  {
    if (binding.isField())
    {
      String context = "";
      if (binding.getDeclaringClass() == null)
      {
        context = "array";
      }
      else
      {
        if (!binding.getDeclaringClass().isTopLevel())
        {
          context = "nested ";
          if (binding.getDeclaringClass().isAnonymous())
          {
            context += " anonymous";
          }
          if (binding.getDeclaringClass().isLocal())
          {
            context += " local";
          }
        }
        if (binding.getDeclaringClass().isInterface())
        {
          context += " interface";
        }
        else if (binding.getDeclaringClass().isEnum())
        {
          context += " enum";
        }
        else
        {
          context += " class";
        }
      }
      return context;
    }
    return null;
  }

  /**
   * Traverses the self-or-ancestor axis until an enclosing control predicate or method declaration
   * is found. These are specific to unstructured control flow, namely, break, continue, and return
   * statements.
   */
  static ASTNode enclosingControlScope(final ASTNode node)
  {
    ASTNode scope = node; // .getParent();
    while (scope != null && !ASTTools.isControlScope(scope))
    {
      scope = scope.getParent();
    }
    return scope;
  }

  /**
   * Traverses the self-or-ancestor axis until an enclosing loop control predicate is found.
   */
  static ASTNode enclosingLoopScope(final ASTNode node)
  {
    ASTNode scope = node; // .getParent();
    while (scope != null && !ASTTools.isEnclosingLoopScope(scope))
    {
      scope = scope.getParent();
    }
    return scope;
  }

  // /**
  // * Traverses the parent axis until the enclosing type scope for the node is found.
  // */
  // private static ASTNode enclosingMethodScope(final ASTNode node) {
  //
  // ASTNode scope = node.getParent();
  // while (scope != null && !isMethodScope(scope)) {
  // scope = scope.getParent();
  // }
  // return scope;
  // }
  /**
   * Traverses the self-or-ancestor axis until the enclosing type or method scope for the node is
   * found.
   */
  static ASTNode enclosingScope(final ASTNode node)
  {
    ASTNode scope = node; // .getParent();
    while (scope != null && !ASTTools.isEnclosingScope(scope))
    {
      scope = scope.getParent();
    }
    return scope;
  }

  /**
   * Traverses the self-or-ancestor axis until an enclosing switch control predicate is found.
   */
  static ASTNode enclosingSwitchScope(final ASTNode node)
  {
    ASTNode scope = node; // .getParent();
    while (scope != null && !ASTTools.isEnclosingSwitchScope(scope))
    {
      scope = scope.getParent();
    }
    return scope;
  }

  /**
   * Traverses the self-or-ancestor axis until an enclosing try-finally scope is found or the method
   * is complete.
   */
  static ASTNode enclosingTryFinallyScope(final ASTNode node)
  {
    ASTNode scope = node; // .getParent();
    while (scope != null && !ASTTools.isEnclosingTryFinallyScope(scope))
    {
      scope = scope.getParent();
    }
    return scope;
  }

  /**
   * Traverses the self-or-ancestor axis until the enclosing type scope for the node is found.
   */
  static ASTNode enclosingTypeScope(final ASTNode node)
  {
    ASTNode scope = node; // .getParent();
    while (scope != null && !ASTTools.isTypeScope(scope))
    {
      scope = scope.getParent();
    }
    return scope;
  }

  /**
   * Traverses the self-or-ancestor axis until the enclosing non-anonymous class with the given name
   * is found.
   */
  static ASTNode enclosingTypeScope(final ASTNode node, final Name name)
  {
    ASTNode scope = node; // .getParent();
    while (scope != null && !ASTTools.isNamedTypeScope(scope, name))
    {
      scope = scope.getParent();
    }
    return scope;
  }

  static VariableDeclarationFragment enclosingVariableDeclaration(final ASTNode node)
  {
    ASTNode scope = node; // .getParent();
    while (scope != null && !(scope instanceof VariableDeclarationFragment))
    {
      scope = scope.getParent();
    }
    return (VariableDeclarationFragment) scope;
  }

  /**
   * Traverses the self-or-ancestor axis until an enclosing control predicate or method declaration
   * is found. These are specific to variable scopes.
   */
  static ASTNode enclosingVariableScope(final ASTNode node)
  {
    ASTNode scope = node; // .getParent();
    while (scope != null && !ASTTools.isVariableScope(scope))
    {
      scope = scope.getParent();
    }
    return scope;
  }

  static ASTTools getDefault()
  {
    return INSTANCE;
  }

  static boolean isEnclosingLoopScope(final ASTNode node)
  {
    // loop node
    if (node instanceof DoStatement || node instanceof EnhancedForStatement
        || node instanceof ForStatement || node instanceof WhileStatement)
    {
      return true;
    }
    return false;
  }

  static boolean isEnclosingSwitchScope(final ASTNode node)
  {
    // switch statement
    return node instanceof SwitchStatement;
  }

  static boolean isEnclosingTryFinallyScope(final ASTNode node)
  {
    // try node with a finally block
    if (node instanceof TryStatement)
    {
      return ((TryStatement) node).getFinally() != null;
    }
    return false;
  }

  static boolean isInitializerScope(final ASTNode node)
  {
    // a type initializer node
    if (node instanceof Initializer)
    {
      return true;
    }
    // an ArrayAccess is an Expression but not an enclosing scope
    if (node instanceof ArrayAccess)
    {
      return false;
    }
    // a field initializer node
    if (node instanceof Expression && node.getParent() instanceof VariableDeclarationFragment)
    {
      return ((VariableDeclarationFragment) node.getParent()).resolveBinding().isField();
    }
    return false;
  }

  static boolean isMethodScope(final ASTNode node)
  {
    // a method node
    if (node instanceof MethodDeclaration)
    {
      return true;
    }
    return false;
  }

  static boolean isNamedTypeScope(final ASTNode node, final Name name)
  {
    // a type node
    if (!(node instanceof TypeDeclaration) && !(node instanceof EnumDeclaration))
    {
      return false;
    }
    return !((AbstractTypeDeclaration) node).getName().equals(name);
  }

  static boolean isTypeScope(final ASTNode node)
  {
    // a type node
    if (node instanceof AnonymousClassDeclaration || node instanceof TypeDeclaration
        || node instanceof EnumDeclaration)
    {
      return true;
    }
    return false;
  }

  /**
   * TODO: provide lookups for the context in our model, whether using this string or something
   * else; together with the variable identifier, this allows us to fully identify the static
   * element corresponding to a dynamic assignment.
   */
  static String lexicalContext(final ASTNode node)
  {
    // anonymous class
    if (ASTTools.isAnonymousClassDeclaration(node))
    {
      return ASTTools.getDefault().typeName(ASTTools.anonymousClassScope(node).resolveBinding());
    }
    // named class or interface
    if (ASTTools.isTypeDeclaration(node))
    {
      return ASTTools.getDefault().typeName(ASTTools.typeScope(node).resolveBinding());
    }
    // enum
    if (ASTTools.isEnumDeclaration(node))
    {
      return ASTTools.getDefault().typeName(ASTTools.enumScope(node).resolveBinding());
    }
    // method or constructor
    if (ASTTools.isMethodDeclaration(node))
    {
      return ASTTools.getDefault().methodName(ASTTools.methodScope(node).resolveBinding());
    }
    // type or field/variable initializer
    if (ASTTools.isInitializer(node) || ASTTools.isExpression(node))
    {
      return ASTTools.getDefault().methodName(node);
    }
    return "unknown";
  }

  static String lexicalContextType(final ASTNode node)
  {
    // anonymous class
    if (ASTTools.isAnonymousClassDeclaration(node))
    {
      return "anonymous class";
    }
    // named class
    if (ASTTools.isClassDeclaration(node))
    {
      return "class";
    }
    // interface
    if (ASTTools.isTypeDeclaration(node))
    {
      return "interface";
    }
    // enum
    if (ASTTools.isEnumDeclaration(node))
    {
      return "enum";
    }
    // executable scopes
    if (ASTTools.isConstructor(node))
    {
      return "constructor";
    }
    if (ASTTools.isInitializer(node))
    {
      return "initializer";
    }
    if (ASTTools.isMethodDeclaration(node))
    {
      return "method";
    }
    return "unknown";
  }

  static int lineNumber(final ASTNode node)
  {
    return CompilationUnitVisitor.lineNumber(node, node.getStartPosition());
  }

  static int lineNumber(final ASTNode node, final int position)
  {
    return CompilationUnitVisitor.lineNumber(node, position);
  }

  static IVariableBinding resolveVariableBinding(final ASTNode node)
  {
    if (node instanceof EnumConstantDeclaration)
    {
      return ((EnumConstantDeclaration) node).resolveVariable();
    }
    if (node instanceof FieldAccess)
    {
      return ((FieldAccess) node).resolveFieldBinding();
    }
    if (node instanceof Name)
    {
      final IBinding binding = ((Name) node).resolveBinding();
      if (binding instanceof IVariableBinding)
      {
        return (IVariableBinding) binding;
      }
    }
    if (node instanceof SuperFieldAccess)
    {
      return ((SuperFieldAccess) node).resolveFieldBinding();
    }
    if (node instanceof VariableDeclarationFragment)
    {
      return ((VariableDeclarationFragment) node).resolveBinding();
    }
    return null;
  }

  static void toCallList(final StringBuffer buffer, final Set<IResolvedCall> calls)
  {
    int i = 0;
    buffer.append("{");
    for (final IResolvedCall rc : calls)
    {
      i++;
      buffer.append(rc.toString());
      if (i != calls.size())
      {
        buffer.append(", ");
      }
    }
    buffer.append("}");
  }

  static void toVarsList(final StringBuffer buffer, final Set<IResolvedData> uses)
  {
    int i = 0;
    buffer.append("{");
    for (final IResolvedData rv : uses)
    {
      i++;
      buffer.append(rv.toString());
      if (i != uses.size())
      {
        buffer.append(", ");
      }
    }
    buffer.append("}");
  }

  public String methodKey(final Object methodObject)
  {
    // initializers require special handling
    if (methodObject instanceof Initializer || methodObject instanceof EnumConstantDeclaration
        || methodObject instanceof Expression)
    {
      final String typeKey = typeKey(ASTTools.enclosingTypeScope((ASTNode) methodObject));
      return typeKey + "." + initializerKey((ASTNode) methodObject);
    }
    final IMethodBinding method;
    // method declaration
    if ((methodObject instanceof MethodDeclaration))
    {
      method = ((MethodDeclaration) methodObject).resolveBinding();
    }
    // must be method binding, otherwise we'll get a class cast exception!
    else
    {
      method = (IMethodBinding) methodObject;
    }
    return methodBindingKey(method);
  }

  // String constructorKey(final String parentType, final IMethodBinding method) {
  //
  // String methodName = methodName(method);
  // // do not modify "<init>" or "<clinit>"
  // if (methodName.indexOf("<") > 0) {
  // methodName = methodName.substring(0, methodName.indexOf('<'));
  // }
  // return parentType + "." + methodName + "(" + methodBindingSignature(method) + ")";
  // }
  public String methodName(final Object methodObject)
  {
    if (methodObject instanceof Initializer || methodObject instanceof EnumConstantDeclaration
        || methodObject instanceof Expression)
    {
      return initializerName((ASTNode) methodObject);
    }
    final IMethodBinding method;
    // method declaration
    if ((methodObject instanceof MethodDeclaration))
    {
      method = ((MethodDeclaration) methodObject).resolveBinding();
    }
    // must be method binding, otherwise we'll get a class cast exception!
    else
    {
      method = (IMethodBinding) methodObject;
    }
    String methodName = method.getName();
    if (methodName == null || methodName.length() == 0 /* || method.isConstructor() */)
    {
      methodName = "<init>";
    }
    return methodName;
  }

  public String returnTypeKey(final Object methodObject)
  {
    // initializers require special handling
    if (methodObject instanceof Initializer || methodObject instanceof EnumConstantDeclaration
        || methodObject instanceof Expression)
    {
      return "V";
    }
    final IMethodBinding method;
    // method declaration
    if ((methodObject instanceof MethodDeclaration))
    {
      method = ((MethodDeclaration) methodObject).resolveBinding();
    }
    // must be method binding, otherwise we'll get a class cast exception!
    else
    {
      method = (IMethodBinding) methodObject;
    }
    return typeKey(method.getReturnType());
  }

  public String typeKey(final Object typeObject)
  {
    final ITypeBinding type;
    if (typeObject instanceof ASTNode && ASTTools.isTypeScope((ASTNode) typeObject))
    {
      type = ASTTools.typeBinding((ASTNode) typeObject);
    }
    else
    {
      type = ((ITypeBinding) typeObject).getErasure();
    }
    if (type.isClass() || type.isInterface() || type.isEnum())
    {
      return String.format("L%s;", type.getBinaryName().replace(".", "/"));
    }
    if (type.isArray())
    {
      return String.format("[%s", typeKey(type.getComponentType()));
    }
    return type.getKey();
  }

  public String typeName(final Object typeObject)
  {
    final ITypeBinding type;
    if (typeObject instanceof ASTNode && ASTTools.isTypeScope((ASTNode) typeObject))
    {
      type = ASTTools.typeBinding((ASTNode) typeObject);
    }
    else
    {
      type = ((ITypeBinding) typeObject).getErasure();
    }
    return type.isAnonymous() || type.isNested() || type.isLocal() ? type.getBinaryName() : type
        .getQualifiedName();
  }

  /**
   * During static analysis, each initializer must be distinguished since they may appear in many
   * different syntactic structures. At run-time, static initializers are executed during class
   * load, instance initializers during object creation, and local initializers when their source
   * lines are executed.
   */
  private String initializerKey(final ASTNode method)
  {
    final int lineNumber = ASTTools.lineNumber(method);
    final StringBuffer buffer = new StringBuffer("");
    // type or instance initializer
    if (method instanceof Initializer)
    {
      final boolean isStatic = Modifier.isStatic(((Initializer) method).getModifiers());
      buffer.append(isStatic ? "<clinit@" : "<init@");
      buffer.append(lineNumber);
      buffer.append(">()");
      return buffer.toString();
    }
    // field or local variable initializer
    if (method instanceof Expression)
    {
      final VariableDeclarationFragment fragment = (VariableDeclarationFragment) method.getParent();
      final IVariableBinding variable = fragment.resolveBinding();
      // field initializers are part of static or instance initializers
      if (variable.isField())
      {
        final boolean isStatic = Modifier.isStatic(variable.getModifiers());
        buffer.append(isStatic ? "<clinit@" : "<init@");
        buffer.append(lineNumber);
        buffer.append(">()");
        return buffer.toString();
      }
      // local variable initializers are executed during method execution
      else
      {
        buffer.append("<vinit@");
        buffer.append(lineNumber);
        buffer.append(">()");
        return buffer.toString();
      }
    }
    // unknown initializer
    return "<_init@" + lineNumber + ">()";
  }

  private String initializerName(final ASTNode method)
  {
    final StringBuffer buffer = new StringBuffer("");
    // type or instance initializer
    if (method instanceof Initializer)
    {
      final boolean isStatic = Modifier.isStatic(((Initializer) method).getModifiers());
      buffer.append(isStatic ? "<clinit>" : "<init>");
      return buffer.toString();
    }
    // enum constant declaration
    if (method instanceof EnumConstantDeclaration)
    {
      return "<clinit>";
    }
    // field or local variable initializer
    if (method instanceof Expression)
    {
      final VariableDeclarationFragment fragment = (VariableDeclarationFragment) method.getParent();
      final IVariableBinding variable = fragment.resolveBinding();
      // field initializers are part of static or instance initializers
      if (variable.isField())
      {
        final boolean isStatic = Modifier.isStatic(variable.getModifiers());
        buffer.append(isStatic ? "<clinit>" : "<init>");
        return buffer.toString();
      }
      // local variable initializers are executed during method execution
      else
      {
        buffer.append("<vinit>");
        return buffer.toString();
      }
    }
    // unknown initializer
    return "<_init>";
  }

  /**
   * Compute a signature stripping type parameters so that the resulting keys are compatible with
   * JDI keys. Also, no result signature is necessary since result types do not distinguish versions
   * of the same method.
   */
  private final String methodBindingKey(final IMethodBinding method)
  {
    String methodName = methodName(method);
    // do not modify "<init>" or "<clinit>"
    if (methodName.indexOf("<") > 0)
    {
      methodName = methodName.substring(0, methodName.indexOf('<'));
    }
    return typeKey(method.getDeclaringClass()) + "." + methodName + "("
        + methodBindingSignature(method) + ")";
  }

  private final String methodBindingSignature(final IMethodBinding method)
  {
    String signature = "";
    // fix for the default signature of enum constructors
    if (method.isConstructor() && method.getDeclaringClass().isEnum())
    {
      signature += "Ljava/lang/String;I";
    }
    // fix for the signature of non-static member classes
    else if (method.isConstructor() && method.getMethodDeclaration().getDeclaringClass().isMember()
        && !Modifier.isStatic(method.getMethodDeclaration().getDeclaringClass().getModifiers()))
    {
      signature += typeKey(method.getMethodDeclaration().getDeclaringClass().getDeclaringClass());
    }
    for (final ITypeBinding argType : method.getParameterTypes())
    {
      if (argType.isTypeVariable())
      {
        if (argType.getTypeBounds().length == 0)
        {
          signature += "Ljava/lang/Object;";
        }
        else
        {
          signature += typeKey(argType.getTypeBounds()[0]);
        }
      }
      else
      {
        signature += typeKey(argType);
      }
    }
    return signature;
  }

  private final static class CompilationUnitVisitor extends ASTVisitor
  {
    private CompilationUnit cu = null;
    private final static CompilationUnitVisitor INSTANCE = new CompilationUnitVisitor();

    static int lineNumber(final ASTNode node, final int position)
    {
      CompilationUnitVisitor.INSTANCE.cu = null;
      node.getRoot().accept(CompilationUnitVisitor.INSTANCE);
      return CompilationUnitVisitor.INSTANCE.cu == null ? -1 : CompilationUnitVisitor.INSTANCE.cu
          .getLineNumber(position);
    }

    @Override
    public boolean visit(final CompilationUnit node)
    {
      this.cu = node;
      return false;
    }
  }
}