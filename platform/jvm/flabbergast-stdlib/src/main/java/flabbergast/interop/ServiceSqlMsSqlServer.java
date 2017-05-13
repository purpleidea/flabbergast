package flabbergast.interop;

import flabbergast.export.BaseJdbcHostUriService;
import flabbergast.util.Result;
import java.util.Properties;

final class ServiceSqlMsSqlServer extends BaseJdbcHostUriService {
  public ServiceSqlMsSqlServer() {
    super("mssql", "Microsoft SQL Server", "user", "password", 1433);
  }

  @Override
  protected Result<String> parse(String host, int port, String catalog, Properties properties) {
    properties.setProperty("databaseName", catalog);
    return Result.of("jdbc:sqlserver://" + host + ":" + port);
  }

  @Override
  protected void parseProperty(String name, String value, Properties output) {
    throw new IllegalArgumentException(
        String.format("Microsoft SQL Server does not take parameter “%s”.", name));
  }

  @Override
  protected void setFixed(Properties output) {
    output.setProperty("applicationIntent", "ReadOnly");
  }
}
