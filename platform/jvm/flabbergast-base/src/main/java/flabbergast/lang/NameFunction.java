package flabbergast.lang;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Convert a name, which may contain either a string or a number, into a common type. */
public interface NameFunction<T> {
  /**
   * Transform the output from a name function
   *
   * @param original the name function to change
   * @param function the transformation to apply
   * @param <T> the original output type
   * @param <R> the new output type
   */
  static <T, R> NameFunction<R> compose(NameFunction<T> original, Function<? super T, R> function) {
    return new NameFunction<>() {
      @Override
      public R apply(long ordinal) {
        return function.apply(original.apply(ordinal));
      }

      @Override
      public R apply(String name) {
        return function.apply(original.apply(name));
      }
    };
  }

  /**
   * Consume the output of a name function
   *
   * @param function the function to transform the names
   * @param consumer the consumer of the result
   * @param <T> the return type of the function
   */
  static <T> NameConsumer compose(NameFunction<T> function, Consumer<? super T> consumer) {
    return new NameConsumer() {
      @Override
      public void accept(long ordinal) {
        consumer.accept(function.apply(ordinal));
      }

      @Override
      public void accept(String name) {
        consumer.accept(function.apply(name));
      }
    };
  }

  /**
   * Transform the output of a supplier of names
   *
   * @param supplier a supplier of names
   * @param function the function to transform the names
   * @param <T> the return type of the function
   */
  static <T> Supplier<T> compose(Supplier<Name> supplier, NameFunction<T> function) {
    return () -> supplier.get().apply(function);
  }
  /**
   * Convert a number
   *
   * @param ordinal the attribute number
   */
  T apply(long ordinal);
  /**
   * Convert a string
   *
   * @param name the attribute name
   */
  T apply(String name);
}
