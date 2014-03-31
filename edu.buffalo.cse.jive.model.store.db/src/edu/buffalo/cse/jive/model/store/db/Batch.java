package edu.buffalo.cse.jive.model.store.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Batch
{
  public static void main(final String args[])
  {
    try
    {
      Class.forName("org.postgresql.Driver");
    }
    catch (final ClassNotFoundException e)
    {
      e.printStackTrace();
    }
    Connection conn;
    try
    {
      conn = DriverManager.getConnection("jdbc:postgresql://localhost:5830/jurka", "jurka", "");
      final Statement stmt = conn.createStatement();
      stmt.execute("DROP TABLE IF EXISTS t");
      stmt.execute("CREATE TABLE t (a int)");
      stmt.close();
      conn.setAutoCommit(false);
      final PreparedStatement ps = conn.prepareStatement("INSERT INTO t VALUES (?)");
      long t1 = System.currentTimeMillis();
      for (int i = 0; i < 1000000; i++)
      {
        ps.setInt(1, i);
        ps.executeUpdate();
      }
      conn.commit();
      long t2 = System.currentTimeMillis();
      System.out.println("No batch: " + ((double) (t2 - t1)) / 1000);
      t1 = System.currentTimeMillis();
      for (int i = 0; i < 1000000; i++)
      {
        ps.setInt(1, i);
        ps.addBatch();
        if (i % 1000 == 0)
        {
          ps.executeBatch();
        }
      }
      ps.executeBatch();
      ps.clearBatch();
      conn.commit();
      t2 = System.currentTimeMillis();
      System.out.println("With batch: " + ((double) (t2 - t1)) / 1000);
    }
    catch (final SQLException e)
    {
      e.printStackTrace();
    }
  }
}