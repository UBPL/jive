package edu.buffalo.cse.jive.exporter.trace;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IObjectContour;
import edu.buffalo.cse.jive.model.IEventModel.IAssignEvent;
import edu.buffalo.cse.jive.model.IEventModel.IDestroyObjectEvent;
import edu.buffalo.cse.jive.model.IEventModel.IExceptionCatchEvent;
import edu.buffalo.cse.jive.model.IEventModel.IExceptionThrowEvent;
import edu.buffalo.cse.jive.model.IEventModel.IFieldReadEvent;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodCallEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodExitEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMethodReturnedEvent;
import edu.buffalo.cse.jive.model.IEventModel.IMonitorEvent;
import edu.buffalo.cse.jive.model.IEventModel.INewObjectEvent;
import edu.buffalo.cse.jive.model.IEventModel.INewThreadEvent;
import edu.buffalo.cse.jive.model.IEventModel.IRealTimeEvent;
import edu.buffalo.cse.jive.model.IEventModel.IRealTimeThreadEvent;
import edu.buffalo.cse.jive.model.IEventModel.IScopeAllocEvent;
import edu.buffalo.cse.jive.model.IEventModel.IScopeAssignEvent;
import edu.buffalo.cse.jive.model.IEventModel.IScopeBackingAllocEvent;
import edu.buffalo.cse.jive.model.IEventModel.IScopeEvent;
import edu.buffalo.cse.jive.model.IEventModel.IThreadTimedEvent;
import edu.buffalo.cse.jive.model.IEventModel.ITypeLoadEvent;
import edu.buffalo.cse.jive.model.IEventModel.IVarDeleteEvent;
import edu.buffalo.cse.jive.model.IExecutionModel;
import edu.buffalo.cse.jive.model.IModel.IContourReference;
import edu.buffalo.cse.jive.model.IModel.IFileValue;
import edu.buffalo.cse.jive.model.IModel.ILineValue;
import edu.buffalo.cse.jive.model.IModel.IOutOfModelMethodKeyReference;
import edu.buffalo.cse.jive.model.IModel.IOutOfModelMethodReference;
import edu.buffalo.cse.jive.model.IModel.IResolvedValue;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;
import edu.buffalo.cse.jive.model.IModel.IValue;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IMethodDependenceGraph;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedCall;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedData;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedLazyData;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedLine;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedNode;
import edu.buffalo.cse.jive.model.IStaticAnalysis.IResolvedThis;
import edu.buffalo.cse.jive.model.IStaticModel.IDataNode;
import edu.buffalo.cse.jive.model.IStaticModel.IFileNode;
import edu.buffalo.cse.jive.model.IStaticModel.IMethodNode;
import edu.buffalo.cse.jive.model.IStaticModel.INode;
import edu.buffalo.cse.jive.model.IStaticModel.INodeRef;
import edu.buffalo.cse.jive.model.IStaticModel.IRootNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNode;
import edu.buffalo.cse.jive.model.IStaticModel.ITypeNodeRef;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;

public class JDBCExporter
{
  private static final AtomicInteger SCHEMA_ID = new AtomicInteger(0);
  private static final String CREATE_SCHEMA = "CREATE SCHEMA %s;";
  private static final String ALTER_SCHEMA = "ALTER DEFAULT PRIVILEGES IN SCHEMA %s GRANT ALL ON TABLES TO public;";
  private static final String GRANT_SCHEMA = "GRANT ALL ON SCHEMA %s TO public;";
  private static final String SEARCH_PATH = "SET search_path TO %s,ctdb,public;";
  private IExecutionModel model;

  public JDBCExporter(final IExecutionModel model)
  {
    this.model = model;
  }

  private int createContour(final Connection conn, final IContour contour, final IJiveEvent event,
      final PreparedStatement pstmt_contour) throws SQLException
  {
    int stmtCount = 0;
    if (event instanceof INewObjectEvent && contour.parent() != null)
    {
      stmtCount += createContour(conn, contour.parent(), event, pstmt_contour);
    }
    pstmt_contour.setLong(1, contour.id());
    pstmt_contour.setLong(2, contour.schema().id());
    if (contour.parent() != null)
    {
      pstmt_contour.setLong(3, contour.parent().id());
    }
    else
    {
      pstmt_contour.setNull(3, java.sql.Types.BIGINT);
    }
    if (contour instanceof IObjectContour)
    {
      pstmt_contour.setLong(4, ((IObjectContour) contour).oid());
    }
    else
    {
      pstmt_contour.setNull(4, java.sql.Types.BIGINT);
    }
    pstmt_contour.setLong(5, contour.ordinalId());
    pstmt_contour.setInt(6, contour.kind().ordinal() + 1);
    pstmt_contour.setLong(7, event.eventId());
    pstmt_contour.addBatch();
    stmtCount++;
    return stmtCount;
  }

  private int destroyContour(final Connection conn, final IContour contour, final IJiveEvent event,
      final PreparedStatement pstmt_contour) throws SQLException
  {
    int stmtCount = 0;
    if (event instanceof IDestroyObjectEvent && contour.parent() != null)
    {
      stmtCount += destroyContour(conn, contour.parent(), event, pstmt_contour);
    }
    pstmt_contour.setLong(1, event.eventId());
    pstmt_contour.setLong(2, contour.id());
    pstmt_contour.addBatch();
    stmtCount++;
    return stmtCount;
  }

  private void exportASTDataNodes(final PreparedStatement pstmt,
      final PreparedStatement pstmt_data, final Iterator<IDataNode> iterator) throws SQLException
  {
    while (iterator.hasNext())
    {
      final IDataNode data = iterator.next();
      // add the data node
      setNodeParams(pstmt, data);
      pstmt.addBatch();
      // process data node
      pstmt_data.setLong(1, data.id());
      pstmt_data.setLong(2, data.defaultValue().id());
      pstmt_data.setInt(3, data.index());
      pstmt_data.setLong(4, data.type().id());
      pstmt_data.addBatch();
    }
  }

  private void exportASTNodeRefs(final Connection conn, final Iterator<? extends INodeRef> iterator)
      throws SQLException
  {
    final String SQL_NODEREF_METHOD = "INSERT INTO noderef_method(refId, key) VALUES(?,?);";
    final String SQL_NODEREF_TYPE = "INSERT INTO noderef_type(refId, key) VALUES(?,?);";
    final PreparedStatement pstmt_method = conn.prepareStatement(SQL_NODEREF_METHOD);
    final PreparedStatement pstmt_type = conn.prepareStatement(SQL_NODEREF_TYPE);
    final PreparedStatement[] pstmts = new PreparedStatement[]
    { pstmt_method, pstmt_type };
    try
    {
      while (iterator.hasNext())
      {
        final INodeRef ref = iterator.next();
        // process node reference
        if (ref instanceof ITypeNodeRef)
        {
          pstmt_type.setLong(1, ref.id());
          pstmt_type.setString(2, ref.key());
          pstmt_type.addBatch();
        }
        else
        {
          pstmt_method.setLong(1, ref.id());
          pstmt_method.setString(2, ref.key());
          pstmt_method.addBatch();
        }
      }
      // execute batch
      JDBCTools.executeBatches(pstmts);
    }
    finally
    {
      JDBCTools.closeStatements(pstmts);
    }
  }

  private void exportASTNodes(final Connection conn, final IRootNode root) throws SQLException
  {
    final String SQL_NODE = "INSERT INTO node(nid, kind, lineFrom, lineTo, modifiers, name, origin, parentId, visibility) VALUES(?,?,?,?,?::INT[],?,?,?,?);";
    final String SQL_NODE_TYPE = "INSERT INTO node_type(nid, key, defaultValueId, superClassRefId) VALUES(?,?,?,?);";
    final String SQL_NODE_METHOD = "INSERT INTO node_method(nid, key, index, returnTypeRefId) VALUES(?,?,?,?);";
    final String SQL_NODE_DATA = "INSERT INTO node_data(nid, defaultValueId, index, typeRefId) VALUES(?,?,?,?);";
    final String SQL_NODE_TYPE_INTF = "INSERT INTO node_type_interface(nid, refId) VALUES(?,?);";
    final String SQL_NODE_METHOD_EXCP = "INSERT INTO node_method_exception(nid, refId) VALUES(?,?);";
    final PreparedStatement pstmt = conn.prepareStatement(SQL_NODE);
    final PreparedStatement pstmt_type = conn.prepareStatement(SQL_NODE_TYPE);
    final PreparedStatement pstmt_method = conn.prepareStatement(SQL_NODE_METHOD);
    final PreparedStatement pstmt_data = conn.prepareStatement(SQL_NODE_DATA);
    final PreparedStatement pstmt_intfs = conn.prepareStatement(SQL_NODE_TYPE_INTF);
    final PreparedStatement pstmt_excps = conn.prepareStatement(SQL_NODE_METHOD_EXCP);
    final PreparedStatement[] pstmts = new PreparedStatement[]
    { pstmt, pstmt_type, pstmt_method, pstmt_data, pstmt_intfs, pstmt_excps };
    try
    {
      // add the root node
      setNodeParams(pstmt, root);
      pstmt.addBatch();
      // add the types nested under the root node
      exportASTTypeNodes(pstmt, pstmt_type, pstmt_method, pstmt_data, pstmt_intfs, pstmt_excps,
          root.types().iterator());
      // add the files nested under the root node
      final Iterator<? extends IFileNode> files = root.files().iterator();
      while (files.hasNext())
      {
        // add the file node
        final IFileNode file = files.next();
        setNodeParams(pstmt, file);
        pstmt.addBatch();
        // add the types nested under the file node
        exportASTTypeNodes(pstmt, pstmt_type, pstmt_method, pstmt_data, pstmt_intfs, pstmt_excps,
            file.types().iterator());
      }
      // execute all batches
      JDBCTools.executeBatches(pstmts);
    }
    finally
    {
      JDBCTools.closeStatements(pstmts);
    }
  }

  private void exportASTTypeNodes(final PreparedStatement pstmt,
      final PreparedStatement pstmt_type, final PreparedStatement pstmt_method,
      final PreparedStatement pstmt_data, final PreparedStatement pstmt_intfs,
      final PreparedStatement pstmt_excps, final Iterator<? extends ITypeNode> iterator)
      throws SQLException
  {
    while (iterator.hasNext())
    {
      final ITypeNode type = iterator.next();
      // add the type node
      setNodeParams(pstmt, type);
      pstmt.addBatch();
      // process type node
      pstmt_type.setLong(1, type.id());
      pstmt_type.setString(2, type.key());
      if (type.defaultValue() != null)
      {
        pstmt_type.setLong(3, type.defaultValue().id());
      }
      else
      {
        pstmt_type.setNull(3, java.sql.Types.BIGINT);
      }
      if (type.superClass() != null)
      {
        pstmt_type.setLong(4, type.superClass().id());
      }
      else
      {
        pstmt_type.setNull(4, java.sql.Types.BIGINT);
      }
      pstmt_type.addBatch();
      // process field nodes
      exportASTDataNodes(pstmt, pstmt_data, type.dataMembers().values().iterator());
      // process method nodes
      final Iterator<? extends IMethodNode> methods = type.methodMembers().values().iterator();
      while (methods.hasNext())
      {
        final IMethodNode method = methods.next();
        // add the method node
        setNodeParams(pstmt, method);
        pstmt.addBatch();
        pstmt_method.setLong(1, method.id());
        pstmt_method.setString(2, method.key());
        pstmt_method.setInt(3, method.index());
        if (method.returnType() != null)
        {
          pstmt_method.setLong(4, method.returnType().id());
        }
        else
        {
          pstmt_method.setNull(4, java.sql.Types.BIGINT);
        }
        pstmt_method.addBatch();
        exportASTDataNodes(pstmt, pstmt_data, method.dataMembers().values().iterator());
        exportASTTypeNodes(pstmt, pstmt_type, pstmt_method, pstmt_data, pstmt_intfs, pstmt_excps,
            method.localTypes().iterator());
        // process method exceptions
        final Iterator<? extends ITypeNodeRef> exceptions = method.thrownExceptions().iterator();
        while (exceptions.hasNext())
        {
          final ITypeNodeRef excp = exceptions.next();
          pstmt_excps.setLong(1, method.id());
          pstmt_excps.setLong(2, excp.id());
          pstmt_excps.addBatch();
        }
      }
      // process super interfaces
      final Iterator<? extends ITypeNodeRef> interfaces = type.superInterfaces().iterator();
      while (interfaces.hasNext())
      {
        final ITypeNodeRef intf = interfaces.next();
        pstmt_intfs.setLong(1, type.id());
        pstmt_intfs.setLong(2, intf.id());
        pstmt_intfs.addBatch();
      }
    }
  }

  private void exportDynamic(final Connection conn) throws SQLException
  {
    exportEvents(conn, model.traceView().events().iterator());
  }

  /**
   * TODO: reduce the cyclomatic complexity.
   */
  private void exportEvents(final Connection conn, final Iterator<? extends IJiveEvent> iterator)
      throws SQLException
  {
    final int BATCH_SIZE = 20000;
    //
    // SQL
    //
    final String SQL_EVENT_DEFAULT = "INSERT INTO event(eventId, parentId, threadId, lineId, kind) VALUES(?,?,?,?,?);";
    final String SQL_EVENT_ECATCH = "INSERT INTO event_ecatch(eventId, exceptionId) VALUES(?,?);";
    final String SQL_EVENT_ETHROW = "INSERT INTO event_ethrow(eventId, throwerId, exceptionId, framePopped) VALUES(?,?,?,?);";
    final String SQL_EVENT_FREAD = "INSERT INTO event_fread(eventId, contourId, schemaId) VALUES(?,?,?);";
    final String SQL_EVENT_MCALL = "INSERT INTO event_mcall(eventId, callerId, targetId) VALUES(?,?,?);";
    final String SQL_EVENT_MEXIT = "INSERT INTO event_mexit(eventId, returnContextId, returnValueId) VALUES(?,?,?);";
    final String SQL_EVENT_MRET = "INSERT INTO event_mret(eventId, terminatorId) VALUES(?,?);";
    final String SQL_CONTOUR = "INSERT INTO contour(contourId, schemaId, parentId, oid, ordinal, kind, fromTime) VALUES(?,?,?,?,?,?,?);";
    final String SQL_CONTOUR_MEMBER = "INSERT INTO contour_member(contourId, schemaId, valueId, fromTime) VALUES(?,?,?,?);";
    //
    // Ji.Fi 1.0
    //
    final String SQL_RTEVENT = "INSERT INTO event_rt(eventId, eventTime) VALUES(?,?);";
    final String SQL_RTEVENT_MONITOR = "INSERT INTO event_rtmonitor(eventId, monitor) VALUES(?,?);";
    final String SQL_RTEVENT_SLEEP = "INSERT INTO event_rtsleep(eventId, wakeTime) VALUES(?,?);";
    final String SQL_RTEVENT_THREAD = "INSERT INTO event_rtthread(eventId, priority, scheduler) VALUES(?,?,?);";
    final String SQL_RTEVENT_THREAD_NEW = "INSERT INTO event_rtthreadnew(eventId, newThreadId) VALUES(?,?);";
    //
    // Ji.Fi 1.1
    //
    final String SQL_RT_BACKING_ALLOC = "INSERT INTO event_rtbackingalloc(eventId, size) VALUES(?,?);";
    final String SQL_RTSCOPE_ASSIGN = "INSERT INTO event_rtscopeassign(eventId, scopeLHS, indexLHS, lhs, scopeRHS, indexRHS, rhs) VALUES(?,?,?,?,?,?,?);";
    final String SQL_RTSCOPE_ALLOC = "INSERT INTO event_rtscopealloc(eventId, size, immortal) VALUES(?,?,?);";
    final String SQL_RTSCOPE = "INSERT INTO event_rtscope(eventId, scope) VALUES(?,?);";
    final String SQL_RTDESTROY = "UPDATE contour SET toTime = ? WHERE contourId = ?;";
    //
    // Prepared Statements
    //
    final PreparedStatement pstmt = conn.prepareStatement(SQL_EVENT_DEFAULT);
    final PreparedStatement pstmt_ecatch = conn.prepareStatement(SQL_EVENT_ECATCH);
    final PreparedStatement pstmt_ethrow = conn.prepareStatement(SQL_EVENT_ETHROW);
    final PreparedStatement pstmt_fread = conn.prepareStatement(SQL_EVENT_FREAD);
    final PreparedStatement pstmt_mcall = conn.prepareStatement(SQL_EVENT_MCALL);
    final PreparedStatement pstmt_mexit = conn.prepareStatement(SQL_EVENT_MEXIT);
    final PreparedStatement pstmt_mret = conn.prepareStatement(SQL_EVENT_MRET);
    final PreparedStatement pstmt_contour = conn.prepareStatement(SQL_CONTOUR);
    final PreparedStatement pstmt_cmember = conn.prepareStatement(SQL_CONTOUR_MEMBER);
    //
    // Ji.Fi 1.0
    //
    final PreparedStatement pstmt_rt = conn.prepareStatement(SQL_RTEVENT);
    final PreparedStatement pstmt_rtmonitor = conn.prepareStatement(SQL_RTEVENT_MONITOR);
    final PreparedStatement pstmt_rttimed = conn.prepareStatement(SQL_RTEVENT_SLEEP);
    final PreparedStatement pstmt_rtthread = conn.prepareStatement(SQL_RTEVENT_THREAD);
    final PreparedStatement pstmt_rtthreadnew = conn.prepareStatement(SQL_RTEVENT_THREAD_NEW);
    //
    // Ji.Fi 1.1
    //
    final PreparedStatement pstmt_rtbackingalloc = conn.prepareStatement(SQL_RT_BACKING_ALLOC);
    final PreparedStatement pstmt_rtscopeassign = conn.prepareStatement(SQL_RTSCOPE_ASSIGN);
    final PreparedStatement pstmt_rtscopealloc = conn.prepareStatement(SQL_RTSCOPE_ALLOC);
    final PreparedStatement pstmt_rtscope = conn.prepareStatement(SQL_RTSCOPE);
    final PreparedStatement pstmt_rtdestroy = conn.prepareStatement(SQL_RTDESTROY);
    int stmtCount = 0;
    /**
     * The order here is important-- e.g., the contour and contour member records must exist so that
     * the trigger on the method exit events correctly updates those tables.
     */
    final PreparedStatement[] pstmts = new PreparedStatement[]
    { pstmt, pstmt_ecatch, pstmt_ethrow, pstmt_fread, pstmt_mcall, pstmt_mret, pstmt_contour,
        pstmt_cmember, pstmt_mexit, pstmt_rttimed, pstmt_rtthread, pstmt_rtthreadnew,
        pstmt_rtmonitor, pstmt_rt, pstmt_rtbackingalloc, pstmt_rtscope, pstmt_rtscopealloc,
        pstmt_rtscopeassign, pstmt_rtdestroy };
    try
    {
      while (iterator.hasNext())
      {
        final IJiveEvent event = iterator.next();
        //
        // default processing for all events
        //
        pstmt.setLong(1, event.eventId());
        if (event.parent() == null)
        {
          pstmt.setNull(2, java.sql.Types.BIGINT);
        }
        else
        {
          pstmt.setLong(2, event.parent().eventId());
        }
        pstmt.setLong(3, event.thread().id());
        pstmt.setLong(4, event.line().id());
        pstmt.setInt(5, event.kind().ordinal() + 1);
        pstmt.addBatch();
        stmtCount++;
        //
        // events that require additional data stored
        //
        // process catch
        if (event instanceof IExceptionCatchEvent)
        {
          pstmt_ecatch.setLong(1, event.eventId());
          pstmt_ecatch.setLong(2, ((IExceptionCatchEvent) event).exception().id());
          pstmt_ecatch.addBatch();
          stmtCount++;
        }
        // process throw
        else if (event instanceof IExceptionThrowEvent)
        {
          pstmt_ethrow.setLong(1, event.eventId());
          pstmt_ethrow.setLong(2, ((IExceptionThrowEvent) event).thrower().id());
          if (((IExceptionThrowEvent) event).exception() == null)
          {
            pstmt_ethrow.setNull(3, java.sql.Types.BIGINT);
          }
          else
          {
            pstmt_ethrow.setLong(3, ((IExceptionThrowEvent) event).exception().id());
          }
          pstmt_ethrow.setBoolean(4, ((IExceptionThrowEvent) event).framePopped());
          pstmt_ethrow.addBatch();
          stmtCount++;
        }
        // process field read
        else if (event instanceof IFieldReadEvent)
        {
          pstmt_fread.setLong(1, event.eventId());
          pstmt_fread.setLong(2, ((IFieldReadEvent) event).contour().id());
          pstmt_fread.setLong(3, ((IFieldReadEvent) event).member().schema().id());
          pstmt_fread.addBatch();
          stmtCount++;
        }
        // process method called
        else if (event instanceof IMethodCallEvent)
        {
          pstmt_mcall.setLong(1, event.eventId());
          pstmt_mcall.setLong(2, ((IMethodCallEvent) event).caller().id());
          pstmt_mcall.setLong(3, ((IMethodCallEvent) event).target().id());
          pstmt_mcall.addBatch();
          stmtCount++;
        }
        // process method exit
        else if (event instanceof IMethodExitEvent)
        {
          pstmt_mexit.setLong(1, event.eventId());
          pstmt_mexit.setLong(2, ((IMethodExitEvent) event).returnContext().id());
          pstmt_mexit.setLong(3, ((IMethodExitEvent) event).returnValue().id());
          pstmt_mexit.addBatch();
          stmtCount++;
        }
        // process method returned
        else if (event instanceof IMethodReturnedEvent)
        {
          pstmt_mret.setLong(1, event.eventId());
          pstmt_mret.setLong(2, ((IMethodReturnedEvent) event).terminator().eventId());
          pstmt_mret.addBatch();
          stmtCount++;
        }
        // process assign events
        else if (event instanceof IAssignEvent)
        {
          final IAssignEvent assign = (IAssignEvent) event;
          pstmt_cmember.setLong(1, assign.contour().id());
          pstmt_cmember.setLong(2, assign.member().schema().id());
          pstmt_cmember.setLong(3, assign.newValue().id());
          pstmt_cmember.setLong(4, event.eventId());
          pstmt_cmember.addBatch();
          stmtCount++;
        }
        // process delete events
        else if (event instanceof IVarDeleteEvent)
        {
          final IVarDeleteEvent delete = (IVarDeleteEvent) event;
          pstmt_cmember.setLong(1, delete.contour().id());
          pstmt_cmember.setLong(2, delete.member().schema().id());
          pstmt_cmember.setLong(3, model.valueFactory().createUninitializedValue().id());
          pstmt_cmember.setLong(4, event.eventId());
          pstmt_cmember.addBatch();
          stmtCount++;
        }
        // process real-time monitor events
        else if (event instanceof IMonitorEvent)
        {
          final IMonitorEvent monitor = (IMonitorEvent) event;
          pstmt_rtmonitor.setLong(1, event.eventId());
          pstmt_rtmonitor.setString(2, monitor.monitor());
          pstmt_rtmonitor.addBatch();
          stmtCount++;
        }
        else if (event instanceof IScopeBackingAllocEvent)
        {
          final IScopeBackingAllocEvent scope = (IScopeBackingAllocEvent) event;
          pstmt_rtbackingalloc.setLong(1, event.eventId());
          pstmt_rtbackingalloc.setLong(2, scope.size());
          pstmt_rtbackingalloc.addBatch();
          stmtCount++;
        }
        else if (event instanceof IScopeAssignEvent)
        {
          final IScopeAssignEvent scope = (IScopeAssignEvent) event;
          pstmt_rtscopeassign.setLong(1, event.eventId());
          pstmt_rtscopeassign.setString(2, scope.scope());
          pstmt_rtscopeassign.setInt(3, scope.indexLHS());
          pstmt_rtscopeassign.setLong(4, scope.lhs());
          pstmt_rtscopeassign.setString(5, scope.scopeRHS());
          pstmt_rtscopeassign.setInt(6, scope.indexRHS());
          pstmt_rtscopeassign.setLong(7, scope.rhs());
          pstmt_rtscopeassign.addBatch();
          stmtCount++;
        }
        // process real-time monitor events
        else if (event instanceof IScopeEvent)
        {
          final IScopeEvent scope = (IScopeEvent) event;
          pstmt_rtscope.setLong(1, event.eventId());
          pstmt_rtscope.setString(2, scope.scope());
          pstmt_rtscope.addBatch();
          stmtCount++;
          if (event instanceof IScopeAllocEvent)
          {
            final IScopeAllocEvent alloc = (IScopeAllocEvent) event;
            pstmt_rtscopealloc.setLong(1, event.eventId());
            pstmt_rtscopealloc.setLong(2, alloc.size());
            pstmt_rtscopealloc.setBoolean(3, alloc.isImmortal());
            pstmt_rtscopealloc.addBatch();
            stmtCount++;
          }
        }
        // process object destroyed
        else if (event instanceof IDestroyObjectEvent)
        {
          final IDestroyObjectEvent destroy = (IDestroyObjectEvent) event;
          // destroy contour
          if (destroy.destroyedContour() != null)
          {
            stmtCount += destroyContour(conn, destroy.destroyedContour(), event, pstmt_rtdestroy);
          }
        }
        // process real-time thread events
        else if (event instanceof IRealTimeThreadEvent)
        {
          final IRealTimeThreadEvent rtt = (IRealTimeThreadEvent) event;
          pstmt_rtthread.setLong(1, event.eventId());
          pstmt_rtthread.setInt(2, rtt.priority());
          pstmt_rtthread.setString(3, rtt.scheduler());
          pstmt_rtthread.addBatch();
          stmtCount++;
        }
        // process real-time sleep events
        else if (event instanceof IThreadTimedEvent)
        {
          final IThreadTimedEvent timed = (IThreadTimedEvent) event;
          pstmt_rttimed.setLong(1, event.eventId());
          pstmt_rttimed.setLong(2, timed.wakeTime());
          pstmt_rttimed.addBatch();
          stmtCount++;
        }
        // process new thread events
        else if (event instanceof INewThreadEvent)
        {
          final INewThreadEvent newThread = (INewThreadEvent) event;
          pstmt_rtthreadnew.setLong(1, event.eventId());
          pstmt_rtthreadnew.setLong(2, newThread.newThreadId());
          pstmt_rtthreadnew.addBatch();
          stmtCount++;
        }
        //
        // standard processing for real-time events
        //
        if (event instanceof IRealTimeEvent)
        {
          final IRealTimeEvent rte = (IRealTimeEvent) event;
          pstmt_rt.setLong(1, event.eventId());
          pstmt_rt.setLong(2, rte.timestamp());
          pstmt_rt.addBatch();
          stmtCount++;
        }
        // retrieve the newly created contour, if any
        final IContour contour = event instanceof ITypeLoadEvent ? ((ITypeLoadEvent) event)
            .newContour() : event instanceof INewObjectEvent ? ((INewObjectEvent) event)
            .newContour() : event instanceof IMethodCallEvent ? ((IMethodCallEvent) event)
            .execution() : null;
        // process contour
        if (contour != null)
        {
          stmtCount += createContour(conn, contour, event, pstmt_contour);
        }
        // execute in batch for every BATCH_SIZE events
        if (stmtCount >= BATCH_SIZE)
        {
          JDBCTools.executeBatches(pstmts);
          conn.commit();
          stmtCount = 0;
        }
      }
      // execute last few pending updates
      JDBCTools.executeBatches(pstmts);
    }
    finally
    {
      JDBCTools.closeStatements(pstmts);
    }
  }

  private void exportFiles(final Connection conn, final Iterator<? extends IFileValue> iterator)
      throws SQLException
  {
    final String SQL_VALUE_FILE = "INSERT INTO value_file(vfid, name) VALUES(?,?);";
    final PreparedStatement pstmt = conn.prepareStatement(SQL_VALUE_FILE);
    try
    {
      while (iterator.hasNext())
      {
        final IFileValue file = iterator.next();
        // default processing for all files
        pstmt.setLong(1, file.id());
        pstmt.setString(2, file.name());
        pstmt.addBatch();
      }
      pstmt.executeBatch();
    }
    finally
    {
      JDBCTools.closeStatement(pstmt);
    }
  }

  private void exportLines(final Connection conn, final Iterator<? extends ILineValue> iterator)
      throws SQLException
  {
    final String SQL_VALUE_LINE = "INSERT INTO value_line(vlid, vfid, number) VALUES(?,?,?);";
    final PreparedStatement pstmt = conn.prepareStatement(SQL_VALUE_LINE);
    try
    {
      while (iterator.hasNext())
      {
        final ILineValue line = iterator.next();
        // default processing for all lines
        pstmt.setLong(1, line.id());
        pstmt.setLong(2, line.file().id());
        pstmt.setInt(3, line.lineNumber());
        pstmt.addBatch();
      }
      pstmt.executeBatch();
    }
    finally
    {
      JDBCTools.closeStatement(pstmt);
    }
  }

  private void exportMDG(final Connection conn, final IMethodNode method,
      final IMethodDependenceGraph mdg) throws SQLException
  {
    final String SQL_MDG = "INSERT INTO mdg(methodId, line, hasSystemExit) VALUES(?,?,?);";
    final String SQL_RLINE = "INSERT INTO rline(methodId, line, parentLine, hasConditional, isControl, isLoopControl, kind) VALUES(?,?,?,?,?,?,?);";
    final String SQL_RLINE_DEFS = "INSERT INTO rline_defs(methodId, line, rnid) VALUES(?,?,?);";
    final String SQL_RLINE_USES = "INSERT INTO rline_uses(methodId, line, rnid) VALUES(?,?,?);";
    final String SQL_RLINE_JUMPS = "INSERT INTO rline_jumps(methodId, line, jumpLine) VALUES(?,?,?);";
    final PreparedStatement pstmt_mdg = conn.prepareStatement(SQL_MDG);
    final PreparedStatement pstmt_rline = conn.prepareStatement(SQL_RLINE);
    final PreparedStatement pstmt_rdefs = conn.prepareStatement(SQL_RLINE_DEFS);
    final PreparedStatement pstmt_ruses = conn.prepareStatement(SQL_RLINE_USES);
    final PreparedStatement pstmt_rjumps = conn.prepareStatement(SQL_RLINE_JUMPS);
    final PreparedStatement[] pstmts = new PreparedStatement[]
    { pstmt_mdg, pstmt_rline, pstmt_rdefs, pstmt_ruses, pstmt_rjumps };
    try
    {
      // process mdg
      pstmt_mdg.setLong(1, method.id());
      if (mdg.parent() != null)
      {
        pstmt_mdg.setInt(2, mdg.parent().lineNumber());
      }
      else
      {
        pstmt_mdg.setNull(2, java.sql.Types.INTEGER);
      }
      pstmt_mdg.setBoolean(3, mdg.hasSystemExit());
      pstmt_mdg.addBatch();
      // process all lines
      for (final IResolvedLine rline : mdg.dependenceMap().values())
      {
        // process line
        exportResolvedLine(conn, pstmt_rline, pstmt_rdefs, pstmt_ruses, pstmt_rjumps, rline, method);
      }
      // execute batch
      JDBCTools.executeBatches(pstmts);
    }
    finally
    {
      JDBCTools.closeStatements(pstmts);
    }
  }

  private void exportMDGs(final Connection conn, final IRootNode root) throws SQLException
  {
    // retrieve the root files
    final Iterator<? extends IFileNode> files = root.files().iterator();
    // process each file
    while (files.hasNext())
    {
      // retrieve the file node
      final IFileNode file = files.next();
      // retrieve the file's types
      final Iterator<? extends ITypeNode> types = file.types().iterator();
      // process each type in the file
      while (types.hasNext())
      {
        exportTypeMDGs(conn, types.next());
      }
    }
  }

  private void exportResolvedLine(final Connection conn, final PreparedStatement pstmt_rline,
      final PreparedStatement pstmt_rdefs, final PreparedStatement pstmt_ruses,
      final PreparedStatement pstmt_jumps, final IResolvedLine rline, final IMethodNode method)
      throws SQLException
  {
    // process the line
    pstmt_rline.setLong(1, method.id());
    pstmt_rline.setLong(2, rline.lineNumber());
    if (rline.parent() != null)
    {
      pstmt_rline.setInt(3, rline.parent().lineNumber());
    }
    else
    {
      pstmt_rline.setNull(3, java.sql.Types.INTEGER);
    }
    pstmt_rline.setBoolean(4, rline.hasConditional());
    pstmt_rline.setBoolean(5, rline.isControl());
    pstmt_rline.setBoolean(6, rline.isLoopControl());
    pstmt_rline.setInt(7, rline.kind().ordinal() + 1);
    pstmt_rline.addBatch();
    // process relationship line --> defs
    for (final IResolvedNode node : rline.definitions())
    {
      pstmt_rdefs.setLong(1, method.id());
      pstmt_rdefs.setInt(2, rline.lineNumber());
      pstmt_rdefs.setLong(3, node.id());
      pstmt_rdefs.addBatch();
    }
    // process relationship line --> uses
    for (final IResolvedNode node : rline.uses())
    {
      pstmt_ruses.setLong(1, method.id());
      pstmt_ruses.setInt(2, rline.lineNumber());
      pstmt_ruses.setLong(3, node.id());
      pstmt_ruses.addBatch();
    }
    // process relationship line --> jumps
    for (final IResolvedLine node : rline.jumpDependences())
    {
      pstmt_jumps.setLong(1, method.id());
      pstmt_jumps.setInt(2, rline.lineNumber());
      pstmt_jumps.setInt(3, node.lineNumber());
      pstmt_jumps.addBatch();
    }
  }

  private void exportResolvedNodes(final Connection conn,
      final Iterator<? extends IResolvedNode> iterator) throws SQLException
  {
    final String SQL_RNODE = "INSERT INTO rnode(rnid, isActual, isLHS, qualifierOf, sourceIndex) VALUES(?,?,?,?,?);";
    final String SQL_RCALL = "INSERT INTO rcall(rnid, methodRefId, size) VALUES(?,?,?);";
    final String SQL_RCALL_ARGS = "INSERT INTO rcall_uses(rcallId, argument, rnid) VALUES(?,?,?);";
    final String SQL_RDATA = "INSERT INTO rdata(rnid, dataId, isDef) VALUES(?,?,?);";
    final String SQL_RDATA_LAZY = "INSERT INTO rdata_lazy(rnid, name, typeRefId, isDef) VALUES(?,?,?,?);";
    final String SQL_RTHIS = "INSERT INTO rthis(rnid, typeId) VALUES(?,?);";
    final PreparedStatement pstmt_rnode = conn.prepareStatement(SQL_RNODE);
    final PreparedStatement pstmt_rcall = conn.prepareStatement(SQL_RCALL);
    final PreparedStatement pstmt_rcall_args = conn.prepareStatement(SQL_RCALL_ARGS);
    final PreparedStatement pstmt_rdata = conn.prepareStatement(SQL_RDATA);
    final PreparedStatement pstmt_rdata_lazy = conn.prepareStatement(SQL_RDATA_LAZY);
    final PreparedStatement pstmt_rthis = conn.prepareStatement(SQL_RTHIS);
    final PreparedStatement[] pstmts = new PreparedStatement[]
    { pstmt_rnode, pstmt_rcall, pstmt_rcall_args, pstmt_rdata, pstmt_rdata_lazy, pstmt_rthis };
    try
    {
      while (iterator.hasNext())
      {
        final IResolvedNode node = iterator.next();
        // common processing
        pstmt_rnode.setLong(1, node.id());
        pstmt_rnode.setBoolean(2, node.isActual());
        pstmt_rnode.setBoolean(3, node.isLHS());
        if (node.qualifierOf() != null)
        {
          pstmt_rnode.setLong(4, node.qualifierOf().id());
        }
        else
        {
          pstmt_rnode.setNull(4, java.sql.Types.BIGINT);
        }
        pstmt_rnode.setInt(5, node.sourceIndex());
        pstmt_rnode.addBatch();
        // process resolved call
        if (node instanceof IResolvedCall)
        {
          pstmt_rcall.setLong(1, node.id());
          pstmt_rcall.setLong(2, ((IResolvedCall) node).call().id());
          pstmt_rcall.setInt(3, ((IResolvedCall) node).size());
          pstmt_rcall.addBatch();
          // process call --> arguments
          for (int i = 0; i < ((IResolvedCall) node).size(); i++)
          {
            for (final IResolvedNode arg : ((IResolvedCall) node).uses(i))
            {
              pstmt_rcall_args.setLong(1, node.id());
              pstmt_rcall_args.setInt(2, i);
              pstmt_rcall_args.setLong(3, arg.id());
              pstmt_rcall_args.addBatch();
            }
          }
        }
        else if (node instanceof IResolvedLazyData)
        {
          pstmt_rdata_lazy.setLong(1, node.id());
          pstmt_rdata_lazy.setString(2, ((IResolvedLazyData) node).name());
          pstmt_rdata_lazy.setLong(3, ((IResolvedLazyData) node).type().id());
          pstmt_rdata_lazy.setBoolean(4, ((IResolvedLazyData) node).isDef());
          pstmt_rdata_lazy.addBatch();
        }
        else if (node instanceof IResolvedData)
        {
          pstmt_rdata.setLong(1, node.id());
          pstmt_rdata.setLong(2, ((IResolvedData) node).data().id());
          pstmt_rdata.setBoolean(3, ((IResolvedData) node).isDef());
          pstmt_rdata.addBatch();
        }
        else if (node instanceof IResolvedThis)
        {
          pstmt_rthis.setLong(1, node.id());
          pstmt_rthis.setLong(2, ((IResolvedThis) node).type().id());
          pstmt_rthis.addBatch();
        }
      }
      JDBCTools.executeBatches(pstmts);
    }
    finally
    {
      JDBCTools.closeStatements(pstmts);
    }
  }

  private void exportStatic(final Connection conn) throws SQLException
  {
    exportFiles(conn, model.store().getFiles().iterator());
    exportLines(conn, model.store().getLines().iterator());
    exportThreads(conn, model.store().getThreads().iterator());
    exportValues(conn, model.store().getValues().iterator());
    exportASTNodeRefs(conn, model.store().lookupNodeRefs().iterator());
    exportASTNodes(conn, model.store().lookupRoot());
    exportResolvedNodes(conn, model.store().lookupResolvedNodes().iterator());
    exportMDGs(conn, model.store().lookupRoot());
  }

  private void exportThreads(final Connection conn, final Iterator<? extends IThreadValue> iterator)
      throws SQLException
  {
    final String SQL_VALUE_THREAD = "INSERT INTO value_thread(vtid, name) VALUES(?,?);";
    final PreparedStatement pstmt = conn.prepareStatement(SQL_VALUE_THREAD);
    try
    {
      while (iterator.hasNext())
      {
        final IThreadValue thread = iterator.next();
        // default processing for all threads
        pstmt.setLong(1, thread.id());
        pstmt.setString(2, thread.name());
        pstmt.addBatch();
      }
      pstmt.executeBatch();
    }
    finally
    {
      JDBCTools.closeStatement(pstmt);
    }
  }

  private void exportTypeMDGs(final Connection conn, final ITypeNode type) throws SQLException
  {
    final Iterator<? extends IMethodNode> iterator = type.methodMembers().values().iterator();
    while (iterator.hasNext())
    {
      final IMethodNode method = iterator.next();
      // process this method's MDG
      final IMethodDependenceGraph mdg = method.getDependenceGraph();
      if (mdg != null)
      {
        exportMDG(conn, method, mdg);
      }
      // process the MDGs of this method's local types
      for (final ITypeNode localType : method.localTypes())
      {
        exportTypeMDGs(conn, localType);
      }
    }
  }

  private void exportValues(final Connection conn, final Iterator<? extends IValue> iterator)
      throws SQLException
  {
    final int BATCH_SIZE = 512;
    final String SQL_VALUE = "INSERT INTO value(vid, kind, value) VALUES(?,?,?);";
    final String SQL_VALUE_CREF = "INSERT INTO value_cref(vid, kind, contourId) VALUES(?,?,?);";
    final String SQL_VALUE_OM_MCREF = "INSERT INTO value_om_mcref(vid, methodId) VALUES(?,?);";
    final String SQL_VALUE_OM_MKEYREF = "INSERT INTO value_om_mkeyref(vid, methodKey) VALUES(?,?);";
    final String SQL_VALUE_OM_RESOLVED = "INSERT INTO value_om_resolved(vid, typeName) VALUES(?,?);";
    final PreparedStatement pstmt = conn.prepareStatement(SQL_VALUE);
    final PreparedStatement pstmt_cref = conn.prepareStatement(SQL_VALUE_CREF);
    final PreparedStatement pstmt_omcref = conn.prepareStatement(SQL_VALUE_OM_MCREF);
    final PreparedStatement pstmt_omkeyref = conn.prepareStatement(SQL_VALUE_OM_MKEYREF);
    final PreparedStatement pstmt_omr = conn.prepareStatement(SQL_VALUE_OM_RESOLVED);
    final PreparedStatement[] pstmts = new PreparedStatement[]
    { pstmt, pstmt_cref, pstmt_omcref, pstmt_omkeyref, pstmt_omr };
    try
    {
      int i = 0;
      while (iterator.hasNext())
      {
        final IValue value = iterator.next();
        // process contour references (in-model)
        if (value.isContourReference())
        {
          pstmt_cref.setLong(1, value.id());
          pstmt_cref.setInt(2, value.kind().ordinal() + 1);
          pstmt_cref.setLong(3, ((IContourReference) value).contour().id());
          pstmt_cref.addBatch();
        }
        else
        {
          // default processing for all non-reference values
          pstmt.setLong(1, value.id());
          pstmt.setInt(2, value.kind().ordinal() + 1);
          pstmt.setString(3, value.value());
          pstmt.addBatch();
          // process method contour references (out-of-model)
          if (value.isOutOfModelMethodReference())
          {
            pstmt_omcref.setLong(1, value.id());
            pstmt_omcref.setLong(2, ((IOutOfModelMethodReference) value).method().id());
            pstmt_omcref.addBatch();
          }
          // process method key references (out-of-model)
          else if (value.isOutOfModelMethodKeyReference())
          {
            pstmt_omkeyref.setLong(1, value.id());
            pstmt_omkeyref.setString(2, ((IOutOfModelMethodKeyReference) value).key());
            pstmt_omkeyref.addBatch();
          }
          // process resolved values (out-of-model)
          else if (value.isResolved())
          {
            pstmt_omr.setLong(1, value.id());
            pstmt_omr.setString(2, ((IResolvedValue) value).typeName());
            pstmt_omr.addBatch();
          }
        }
        i++;
        // execute in batch for every BATCH_SIZE events
        if (i % BATCH_SIZE == 0)
        {
          JDBCTools.executeBatches(pstmts);
        }
      }
      JDBCTools.executeBatches(pstmts);
    }
    finally
    {
      JDBCTools.closeStatements(pstmts);
    }
  }

  private void setNodeParams(final PreparedStatement pstmt, final INode node) throws SQLException
  {
    // default processing for all events
    pstmt.setLong(1, node.id());
    pstmt.setInt(2, node.kind().ordinal() + 1);
    pstmt.setInt(3, node.lineFrom());
    pstmt.setInt(4, node.lineTo());
    pstmt.setString(5, NodeModifier.toString(node.modifiers()));
    pstmt.setString(6, node.name());
    pstmt.setInt(7, node.origin().ordinal() + 1);
    if (node.parent() != null)
    {
      pstmt.setLong(8, node.parent().id());
    }
    else
    {
      pstmt.setNull(8, java.sql.Types.BIGINT);
    }
    pstmt.setInt(9, node.visibility().ordinal() + 1);
  }

  public boolean export(final String jdbcURL)
  {
    String url = jdbcURL;
    String schema;
    // explicitly provided schema name
    // jive export 92 TRACE jdbc:postgresql//localhost/jive?user=jive&schema=list10
    if (jdbcURL.contains("&schema="))
    {
      schema = jdbcURL.substring(jdbcURL.lastIndexOf('=') + 1);
      url = jdbcURL.substring(0, jdbcURL.lastIndexOf('&'));
    }
    else
    {
      try
      {
        schema = File.createTempFile("jive-", "").getName();
      }
      catch (final IOException e)
      {
        schema = "jive-" + JDBCExporter.SCHEMA_ID.incrementAndGet();
      }
    }
    final Connection conn = JDBCTools.getConnection(url);
    if (conn == null)
    {
      return false;
    }
    try
    {
      // create new schema for export
      long start = System.nanoTime();
      conn.createStatement().executeUpdate(String.format(JDBCExporter.CREATE_SCHEMA, schema));
      conn.createStatement().executeUpdate(String.format(JDBCExporter.ALTER_SCHEMA, schema));
      conn.createStatement().executeUpdate(String.format(JDBCExporter.GRANT_SCHEMA, schema));
      conn.createStatement().executeUpdate(String.format(JDBCExporter.SEARCH_PATH, schema));
      // load the schema script and run it
      InputStream is;
      try
      {
        is = getClass().getResource("/resources/jive-schema.sql").openStream();
        java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8");
        final String script = scanner.useDelimiter("\\A").next();
        conn.createStatement().executeUpdate(script);
        scanner.close();
      }
      catch (final Exception e)
      {
        conn.rollback();
        return false;
      }
      conn.commit();
      System.err.println("Created schema " + schema + " in "
          + ((System.nanoTime() - start) / 1000000) + "ms.");
      // export static model into new schema
      start = System.nanoTime();
      exportStatic(conn);
      conn.commit();
      System.err.println("Exported the static model in " + ((System.nanoTime() - start) / 1000000)
          + "ms.");
      // export dynamic model into new schema
      start = System.nanoTime();
      exportDynamic(conn);
      conn.commit();
      // done with the export
      System.err.println("Exported the dynamic model in " + ((System.nanoTime() - start) / 1000000)
          + "ms.");
      // post-process dynamic model
      start = System.nanoTime();
      final CallableStatement proc = conn.prepareCall("{ call db_after_load() }");
      proc.execute();
      proc.close();
      conn.commit();
      // done
      System.err.println("Post-processed the dynamic model in "
          + ((System.nanoTime() - start) / 1000000) + "ms.");
    }
    catch (final SQLException e)
    {
      JDBCTools.handleSQLException(e);
      return false;
    }
    finally
    {
      JDBCTools.closeConnection(conn);
    }
    return true;
  }
}
