package flabbergast.util;

import java.util.function.Function;

/** Identical to {@link Function}, but can throw an exception */
public interface WhinyFunction<T, R> {
  /** Create a consumer that first converts its input */
  default WhinyConsumer<T> andConsume(WhinyConsumer<? super R> consumer) {
    return x -> consumer.accept(apply(x));
  }
  /** Create a consumer that first converts its input */
  default WhinySupplier<R> with(T value) {
    return () -> apply(value);
  }

  /**
   * Convert an input value
   *
   * @param arg the value to be converted
   */
  R apply(T arg) throws Exception;
}
