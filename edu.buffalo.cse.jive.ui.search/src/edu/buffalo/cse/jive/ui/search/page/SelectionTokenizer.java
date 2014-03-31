package edu.buffalo.cse.jive.ui.search.page;

import org.eclipse.gef.editparts.AbstractEditPart;
import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourTokens;
import edu.buffalo.cse.jive.model.IEventModel.IInitiatorEvent;
import edu.buffalo.cse.jive.model.IEventModel.IThreadStartEvent;

/**
 * Utility class used to tokenize an {@code ISelection} into various components representing Java
 * elements. This class can be used to initialize query input fields.
 */
class SelectionTokenizer
{
  private static final String EMPTY_STRING = "";
  private String className = SelectionTokenizer.EMPTY_STRING;
  private String methodName = SelectionTokenizer.EMPTY_STRING;
  private String variableName = SelectionTokenizer.EMPTY_STRING;

  SelectionTokenizer(final ISelection selection)
  {
    tokenize(selection);
  }

  /**
   * Fully-qualified class name associated with the {@code Selection}, or the empty string if there
   * is none.
   */
  public String getClassName()
  {
    return className;
  }

  /**
   * Method name associated with the {@code ISelection}, or the empty string if there is none.
   */
  public String getMethodName()
  {
    return methodName;
  }

  /**
   * Variable name associated with the {@code ISelection}, or the empty string if there is none.
   */
  public String getVariableName()
  {
    return variableName;
  }

  /**
   * Initializes the fields using an {@code IField}.
   */
  private void initialize(final IField field)
  {
    initialize(field.getDeclaringType());
    variableName = field.getElementName();
  }

  /**
   * Initializes the fields using an {@code ILocalVariable}.
   * 
   * @param variable
   *          the selected local variable
   * @throws JavaModelException
   *           if there is a failure in the Java model
   */
  private void initialize(final ILocalVariable variable) throws JavaModelException
  {
    final IJavaElement parent = variable.getParent();
    if (parent.getElementType() == IJavaElement.METHOD)
    {
      initialize((IMethod) parent);
    }
    variableName = variable.getElementName();
  }

  /**
   * Initializes the fields using an {@code IMethod}.
   * 
   * @param method
   *          the selected method
   * @throws JavaModelException
   *           if there is a failure in the Java model
   */
  private void initialize(final IMethod method) throws JavaModelException
  {
    initialize(method.getDeclaringType());
    if (method.isConstructor())
    {
      methodName = className;
    }
    else
    {
      methodName = method.getElementName();
    }
  }

  /**
   * Initializes the fields using an {@code IPackageFragment}.
   * 
   * @param packageFragment
   *          the selected package fragment
   */
  private void initialize(final IPackageFragment packageFragment)
  {
    className = packageFragment.getElementName();
  }

  /**
   * Initializes the fields using an {@code IType}.
   * 
   * @param type
   *          the selected type
   */
  private void initialize(final IType type)
  {
    className = type.getFullyQualifiedName('$');
  }

  /**
   * Tokenizes the supplied {@code ContourEditPart} by tokenizing the {@code ContourID} referring to
   * its model.
   * 
   * @param editPart
   */
  private void tokenize(final IContour contour)
  {
    final IContourTokens tokenizer = contour.tokenize();
    className = tokenizer.className();
    methodName = tokenizer.methodName();
    if (methodName == null)
    {
      methodName = SelectionTokenizer.EMPTY_STRING;
    }
  }

  /**
   * Tokenizes the supplied {@code ExecutionOccurrenceEditPart} by tokenizing the {@code ContourID}
   * referring to its model.
   * 
   * @param editPart
   */
  private void tokenize(final IInitiatorEvent initiator)
  {
    if (!(initiator instanceof IThreadStartEvent))
    {
      final IContourTokens tokenizer = initiator.execution().tokenize();
      className = tokenizer.className();
      methodName = tokenizer.methodName();
    }
  }

  /**
   * Tokenizes the supplied {@code IJavaElement} depending on its type.
   * 
   * @param element
   *          the element to tokenize
   * @throws JavaModelException
   *           if there is a failure in the Java model
   */
  private void tokenize(final IJavaElement element) throws JavaModelException
  {
    switch (element.getElementType())
    {
      case IJavaElement.PACKAGE_FRAGMENT:
        initialize((IPackageFragment) element);
        break;
      case IJavaElement.TYPE:
        initialize((IType) element);
        break;
      case IJavaElement.FIELD:
        initialize((IField) element);
        break;
      case IJavaElement.METHOD:
        initialize((IMethod) element);
        break;
      case IJavaElement.LOCAL_VARIABLE:
        initialize((ILocalVariable) element);
        break;
    }
  }

  /**
   * Tokenizes the supplied {@code ISelection} by delegating to the appropriate tokenizer.
   */
  private void tokenize(final ISelection selection)
  {
    if (selection instanceof IStructuredSelection)
    {
      tokenize((IStructuredSelection) selection);
    }
    else if (selection instanceof ITextSelection)
    {
      tokenize((ITextSelection) selection);
    }
  }

  /**
   * Tokenizes the first element in the {@code IStructuredSelection} by delegating to the
   * appropriate tokenizer.
   */
  private void tokenize(final IStructuredSelection selection)
  {
    final Object element = selection.getFirstElement();
    if (element instanceof IJavaElement)
    {
      try
      {
        tokenize((IJavaElement) element);
      }
      catch (final JavaModelException e)
      {
        // do nothing
      }
    }
    else if (element instanceof AbstractEditPart)
    {
      final Object model = ((AbstractEditPart) element).getModel();
      if (model instanceof IInitiatorEvent)
      {
        tokenize((IInitiatorEvent) model);
      }
      else if (model instanceof IContour)
      {
        tokenize((IContour) model);
      }
    }
  }

  /**
   * Tokenizes the supplied {@code ITextSelection} if the selected text corresponds to an
   * {@code IJavaElement}.
   */
  private void tokenize(final ITextSelection selection)
  {
    final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
        .getActivePage();
    if (activePage != null)
    {
      final IEditorPart editor = activePage.getActiveEditor();
      final IEditorInput editorInput = editor.getEditorInput();
      final IJavaElement element = JavaUI.getEditorInputJavaElement(editorInput);
      if (element instanceof ICodeAssist)
      {
        final ICodeAssist root = (ICodeAssist) element;
        try
        {
          final int offset = selection.getOffset();
          final int length = selection.getLength();
          final IJavaElement[] elements = root.codeSelect(offset, length);
          if (elements.length > 0)
          {
            tokenize(elements[0]);
          }
        }
        catch (final JavaModelException e)
        {
          // do nothing
        }
      }
    }
  }
}
