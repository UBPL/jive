package edu.buffalo.cse.jive.model.store.db;

public interface ConnectionParams
{
  public String connectionURL();

  public String databaseName();

  public String driver();

  public String host();

  public String password();

  public String port();

  public String protocol();

  public String userName();

  public static class ConnectionParamsFactory
  {
    public static ConnectionParams mysqlParams(final String databaseName, final String userName,
        final String password)
    {
      return DriverDefaults.MYSQL.createParams(databaseName, userName, password);
    }

    public static ConnectionParams mysqlParams(final String host, final String databaseName,
        final String userName, final String password)
    {
      return DriverDefaults.MYSQL.createParams(host, databaseName, userName, password);
    }

    public static ConnectionParams postgresParams(final String databaseName, final String userName,
        final String password)
    {
      return DriverDefaults.POSTGRESQL.createParams(databaseName, userName, password);
    }

    public static ConnectionParams postgresParams(final String host, final String databaseName,
        final String userName, final String password)
    {
      return DriverDefaults.POSTGRESQL.createParams(host, databaseName, userName, password);
    }

    private static enum DriverDefaults
    {
      MYSQL("com.mysql.jdbc.Driver", "jdbc:mysql", "3306"),
      POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql", "5432");
      private static final String CONNECTION_URL = "%s://%s:%s/%s";
      private static final String CONNECTION_HOST = "127.0.0.1";
      private final String driver;
      private final String port;
      private final String protocol;

      private DriverDefaults(final String driver, final String protocol, final String port)
      {
        this.driver = driver;
        this.port = port;
        this.protocol = protocol;
      }

      ConnectionParams createParams(final String databaseName, final String userName,
          final String password)
      {
        return createParams(protocol, DriverDefaults.CONNECTION_HOST, port, databaseName, userName,
            password);
      }

      ConnectionParams createParams(final String host, final String databaseName,
          final String userName, final String password)
      {
        return createParams(protocol, host, port, databaseName, userName, password);
      }

      ConnectionParams createParams(final String protocol, final String host, final String port,
          final String databaseName, final String userName, final String password)
      {
        return new ConnectionParams()
          {
            @Override
            public String connectionURL()
            {
              return String.format(DriverDefaults.CONNECTION_URL, protocol(), host(), port(),
                  databaseName());
            }

            @Override
            public String databaseName()
            {
              return databaseName;
            }

            @Override
            public String driver()
            {
              return driver;
            }

            @Override
            public String host()
            {
              return host;
            }

            @Override
            public String password()
            {
              return password;
            }

            @Override
            public String port()
            {
              return port;
            }

            @Override
            public String protocol()
            {
              return protocol;
            }

            @Override
            public String userName()
            {
              return userName;
            }
          };
      }
    }
  }
}