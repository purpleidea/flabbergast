package flabbergast.interop;

import flabbergast.lang.*;
import flabbergast.util.Result;
import java.io.DataInputStream;
import java.net.URI;
import java.net.URL;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Helper class to read contents from Java {@link URL} objects. */
abstract class UrlConnectionService implements UriService {
  private final String name;
  private final boolean external;

  protected UrlConnectionService(String name, boolean external) {
    this.name = name;
    this.external = external;
  }

  protected abstract Result<URL> convert(URI uri, ResourcePathFinder finder);

  @Override
  public Stream<UriHandler> create(ResourcePathFinder finder, Predicate<ServiceFlag> flags) {
    return external && flags.test(ServiceFlag.SANDBOXED)
        ? Stream.empty()
        : Stream.of(
            new UriHandler() {
              @Override
              public String name() {
                return name;
              }

              @Override
              public int priority() {
                return 0;
              }

              @Override
              public Result<Promise<Any>> resolveUri(UriExecutor executor, URI uri) {
                return convert(uri, finder)
                    .map(
                        url -> {
                          final var conn = url.openConnection();
                          final var data = new byte[conn.getContentLength()];
                          try (final var inputStream = new DataInputStream(conn.getInputStream())) {
                            inputStream.readFully(data);
                          }
                          return Any.of(data);
                        });
              }
            });
  }
}
