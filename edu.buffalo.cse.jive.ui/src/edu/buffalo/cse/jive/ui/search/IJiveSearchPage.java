package edu.buffalo.cse.jive.ui.search;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A page used by the Search dialog to define the user input fields of an {@code IJiveSearchQuery}
 * and to create the query when the Search button is selected.
 */
public interface IJiveSearchPage
{
  /**
   * Creates the UI controls for the search page. The page's top-level UI {@code Control} should
   * have the supplied {@code Composite} as its parent.
   * 
   * @param parent
   *          the parent of the page's top-level UI control
   */
  void createControl(Composite parent);

  /**
   * Returns a new {@code IJiveSearchQuery} to be used to perform the search. This method is called
   * when the Search button is selected.
   * 
   * @return a new search query to be run
   */
  IJiveSearchQuery createSearchQuery();

  /**
   * Returns the page's top-level UI control which was created by {@link #createControl(Composite)}.
   * 
   * @return the page's top-level UI control
   */
  Control getControl();

  /**
   * Initializes the search page's input fields using the provided {@code ISelection}. This method
   * is called whenever the search page is displayed by the Search dialog.
   * 
   * @param selection
   *          the current workbench selection
   */
  void initializeInput(ISelection selection);

  /**
   * Returns whether the search page's input is valid. This method should be called to determine if
   * the Search button should be enabled.
   * 
   * @return <code>true</code> if the input is valid, <code>false</code> otherwise
   */
  boolean isInputValid();

  /**
   * Sets the supplied {@code ISearchPageContainer} as the container of the page. This allows the
   * page to enable or disable the Search button based on the input fields.
   * 
   * @param container
   *          the search container for the page
   */
  void setContainer(ISearchPageContainer container);
}
