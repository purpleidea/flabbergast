package flabbergast.export;

import flabbergast.lang.ResourcePathFinder;
import flabbergast.lang.UriService;
import flabbergast.util.Result;
import java.net.URI;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * The base for connecting to a JDBC database using <tt>From sql+x://host/db</tt>
 *
 * <p>Non-abstract derived classes should be provided by the module system for {@link UriService} to
 * be available to Flabbergast programs.
 */
public abstract class BaseJdbcHostUriService extends BaseJdbcUriService {
  private static final Pattern COLON = Pattern.compile(":");
  private final int defaultPort;
  private final String passwordParam;
  private final String userParam;

  /**
   * Create a new JDBC connection handler.
   *
   * @param uriSchema the name to appear in the URI ("foo" will result in "sql+foo:")
   * @param friendlyName the name of the database as it should be shown to the user
   * @param userParam the key for the JDBC {@link Properties} that stores the connection's user name
   * @param passwordParam the key for the JDBC {@link Properties} that stores the connection's
   *     password
   * @param defaultPort the port to use if the user does not specify
   */
  public BaseJdbcHostUriService(
      String uriSchema,
      String friendlyName,
      String userParam,
      String passwordParam,
      int defaultPort) {
    super(uriSchema, friendlyName, false);
    this.userParam = userParam;
    this.passwordParam = passwordParam;
    this.defaultPort = defaultPort;
  }

  /** Create the JDBC URI from the connection information */
  protected abstract Result<String> parse(
      String host, int port, String catalog, Properties properties);

  @Override
  protected final Result<String> parse(URI uri, Properties properties, ResourcePathFinder finder) {
    return Result.of(uri)
        .map(URI::parseServerAuthority)
        .filter(x -> x.getHost() != null, "Host not defined.")
        .filter(x -> x.getPath() != null, "Catalog not defined.")
        .flatMap(
            x -> {
              if (x.getUserInfo() != null) {
                final var user = COLON.split(x.getUserInfo(), 2);
                properties.setProperty(userParam, user[0]);
                if (user.length > 1) {
                  properties.setProperty(passwordParam, user[1]);
                }
              }

              return parse(
                  uri.getHost(),
                  uri.getPort() == -1 ? defaultPort : uri.getPort(),
                  uri.getPath(),
                  properties);
            });
  }
}
