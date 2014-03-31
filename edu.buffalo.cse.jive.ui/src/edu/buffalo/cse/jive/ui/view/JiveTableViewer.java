package edu.buffalo.cse.jive.ui.view;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_SM_EXECUTIONS_FILTERED_METHOD_ACTIVATION;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_SM_EXECUTIONS_METHOD_ACTIVATION;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;

import edu.buffalo.cse.jive.model.IEventModel.IAssignEvent;
import edu.buffalo.cse.jive.model.IEventModel.IExceptionCatchEvent;
import edu.buffalo.cse.jive.model.IEventModel.IExceptionThrowEvent;
import edu.buffalo.cse.jive.model.IEventModel.IFieldReadEvent;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.ILineStepEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodExitEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodReturnedEvent;
import edu.buffalo.cse.jive.model.IEventModel.INewObjectEvent;
import edu.buffalo.cse.jive.model.IModel.IValue;
import edu.buffalo.cse.jive.preferences.ImageInfo;

/**
 * Note: all sorting and comparator functionality has been dropped from the trace view since this is
 * a very expensive feature. If advanced processing/viewing/sorting/reporting is necessary, an RDBMS
 * based approach should be used instead of this mechanism for viewing the event store.
 */
public class JiveTableViewer extends TableViewer
{
  private static final int LAST_DEFAULT_COL = 3;
  private static final ColumnLabelProvider assignContourLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return ((IAssignEvent) element).contour().signature();
      }
    };
  private static final ColumnLabelProvider assignValueLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return ((IAssignEvent) element).newValue().toString();
      }
    };
  private static final ColumnLabelProvider assignVariableLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return ((IAssignEvent) element).member().name();
      }
    };
  private static final ColumnLabelProvider accessContourLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return ((IFieldReadEvent) element).contour().signature();
      }
    };
  private static final ColumnLabelProvider accessFieldLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return ((IFieldReadEvent) element).member().name();
      }
    };
  private static final ColumnLabelProvider assignInvariantLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return String.format("%s = %s", ((IAssignEvent) element).member().name(),
            ((IAssignEvent) element).newValue().toString());
      }
    };
  // private static final ColumnLabelProvider callActualsLabel = new ColumnLabelProvider() {
  //
  // @Override
  // public String getText(final Object element) {
  //
  // return ((MethodCallEvent) element).actualParams().toString();
  // }
  // };
  private static final ColumnLabelProvider callCallerLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return ((IMethodCallEvent) element).caller().toString();
      }
    };
  private static final ColumnLabelProvider callTargetLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return ((IMethodCallEvent) element).target().toString();
      }
    };
  private static final ColumnLabelProvider catchCatcherIdLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return ((IExceptionCatchEvent) element).contour().signature();
      }
    };
  private static final ColumnLabelProvider catchExceptionLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return ((IExceptionCatchEvent) element).exception().toString();
      }
    };
  private static final ColumnLabelProvider catchVariableIdLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        final IExceptionCatchEvent ece = ((IExceptionCatchEvent) element);
        return ece.member() != null ? ece.member().name() : "<unknown>";
      }
    };
  private static final ColumnLabelProvider detailsLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return element instanceof ILineStepEvent ? "" : ((IJiveEvent) element).details();
      }
    };
  private static final ColumnLabelProvider eventLabel = new ColumnLabelProvider()
    {
      @Override
      public Image getImage(final Object element)
      {
        final IJiveEvent event = (IJiveEvent) element;
        if (event instanceof IMethodCallEvent)
        {
          final IMethodCallEvent mca = (IMethodCallEvent) event;
          if (!mca.inModel())
          {
            return IM_SM_EXECUTIONS_FILTERED_METHOD_ACTIVATION.enabledImage();
          }
          else
          {
            return IM_SM_EXECUTIONS_METHOD_ACTIVATION.enabledImage();
          }
        }
        return ImageInfo.imageInfo(event.kind()).enabledImage();
      }

      @Override
      public String getText(final Object element)
      {
        return ((IJiveEvent) element).kind().eventName();
      }
    };
  private static final ColumnLabelProvider sourceLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return ((IJiveEvent) element).line().toString();
      }
    };
  private static final ColumnLabelProvider newContourLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return ((INewObjectEvent) element).newContour().signature();
      }
    };
  private static final ColumnLabelProvider numberLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return String.valueOf(((IJiveEvent) element).eventId());
      }
    };
  private static final ColumnLabelProvider returnPreviousContextLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return ((IMethodReturnedEvent) element).parent().execution().signature();
      }
    };
  private static final ColumnLabelProvider returnValueLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return getStringValue(((IMethodExitEvent) element).returnValue());
      }

      private String getStringValue(final IValue returnValue)
      {
        return returnValue == null ? "" : String.valueOf(returnValue);
      }
    };
  private static final ColumnLabelProvider threadIdLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return element == null || ((IJiveEvent) element).thread() == null ? ""
            : ((IJiveEvent) element).thread().name();
      }
    };
  private static final ColumnLabelProvider throwExceptionLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return ((IExceptionThrowEvent) element).exception().toString();
      }
    };
  private static final ColumnLabelProvider throwFramePoppedLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return String.valueOf(((IExceptionThrowEvent) element).framePopped());
      }
    };
  private static final ColumnLabelProvider throwThrowerLabel = new ColumnLabelProvider()
    {
      @Override
      public String getText(final Object element)
      {
        return ((IExceptionThrowEvent) element).thrower().toString();
      }
    };

  /**
   * Specialized table for AssignEvent.
   */
  public static TableViewer configureAssignEventTable(final TableViewer viewer)
  {
    JiveTableViewer.configureDefaultTable(viewer);
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 1,
        "Context", 120, SWT.LEFT, JiveTableViewer.assignContourLabel);
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 2,
        "Variable", 80, SWT.LEFT, JiveTableViewer.assignVariableLabel);
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 3, "Value",
        60, SWT.LEFT, JiveTableViewer.assignValueLabel);
    return viewer;
  }

  /**
   * Specialized table for CallEvent.
   */
  public static TableViewer configureCallEventTable(final TableViewer viewer)
  {
    JiveTableViewer.configureDefaultTable(viewer);
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 1, "Method",
        120, SWT.LEFT, JiveTableViewer.callTargetLabel);
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 2, "Caller",
        120, SWT.LEFT, JiveTableViewer.callCallerLabel);
    // createTableViewerColumn(viewer, 5, "Actuals", 120, SWT.LEFT, callActualsLabel);
    return viewer;
  }

  /**
   * Specialized table for CatchEvent.
   */
  public static TableViewer configureCatchEventTable(final TableViewer viewer)
  {
    JiveTableViewer.configureDefaultTable(viewer);
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 1,
        "Catcher", 140, SWT.LEFT, JiveTableViewer.catchCatcherIdLabel);
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 2,
        "Variable", 80, SWT.LEFT, JiveTableViewer.catchVariableIdLabel);
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 3,
        "Exception", 140, SWT.LEFT, JiveTableViewer.catchExceptionLabel);
    return viewer;
  }

  /**
   * Default table with ThreadId, Number, and Event columns.
   */
  public static TableViewer configureDefaultTable(final TableViewer viewer)
  {
    JiveTableViewer.createTableViewerColumn(viewer, 0, "Thread", 60, SWT.LEFT,
        JiveTableViewer.threadIdLabel);
    JiveTableViewer.createTableViewerColumn(viewer, 1, "Number", 60, SWT.RIGHT,
        JiveTableViewer.numberLabel);
    JiveTableViewer.createTableViewerColumn(viewer, 2, "Event", 120, SWT.LEFT,
        JiveTableViewer.eventLabel);
    JiveTableViewer.createTableViewerColumn(viewer, 3, "Source", 160, SWT.LEFT,
        JiveTableViewer.sourceLabel);
    return viewer;
  }

  /**
   * Specialized table for FieldReadEvent.
   */
  public static TableViewer configureFieldReadEventTable(final TableViewer viewer)
  {
    JiveTableViewer.configureDefaultTable(viewer);
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 1,
        "Context", 120, SWT.LEFT, JiveTableViewer.accessContourLabel);
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 2,
        "Variable", 120, SWT.LEFT, JiveTableViewer.accessFieldLabel);
    return viewer;
  }

  /**
   * Specialized table for invariant violation searches.
   */
  public static TableViewer configureInvariantViolatedTable(final TableViewer viewer)
  {
    JiveTableViewer.configureDefaultTable(viewer);
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 1,
        "Violation", 120, SWT.LEFT, JiveTableViewer.assignInvariantLabel);
    return viewer;
  }

  /**
   * Specialized table for NewEvent.
   */
  public static TableViewer configureNewEventTable(final TableViewer viewer)
  {
    JiveTableViewer.configureDefaultTable(viewer);
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 1, "Object",
        260, SWT.LEFT, JiveTableViewer.newContourLabel);
    return viewer;
  }

  /**
   * Specialized table for ReturnEvent.
   */
  public static TableViewer configureReturnEventTable(final TableViewer viewer)
  {
    JiveTableViewer.configureDefaultTable(viewer);
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 1, "Method",
        120, SWT.LEFT, JiveTableViewer.returnPreviousContextLabel);
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 2,
        "Return Value", 120, SWT.RIGHT, JiveTableViewer.returnValueLabel);
    return viewer;
  }

  /**
   * Specialized table for StepEvent.
   */
  public static TableViewer configureStepEventTable(final TableViewer viewer)
  {
    JiveTableViewer.configureDefaultTable(viewer);
    return viewer;
  }

  /**
   * Specialized table for ThrowEvent.
   */
  public static TableViewer configureThrowEventTable(final TableViewer viewer)
  {
    JiveTableViewer.configureDefaultTable(viewer);
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 1,
        "Thrower", 140, SWT.LEFT, JiveTableViewer.throwThrowerLabel);
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 2,
        "Exception", 140, SWT.LEFT, JiveTableViewer.throwExceptionLabel);
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 3,
        "Frame Popped?", 90, SWT.LEFT, JiveTableViewer.throwFramePoppedLabel);
    return viewer;
  }

  /**
   * Event table with ThreadId, Number, Event, and Details columns.
   */
  public static TableViewer configureTraceTable(final TableViewer viewer)
  {
    JiveTableViewer.configureDefaultTable(viewer);
    // viewer.setComparator(new JiveEventTableViewerComparator());
    JiveTableViewer.createTableViewerColumn(viewer, JiveTableViewer.LAST_DEFAULT_COL + 1,
        "Details", 260, SWT.LEFT, JiveTableViewer.detailsLabel);
    return viewer;
  }

  private static TableViewerColumn createTableViewerColumn(final TableViewer viewer,
      final int colNumber, final String title, final int bound, final int alignment,
      final ColumnLabelProvider label)
  {
    final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
    final TableColumn column = viewerColumn.getColumn();
    column.setAlignment(alignment);
    column.setText(title);
    column.setWidth(bound);
    column.setResizable(true);
    column.setMoveable(true);
    viewerColumn.setLabelProvider(label);
    // if (viewer instanceof JiveTableViewer) {
    // column
    // .addSelectionListener(((JiveTableViewer) viewer).getSelectionAdapter(column, colNumber));
    // }
    return viewerColumn;
  }

  public JiveTableViewer(final Composite parent)
  {
    super(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION /* | SWT.VIRTUAL */);
    getTable().setHeaderVisible(true);
  }

  // private SelectionAdapter getSelectionAdapter(final TableColumn column, final int index) {
  //
  // final SelectionAdapter selectionAdapter = new SelectionAdapter() {
  //
  // @Override
  // public void widgetSelected(final SelectionEvent e) {
  //
  // if (getComparator() == null || !(getComparator() instanceof JiveTableViewerComparator)) {
  // return;
  // }
  // ((JiveTableViewerComparator) getComparator()).setColumn(index);
  // int dir = getTable().getSortDirection();
  // if (getTable().getSortColumn() == column) {
  // dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
  // }
  // else {
  // dir = SWT.DOWN;
  // }
  // getTable().setSortDirection(dir);
  // getTable().setSortColumn(column);
  // refresh();
  // }
  // };
  // return selectionAdapter;
  // }
  public static abstract class JiveTableViewerComparator extends ViewerComparator
  {
    public abstract void setColumn(int index);
  }

  @SuppressWarnings("unused")
  private static class JiveEventTableViewerComparator extends JiveTableViewerComparator
  {
    private static final int ASCENDING = 0;
    private static final int DESCENDING = 1;
    private int direction = JiveEventTableViewerComparator.ASCENDING;
    private int propertyIndex;

    public JiveEventTableViewerComparator()
    {
      this.propertyIndex = 1;
      direction = JiveEventTableViewerComparator.ASCENDING;
    }

    @Override
    public int compare(final Viewer viewer, final Object e1, final Object e2)
    {
      // { "Thread", "Number", "Event", "Source", "Details" };
      final IJiveEvent je1 = (IJiveEvent) e1;
      final IJiveEvent je2 = (IJiveEvent) e2;
      final int defaultRc = je1.eventId() > je2.eventId() ? 1 : je1.eventId() < je2.eventId() ? -1
          : 0;
      int rc = 0;
      switch (propertyIndex)
      {
        case 0:
          rc = (direction == JiveEventTableViewerComparator.DESCENDING ? -1 : 1)
              * je1.thread().name().compareTo(je2.thread().name());
          break;
        case 2:
          rc = (direction == JiveEventTableViewerComparator.DESCENDING ? -1 : 1)
              * je1.kind().toString().compareTo(je2.kind().toString());
          break;
        case 3:
          rc = (direction == JiveEventTableViewerComparator.DESCENDING ? -1 : 1)
              * je1.line().toString().compareTo(je2.line().toString());
        case 4:
          rc = (direction == JiveEventTableViewerComparator.DESCENDING ? -1 : 1)
              * je1.details().compareTo(je2.details());
          break;
      }
      // tie breaker
      if (rc == 0)
      {
        rc = defaultRc;
      }
      return rc;
    }

    @Override
    public void setColumn(final int column)
    {
      if (column == this.propertyIndex)
      {
        // Same column as last sort; toggle the direction
        direction = 1 - direction;
      }
      else
      {
        // New column; do an ascending sort
        this.propertyIndex = column;
        direction = JiveEventTableViewerComparator.ASCENDING;
      }
    }
  }
}