package flabbergast.lang;

import java.util.function.Function;

/** Convert two names, which may contain either a string or a number, into a common type. */
public interface NameBiFunction<T> {
  /**
   * Bind a single name to a function
   *
   * @param left the name to bind
   * @param function the function to bind
   * @param <T> the return type of the function
   */
  static <T> NameFunction<T> capture(long left, NameBiFunction<T> function) {
    return new NameFunction<>() {
      @Override
      public T apply(long ordinal) {
        return function.apply(left, ordinal);
      }

      @Override
      public T apply(String name) {
        return function.apply(left, name);
      }
    };
  }

  /**
   * Bind a single name to a function
   *
   * @param left the name to bind
   * @param function the function to bind
   * @param <T> the return type of the function
   */
  static <T> NameFunction<T> capture(String left, NameBiFunction<T> function) {
    return new NameFunction<>() {
      @Override
      public T apply(long ordinal) {
        return function.apply(left, ordinal);
      }

      @Override
      public T apply(String name) {
        return function.apply(left, name);
      }
    };
  }

  /**
   * Modify the output of a name function
   *
   * @param original the original name function
   * @param function the transformation to apply
   * @param <T> the original return type
   * @param <R> the transformed return type
   */
  static <T, R> NameBiFunction<R> compose(
      NameBiFunction<T> original, Function<? super T, R> function) {
    return new NameBiFunction<>() {
      @Override
      public R apply(long left, long right) {
        return function.apply(original.apply(left, right));
      }

      @Override
      public R apply(long left, String right) {
        return function.apply(original.apply(left, right));
      }

      @Override
      public R apply(String left, long right) {
        return function.apply(original.apply(left, right));
      }

      @Override
      public R apply(String left, String right) {
        return function.apply(original.apply(left, right));
      }
    };
  }

  /**
   * Convert a function that takes two name arguments into a curried form.
   *
   * @param function the function to convert
   * @param <T> the return type of the function
   */
  static <T> NameFunction<NameFunction<T>> convert(NameBiFunction<T> function) {
    return new NameFunction<>() {
      @Override
      public NameFunction<T> apply(long left) {
        return capture(left, function);
      }

      @Override
      public NameFunction<T> apply(String left) {
        return capture(left, function);
      }
    };
  }

  /**
   * Convert two attribute numbers into a common type
   *
   * @param left the attribute number of the receiver
   * @param right the attribute number of the parameter
   */
  T apply(long left, long right);
  /**
   * Convert an attribute number and name into a common type
   *
   * @param left the attribute number of the receiver
   * @param right the attribute name of the parameter
   */
  T apply(long left, String right);
  /**
   * Convert an attribute name and number into a common type
   *
   * @param left the attribute name of the receiver
   * @param right the attribute number of the parameter
   */
  T apply(String left, long right);
  /**
   * Convert two attribute names into a common type
   *
   * @param left the attribute name of the receiver
   * @param right the attribute name of the parameter
   */
  T apply(String left, String right);
}
