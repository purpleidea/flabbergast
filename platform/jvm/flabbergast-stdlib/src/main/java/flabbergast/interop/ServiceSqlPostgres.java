package flabbergast.interop;

import flabbergast.export.BaseJdbcHostUriService;
import flabbergast.util.Result;
import java.util.Properties;
import java.util.regex.Pattern;

final class ServiceSqlPostgres extends BaseJdbcHostUriService {
  ServiceSqlPostgres() {
    super("postgresql", "PostgreSQL", "user", "password", 5432);
  }

  @Override
  protected Result<String> parse(String host, int port, String catalog, Properties properties) {

    return Result.of("jdbc:postgresql://" + host + ":" + port + "/" + catalog);
  }

  @Override
  protected void parseProperty(String name, String value, Properties output) {
    switch (name) {
      case "ssl":
        if (!Pattern.matches("^(true|false)$", value)) {
          throw new IllegalArgumentException("The value for “ssl” is not allowed.");
        }
        output.setProperty("ssl", value);
        break;

      default:
        throw new IllegalArgumentException(
            String.format("PostgreSQL does not take parameter “%s”.", name));
    }
  }

  @Override
  protected void setFixed(Properties output) {
    output.setProperty("readOnly", "true");
  }
}
