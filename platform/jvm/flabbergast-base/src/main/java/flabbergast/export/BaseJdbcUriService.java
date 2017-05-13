package flabbergast.export;

import flabbergast.lang.*;
import flabbergast.util.Result;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The base for connecting to a JDBC database using <tt>From sql+x:</tt>
 *
 * <p>Non-abstract derived classes should be provided by the module system for {@link UriService} to
 * be available to Flabbergast programs.
 */
public abstract class BaseJdbcUriService implements UriService {
  private static final Pattern EQUALSIGN = Pattern.compile("=");
  private static final Pattern QUESTIONMARK = Pattern.compile("\\?");

  private final class JdbcUriHandler implements UriHandler {
    private ResourcePathFinder finder;

    @Override
    public String name() {
      return "JDBC gateway for " + friendlyName;
    }

    @Override
    public int priority() {
      return 0;
    }

    @Override
    public Result<Promise<Any>> resolveUri(UriExecutor executor, URI uri) {
      final var properties = new Properties();
      return Result.of(uri)
          .filter(x -> x.getScheme().equals(uriPrefix))
          .flatMap(
              x -> {
                setFixed(properties);
                Result.of(x.getQuery())
                    .reduce(
                        () -> {
                          final var parts = QUESTIONMARK.split(uri.getSchemeSpecificPart(), 2);
                          return Result.of(parts.length > 1 ? parts[1] : null);
                        })
                    .flatStream(AMPERSAND::splitAsStream)
                    .forEach(
                        paramChunk ->
                            Result.of(EQUALSIGN.split(paramChunk, 2))
                                .filter(
                                    chunkParts -> chunkParts.length == 2,
                                    String.format("Invalid URL parameter “%s”.", paramChunk))
                                .forEach(chunk -> parseProperty(chunk[0], chunk[1], properties)));
                return parse(x, properties, finder);
              })
          .map(jdbcUri -> DriverManager.getConnection(jdbcUri, properties))
          .filter(Objects::nonNull, "Failed to createFromValues connection.")
          .peek(
              connection -> {
                connection.setAutoCommit(false);
                connection.setReadOnly(true);
              })
          .map(
              connection ->
                  Frame.proxyOf(
                      "sql" + connection.hashCode(),
                      uri.toString(),
                      connection,
                      Stream.concat(
                          Stream.of(ProxyAttribute.fixed("uri", Any.of(uri.toString()))),
                          attributeProxies.stream())))
          .map(Any::of);
    }

    public void setFinder(ResourcePathFinder finder) {
      this.finder = finder;
    }
  }

  private interface SqlFunction<T, R> {
    static <T, R> Function<T, R> silence(SqlFunction<T, R> function) {
      return arg -> {
        try {
          return function.apply(arg);
        } catch (final SQLException e) {
          return null;
        }
      };
    }

    R apply(T arg) throws SQLException;
  }

  private static final Pattern AMPERSAND = Pattern.compile("&");

  private final boolean allowSandboxed;

  private final String friendlyName;

  private final String uriPrefix;

  private final List<ProxyAttribute<Connection>> attributeProxies;

  /**
   * Create a JDBC connection handler.
   *
   * @param uriSchema the name to appear in the URI ("foo" will result in "sql+foo:")
   * @param friendlyName the name of the database as it should be shown to the user
   * @param allowSandboxed allow access to this database even when {@link ServiceFlag#SANDBOXED}
   */
  protected BaseJdbcUriService(String uriSchema, String friendlyName, boolean allowSandboxed) {
    this.allowSandboxed = allowSandboxed;
    final var plusIndex = uriSchema.indexOf('+');
    uriPrefix = "sql+" + uriSchema;
    this.friendlyName = friendlyName;
    attributeProxies =
        List.of(
            ProxyAttribute.fixed(
                "provider",
                Any.of(plusIndex == -1 ? uriSchema : uriSchema.substring(0, plusIndex))),
            ProxyAttribute.extractStr("database", SqlFunction.silence(Connection::getCatalog)),
            ProxyAttribute.extractStr(
                "product_name", SqlFunction.silence(c -> c.getMetaData().getDatabaseProductName())),
            ProxyAttribute.extractStr(
                "product_version",
                SqlFunction.silence(c -> c.getMetaData().getDatabaseProductVersion())),
            ProxyAttribute.extractStr(
                "driver_name", SqlFunction.silence(c -> c.getMetaData().getDriverName())),
            ProxyAttribute.extractStr(
                "driver_version", SqlFunction.silence(c -> c.getMetaData().getDriverVersion())),
            ProxyAttribute.fixed("platform", Any.of("JDBC")));
  }

  @Override
  public final Stream<UriHandler> create(ResourcePathFinder finder, Predicate<ServiceFlag> flags) {
    if (!allowSandboxed && flags.test(ServiceFlag.SANDBOXED)) {
      return Stream.empty();
    }
    final var handler = new JdbcUriHandler();
    handler.setFinder(finder);
    return Stream.of(handler);
  }

  /**
   * Create a JDBC URI string (or error) for the database
   *
   * @param uri the URI provided by the user
   * @param properties the properties for the database connection, filled with all the user-provided
   *     parameters and fixed strings
   * @param finder a resource finder if the database is on disk
   */
  protected abstract Result<String> parse(
      URI uri, Properties properties, ResourcePathFinder finder);

  /**
   * Update the connection properties for a provided URI parameter
   *
   * @param name the parameter's name, as specified in Flabbergast
   * @param value the parameter's value, as specified in Flabbergast
   * @param output the properties object to modify
   */
  protected abstract void parseProperty(String name, String value, Properties output);

  /**
   * Set any fixed parameters on the connection
   *
   * <p>This happens before {@link #parseProperty(String, String, Properties)}, so it can override
   * values if necessary.
   */
  protected abstract void setFixed(Properties properties);
}
