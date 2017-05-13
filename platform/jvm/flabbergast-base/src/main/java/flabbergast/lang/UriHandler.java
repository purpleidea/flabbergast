package flabbergast.lang;

import flabbergast.util.Result;
import java.net.URI;

/** Resolver for <tt>From</tt> URIs */
public interface UriHandler {
  /** A human-friendly name for this URI handler. */
  String name();

  /**
   * The relative order of this resolver
   *
   * <p>If a resolver has a lower number, it will be given the opportunity to return a URI first. If
   * it passes, another resolver may have a chance.
   */
  int priority();

  /**
   * Resolve a URI
   *
   * @param executor a callback to start a computation
   * @param uri the URI string provided by the Flabbergast program
   */
  Result<Promise<Any>> resolveUri(UriExecutor executor, URI uri);
}
