package edu.buffalo.cse.jive.internal.debug.jdi.model;

import static edu.buffalo.cse.jive.model.IStaticModel.NodeKind.NK_ARRAY;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeKind.NK_CLASS;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeKind.NK_ENUM;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeKind.NK_INTERFACE;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_ABSTRACT;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_ARGUMENT;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_BRIDGE;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_CONSTRUCTOR;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_FINAL;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_STATIC;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_SYNTHETIC;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_TRANSIENT;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeModifier.NM_VOLATILE;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeOrigin.NO_JDI;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility.NV_PACKAGE;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility.NV_PRIVATE;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility.NV_PROTECTED;
import static edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility.NV_PUBLIC;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayType;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.TypeComponent;
import com.sun.jdi.VirtualMachine;

import edu.buffalo.cse.jive.debug.jdi.model.IModelFilter;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.INode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNodeRef;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory.IStaticModelDelegate;

class StaticModelDelegateForJDI implements IStaticModelDelegate
{
  private final VirtualMachine vm;
  private IStaticModelDelegate upstream;
  private final IExecutionModel model;
  private final IModelFilter filter;

  StaticModelDelegateForJDI(final IExecutionModel model, final VirtualMachine vm,
      final IModelFilter filter)
  {
    this.vm = vm;
    this.model = model;
    this.filter = filter;
  }

  // @Override
  // public IMethodNode resolveMethod(final ITypeNode type, final String methodKey) {
  //
  // System.err.println(methodKey);
  // final List<?> classes = vm.classesByName(type.name());
  // for (final Object o : classes) {
  // final ReferenceType refType = (ReferenceType) o;
  // // create all methods for this type
  // for (int i = 0; i < refType.methods().size(); i++) {
  // final Method method = (Method) refType.methods().get(i);
  // String signature = method.signature();
  // signature = signature.substring(0, signature.indexOf(')') + 1);
  // if (method.isConstructor()
  // && methodKey.equals(refType.signature() + "." + constructorName(refType.name())
  // + signature)) {
  // return createMethod(method, methodKey, type);
  // }
  // else if (methodKey.equals(refType.signature() + "." + method.name() + signature)) {
  // return createMethod(method, methodKey, type);
  // }
  // }
  // }
  // return null;
  // }
  @Override
  public IMethodNode resolveMethod(final ITypeNode type, final Object methodObject,
      final String methodKey)
  {
    final Method method = (Method) methodObject;
    String signature = method.signature();
    signature = signature.substring(0, signature.indexOf(')') + 1);
    if (method.isSynthetic() || method.isBridge())
    {
      // System.err.println("synthetic/bridge:" + methodKey);
      return createMethod(method, methodKey, type);
    }
    else if (method.isConstructor()
        && methodKey.equals(method.declaringType().signature() + "."
            + constructorName(method.declaringType().name()) + signature))
    {
      // System.err.println("constructor:" + methodKey);
      return createMethod(method, methodKey, type);
    }
    else if (methodKey.equals(method.declaringType().signature() + "." + method.name() + signature))
    {
      // System.err.println("out-of-model method call:" + methodKey);
      return createMethod(method, methodKey, type);
    }
    return null;
  }

  // JDI type nodes are placed under the root node
  @Override
  public ITypeNodeRef resolveType(final String typeKey, final String typeName)
  {
    final List<?> classes = vm.classesByName(typeName);
    for (final Object o : classes)
    {
      // final int[] lineRange = { -1, -1 };
      final ReferenceType type = (ReferenceType) o;
      if (modelFactory().lookupTypeNode(type.signature()) == null)
      {
        // String fileName = "";
        // final int[] lineRange = { -1, -1 };
        // try {
        // fileName = type.sourceName();
        // computeLineRange(lineRange, type.allLineLocations());
        // }
        // catch (final AbsentInformationException e) {
        // // cowardly ignore
        // }
        // if (!"".equals(fileName)) {
        // final IFileNode parent = modelFactory().createFileNode(fileName, lineRange[0],
        // lineRange[1], NO_JDI);
        // createType(type, parent);
        // }
        // else {
        // createType(type, modelFactory().root());
        // }
        createType(type, modelFactory().lookupRoot());
      }
    }
    /**
     * This may return null if the type node was not resolved-- the upstream should handle this case
     * by returning a reference if a type node is not found.
     */
    return modelFactory().lookupTypeNode(typeKey);
  }

  @Override
  public void setUpstream(final IStaticModelDelegate upstream)
  {
    this.upstream = upstream;
  }

  // private Type createTypeForJava(final String typeName) {
  //
  // Type varType = modelFactory().findType(typeName);
  // if (varType == null) {
  // final IValue defaultValue = createDefaultValueForJava(typeName);
  // varType = modelFactory().createType(typeName, defaultValue);
  // }
  // return varType;
  // }
  // private IValue createDefaultValueForJava(final String typeName) {
  //
  // IValue defaultValue;
  // if (typeName.equals("boolean")) {
  // defaultValue = modelFactory().createPrimitiveValue("false");
  // }
  // else if (typeName.equals("byte") || typeName.equals("int") || typeName.equals("long")
  // || typeName.equals("short")) {
  // defaultValue = modelFactory().createPrimitiveValue("0");
  // }
  // else if (typeName.equals("double") || typeName.equals("float")) {
  // defaultValue = modelFactory().createPrimitiveValue("0.0");
  // }
  // else if (typeName.equals("char")) {
  // defaultValue = modelFactory().createPrimitiveValue("''");
  // }
  // else {
  // defaultValue = modelFactory().createNullValue();
  // }
  // return defaultValue;
  // }
  // private void createFieldSchema(final Field field, final ContextSchema schema) {
  //
  // final String varName = field.name();
  // final Type varType = createTypeForJava(field.typeName());
  // // A field belongs to a class, and a class cannot declare two fields
  // // with the same name. Hence, each variable has a unique name.
  // final int varIndex = field.declaringType().fields().indexOf(field);
  // schema.addDataMember(varName, varType, varIndex,
  // field.isStatic() ? MemberSchemaKind.MSK_STATIC_FIELD : MemberSchemaKind.MSK_FIELD, field
  // .isPrivate() ? NodeVisibility.NV_PRIVATE
  // : field.isProtected() ? NodeVisibility.NV_PROTECTED
  // : field.isPackagePrivate() ? NodeVisibility.NV_PACKAGE : NodeVisibility.NV_PUBLIC);
  // }
  //
  private void computeLineRange(final int[] lineRange, final List<?> locations)
  {
    int lineFrom = -1;
    int lineTo = -1;
    for (final Object o : locations)
    {
      final Location location = (Location) o;
      if (lineFrom == -1 || lineFrom > location.lineNumber())
      {
        lineFrom = location.lineNumber();
      }
      if (lineTo == -1 || lineTo < location.lineNumber())
      {
        lineTo = location.lineNumber();
      }
    }
    lineRange[0] = lineFrom;
    lineRange[1] = lineTo;
  }

  private String constructorName(final String typeName)
  {
    String lastName = typeName;
    if (typeName.indexOf("$") != -1)
    {
      lastName = typeName.substring(typeName.lastIndexOf("$") + 1);
      try
      {
        Integer.parseInt(lastName);
        lastName = "<init>";
      }
      catch (final NumberFormatException nfe)
      {
        // not a number, so last name is an actual name
      }
    }
    else if (typeName.indexOf(".") != -1)
    {
      lastName = typeName.substring(typeName.lastIndexOf(".") + 1);
    }
    return lastName;
  }

  private void createAllFields(final ReferenceType type, final ITypeNode typeNode,
      final int[] lineRange)
  {
    // fields declared in this type
    // if (type instanceof ArrayType) {
    // createArrayFields((ArrayType) type, typeNode);
    // }
    // else {
    for (int i = 0; i < type.fields().size(); i++)
    {
      createField(type.fields().get(i), lineRange[0], lineRange[1], typeNode);
    }
    // }
  }

  // private void createArrayFields(final ArrayType type, final ITypeNode typeNode) {
  //
  // final ITypeNodeRef fieldTypeNode = upstream.resolveType("I", "int");
  // typeNode.addDataMember("length", -1, -1, fieldTypeNode, NO_JDI,
  // new LinkedHashSet<NodeModifier>(), NV_PUBLIC, model.valueFactory()
  // .createPrimitiveValue("0"));
  // }
  private ITypeNodeRef createClassType(final ClassType classType)
  {
    // process class
    if (classType != null)
    {
      return upstream.resolveType(classType.signature(), classType.name());
    }
    return null;
  }

  private ITypeNodeRef createInterfaceType(final InterfaceType type)
  {
    // process class
    if (type != null)
    {
      return upstream.resolveType(type.signature(), type.name());
    }
    return null;
  }

  private Set<NodeModifier> createModifiers(final LocalVariable var)
  {
    final Set<NodeModifier> modifiers = new LinkedHashSet<NodeModifier>();
    // final
    if (var.isArgument())
    {
      modifiers.add(NM_ARGUMENT);
    }
    return modifiers;
  }

  /**
   * TODO: reduce the cyclomatic complexity.
   */
  private Set<NodeModifier> createModifiers(final TypeComponent member)
  {
    final Set<NodeModifier> modifiers = new LinkedHashSet<NodeModifier>();
    // final
    if (member.isFinal())
    {
      modifiers.add(NM_FINAL);
    }
    // static
    if (member.isStatic())
    {
      modifiers.add(NM_STATIC);
    }
    // transient
    if (member instanceof Field && ((Field) member).isTransient())
    {
      modifiers.add(NM_TRANSIENT);
    }
    // volatile
    if (member instanceof Field && ((Field) member).isVolatile())
    {
      modifiers.add(NM_VOLATILE);
    }
    // bridge method (for covariance in generic types)
    if (member instanceof Method && ((Method) member).isBridge())
    {
      modifiers.add(NM_BRIDGE);
    }
    // constructor
    if (member instanceof Method && ((Method) member).isConstructor())
    {
      modifiers.add(NM_CONSTRUCTOR);
    }
    // compiler generated method (e.g., bridges, constructors)
    if (member instanceof Method && ((Method) member).isSynthetic())
    {
      modifiers.add(NM_SYNTHETIC);
    }
    return modifiers;
  }

  private Set<ITypeNodeRef> createSuperInterfaces(final List<?> interfaces)
  {
    final Set<ITypeNodeRef> superInterfaceNodes = new LinkedHashSet<ITypeNodeRef>();
    // process implemented interfaces
    if (interfaces != null)
    {
      for (final Object o : interfaces)
      {
        superInterfaceNodes.add(createInterfaceType((InterfaceType) o));
      }
    }
    return superInterfaceNodes;
  }

  private ITypeNode createType(final ReferenceType type, final INode parentNode)
  {
    final NodeKind kind = (type instanceof InterfaceType) ? NK_INTERFACE
        : (type instanceof ArrayType) ? NK_ARRAY
            : ((type instanceof ClassType) && ((ClassType) type).isEnum()) ? NK_ENUM : NK_CLASS;
    ClassType classType = null;
    List<?> interfaces = null;
    if (type instanceof InterfaceType)
    {
      final InterfaceType resolvedType = (InterfaceType) type;
      // super interfaces
      interfaces = resolvedType.superinterfaces();
    }
    else if (type instanceof ClassType)
    {
      final ClassType resolvedType = (ClassType) type;
      // super class
      classType = resolvedType.superclass();
      // implemented interfaces
      interfaces = resolvedType.interfaces();
    }
    else if (type instanceof ArrayType)
    {
      // super class
      final List<?> classes = vm.classesByName("java.lang.Object");
      classType = (ClassType) classes.get(0);
    }
    // this node is guaranteed to be correctly resolved at this point (it's the super!)
    final ITypeNodeRef superRef = createClassType(classType);
    final Set<ITypeNodeRef> superInterfaceNodes = createSuperInterfaces(interfaces);
    final int[] lineRange =
    { -1, -1 };
    try
    {
      computeLineRange(lineRange, type.allLineLocations());
    }
    catch (final AbsentInformationException e)
    {
      // cowardly ignore
    }
    final ITypeNode typeNode = modelFactory().createTypeNode(type.signature(), type.name(),
        parentNode, lineRange[0], lineRange[1], kind, NO_JDI, createTypeModifier(type),
        createTypeVisibility(type), superRef == null ? null : superRef.node(), superInterfaceNodes,
        model.valueFactory().createNullValue());
    // necessary for super types, snapshot creation, etc
    createAllFields(type, typeNode, lineRange);
    return typeNode;
  }

  private Set<NodeModifier> createTypeModifier(final ReferenceType type)
  {
    final Set<NodeModifier> modifiers = new LinkedHashSet<NodeModifier>();
    // abstract
    if (type.isAbstract())
    {
      modifiers.add(NM_ABSTRACT);
    }
    // final
    if (type.isFinal())
    {
      modifiers.add(NM_FINAL);
    }
    // static
    if (type.isStatic())
    {
      modifiers.add(NM_STATIC);
    }
    return modifiers;
  }

  private NodeVisibility createTypeVisibility(final ReferenceType type)
  {
    return type.isPackagePrivate() ? NV_PACKAGE : type.isPrivate() ? NV_PRIVATE : type
        .isProtected() ? NV_PROTECTED : NV_PUBLIC;
  }

  private void createVariable(final LocalVariable var, final int lineFrom, final int lineTo,
      final IMethodNode methodNode)
  {
    ITypeNodeRef varTypeNode;
    try
    {
      varTypeNode = upstream.resolveType(var.type().signature(), var.typeName());
    }
    catch (final ClassNotLoadedException e)
    {
      varTypeNode = modelFactory().lookupTypeRefByName(var.typeName());
    }
    methodNode.addDataMember(var.name(), lineFrom, lineTo, varTypeNode, NO_JDI,
        createModifiers(var), NodeVisibility.NV_LOCAL, model.valueFactory()
            .createUninitializedValue());
  }

  private NodeVisibility createVisibility(final TypeComponent member)
  {
    return member.isPackagePrivate() ? NV_PACKAGE : member.isPrivate() ? NV_PRIVATE : member
        .isProtected() ? NV_PROTECTED : NV_PUBLIC;
  }

  private IStaticModelFactory modelFactory()
  {
    return this.model.staticModelFactory();
  }

  void createField(final Field field, final int lineFrom, final int lineTo, final ITypeNode typeNode)
  {
    // this field never generates a field read or field write event
    if (!filter.acceptsType(field.declaringType()) || !filter.acceptsField(field))
    {
      return;
    }
    ITypeNodeRef fieldTypeNode;
    try
    {
      fieldTypeNode = upstream.resolveType(field.type().signature(), field.typeName());
    }
    catch (final ClassNotLoadedException e)
    {
      fieldTypeNode = modelFactory().lookupTypeRefByName(field.typeName());
    }
    typeNode.addDataMember(field.name(), lineFrom, lineTo, fieldTypeNode, NO_JDI,
        createModifiers(field), createVisibility(field),
        fieldTypeNode instanceof ITypeNode ? ((ITypeNode) fieldTypeNode).defaultValue() : model
            .valueFactory().createNullValue());
  }

  /**
   * This should be an out-of-model method, so there is no point in exposing local variables.
   */
  IMethodNode createMethod(final Method method, final String methodKey, final ITypeNode typeNode)
  {
    // synthetic
    // if (method.isSynthetic()) {
    // return null;
    // }
    final int[] lineRange =
    { -1, -1 };
    try
    {
      computeLineRange(lineRange, method.allLineLocations());
    }
    catch (final AbsentInformationException e)
    {
      // cowardly ignore
    }
    ITypeNodeRef returnTypeNode;
    try
    {
      returnTypeNode = upstream.resolveType(method.returnType().signature(),
          method.returnTypeName());
    }
    catch (final ClassNotLoadedException e)
    {
      returnTypeNode = model.staticModelFactory().lookupTypeRefByName(method.returnTypeName());
    }
    final String methodName = method.isConstructor() ? constructorName(method.declaringType()
        .name()) : method.name();
    final IMethodNode methodNode = typeNode.addMethodMember(methodKey, methodName, lineRange[0],
        lineRange[1], returnTypeNode, NO_JDI, createModifiers(method), createVisibility(method),
        Collections.<ITypeNodeRef> emptySet());
    // when using JDI resolution, local variables are relevant
    try
    {
      for (int i = 0; i < method.variables().size(); i++)
      {
        createVariable(method.variables().get(i), lineRange[0], lineRange[1], methodNode);
      }
    }
    catch (final AbsentInformationException e)
    {
      // cowardly ignore
    }
    return methodNode;
  }
}
