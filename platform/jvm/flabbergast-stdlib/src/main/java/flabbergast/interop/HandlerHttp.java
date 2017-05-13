package flabbergast.interop;

import flabbergast.lang.*;
import flabbergast.util.Pair;
import flabbergast.util.Result;
import java.io.DataInputStream;
import java.net.URI;
import java.util.stream.Stream;

/** Allow fetching content from HTTP servers */
final class HandlerHttp implements UriHandler {
  static final HandlerHttp INSTANCE = new HandlerHttp();

  private HandlerHttp() {}

  @Override
  public String name() {
    return "HTTP files";
  }

  @Override
  public int priority() {
    return 0;
  }

  @Override
  public final Result<Promise<Any>> resolveUri(UriExecutor runner, URI uri) {

    return Result.of(uri)
        .filter(x -> uri.getScheme().equals("http") || uri.getScheme().equals("https"))
        .map(
            x -> {
              final var conn = x.toURL().openConnection();
              final var data = new byte[conn.getContentLength()];
              try (final var inputStream = new DataInputStream(conn.getInputStream())) {
                inputStream.readFully(data);
              }
              return Any.of(
                  Frame.createFromValues(
                      "http" + uri.hashCode(),
                      uri.toString(),
                      Stream.of(
                          Pair.of(Name.of("data"), Any.of(data)),
                          Pair.of(Name.of("content_type"), Any.of(conn.getContentType())))));
            });
  }
}
