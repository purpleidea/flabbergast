package flabbergast.export;

import flabbergast.lang.Any;
import flabbergast.lang.Promise;
import flabbergast.lang.UriExecutor;
import java.time.Instant;

/** Description of an implementation of a particular library */
public interface LibraryResult {
  /**
   * The relative effort to load this library
   *
   * <p>Precompiled libraries should have lower score than non-precompiled libraries and local
   * libraries should have a lower score than remote implementations.
   */
  int cost();

  /** Load the library */
  Promise<Any> load(UriExecutor executor);

  /**
   * The timestamp of the <b>modification time of source file</b> for this library. Libraries with
   * the same timestamp are assumed to be the same version.
   */
  Instant timestamp();
}
