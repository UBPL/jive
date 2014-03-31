package edu.buffalo.cse.jive.launch.ui;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import edu.buffalo.cse.jive.preferences.PreferenceInitializer;

public class JiveTabForm
{
  private static Composite createComposite(final Composite parent, final int columns)
  {
    final Composite control = new Composite(parent, SWT.NONE);
    control.setLayout(new GridLayout(columns, true));
    final GridData layoutData = new GridData(GridData.FILL, GridData.FILL, true, true);
    layoutData.horizontalSpan = 1;
    control.setLayoutData(layoutData);
    return control;
  }

  private final Composite control;
  /**
   * The button to add an exclusion filter to the filters list.
   */
  private Button addFilterButton;
  /**
   * The text field used to add exclusion filters.
   */
  private Text addFilterText;
  /**
   * The check button used to enable JIVE.
   */
  private Button enableJive;
  /**
   * The check button used to enable local events.
   */
  private Button enableLocalEvents;
  /**
   * The check button used to enable array events.
   */
  private Button enableArrayEvents;
  /**
   * The check button used to enable manually starting JIVE.
   */
  private Button enableManualStart;
  /**
   * The list to display the exclusion filters.
   */
  private org.eclipse.swt.widgets.List filtersList;
  /**
   * The button to remove a filter from the filters list.
   */
  private Button removeFilterButton;
  /**
   * This form's owner.
   */
  private final AbstractJiveTab tab;

  JiveTabForm(final AbstractJiveTab owner, final Composite parent)
  {
    tab = owner;
    control = JiveTabForm.createComposite(parent, 1);
    // Control for enabling JIVE
    createControlEnableJive();
    // Control for manually starting JIVE
    createControlManualStart();
    // Control for enabling local events
    createControlLocalEvents();
    // Control for enabling array events
    createControlEnableArrayEvents();
    final Composite c1 = JiveTabForm.createComposite(control, 2);
    // Control for entering exclusion filters
    createControlAddFilter(c1);
    // Control for adding a filter
    createControlAddFilterButton(c1);
    // Control for listing filters
    createControlFilterList(c1);
    // Control for removing a filter
    createControlRemoveFilter(c1);
  }

  public Control control()
  {
    return control;
  }

  /**
   * Adds the user-provided filter to the filters list.
   */
  private void addFilter()
  {
    final String newFilter = addFilterText.getText().trim();
    if (!newFilter.isEmpty())
    {
      if (validateFilter(newFilter))
      {
        for (final String filter : filtersList.getItems())
        {
          if (newFilter.equals(filter))
          {
            return;
          }
        }
        filtersList.add(newFilter);
        addFilterText.setText("Enter Exclusion Filter");
      }
      else
      {
        addFilterText.setText("Enter Exclusion Filter");
      }
    }
    else
    {
      addFilterText.setText("Enter Exclusion Filter");
    }
  }

  private void createControlAddFilter(final Composite c1)
  {
    addFilterText = new Text(c1, SWT.BORDER);
    addFilterText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    addFilterText.setToolTipText("Enter Exclusion Filter");
    addFilterText.setText("Enter Exclusion Filter");
    addFilterText.addKeyListener(new KeyListener()
      {
        @Override
        public void keyPressed(final KeyEvent e)
        {
        }

        @Override
        public void keyReleased(final KeyEvent e)
        {
          final String data = addFilterText.getText();
          addFilterButton.setEnabled(data != null && !data.trim().isEmpty());
        }
      });
    addFilterText.addFocusListener(new FocusListener()
      {
        @Override
        public void focusGained(final FocusEvent e)
        {
          if (addFilterText.getText().trim().equals("Enter Exclusion Filter"))
          {
            addFilterText.setText("");
          }
        }

        @Override
        public void focusLost(final FocusEvent e)
        {
          if (addFilterText.getText().trim().isEmpty())
          {
            addFilterText.setText("Enter Exclusion Filter");
          }
        }
      });
  }

  private void createControlAddFilterButton(final Composite c1)
  {
    addFilterButton = new Button(c1, SWT.NONE);
    addFilterButton.setText("Add Filter");
    addFilterButton.setEnabled(false);
    addFilterButton.addSelectionListener(new SelectionListener()
      {
        @Override
        public void widgetDefaultSelected(final SelectionEvent e)
        {
        }

        @Override
        public void widgetSelected(final SelectionEvent e)
        {
          addFilter();
          addFilterButton.setEnabled(false);
          tab.widgetModified();
        }
      });
  }

  private void createControlEnableJive()
  {
    enableJive = new Button(control, SWT.CHECK);
    enableJive.setText("Enable debugging with JIVE."); // TODO Add NLS support
    enableJive.setLayoutData(new GridData());
    enableJive.setSelection(false);
    enableJive.addSelectionListener(new SelectionListener()
      {
        @Override
        public void widgetDefaultSelected(final SelectionEvent e)
        {
        }

        @Override
        public void widgetSelected(final SelectionEvent e)
        {
          tab.widgetModified();
        }
      });
  }

  private void createControlEnableArrayEvents()
  {
    enableArrayEvents = new Button(control, SWT.CHECK);
    enableArrayEvents.setText("Enable array events?"); // TODO Add NLS support
    enableArrayEvents.setLayoutData(new GridData());
    enableArrayEvents.setSelection(false);
    enableArrayEvents.addSelectionListener(new SelectionListener()
      {
        @Override
        public void widgetDefaultSelected(final SelectionEvent e)
        {
        }

        @Override
        public void widgetSelected(final SelectionEvent e)
        {
          tab.widgetModified();
        }
      });
  }

  private void createControlFilterList(final Composite c1)
  {
    filtersList = new org.eclipse.swt.widgets.List(c1, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL
        | SWT.H_SCROLL);
    filtersList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    filtersList.addSelectionListener(new SelectionListener()
      {
        @Override
        public void widgetDefaultSelected(final SelectionEvent e)
        {
        }

        @Override
        public void widgetSelected(final SelectionEvent e)
        {
          removeFilterButton.setEnabled(filtersList.getSelectionCount() > 0);
        }
      });
  }

  private void createControlLocalEvents()
  {
    enableLocalEvents = new Button(control, SWT.CHECK);
    enableLocalEvents.setText("Enable local assign/delete events?"); // TODO Add NLS support
    enableLocalEvents.setLayoutData(new GridData());
    enableLocalEvents.setSelection(true);
    enableLocalEvents.addSelectionListener(new SelectionListener()
      {
        @Override
        public void widgetDefaultSelected(final SelectionEvent e)
        {
        }

        @Override
        public void widgetSelected(final SelectionEvent e)
        {
          tab.widgetModified();
        }
      });
  }

  private void createControlManualStart()
  {
    enableManualStart = new Button(control, SWT.CHECK);
    enableManualStart.setText("Start JIVE manually."); // TODO Add NLS support
    enableManualStart.setLayoutData(new GridData());
    enableManualStart.setSelection(false);
    enableManualStart.addSelectionListener(new SelectionListener()
      {
        @Override
        public void widgetDefaultSelected(final SelectionEvent e)
        {
        }

        @Override
        public void widgetSelected(final SelectionEvent e)
        {
          tab.widgetModified();
        }
      });
  }

  private void createControlRemoveFilter(final Composite c1)
  {
    removeFilterButton = new Button(c1, SWT.NONE);
    removeFilterButton.setText("Remove Filter");
    removeFilterButton.setEnabled(false);
    removeFilterButton.addSelectionListener(new SelectionListener()
      {
        @Override
        public void widgetDefaultSelected(final SelectionEvent e)
        {
        }

        @Override
        public void widgetSelected(final SelectionEvent e)
        {
          removeFilters();
          addFilterText.setText("Enter Exclusion Filter");
          // set it to false if done deleting.
          removeFilterButton.setEnabled(false);
          tab.widgetModified();
        }
      });
  }

  /**
   * Removes the selected filters from the filter list.
   */
  private void removeFilters()
  {
    final int[] selectedFilters = filtersList.getSelectionIndices();
    filtersList.remove(selectedFilters);
  }

  /**
   * Validates event filters entered by the user.
   * 
   * @param filter
   *          the event filter to validate
   * @return <code>true</code> if the filter is valid, otherwise <code>false</code>
   */
  private boolean validateFilter(final String filter)
  {
    return !filter.contains(PreferenceInitializer.EVENT_FILTER_DELIMITER);
  }

  /**
   * Enables the tab's controls if the argument is <code>true</code>, and disables them otherwise.
   * 
   * @param enabled
   *          the new enabled state of each control
   */
  void enableControls(final boolean enabled)
  {
    enableJive.setEnabled(enabled);
    enableLocalEvents.setEnabled(enabled);
    enableArrayEvents.setEnabled(enabled);
    enableManualStart.setEnabled(enabled);
    addFilterText.setEnabled(enabled);
    addFilterButton.setEnabled(enabled && addFilterButton.isEnabled());
    removeFilterButton.setEnabled(enabled && removeFilterButton.isEnabled());
    filtersList.setEnabled(enabled);
  }

  void enableJive(final boolean value)
  {
    enableJive.setSelection(value);
  }

  void enableLocalEvents(final boolean value)
  {
    enableLocalEvents.setSelection(value);
  }

  void enableArrayEvents(final boolean value)
  {
    enableArrayEvents.setSelection(value);
  }

  void enableManualStart(final boolean value)
  {
    enableManualStart.setSelection(value);
  }

  Collection<? extends String> filterList()
  {
    return Arrays.asList(filtersList.getItems());
  }

  boolean generateLocalEvents()
  {
    return enableLocalEvents.getSelection();
  }

  boolean generateArrayEvents()
  {
    return enableArrayEvents.getSelection();
  }

  boolean isJiveEnabled()
  {
    return enableJive.getSelection();
  }

  boolean isManualStart()
  {
    return enableManualStart.getSelection();
  }

  void updateFilters(final List<String> exclusionFilters)
  {
    if (filtersList.getItemCount() == 0)
    {
      for (final String filter : exclusionFilters)
      {
        filtersList.add(filter);
      }
    }
  }
}