package flabbergast.interop;

import flabbergast.export.BaseJdbcHostUriService;
import flabbergast.util.Result;
import java.util.Properties;

final class ServiceSqlMySql extends BaseJdbcHostUriService {
  ServiceSqlMySql() {
    super("mysql", "mySQL", "user", "password", 3306);
  }

  @Override
  protected Result<String> parse(String host, int port, String catalog, Properties properties) {
    return Result.of("jdbc:mysql://" + host + ":" + port + "/" + catalog);
  }

  @Override
  protected void parseProperty(String name, String value, Properties output) {
    throw new IllegalArgumentException(String.format("mySQL does not take parameter “%s”.", name));
  }

  @Override
  protected void setFixed(Properties output) {
    output.setProperty("characterEncoding", "UTF-8");
    output.setProperty("useLegacyDatetimeCode", "false");
    output.setProperty("serverTimezone", "UTC");
  }
}
