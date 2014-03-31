package edu.buffalo.cse.jive.internal.core.ast;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * Collects local and anonymous types declared in initializer expressions (fields and variables) as
 * well as within method bodies. This will also correctly collect anonymous typed created as actual
 * arguments to method invocations within the method's body. Note that this only finds types nested
 * within the types it finds-- those are processed separately.
 */
public final class LocalAndAnonymousTypeVisitor extends ASTVisitor
{
  private final List<AnonymousClassDeclaration> acdList;
  private final List<TypeDeclaration> tdList;

  public LocalAndAnonymousTypeVisitor()
  {
    this.acdList = new LinkedList<AnonymousClassDeclaration>();
    this.tdList = new LinkedList<TypeDeclaration>();
  }

  public List<AnonymousClassDeclaration> anonymousClassDeclarationNodes()
  {
    return this.acdList;
  }

  public List<TypeDeclaration> typeDeclarationNodes()
  {
    return this.tdList;
  }

  @Override
  public boolean visit(final ClassInstanceCreation node)
  {
    if (node.getAnonymousClassDeclaration() != null)
    {
      // collect
      acdList.add(node.getAnonymousClassDeclaration());
    }
    // do not visit children
    return false;
  }

  @Override
  public boolean visit(final TypeDeclaration node)
  {
    // collect
    tdList.add(node);
    // do not visit children
    return false;
  }
}
