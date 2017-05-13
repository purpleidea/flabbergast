package flabbergast.util;

import java.util.function.Function;

/**
 * Converts streams of items into stream of pairs with an index
 *
 * @param <C> the index type
 * @param <T> the value type
 */
public interface Numberer<C, T> extends Function<T, Pair<C, T>> {
  /** The total number of items seen by this instance */
  int size();
}
