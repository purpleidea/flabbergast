package flabbergast.lang;

import java.util.function.Consumer;

/**
 * The result of an asychronously evaluated computation.
 *
 * <p>Evaluating Flabbergast attribute values requires non-linear program flow due to the lookup
 * semantics. For each value to be computed, the scheduler creates a {@link Future} that serves as a
 * write-only interface and a matched {@link Promise} that serves to read the value.
 *
 * <p>Many readers can be waiting for the results of a promise.
 */
public abstract class Promise<T> {
  /** A promise that will never finish. */
  public static <T> Promise<T> broken() {
    return new Promise<>() {

      @Override
      public void accept(PromiseConsumer<? super T> consumer) {
        consumer.unfinished();
      }

      @Override
      public <R> R apply(PromiseFunction<? super T, R> function) {
        return function.unfinished();
      }

      @Override
      public void await(Future<?> waiter, Consumer<? super T> consumer) {
        // Do nothing.
      }
    };
  }

  Promise() {}

  /** Get the result of this computation or an indication that it is not yet finished. */
  public abstract void accept(PromiseConsumer<? super T> consumer);

  /**
   * Process the result of this computation or an indication that it is not yet finished and return
   * a value for each case.
   */
  public abstract <R> R apply(PromiseFunction<? super T, R> function);

  /**
   * Wait for this value.
   *
   * <p>This should only be called by the task master
   */
  abstract void await(Future<?> waiter, Consumer<? super T> consumer);
}
