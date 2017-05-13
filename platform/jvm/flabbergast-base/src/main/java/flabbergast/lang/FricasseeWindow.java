package flabbergast.lang;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Definition for how long a grouping window should be for {@link FricasseeGrouper#windowed(
 * FricasseeWindow, FricasseeWindow)}
 *
 * @param <T> the key type being used for grouping
 */
public abstract class FricasseeWindow<T> {
  /** Create non-overlapping windows by starting the next window at the end of the previous one */
  public static final FricasseeWindow<Void> NON_OVERLAPPING =
      new FricasseeWindow<>() {
        @Override
        void compute(
            Future<?> future,
            SourceReference sourceReference,
            Context context,
            Consumer<? super Void> consumer) {
          consumer.accept(null);
        }

        @Override
        <N> Optional<Integer> findEnd(int start, List<WindowItem<Void, N>> windowItems) {
          return start < windowItems.size() ? Optional.of(windowItems.size()) : Optional.empty();
        }

        @Override
        <L> Optional<Integer> findNext(int start, int end, List<WindowItem<L, Void>> windowItems) {
          return end < windowItems.size() ? Optional.of(end) : Optional.empty();
        }
      };

  /**
   * The window size is set to a fixed number of items
   *
   * @param size the number of items
   */
  public static FricasseeWindow<Void> count(int size) {
    return new FricasseeWindow<>() {
      @Override
      void compute(
          Future<?> future,
          SourceReference sourceReference,
          Context context,
          Consumer<? super Void> consumer) {
        consumer.accept(null);
      }

      @Override
      <N> Optional<Integer> findEnd(int start, List<WindowItem<Void, N>> items) {
        if (start + size > items.size()) return Optional.empty();
        return Optional.of(start + size);
      }

      @Override
      <L> Optional<Integer> findNext(int start, int end, List<WindowItem<L, Void>> windowItems) {
        return start + size < windowItems.size() ? Optional.of(start + size) : Optional.empty();
      }
    };
  }

  /**
   * The window size is set to a fixed range of items
   *
   * @param definition compute the integer value used to determine the duration value
   * @param duration the length of the range
   */
  public static FricasseeWindow<Long> duration(Definition definition, long duration) {
    return duration(definition, AnyConverter.asInt(false), x -> x + duration);
  }

  /**
   * The window size is set to a fixed range of items
   *
   * @param definition compute the floating-point value used to determine the duration value
   * @param duration the length of the range
   */
  public static FricasseeWindow<Double> duration(Definition definition, double duration) {
    return duration(definition, AnyConverter.asFloat(false), x -> x + duration);
  }

  /**
   * The window size is set to a fixed range of items
   *
   * @param definition compute the value used to determine the duration value
   * @param converter the converter for the expression
   * @param computeDuration compute the end of a range given its beginning
   * @param <T> the type key being used for grouping
   */
  public static <T> FricasseeWindow<T> duration(
      Definition definition, AnyBidiConverter<T> converter, UnaryOperator<T> computeDuration) {
    return new FricasseeWindow<>() {
      @Override
      void compute(
          Future<?> future,
          SourceReference sourceReference,
          Context context,
          Consumer<? super T> consumer) {
        future.launch(
            definition,
            sourceReference,
            context,
            converter.asConsumer(future, sourceReference, TypeErrorLocation.WINDOW, consumer));
      }

      @Override
      <N> Optional<Integer> findEnd(int start, List<WindowItem<T, N>> items) {
        final var endValue = computeDuration.apply(items.get(start).lengthValue());
        var end = start + 1;
        while (end < items.size()
            && converter.compare(items.get(end).lengthValue(), endValue) <= 0) {
          end++;
        }
        return Optional.of(end);
      }

      @Override
      <L> Optional<Integer> findNext(int start, int end, List<WindowItem<L, T>> windowItems) {
        final var nextValue = computeDuration.apply(windowItems.get(start).nextValue());
        var next = start + 1;
        while (next < windowItems.size()
            && converter.compare(windowItems.get(next).nextValue(), nextValue) < 0) {
          next++;
        }
        return next < windowItems.size() ? Optional.of(next) : Optional.empty();
      }
    };
  }

  /**
   * The window size is set to a variable range
   *
   * @param definition compute the integer value used to determine the session value
   * @param adjacent the distance between two successive values that can be neighbouring items in a
   *     session
   * @param wholeSession the distance between the first and last values that can be in the same
   *     session
   */
  public static FricasseeWindow<Long> session(
      Definition definition, long adjacent, long wholeSession) {
    return session(
        definition,
        AnyConverter.asInt(false),
        (x, y) -> Math.abs(x - y) <= adjacent,
        (x, y) -> Math.abs(x - y) < wholeSession);
  }
  /**
   * The window size is set to a variable range
   *
   * @param definition compute the floating-point value used to determine the session value
   * @param adjacent the distance between two successive values that can be neighbouring items in a
   *     session
   * @param wholeSession the distance between the first and last values that can be in the same
   *     session
   */
  public static FricasseeWindow<Double> session(
      Definition definition, double adjacent, double wholeSession) {
    return session(
        definition,
        AnyConverter.asFloat(false),
        (x, y) -> Math.abs(x - y) <= adjacent,
        (x, y) -> Math.abs(x - y) < wholeSession);
  }
  /**
   * The window size is set to a variable range
   *
   * @param definition compute the value used to determine the session value
   * @param adjacent checks that two successive items are close enough together to continue building
   *     the window
   * @param wholeSession checks that the start and end items are close enough together to continue
   *     building the window
   * @param <T> the type key being used for grouping
   */
  public static <T> FricasseeWindow<T> session(
      Definition definition,
      AnyConverter<T> converter,
      BiPredicate<T, T> adjacent,
      BiPredicate<T, T> wholeSession) {
    return new FricasseeWindow<>() {
      @Override
      void compute(
          Future<?> future,
          SourceReference sourceReference,
          Context context,
          Consumer<? super T> consumer) {
        future.launch(
            definition,
            sourceReference,
            context,
            converter.asConsumer(future, sourceReference, TypeErrorLocation.WINDOW, consumer));
      }

      @Override
      <N> Optional<Integer> findEnd(int start, List<WindowItem<T, N>> items) {
        var end = start + 1;
        while (end < items.size()
            && adjacent.test(items.get(end - 1).lengthValue(), items.get(end).lengthValue())
            && wholeSession.test(items.get(start).lengthValue(), items.get(end).lengthValue())) {
          end++;
        }
        return Optional.of(end);
      }

      @Override
      <L> Optional<Integer> findNext(int start, int otherEnd, List<WindowItem<L, T>> items) {
        var end = start + 1;
        while (end < items.size()
            && adjacent.test(items.get(end - 1).nextValue(), items.get(end).nextValue())
            && wholeSession.test(items.get(start).nextValue(), items.get(end).nextValue())) {
          end++;
        }
        return Optional.of(end);
      }
    };
  }

  private FricasseeWindow() {}

  abstract void compute(
      Future<?> future,
      SourceReference sourceReference,
      Context context,
      Consumer<? super T> consumer);

  abstract <N> Optional<Integer> findEnd(int start, List<WindowItem<T, N>> items);

  abstract <L> Optional<Integer> findNext(int start, int end, List<WindowItem<L, T>> items);
}
