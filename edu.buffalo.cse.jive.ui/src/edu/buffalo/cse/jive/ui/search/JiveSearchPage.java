package edu.buffalo.cse.jive.ui.search;

import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;

/**
 * An abstract implementation of an {@code IJiveSearchPage} used to update the status of the Search
 * button. This class also implements the {@code ModifyListener} interface, so input controls that
 * need their content checked for validity can simply add this class as a listener.
 */
public abstract class JiveSearchPage implements IJiveSearchPage, ModifyListener
{
  private ISearchPageContainer container;

  @Override
  public void modifyText(final ModifyEvent e)
  {
    updatePerformAction();
  }

  @Override
  public void setContainer(final ISearchPageContainer container)
  {
    this.container = container;
  }

  /**
   * Returns the search page container in which this page is hosted.
   */
  protected ISearchPageContainer getContainer()
  {
    return container;
  }

  /**
   * Updates the search button based on the validity of the input.
   */
  protected void updatePerformAction()
  {
    getContainer().setPerformActionEnabled(isInputValid());
  }
}