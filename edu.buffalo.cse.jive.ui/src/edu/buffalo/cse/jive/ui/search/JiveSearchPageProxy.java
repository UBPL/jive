package edu.buffalo.cse.jive.ui.search;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;

import edu.buffalo.cse.jive.ui.JiveUIPlugin;

public class JiveSearchPageProxy extends DialogPage implements ISearchPage,
    ISelectionChangedListener
{
  private ISearchPageContainer container;
  private IJiveSearchPage currentSearchPage;
  private List<JiveSearchPageDescriptor> descriptorList;
  private TableViewer queryTableViewer;
  private Composite searchPageArea;
  private StackLayout searchPageAreaLayout;

  public JiveSearchPageProxy()
  {
  }

  public JiveSearchPageProxy(final String title)
  {
    super(title);
  }

  public JiveSearchPageProxy(final String title, final ImageDescriptor image)
  {
    super(title, image);
  }

  @Override
  public void createControl(final Composite parent)
  {
    initializeDialogUnits(parent);
    final Composite control = new Composite(parent, SWT.NONE);
    control.setLayout(new RowLayout());
    setControl(control);
    queryTableViewer = new TableViewer(control, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
    queryTableViewer.setContentProvider(new IStructuredContentProvider()
      {
        @Override
        public void dispose()
        {
          // TODO Auto-generated method stub
        }

        @Override
        public Object[] getElements(final Object inputElement)
        {
          return descriptorList.toArray();
        }

        @Override
        public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput)
        {
          // TODO Auto-generated method stub
        }
      });
    queryTableViewer.setLabelProvider(new LabelProvider()
      {
        @Override
        public Image getImage(final Object element)
        {
          final JiveSearchPageDescriptor descriptor = (JiveSearchPageDescriptor) element;
          return descriptor.getSearchQueryIcon();
        }

        @Override
        public String getText(final Object element)
        {
          final JiveSearchPageDescriptor descriptor = (JiveSearchPageDescriptor) element;
          return descriptor.getSearchQueryName();
        }
      });
    queryTableViewer.addPostSelectionChangedListener(this);
    searchPageArea = new Composite(control, SWT.NONE);
    searchPageAreaLayout = new StackLayout();
    searchPageArea.setLayout(searchPageAreaLayout);
    initializeSearchPages();
  }

  @Override
  public void dispose()
  {
    queryTableViewer.removePostSelectionChangedListener(this);
    super.dispose();
  }

  @Override
  public boolean performAction()
  {
    if (currentSearchPage != null && currentSearchPage.isInputValid())
    {
      final IJiveSearchQuery query = currentSearchPage.createSearchQuery();
      NewSearchUI.runQueryInBackground(query);
      return true;
    }
    return false;
  }

  @Override
  public void selectionChanged(final SelectionChangedEvent event)
  {
    if (!event.getSelection().isEmpty())
    {
      final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
      final JiveSearchPageDescriptor descriptor = (JiveSearchPageDescriptor) selection
          .getFirstElement();
      changeToPageFor(descriptor);
    }
  }

  @Override
  public void setContainer(final ISearchPageContainer container)
  {
    this.container = container;
  }

  @Override
  public void setVisible(final boolean visible)
  {
    super.setVisible(visible);
    if (!descriptorList.isEmpty())
    {
      final JiveSearchPageDescriptor descriptor = descriptorList.get(0);
      changeToPageFor(descriptor);
      queryTableViewer.getTable().select(0);
    }
  }

  private void changeToPageFor(final JiveSearchPageDescriptor descriptor)
  {
    try
    {
      final IJiveSearchPage searchPage = descriptor.getSearchPage();
      currentSearchPage = searchPage;
      searchPageAreaLayout.topControl = searchPage.getControl();
      searchPageArea.layout();
      searchPage.initializeInput(getContainer().getSelection());
      getContainer().setPerformActionEnabled(searchPage.isInputValid());
    }
    catch (final CoreException e)
    {
      JiveUIPlugin.log(e.getStatus());
    }
    catch (final Exception e)
    {
      JiveUIPlugin.log(e);
    }
  }

  private ISearchPageContainer getContainer()
  {
    return container;
  }

  private void initializeSearchPages()
  {
    descriptorList = JiveUIPlugin.getDefault().getSearchPageDescriptors();
    queryTableViewer.setInput(descriptorList);
    for (final JiveSearchPageDescriptor descriptor : descriptorList)
    {
      try
      {
        final IJiveSearchPage page = descriptor.getSearchPage();
        page.createControl(searchPageArea);
        page.setContainer(getContainer());
      }
      catch (final CoreException e)
      {
        JiveUIPlugin.log(e.getStatus());
      }
      catch (final Exception e)
      {
        JiveUIPlugin.log(e);
      }
    }
  }
}
