package flabbergast.util;

import java.util.function.Supplier;

/** Identical to {@link Supplier}, but can throw an exception */
public interface WhinySupplier<T> {
  /** Retries a value and immediately modifies it */
  default <R> WhinySupplier<R> andThen(WhinyFunction<? super T, R> function) {
    return () -> function.apply(get());
  }

  /** Retrieves a value */
  T get() throws Exception;
}
