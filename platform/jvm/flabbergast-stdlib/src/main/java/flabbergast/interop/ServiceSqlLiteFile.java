package flabbergast.interop;

import flabbergast.export.BaseJdbcUriService;
import flabbergast.lang.ResourcePathFinder;
import flabbergast.util.Result;
import java.net.URI;
import java.util.Properties;
import java.util.regex.Pattern;

final class ServiceSqlLiteFile extends BaseJdbcUriService {
  ServiceSqlLiteFile() {
    super("sqlite", "SQLite", false);
  }

  @Override
  protected Result<String> parse(URI uri, Properties properties, ResourcePathFinder finder) {
    if (!Pattern.matches(".*", uri.getSchemeSpecificPart())) {
      return Result.error("Invalid file specified.");
    }
    return Result.of("jdbc:sqlite:" + uri.getSchemeSpecificPart());
  }

  @Override
  protected void parseProperty(String name, String value, Properties output) {
    throw new IllegalArgumentException(String.format("SQLite does not take parameter “%s”.", name));
  }

  @Override
  protected void setFixed(Properties output) {
    output.setProperty("open_mode", "1");
  }
}
