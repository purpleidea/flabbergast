package flabbergast.lang;

import java.util.ServiceLoader;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A createFromValues a URI handler for the appropriate situation
 *
 * <p>The Flabbergast runtime will allow implementers of this class to inject URI handlers into a
 * {@link Scheduler} using {@link Scheduler.Builder#add(UriService)} or it will find all implements
 * exported by the Java module system when {@link Scheduler.Builder#defaultUriServices()} is called.
 *
 * @see ServiceLoader
 */
public interface UriService {

  /** A URI service which has no handlers */
  UriService EMPTY = (finder, flags) -> Stream.empty();
  /**
   * Provide {@link UriHandler} instances appropriate for the current configuration
   *
   * @param finder a utility to find resource files on disk based on the users's setup
   * @param flags the configuration of what kinds of handlers should be included and excluded, based
   *     mostly on security concerns
   */
  Stream<UriHandler> create(ResourcePathFinder finder, Predicate<ServiceFlag> flags);
}
