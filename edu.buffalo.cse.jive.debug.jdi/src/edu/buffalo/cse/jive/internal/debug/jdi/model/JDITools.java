package edu.buffalo.cse.jive.internal.debug.jdi.model;

import com.sun.jdi.Method;
import com.sun.jdi.Type;

enum JDITools
{
  INSTANCE;
  static JDITools getDefault()
  {
    return INSTANCE;
  }

  public String methodKey(final Object methodObject)
  {
    final Method method = (Method) methodObject;
    return method.signature();
  }

  public String methodName(final Object methodObject)
  {
    final Method method = (Method) methodObject;
    return method.name();
  }

  public String typeKey(final Object typeObject)
  {
    final Type type = (Type) typeObject;
    return type.signature();
  }

  public String typeName(final Object typeObject)
  {
    final Type type = (Type) typeObject;
    return type.name();
  }
}