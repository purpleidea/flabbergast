package flabbergast.lang;

import java.util.function.Consumer;

/** A handler for the last operation in a lookup operation */
public interface LookupLastOperation {
  /**
   * Wait for a promise to be evaluated
   *
   * @param promise the promise to evaluate
   * @param consumer a handler for the result
   */
  void await(Promise<Any> promise, Consumer<Any> consumer);

  /** Indicate that this column has hit an error. */
  void fail();

  /**
   * Indicate that this step has found a value that needs further exploration or collection (if at
   * the end of a column)
   *
   * @param result the value to be emitted
   */
  void finish(Any result);
}
