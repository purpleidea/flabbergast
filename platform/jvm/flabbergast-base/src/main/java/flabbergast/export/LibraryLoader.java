package flabbergast.export;

import flabbergast.lang.ResourcePathFinder;
import flabbergast.lang.ServiceFlag;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Load Flabbergast library code
 *
 * <p>This is a specialisation of <tt>From lib:</tt> loading to handle that precompiled versions of
 * the same library might be available. A library loader may provide multiple versions of the same
 * library and then the one that is cheapest to load will be selected.
 */
public interface LibraryLoader {
  /**
   * Find all implementations of a library
   *
   * <p>This assumes that all libraries are equivalent, but it can select the one with the lowest
   * cost to load (e.g., precompiled, if allowed).
   *
   * @param finder the resource finder for locating files
   * @param flags the loading restrictions
   * @param libraryName the name of the library; this is the trailing part of the <tt>From lib:</tt>
   *     expression
   * @return candidate ways to load this library
   */
  Stream<LibraryResult> findAll(
      ResourcePathFinder finder, Predicate<ServiceFlag> flags, String libraryName);
}
