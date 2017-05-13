package flabbergast.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Transform a collection of input data to output data asynchronously
 *
 * @param <T> the input type
 * @param <R> the output type
 */
public interface ConcurrentMapper<T, R> {

  /**
   * Asynchronously transform a stream into a list of items
   *
   * @param input the input data stream
   * @param mapper the transformer
   * @param <T> the input type
   * @param <R> the output type
   */
  static <T, R> void process(Stream<? extends T> input, ConcurrentMapper<T, R> mapper) {
    final var interlock = new AtomicInteger(1);
    final var output = new ConcurrentLinkedDeque<Pair<Integer, R>>();
    input
        .map(Pair.number())
        .forEachOrdered(
            pair -> {
              interlock.incrementAndGet();
              mapper.process(
                  pair.second(),
                  pair.first(),
                  value -> {
                    output.add(Pair.of(pair.first(), value));
                    if (interlock.decrementAndGet() == 0) {
                      mapper.emit(
                          output
                              .stream()
                              .sorted(Comparator.comparing(Pair::first))
                              .map(Pair::second)
                              .collect(Collectors.toList()));
                    }
                  });
            });
    if (interlock.decrementAndGet() == 0) {
      mapper.emit(
          output
              .stream()
              .sorted(Comparator.comparing(Pair::first))
              .map(Pair::second)
              .collect(Collectors.toList()));
    }
  }
  /**
   * Asynchronously transform a collection into a list of items
   *
   * @param input the input data collection
   * @param mapper the transformer
   * @param <T> the input type
   * @param <R> the output type
   */
  static <T, R> void process(Collection<? extends T> input, ConcurrentMapper<T, R> mapper) {
    final var output = new AtomicReferenceArray<R>(input.size());
    final var interlock = new AtomicInteger(input.size());
    var i = 0;
    for (final var value : input) {
      final var index = i++;
      mapper.process(
          value,
          index,
          v -> {
            output.set(index, v);
            if (interlock.decrementAndGet() == 0) {
              final var list = new ArrayList<R>(output.length());
              for (var j = 0; j < input.size(); j++) {
                list.add(j, output.get(j));
              }
              mapper.emit(list);
            }
          });
    }
  }

  /**
   * Process the collected output
   *
   * @param output the transformed items in input order
   */
  void emit(List<R> output);

  /**
   * Convert one input item
   *
   * @param item the input value
   * @param index the position of the input item in the input collection
   * @param output a callback to collect the output; this can be called at most once; if not called,
   *     the process will never complete
   */
  void process(T item, int index, Consumer<R> output);
}
