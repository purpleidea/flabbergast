package flabbergast.interop;

import flabbergast.export.BaseJdbcResourceUriService;
import flabbergast.util.Result;
import java.io.File;
import java.util.Properties;

final class ServiceSqlLiteResource extends BaseJdbcResourceUriService {
  ServiceSqlLiteResource() {
    super("sqlite+res", "SQLite via Resource", ".sqlite", ".sqlite3", ".db", ".db3", ".s3db");
  }

  @Override
  protected Result<String> parse(File file, Properties properties) {
    return Result.of("jdbc:sqlite:" + file.getAbsolutePath());
  }

  @Override
  protected void parseProperty(String name, String value, Properties output) {
    throw new IllegalArgumentException(
        String.format("SQLite via Resource does not take parameter “%s”.", name));
  }

  @Override
  protected void setFixed(Properties output) {
    output.setProperty("open_mode", "1");
  }
}
