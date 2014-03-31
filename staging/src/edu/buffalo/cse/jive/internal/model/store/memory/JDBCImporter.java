package edu.buffalo.cse.jive.internal.model.store.memory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IEventModel.EventKind;
import edu.buffalo.cse.jive.model.IEventModel.IJiveEvent;
import edu.buffalo.cse.jive.model.IModel.IContourReference;
import edu.buffalo.cse.jive.model.IModel.ILineValue;
import edu.buffalo.cse.jive.model.IModel.IOutOfModelMethodKeyReference;
import edu.buffalo.cse.jive.model.IModel.IOutOfModelMethodReference;
import edu.buffalo.cse.jive.model.IModel.IResolvedValue;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;
import edu.buffalo.cse.jive.model.IModel.IValue;
import edu.buffalo.cse.jive.model.IModel.ValueKind;
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
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.model.IStaticModel.NodeOrigin;
import edu.buffalo.cse.jive.model.IStaticModel.NodeVisibility;
import edu.buffalo.cse.jive.model.factory.IStaticModelFactory;
import edu.buffalo.cse.jive.model.factory.IValueFactory;
import edu.buffalo.cse.jive.model.lib.Tools;
import edu.buffalo.cse.jive.model.store.db.JDBCTools;

class JDBCImporter
{
  private final ExecutionModel model;
  private final Store store;

  JDBCImporter()
  {
    this.model = new ExecutionModel(false);
    this.store = model.store();
  }

  private void importASTDataNodes(final PreparedStatement pstmt,
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

  private void importASTNodeRefs(final Connection conn, final Iterator<? extends INodeRef> iterator)
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

  private void importASTNodes(final Connection conn, final IRootNode root) throws SQLException
  {
    final String SQL_NODE = "INSERT INTO node(nid, kind, lineFrom, lineTo, modifiers, name, origin, parentId, visibility) VALUES(?,?,?,?,?::INT[],?,?,?,?);";
    final String SQL_NODE_TYPE = "INSERT INTO node_type(nid, key, defaultValueId, superClassRefId) VALUES(?,?,?,?,?);";
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
      importASTTypeNodes(pstmt, pstmt_type, pstmt_method, pstmt_data, pstmt_intfs, pstmt_excps,
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
        importASTTypeNodes(pstmt, pstmt_type, pstmt_method, pstmt_data, pstmt_intfs, pstmt_excps,
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

  private void importASTTypeNodes(final PreparedStatement pstmt,
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
      if (type.container() != null)
      {
        pstmt_type.setLong(3, type.container().id());
      }
      else
      {
        pstmt_type.setNull(3, java.sql.Types.BIGINT);
      }
      if (type.defaultValue() != null)
      {
        pstmt_type.setLong(4, type.defaultValue().id());
      }
      else
      {
        pstmt_type.setNull(4, java.sql.Types.BIGINT);
      }
      if (type.superClass() != null)
      {
        pstmt_type.setLong(5, type.superClass().id());
      }
      else
      {
        pstmt_type.setNull(5, java.sql.Types.BIGINT);
      }
      pstmt_type.addBatch();
      // process field nodes
      importASTDataNodes(pstmt, pstmt_data, type.dataMembers().values().iterator());
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
        importASTDataNodes(pstmt, pstmt_data, method.dataMembers().values().iterator());
        importASTTypeNodes(pstmt, pstmt_type, pstmt_method, pstmt_data, pstmt_intfs, pstmt_excps,
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

  private void importEvents(final Connection conn) throws SQLException
  {
    final String SQL_EVENT_DEFAULT = "SELECT eventId, parentId, threadId, kind, fileName, lineNumber FROM va_ImportEvent ORDER BY eventId;";
    final String SQL_EVENT_ECATCH = "SELECT eventId, kind, value, contourId FROM va_ImportEventCatch ORDER BY eventId;";
    final String SQL_EVENT_ETHROW = "SELECT eventId, framePopped, throwerKind, throwerValue, throwerValueId, throwerContourId, exceptionKind, exceptionValue, exceptionContourId FROM va_ImportEventThrow ORDER BY eventId;";
    final String SQL_EVENT_FREAD = "SELECT eventId, contourId, schemaId FROM event_fread ORDER BY eventId;";
    final String SQL_EVENT_MCALL = "SELECT eventId, callerId, targetId FROM event_mcall ORDER BY eventId;";
    final String SQL_EVENT_MEXIT = "SELECT eventId, returnContextId, returnValueId FROM event_mexit ORDER BY eventId;";
    final String SQL_EVENT_MRET = "SELECT eventId, terminatorId FROM event_mret ORDER BY eventId;";
    final ResultSet rs = JDBCTools.open(conn, SQL_EVENT_DEFAULT);
    final ResultSet rs_ecatch = JDBCTools.open(conn, SQL_EVENT_ECATCH);
    final ResultSet rs_ethrow = JDBCTools.open(conn, SQL_EVENT_ETHROW);
    final ResultSet rs_fread = JDBCTools.open(conn, SQL_EVENT_FREAD);
    final ResultSet rs_mcall = JDBCTools.open(conn, SQL_EVENT_MCALL);
    final ResultSet rs_mexit = JDBCTools.open(conn, SQL_EVENT_MEXIT);
    final ResultSet rs_mret = JDBCTools.open(conn, SQL_EVENT_MRET);
    final List<IJiveEvent> events = Tools.newArrayList(50000);
    try
    {
      while (rs.next())
      {
        rs.getLong(1);
        // parent event
        final long pid = rs.getLong(2);
        // event thread
        final IThreadValue thread = store.lookupThread(rs.getLong(3));
        // event kind
        final EventKind kind = EventKind.values()[rs.getInt(4) - 1];
        // event line
        final ILineValue line = store.lookupLineValue(rs.getString(5), rs.getInt(6));
        switch (kind)
        {
          case EXCEPTION_CATCH:
            {
              rs_ecatch.next();
              final ValueKind vkind = ValueKind.values()[rs_ecatch.getInt(2) - 1];
              final IValue exception;
              if (vkind == ValueKind.OUT_OF_MODEL)
              {
                exception = model.valueFactory().createOutOfModelValue(rs_ecatch.getString(3));
              }
              else
              {
                final IContour contour = store.lookupContour(rs_ecatch.getLong(4));
                exception = model.valueFactory().createReference(contour);
              }
              // TODO: member (variable) is not used anywhere in Jive other than for display
              // purposes
              events.add(model.eventFactory().createExceptionCatchEvent(thread, line, exception,
                  null));
            }
            break;
          case EXCEPTION_THROW:
            {
              rs_ethrow.next();
              final ValueKind throwerkind = ValueKind.values()[rs_ethrow.getInt(3) - 1];
              final IValue thrower;
              if (throwerkind == ValueKind.OM_METHOD_REFERENCE)
              {
                final ResultSet rsv = JDBCTools.open(
                    conn,
                    String.format("SELECT methodId FROM value_om_mcref WHERE vid = %d;",
                        rs_ethrow.getLong(5)));
                rsv.next();
                final IContour contour = store.lookupContour(rsv.getLong(1));
                thrower = model.valueFactory().createOutOfModelMethodReference(
                    rs_ethrow.getString(4), (IMethodContour) contour);
              }
              else if (throwerkind == ValueKind.OM_METHOD_KEY_REFERENCE)
              {
                final ResultSet rsv = JDBCTools
                    .open(conn, String.format(
                        "SELECT methodKey FROM value_om_mkeyref WHERE vid = %d;",
                        rs_ethrow.getLong(5)));
                rsv.next();
                thrower = model.valueFactory().createOutOfModelMethodKeyReference(
                    rs_ethrow.getString(4), rsv.getString(1));
              }
              else
              {
                final IContour contour = store.lookupContour(rs_ethrow.getLong(6));
                thrower = model.valueFactory().createReference(contour);
              }
              final ValueKind ekind = ValueKind.values()[rs_ethrow.getInt(7) - 1];
              final IValue exception;
              if (ekind == ValueKind.OUT_OF_MODEL)
              {
                exception = model.valueFactory().createOutOfModelValue(rs_ethrow.getString(8));
              }
              else
              {
                final IContour contour = store.lookupContour(rs_ethrow.getLong(9));
                exception = model.valueFactory().createReference(contour);
              }
              events.add(model.eventFactory().createExceptionThrowEvent(thread, line, exception,
                  thrower, rs_ethrow.getBoolean(2)));
            }
            break;
          case FIELD_READ:
            {
              rs_fread.next();
              final IContextContour contour = (IContextContour) store.lookupContour(rs_fread
                  .getLong(2));
              final Collection<IContourMember> members = contour.members();
              final long schemaId = rs_fread.getLong(3);
              IContourMember member = null;
              for (final IContourMember m : members)
              {
                if (m.schema().id() == schemaId)
                {
                  member = m;
                  break;
                }
              }
              events.add(model.eventFactory().createFieldReadEvent(thread, line, contour, member));
            }
            break;
          case FIELD_WRITE:
            {
              // final IAssignEvent assign = (IAssignEvent) event;
              // pstmt_cmember.setLong(1, assign.contour().id());
              // pstmt_cmember.setLong(2, assign.member().schema().id());
              // pstmt_cmember.setLong(3, assign.newValue().id());
              // pstmt_cmember.setLong(4, event.eventId());
              // events.add(model.eventFactory().createFieldWriteEvent(thread, line, contour,
              // newValue,
              // member));
            }
            break;
          case LINE_STEP:
            {
              events.add(model.eventFactory().createLineStepEvent(thread, line));
            }
            break;
          case METHOD_CALL:
            {
              // pstmt_mcall.setLong(1, event.eventId());
              // pstmt_mcall.setLong(2, ((IMethodCallEvent) event).caller().id());
              // pstmt_mcall.setLong(3, ((IMethodCallEvent) event).target().id());
              // events.add(model.eventFactory().createMethodCallEvent(thread, line, caller,
              // target));
            }
            break;
          case METHOD_ENTERED:
            {
              events.add(model.eventFactory().createMethodEnteredEvent(thread, line));
            }
            break;
          case METHOD_EXIT:
            {
              // pstmt_mexit.setLong(1, event.eventId());
              // pstmt_mexit.setLong(2, ((IMethodExitEvent) event).returnContext().id());
              // pstmt_mexit.setLong(3, ((IMethodExitEvent) event).returnValue().id());
              events.add(model.eventFactory().createMethodExitEvent(thread, line));
            }
            break;
          case METHOD_RETURNED:
            {
              // pstmt_mret.setLong(1, event.eventId());
              // pstmt_mret.setLong(2, ((IMethodReturnedEvent) event).terminator().eventId());
              // events.add(model.eventFactory().createMethodReturnedEvent(terminator));
            }
            break;
          case OBJECT_NEW:
            {
              // events.add(model.eventFactory().createNewObjectEvent(thread, line, contour));
            }
            break;
          case SYSTEM_END:
            {
              events.add(model.eventFactory().createSystemExitEvent());
            }
            break;
          case SYSTEM_START:
            break;
          case THREAD_END:
            {
              events.add(model.eventFactory().createThreadEndEvent(thread));
            }
            break;
          case TYPE_LOAD:
            {
              // events.add(model.eventFactory().createTypeLoadEvent(thread, line, contour));
            }
            break;
          case VAR_ASSIGN:
            {
              // final IAssignEvent assign = (IAssignEvent) event;
              // pstmt_cmember.setLong(1, assign.contour().id());
              // pstmt_cmember.setLong(2, assign.member().schema().id());
              // pstmt_cmember.setLong(3, assign.newValue().id());
              // pstmt_cmember.setLong(4, event.eventId());
              // events.add(model.eventFactory().createVarAssignEvent(thread, line, value, member));
            }
            break;
          case VAR_DELETE:
            {
              // final IVarDeleteEvent delete = (IVarDeleteEvent) event;
              // pstmt_cmember.setLong(1, delete.contour().id());
              // pstmt_cmember.setLong(2, delete.member().schema().id());
              // pstmt_cmember.setLong(3,
              // store.model().valueFactory().createUninitializedValue().id());
              // pstmt_cmember.setLong(4, event.eventId());
              // events.add(model.eventFactory().createVarDeleteEvent(thread, line, member));
            }
            break;
        }
      }
    }
    finally
    {
      JDBCTools.closeResultSet(rs);
      JDBCTools.closeResultSet(rs_ecatch);
      JDBCTools.closeResultSet(rs_ethrow);
      JDBCTools.closeResultSet(rs_fread);
      JDBCTools.closeResultSet(rs_mcall);
      JDBCTools.closeResultSet(rs_mexit);
      JDBCTools.closeResultSet(rs_mret);
    }
  }

  private void importMDG(final Connection conn, final IMethodNode method,
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
        importResolvedLine(conn, pstmt_rline, pstmt_rdefs, pstmt_ruses, pstmt_rjumps, rline, method);
      }
      // execute batch
      JDBCTools.executeBatches(pstmts);
    }
    finally
    {
      JDBCTools.closeStatements(pstmts);
    }
  }

  private void importMDGs(final Connection conn, final IRootNode root) throws SQLException
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
        importTypeMDGs(conn, types.next());
      }
    }
  }

  private void importResolvedLine(final Connection conn, final PreparedStatement pstmt_rline,
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

  private void importResolvedNodes(final Connection conn,
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

  private void importStatic(final Connection conn) throws SQLException
  {
    final long start = System.nanoTime();
    final ValueImporter vi = new ValueImporter(conn);
    vi.importLines();
    vi.importThreads();
    final NodeImporter ni = new NodeImporter(vi);
    ni.importNodes(conn);
    importValues(conn, store.values().iterator());
    importASTNodeRefs(conn, store.lookupNodeRefs().iterator());
    importASTNodes(conn, store.lookupRoot());
    importResolvedNodes(conn, store.lookupResolvedNodes().iterator());
    importMDGs(conn, store.lookupRoot());
    conn.commit();
    System.err.println("Imported the static model in " + ((System.nanoTime() - start) / 1000000)
        + "ms.");
  }

  private void importTypeMDGs(final Connection conn, final ITypeNode type) throws SQLException
  {
    final Iterator<? extends IMethodNode> iterator = type.methodMembers().values().iterator();
    while (iterator.hasNext())
    {
      final IMethodNode method = iterator.next();
      // process this method's MDG
      final IMethodDependenceGraph mdg = method.getDependenceGraph();
      if (mdg != null)
      {
        importMDG(conn, method, mdg);
      }
      // process the MDGs of this method's local types
      for (final ITypeNode localType : method.localTypes())
      {
        importTypeMDGs(conn, localType);
      }
    }
  }

  private void importValues(final Connection conn, final Iterator<? extends IValue> iterator)
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

  boolean importFromJDBC(final String jdbcURL, final ExecutionModel model)
  {
    final Connection conn = JDBCTools.getConnection(jdbcURL);
    if (conn == null)
    {
      return false;
    }
    try
    {
      importStatic(conn);
      final long start = System.nanoTime();
      importEvents(conn);
      System.err.println("Imported the dynamic model in " + ((System.nanoTime() - start) / 1000000)
          + "ms.");
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

  /**
   * <pre>
   * NodeImporter
   *   import nodes in order until a missing default value is found...
   * 
   *   ValueImporter
   *     import values in order until the missing default value is created
   *     if a contour reference value is found for which the contour is missing...
   *     
   *       EventImporter
   *         import until the missing contour is created (TypeLoad or NewObject)
   * </pre>
   */
  private final class NodeImporter
  {
    private long nid = -1;
    private final ValueImporter vi;
    private final IStaticModelFactory factory = model.staticModelFactory();
    private final String SQL_NODE = "SELECT nid, kind, lineFrom, lineTo, modifiers, name, origin, parentKind, parentName, parentKey, visibility, isRef FROM va_ImportNode WHERE nid > 66 ORDER BY nid;";
    private final String SQL_NODE_TYPE = "SELECT key, defaultValueId, superClassRefId, superKey FROM va_ImportNodeType WHERE nid > 66 ORDER BY nid;";
    private final String SQL_NODE_DATA = "SELECT defaultValueId, index, typeRefId, typeKey FROM va_ImportNodeData WHERE nid > 66 ORDER BY nid;";
    private final String SQL_NODE_METHOD = "SELECT key, index, returnTypeRefId, returnTypeKey FROM va_ImportNodeMethod WHERE nid > 66 ORDER BY nid;";

    NodeImporter(final ValueImporter vi)
    {
      this.vi = vi;
    }

    void importNodes(final Connection conn) throws SQLException
    {
      // thread, file, and line values have their own ids
      // other values share their ids
      final ResultSet rsNode = JDBCTools.open(conn, SQL_NODE);
      final ResultSet rsType = JDBCTools.open(conn, SQL_NODE_TYPE);
      final ResultSet rsData = JDBCTools.open(conn, SQL_NODE_DATA);
      final ResultSet rsMethod = JDBCTools.open(conn, SQL_NODE_METHOD);
      try
      {
        while (rsNode.next())
        {
          /**
           * Basic node information.
           */
          this.nid = rsNode.getLong(1);
          final NodeKind kind = NodeKind.values()[rsNode.getInt(2) - 1];
          final int from = rsNode.getInt(3);
          final int lineFrom = rsNode.wasNull() ? -1 : from;
          final int to = rsNode.getInt(4);
          final int lineTo = rsNode.wasNull() ? -1 : to;
          final int[] intModifiers = (int[]) rsNode.getArray(5).getArray();
          final String name = rsNode.getString(6);
          final int orig = rsNode.getInt(7);
          final NodeOrigin origin = rsNode.wasNull() ? null : NodeOrigin.values()[orig];
          final NodeKind parentKind = NodeKind.values()[rsNode.getInt(8) - 1];
          final String parentName = rsNode.getString(9);
          final String parentKey = rsNode.getString(10);
          final NodeVisibility visibility = NodeVisibility.values()[rsNode.getInt(11) - 1];
          final boolean isRef = rsNode.getBoolean(12);
          // resolve the node modifiers
          final Set<NodeModifier> modifiers = Tools.newHashSet();
          for (final int modifier : intModifiers)
          {
            modifiers.add(NodeModifier.values()[modifier - 1]);
          }
          // resolve the parent node based on kind and key/name
          final INode parent;
          if (parentKind == NodeKind.NK_ROOT)
          {
            parent = store.lookupRoot();
          }
          else if (parentKind == NodeKind.NK_FILE)
          {
            parent = store.lookupFile(parentName);
          }
          else
          {
            parent = store.lookupNode(parentKey);
          }
          if (kind == NodeKind.NK_FILE)
          {
            factory.createFileNode(name, lineFrom, lineTo, origin);
          }
          else if (kind == NodeKind.NK_ARRAY || kind == NodeKind.NK_CLASS
              || kind == NodeKind.NK_ENUM || kind == NodeKind.NK_INTERFACE)
          {
            rsType.next();
            if (isRef)
            {
              factory.lookupTypeRef(rsType.getString(1));
            }
            else
            {
              // get super node
              final ITypeNodeRef superType = factory.lookupTypeRef(rsType.getString(4));
              Tools.newHashSet();
              // TODO: load all typerefid values associated with this method, load the typerefid
              // values, and add them to the set
              // import values up to the given default value id
              vi.importValues(rsType.getLong(2));
              // get default value
              // add type
              factory.createTypeNode(rsType.getString(1), name, parent, lineFrom, lineTo, kind,
                  origin, modifiers, visibility, superType, null, null);
            }
          }
          else if (kind == NodeKind.NK_FIELD || kind == NodeKind.NK_VARIABLE)
          {
            rsData.next();
            final ITypeNode type = (ITypeNode) parent;
            // get type node
            final ITypeNodeRef dataType = factory.lookupTypeRef(rsData.getString(4));
            // import values up to the given default value id
            vi.importValues(rsType.getLong(2));
            // get default value
            // add method
            type.addDataMember(name, lineFrom, lineTo, dataType, origin, modifiers, visibility,
                null);
          }
          else if (kind == NodeKind.NK_METHOD)
          {
            rsMethod.next();
            final ITypeNode type = (ITypeNode) parent;
            // get type node
            final ITypeNodeRef returnType = factory.lookupTypeRef(rsMethod.getString(4));
            Tools.newHashSet();
            // TODO: load all typerefid values associated with this method, load the typerefid
            // values, and add them to the set
            // add method
            type.addMethodMember(rsMethod.getString(1), name, lineFrom, lineTo, returnType, origin,
                modifiers, visibility, null);
          }
        }
      }
      finally
      {
        JDBCTools.closeResultSet(rsMethod);
        JDBCTools.closeResultSet(rsData);
        JDBCTools.closeResultSet(rsType);
        JDBCTools.closeResultSet(rsNode);
      }
    }
  }

  private final class ValueImporter
  {
    private final long vid = -1;
    private final Connection conn;
    private final IValueFactory factory = model.valueFactory();
    private final ResultSet rsValue;
    private final String SQL_THREADS = "SELECT vtid, name FROM value_thread WHERE vtid >= -1 ORDER BY vtid;";
    private final String SQL_LINES = "SELECT name, number FROM value_file NATURAL JOIN value_line WHERE number > -1 ORDER BY vlid;";
    private final String SQL_VALUES = "SELECT key, index, returnTypeRefId, returnTypeKey FROM va_ImportNodeMethod WHERE nid > 66 ORDER BY nid;";

    ValueImporter(final Connection conn) throws SQLException
    {
      this.conn = conn;
      this.rsValue = JDBCTools.open(conn, SQL_VALUES);
    }

    public void importValues(final long stopId) throws SQLException
    {
      while (vid < stopId && rsValue.next())
      {
        // keep importing until the stopId value is found
      }
    }

    void importLines() throws SQLException
    {
      final ResultSet rs = JDBCTools.open(conn, SQL_LINES);
      try
      {
        while (rs.next())
        {
          factory.createLine(rs.getString(1), rs.getInt(2));
        }
      }
      finally
      {
        JDBCTools.closeResultSet(rs);
      }
    }

    void importThreads() throws SQLException
    {
      final ResultSet rs = JDBCTools.open(conn, SQL_THREADS);
      try
      {
        while (rs.next())
        {
          factory.createThread(rs.getLong(1), rs.getString(2));
        }
      }
      finally
      {
        JDBCTools.closeResultSet(rs);
      }
    }
  }
}
