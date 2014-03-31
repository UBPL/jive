package edu.buffalo.cse.jive.ui.search.page;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_EVENT_STEP;

import java.io.File;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import edu.buffalo.cse.jive.model.IEventModel.ILineStepEvent;
import edu.buffalo.cse.jive.model.IQueryModel.EventQuery;
import edu.buffalo.cse.jive.model.factory.IQueryFactory;
import edu.buffalo.cse.jive.ui.search.IJiveSearchQuery;
import edu.buffalo.cse.jive.ui.search.JiveSearchPage;
import edu.buffalo.cse.jive.ui.search.JiveSearchQuery;
import edu.buffalo.cse.jive.ui.search.form.FormFactory;
import edu.buffalo.cse.jive.ui.search.form.SearchForm.LineExecutedForm;

/**
 * View portion encapsulating a search form.
 */
public class LineExecutedSearchPage extends JiveSearchPage
{
  private LineExecutedForm searchForm;

  @Override
  public void createControl(final Composite parent)
  {
    searchForm = FormFactory.createLineExecutedForm(this, parent);
  }

  @Override
  public IJiveSearchQuery createSearchQuery()
  {
    return new LineExecutedSearchQuery();
  }

  @Override
  public Control getControl()
  {
    return searchForm.control();
  }

  @Override
  public void initializeInput(final ISelection selection)
  {
    if (selection instanceof ITextSelection)
    {
      final ITextSelection textSelection = (ITextSelection) selection;
      final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
          .getActivePage();
      if (activePage != null)
      {
        final IEditorPart editor = activePage.getActiveEditor();
        final IEditorInput editorInput = editor.getEditorInput();
        final IJavaElement element = JavaUI.getEditorInputJavaElement(editorInput);
        if (element instanceof ICompilationUnit)
        {
          final ICompilationUnit unit = (ICompilationUnit) element;
          final String filename = unit.getElementName();
          String packageName = unit.getParent().getElementName();
          if (!packageName.equals(""))
          {
            packageName = packageName.replace('.', File.separatorChar);
            searchForm.setSourcePathText(packageName + File.separatorChar + filename);
          }
          else
          { // default package
            searchForm.setSourcePathText(packageName + filename);
          }
          final int lineNumber = textSelection.getEndLine();
          if (lineNumber != -1)
          {
            searchForm.setLineNumberText(Integer.toString(lineNumber + 1));
          }
        }
      }
    }
  }

  @Override
  public boolean isInputValid()
  {
    if (searchForm.sourcePathText().equals(""))
    {
      return false;
    }
    return searchForm.lineNumberText().matches("[1-9]\\d*");
  }

  /**
   * View portion encapsulating an event predicate query.
   */
  private class LineExecutedSearchQuery extends JiveSearchQuery
  {
    @Override
    public ImageDescriptor getImageDescriptor()
    {
      return IM_EVENT_STEP.enabledDescriptor();
    }

    @Override
    public String getResultLabel(final int matchCount)
    {
      return "'" + searchForm.toString() + "' - " + matchCount
          + (matchCount == 1 ? " execution" : " executions");
    }

    @Override
    public Class<? extends Object> getResultType()
    {
      return ILineStepEvent.class;
    }

    @Override
    protected EventQuery createQuery(final IQueryFactory predicateFactory)
    {
      return predicateFactory.createLineExecutedQuery(searchForm);
    }
  }
}