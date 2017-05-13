package flabbergast.export;

import flabbergast.lang.ResourcePathFinder;
import flabbergast.lang.UriService;
import flabbergast.util.Result;
import flabbergast.util.WhinyPredicate;
import java.io.File;
import java.net.URI;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * The base for connecting to a JDBC database using <tt>From sql+x:resource</tt>
 *
 * <p>Non-abstract derived classes should be provided by the module system for {@link UriService} to
 * be available to Flabbergast programs.
 */
public abstract class BaseJdbcResourceUriService extends BaseJdbcUriService {
  private static final WhinyPredicate<String> VALID_FRAGMENT =
      Pattern.compile("^([A-Za-z0-9]*/)*[A-Za-z0-9]+$").asMatchPredicate()::test;
  private final String[] extensions;

  /**
   * Create a new JDBC connection handler.
   *
   * @param uriSchema the name to appear in the URI ("foo" will result in "sql:foo:")
   * @param friendlyName the name of the database as it should be shown to the user
   * @param extensions a list of file extensions to check for in the resource directories; these
   *     must include a period at the start (if desired)
   */
  public BaseJdbcResourceUriService(String uriSchema, String friendlyName, String... extensions) {
    super(uriSchema, friendlyName, true);
    this.extensions = extensions;
  }

  /**
   * Create the JDBC URI from the connection information
   *
   * @param file the file containing the database
   */
  protected abstract Result<String> parse(File file, Properties properties);

  @Override
  protected final Result<String> parse(URI uri, Properties properties, ResourcePathFinder finder) {
    return Result.of(uri.getSchemeSpecificPart())
        .filter(VALID_FRAGMENT, "Invalid resource name specified.")
        .optionalMap(
            fragment -> finder.find(fragment, extensions),
            "Cannot find resource “" + uri.toString() + "”.")
        .flatMap(path -> parse(path, properties));
  }
}
