package flabbergast.interop;

import flabbergast.export.BaseJdbcHostUriService;
import flabbergast.util.Result;
import java.util.Properties;

final class ServiceSqlOracleHost extends BaseJdbcHostUriService {
  ServiceSqlOracleHost() {
    super("oracle+host", "Oracle via Host", "user", "password", 1521);
  }

  @Override
  protected Result<String> parse(String host, int port, String catalog, Properties properties) {
    return Result.of(
        String.format(
            "jdbc:oracle:oci:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=%1$s)(PORT=%2$d)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=%3$s)))",
            host, port, catalog));
  }

  @Override
  protected void parseProperty(String name, String value, Properties output) {

    throw new IllegalArgumentException(
        String.format("Oracle via Host does not take parameter “%s”.", name));
  }

  @Override
  protected void setFixed(Properties output) {}
}
