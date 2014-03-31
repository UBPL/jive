package edu.buffalo.cse.jive.model.store.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.postgresql.ds.PGSimpleDataSource;

public class JDBCTools
{
  public static boolean closeConnection(final Connection conn)
  {
    if (conn == null)
    {
      return true;
    }
    try
    {
      conn.close();
      return true;
    }
    catch (final SQLException e)
    {
      JDBCTools.handleSQLException(e);
    }
    return false;
  }

  public static boolean closeResultSet(final ResultSet rs)
  {
    if (rs == null)
    {
      return true;
    }
    try
    {
      rs.close();
      return true;
    }
    catch (final SQLException e)
    {
      JDBCTools.handleSQLException(e);
    }
    return false;
  }

  public static boolean closeStatement(final PreparedStatement pstmt)
  {
    if (pstmt == null)
    {
      return true;
    }
    try
    {
      pstmt.close();
      return true;
    }
    catch (final SQLException e)
    {
      JDBCTools.handleSQLException(e);
    }
    return false;
  }

  public static void closeStatements(final PreparedStatement[] pstmts) throws SQLException
  {
    for (final PreparedStatement pstmt : pstmts)
    {
      try
      {
        pstmt.close();
      }
      catch (final SQLException e)
      {
        JDBCTools.handleSQLException(e);
      }
    }
  }

  public static void executeBatches(final PreparedStatement[] pstmts) throws SQLException
  {
    for (final PreparedStatement pstmt : pstmts)
    {
      pstmt.executeBatch();
    }
  }

  /**
   * Returns a manual commit, read uncommitted connection.
   */
  public static Connection getConnection(final String jdbcURL)
  {
    // for the time being, only postgresql
    if (!jdbcURL.startsWith("jdbc:postgresql"))
    {
      return null;
    }
    // try
    // {
    // Class.forName("org.postgresql.Driver");
    // }
    // catch (final ClassNotFoundException e)
    // {
    // e.printStackTrace();
    // return null;
    // }
    try
    {
      // jdbc:postgresql://localhost/jive?user=jive&password=
      final PGSimpleDataSource ds = new PGSimpleDataSource();
      ds.setUser("jive");
      ds.setDatabaseName("jive");
      ds.setServerName("localhost");
      ds.setPassword("");
      final Connection conn = ds.getConnection();
      conn.setAutoCommit(false);
      conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
      return conn;
    }
    catch (final SQLException e)
    {
      e.printStackTrace();
    }
    return null;
  }

  public static void handleSQLException(final SQLException e)
  {
    final SQLException e2 = e.getNextException();
    if (e2 != null)
    {
      e2.printStackTrace();
    }
    else
    {
      e.printStackTrace();
    }
  }

  public static ResultSet open(final Connection conn, final String query) throws SQLException
  {
    return conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
        .executeQuery(query);
  }
}
