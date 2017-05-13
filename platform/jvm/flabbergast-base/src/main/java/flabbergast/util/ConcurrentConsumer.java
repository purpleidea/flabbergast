package flabbergast.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Process a collection of items asynchronously
 *
 * @param <T> the type of the items being processed
 */
public interface ConcurrentConsumer<T> {

  /**
   * Process items from an iterator
   *
   * <p>Unlike other methods, items are not executed concurrently.
   *
   * @param iterator the input data
   * @param consumer a processor for the items
   * @param <T> the type of the items
   */
  static <T> void iterate(Iterator<? extends T> iterator, ConcurrentConsumer<T> consumer) {
    new Runnable() {
      private int index;

      @Override
      public void run() {
        if (iterator.hasNext()) {
          final var current = iterator.next();
          consumer.process(current, index++, this);
        } else {
          consumer.complete();
        }
      }
    }.run();
  }

  /**
   * Process as collection of items
   *
   * @param input the collection of input items
   * @param consumer a processor for the items
   * @param <T> the type of the items
   */
  static <T> void process(Collection<? extends T> input, ConcurrentConsumer<T> consumer) {
    final var interlock = new AtomicInteger(input.size());
    var i = 0;
    for (final var value : input) {
      final var index = i++;
      consumer.process(
          value,
          index,
          () -> {
            if (interlock.decrementAndGet() == 0) {
              consumer.complete();
            }
          });
    }
  }

  /**
   * Process as stream of items
   *
   * @param input the stream of input items
   * @param consumer a processor for the items
   * @param <T> the type of the items
   */
  static <T> void process(Stream<? extends T> input, ConcurrentConsumer<T> consumer) {
    final var interlock = new AtomicInteger(1);
    input
        .map(Pair.number())
        .forEachOrdered(
            pair -> {
              interlock.incrementAndGet();
              consumer.process(
                  pair.second(),
                  pair.first(),
                  () -> {
                    if (interlock.decrementAndGet() == 0) {
                      consumer.complete();
                    }
                  });
            });
    if (interlock.decrementAndGet() == 0) {
      consumer.complete();
    }
  }

  /** Called when the input data is exhausted and all consumers have finished */
  void complete();

  /**
   * Process an input item
   *
   * @param item the item to process
   * @param index the position of the item in the input
   * @param complete a callback to indicate that this item has been processed successfully
   */
  void process(T item, int index, Runnable complete);
}
