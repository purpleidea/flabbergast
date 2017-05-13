package flabbergast.util;

import java.util.function.Consumer;

/** Identical to {@link Consumer}, but can throw an exception */
public interface WhinyConsumer<T> {
  /**
   * Consume a value
   *
   * @param arg the value to be consumed
   */
  void accept(T arg) throws Exception;
}
