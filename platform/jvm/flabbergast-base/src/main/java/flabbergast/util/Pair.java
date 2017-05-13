package flabbergast.util;

import flabbergast.lang.Name;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.*;

/** A key-value pair */
public final class Pair<T, U> {

  /**
   * Create a consumer for pairs that unboxes the pair and provides the values individually.
   *
   * @param consumer the consumer of the two values in the pair
   * @param <T> the type of the first item in the pair
   * @param <U> the type of the second item in the pair
   */
  public static <T, U> Consumer<Pair<T, U>> consume(BiConsumer<? super T, ? super U> consumer) {
    return p -> consumer.accept(p.first(), p.second());
  }

  /**
   * Create a function which numbers items and returns them as numbered pairs
   *
   * @param <T> the type of the items
   */
  public static <T> Numberer<Integer, T> number() {
    return number(Integer::valueOf);
  }

  /**
   * Create a function which numbers items using a labelling function
   *
   * @param namer the labelling function
   * @param <C> the type of the label
   * @param <T> the type of the items
   */
  public static <C, T> Numberer<C, T> number(IntFunction<? extends C> namer) {
    return new Numberer<>() {
      private int count;

      @Override
      public Pair<C, T> apply(T t) {
        return of(namer.apply(count++), t);
      }

      @Override
      public int size() {
        return count;
      }
    };
  }

  /** Create a pair by copying the values in an {@link Entry} */
  public static <T, U> Pair<T, U> of(Entry<? extends T, ? extends U> entry) {
    return new Pair<>(entry);
  }

  /** Create a pair from two components */
  public static <T, U> Pair<T, U> of(T first, U second) {
    return new Pair<>(first, second);
  }

  /**
   * Create a function which numbers items as if they were placed in a Flabbergast list
   *
   * @param <T> the type of the items
   */
  public static <T> Numberer<Name, T> ordinate() {
    return number(i -> Name.of(i + 1));
  }

  /**
   * Create a function which transforms pairs by combining their components
   *
   * @param function the function that does the combining
   */
  public static <T, U, R> Function<Pair<T, U>, R> transform(
      BiFunction<? super T, ? super U, R> function) {
    return p -> function.apply(p.first(), p.second());
  }

  /**
   * Create a function which transforms pairs by individually transforming their components
   *
   * @param first the transformer of the first item the in the pair
   * @param second the transformer of the second item in the pair
   */
  public static <T, S, U, V> Function<Pair<T, U>, Pair<S, V>> transform(
      Function<? super T, S> first, Function<? super U, V> second) {
    return p -> of(first.apply(p.first()), second.apply(p.second()));
  }

  private final T first;
  private final U second;

  private Pair(Entry<? extends T, ? extends U> entry) {
    first = entry.getKey();
    second = entry.getValue();
  }

  private Pair(T first, U second) {
    this.first = first;
    this.second = second;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final var pair = (Pair<?, ?>) o;
    return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
  }

  /** Get the first component in a pair */
  public T first() {
    return first;
  }

  @Override
  public int hashCode() {
    return Objects.hash(first, second);
  }

  /** Get the second component in a pair */
  public U second() {
    return second;
  }

  @Override
  public String toString() {
    return "<" + first + ", " + second + ">";
  }
}
