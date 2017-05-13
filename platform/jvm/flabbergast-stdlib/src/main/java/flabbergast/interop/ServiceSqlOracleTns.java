package flabbergast.interop;

import flabbergast.export.BaseJdbcUriService;
import flabbergast.lang.ResourcePathFinder;
import flabbergast.util.Result;
import java.net.URI;
import java.util.Properties;
import java.util.regex.Pattern;

final class ServiceSqlOracleTns extends BaseJdbcUriService {
  ServiceSqlOracleTns() {
    super("oracle+tns", "Oracle via TNS", false);
  }

  @Override
  protected Result<String> parse(URI uri, Properties properties, ResourcePathFinder finder) {
    if (!Pattern.matches("^[A-Za-z0-9_]+$", uri.getSchemeSpecificPart())) {
      return Result.error("Invalid file specified.");
    }
    return Result.of("jdbc:oracle:oci:@" + uri.getSchemeSpecificPart());
  }

  @Override
  protected void parseProperty(String name, String value, Properties output) {
    throw new IllegalArgumentException(
        String.format("Oracle via TNS does not take parameter “%s”.", name));
  }

  @Override
  protected void setFixed(Properties output) {}
}
