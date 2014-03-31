package edu.buffalo.cse.jive.model.store.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public interface Persister
{
  public void close();

  public Batch createBatch();

  public Batch createBatch(int batchSize);

  public interface Batch
  {
    public void add(String statement);

    public void commit();
  }

  public static class PersisterFactory
  {
    public static Persister createPersister(final ConnectionParams params)
    {
      return new JDBCPersister(params);
    }

    private static class JDBCPersister implements Persister
    {
      private final Connection connection;
      private final ConnectionParams params;

      private JDBCPersister(final ConnectionParams params)
      {
        this.params = params;
        this.connection = newConnection();
      }

      @Override
      public void close()
      {
        if (connection == null)
        {
          return;
        }
        try
        {
          try
          {
            connection.commit();
          }
          finally
          {
            if (connection != null)
            {
              connection.close();
            }
          }
        }
        catch (final SQLException se)
        {
          se.printStackTrace();
          throw new IllegalStateException(se);
        }
      }

      @Override
      public Batch createBatch()
      {
        return createBatch(0);
      }

      @Override
      public Batch createBatch(final int batchSize)
      {
        try
        {
          return new JDBCBatch(batchSize);
        }
        catch (final SQLException se)
        {
          se.printStackTrace();
          throw new IllegalStateException(se);
        }
      }

      private Connection newConnection()
      {
        Connection connection = null;
        // load driver
        try
        {
          Class.forName(params.driver());
        }
        catch (final ClassNotFoundException cnfe)
        {
          cnfe.printStackTrace();
          throw new IllegalArgumentException(cnfe);
        }
        // open connection
        try
        {
          connection = DriverManager.getConnection(params.connectionURL(), params.userName(),
              params.password());
          connection.setAutoCommit(false);
          connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        }
        catch (final SQLException se)
        {
          se.printStackTrace();
          throw new IllegalArgumentException(se);
        }
        // return the connection
        return connection;
      }

      private class JDBCBatch implements Batch
      {
        private final Statement batch;
        private int batchCount;
        private final int batchSize;

        private JDBCBatch(final int size) throws SQLException
        {
          this.batchCount = 0;
          this.batchSize = connection.getMetaData().supportsBatchUpdates() && size > 1 ? size : 1;
          this.batch = connection.createStatement();
        }

        @Override
        public void add(final String statement)
        {
          try
          {
            // in immediate mode, commit happens in every call
            if (batchSize == 1)
            {
              batch.execute(statement);
              commit();
            }
            else
            {
              batch.addBatch(statement);
              batchCount++;
              // in batch mode, commit happens after batching batchCount statements
              if (batchSize > 0 && batchCount % batchSize == 0)
              {
                batch.executeBatch();
                commit();
                batch.clearBatch();
              }
            }
          }
          catch (final SQLException se)
          {
            se.printStackTrace();
            throw new IllegalStateException(se);
          }
        }

        @Override
        public void commit()
        {
          try
          {
            connection.commit();
          }
          catch (final SQLException se)
          {
            se.printStackTrace();
            throw new IllegalStateException(se);
          }
        }
      }
    }
  }
}