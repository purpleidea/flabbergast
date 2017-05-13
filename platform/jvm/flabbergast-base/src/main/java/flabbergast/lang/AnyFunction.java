package flabbergast.lang;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Extract a boxed value ({@link Any}) into a specified result type. */
public interface AnyFunction<T> {
  /**
   * Convert a Flabbergast value and consume the result
   *
   * @param function the function to convert the Flabbergast value
   * @param consumer the consumer of the result
   * @param <T> the type of the converted result
   */
  static <T> AnyConsumer compose(AnyFunction<T> function, Consumer<? super T> consumer) {
    return new AnyConsumer() {
      @Override
      public void accept() {
        consumer.accept(function.apply());
      }

      @Override
      public void accept(boolean value) {
        consumer.accept(function.apply(value));
      }

      @Override
      public void accept(byte[] value) {
        consumer.accept(function.apply(value));
      }

      @Override
      public void accept(double value) {
        consumer.accept(function.apply(value));
      }

      @Override
      public void accept(Frame value) {
        consumer.accept(function.apply(value));
      }

      @Override
      public void accept(long value) {
        consumer.accept(function.apply(value));
      }

      @Override
      public void accept(LookupHandler value) {
        consumer.accept(function.apply(value));
      }

      @Override
      public void accept(Str value) {
        consumer.accept(function.apply(value));
      }

      @Override
      public void accept(Template value) {
        consumer.accept(function.apply(value));
      }
    };
  }

  /**
   * Transform the result of an any function
   *
   * @param function the original function
   * @param transformation the transformation to apply to the result
   * @param <T> the original return type
   * @param <R> the new return type
   */
  static <T, R> AnyFunction<R> transform(
      AnyFunction<T> function, Function<? super T, R> transformation) {
    return new AnyFunction<>() {
      @Override
      public R apply() {
        return transformation.apply(function.apply());
      }

      @Override
      public R apply(boolean value) {
        return transformation.apply(function.apply(value));
      }

      @Override
      public R apply(byte[] value) {
        return transformation.apply(function.apply(value));
      }

      @Override
      public R apply(double value) {
        return transformation.apply(function.apply(value));
      }

      @Override
      public R apply(Frame value) {
        return transformation.apply(function.apply(value));
      }

      @Override
      public R apply(long value) {
        return transformation.apply(function.apply(value));
      }

      @Override
      public R apply(LookupHandler value) {
        return transformation.apply(function.apply(value));
      }

      @Override
      public R apply(Str value) {
        return transformation.apply(function.apply(value));
      }

      @Override
      public R apply(Template value) {
        return transformation.apply(function.apply(value));
      }
    };
  }
  /**
   * Transform the output of a supplier of any values
   *
   * @param supplier a supplier of any values
   * @param function the function to transform the values
   * @param <T> the return type of the function
   */
  static <T> Supplier<T> compose(Supplier<Any> supplier, AnyFunction<T> function) {
    return () -> supplier.get().apply(function);
  }
  /** Receive a <tt>Null</tt> value. */
  T apply();

  /** Receive a <tt>Bool</tt> value. */
  T apply(boolean value);
  /** Receive a <tt>Bin</tt> value. */
  T apply(byte[] value);

  /** Receive a <tt>Float</tt> value. */
  T apply(double value);

  /** Receive a <tt>Frame</tt> value. */
  T apply(Frame value);

  /** Receive an <tt>Int</tt> value. */
  T apply(long value);

  /** Receive a <tt>LookupHandler</tt> value. */
  T apply(LookupHandler value);

  /** Receive a <tt>Str</tt> value. */
  T apply(Str value);

  /** Receive a <tt>Template</tt> value. */
  T apply(Template value);
}
