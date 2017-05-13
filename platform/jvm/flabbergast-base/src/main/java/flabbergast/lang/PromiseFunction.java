package flabbergast.lang;

/**
 * A transformer that computes a particular value from a promise for all possible cases
 *
 * @param <R> the return type of the extraction
 */
public interface PromiseFunction<T, R> {
  /** Process the value stored in this finished promise. */
  R apply(T value);
  /** Collect a value when the promise is still executing or in an error state */
  R unfinished();
}
