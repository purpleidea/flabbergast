package flabbergast.compiler;

import java.util.stream.Stream;

/**
 * Produce a stream upon request
 *
 * @param <T> the type of the items in the stream
 */
public interface Streamable<T> {
  /** Begin streaming */
  Stream<T> stream();
}
