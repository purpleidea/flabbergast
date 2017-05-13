package flabbergast.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A monad that stores nothing, a value, or an error
 *
 * @param <T> the type of value that is stored
 */
public abstract class Result<T> {
  /**
   * Create a Maybe monad with no value
   *
   * <p>Most operations on an empty Maybe monad will yield an empty result.
   */
  public static <T> Result<T> empty() {
    return new Result<>() {

      @Override
      public Result<T> combine(Result<T> other) {
        return other;
      }

      @Override
      public Result<T> filter(WhinyPredicate<T> predicate) {
        return this;
      }

      @Override
      public Result<T> filter(WhinyPredicate<T> predicate, String error) {
        return this;
      }

      @Override
      public <R> Result<R> flatMap(WhinyFunction<T, Result<R>> func) {
        return empty();
      }

      @Override
      public void forEach(Consumer<? super T> consumer) {}

      @Override
      public T get() {
        return null;
      }

      @Override
      public <R> Result<R> map(WhinyFunction<? super T, R> func) {
        return empty();
      }

      @Override
      public <R> Result<R> optionalMap(WhinyFunction<? super T, Optional<R>> func) {
        return empty();
      }

      @Override
      public <R> Result<R> optionalMap(
          WhinyFunction<? super T, Optional<R>> func, String errorMessage) {
        return empty();
      }

      @Override
      public T orElse(T other) {
        return other;
      }

      @Override
      public T orElseGet(Supplier<? extends T> other) {
        return other.get();
      }

      @Override
      public T orElseThrow(String error) {
        throw new IllegalStateException(error);
      }

      @Override
      public <R> R orHandle(
          Function<? super T, ? extends R> goodCase,
          String emptyMessage,
          Function<String, ? extends R> errorCase) {
        return errorCase.apply(emptyMessage);
      }

      @Override
      public Result<T> peek(WhinyConsumer<? super T> consumer) {
        return this;
      }

      @Override
      public Result<T> reduce(Supplier<Result<T>> supplier) {
        return supplier.get();
      }

      @Override
      public Stream<T> stream() {
        return Stream.empty();
      }
    };
  }

  /** Create a monad containing an error */
  public static <T> Result<T> error(String message) {
    return new Result<>() {

      @Override
      public Result<T> combine(Result<T> other) {
        return this;
      }

      @Override
      public Result<T> filter(WhinyPredicate<T> predicate) {
        return this;
      }

      @Override
      public Result<T> filter(WhinyPredicate<T> predicate, String error) {
        return this;
      }

      @Override
      public <R> Result<R> flatMap(WhinyFunction<T, Result<R>> func) {
        return error(message);
      }

      @Override
      public void forEach(Consumer<? super T> consumer) {
        throw new IllegalStateException(message);
      }

      @Override
      public T get() {
        throw new IllegalStateException(message);
      }

      @Override
      public <R> Result<R> map(WhinyFunction<? super T, R> func) {
        return error(message);
      }

      @Override
      public <R> Result<R> optionalMap(WhinyFunction<? super T, Optional<R>> func) {
        return error(message);
      }

      @Override
      public <R> Result<R> optionalMap(
          WhinyFunction<? super T, Optional<R>> func, String errorMessage) {
        return error(message);
      }

      @Override
      public T orElse(T other) {
        throw new IllegalStateException(message);
      }

      @Override
      public T orElseGet(Supplier<? extends T> other) {
        throw new IllegalStateException(message);
      }

      @Override
      public T orElseThrow(String error) {
        throw new IllegalStateException(message);
      }

      @Override
      public <R> R orHandle(
          Function<? super T, ? extends R> goodCase,
          String emptyMessage,
          Function<String, ? extends R> errorCase) {
        return errorCase.apply(message);
      }

      @Override
      public Result<T> peek(WhinyConsumer<? super T> consumer) {
        return this;
      }

      @Override
      public Result<T> reduce(Supplier<Result<T>> supplier) {
        return this;
      }

      @Override
      public Stream<T> stream() {
        throw new IllegalStateException(message);
      }
    };
  }
  /**
   * Create a monad containing a value
   *
   * @param arg the value; if null, treated as if an empty monad was created
   */
  public static <T> Result<T> of(T arg) {
    if (arg == null) {
      return empty();
    }
    return new Result<>() {

      @Override
      public Result<T> combine(Result<T> other) {
        return this;
      }

      @Override
      public Result<T> filter(WhinyPredicate<T> predicate) {
        try {
          return predicate.test(arg) ? this : empty();
        } catch (final Exception e) {
          return error(e.getMessage());
        }
      }

      @Override
      public Result<T> filter(WhinyPredicate<T> predicate, String errorMessage) {
        try {
          return predicate.test(arg) ? this : error(errorMessage);
        } catch (final Exception e) {
          return error(e.getMessage());
        }
      }

      @Override
      public <R> Result<R> flatMap(WhinyFunction<T, Result<R>> func) {
        try {
          return func.apply(arg);
        } catch (final Exception e) {
          return error(e.getMessage());
        }
      }

      @Override
      public void forEach(Consumer<? super T> consumer) {
        consumer.accept(arg);
      }

      @Override
      public T get() {
        return arg;
      }

      @Override
      public <R> Result<R> map(WhinyFunction<? super T, R> func) {
        try {
          return of(func.apply(arg));
        } catch (final Exception e) {
          return error(e.getMessage());
        }
      }

      @Override
      public <R> Result<R> optionalMap(WhinyFunction<? super T, Optional<R>> func) {
        try {
          return of(func.apply(arg).orElse(null));
        } catch (final Exception e) {
          return error(e.getMessage());
        }
      }

      @Override
      public <R> Result<R> optionalMap(
          WhinyFunction<? super T, Optional<R>> func, String errorMessage) {
        try {
          return func.apply(arg).map(Result::of).orElseGet(() -> error(errorMessage));
        } catch (final Exception e) {
          return error(e.getMessage());
        }
      }

      @Override
      public T orElse(T other) {
        return arg;
      }

      @Override
      public T orElseGet(Supplier<? extends T> other) {
        return arg;
      }

      @Override
      public T orElseThrow(String error) {
        return arg;
      }

      @Override
      public <R> R orHandle(
          Function<? super T, ? extends R> goodCase,
          String emptyMessage,
          Function<String, ? extends R> errorCase) {
        return goodCase.apply(arg);
      }

      @Override
      public Result<T> peek(WhinyConsumer<? super T> consumer) {
        try {
          consumer.accept(arg);
        } catch (final Exception e) {
          return error(e.getMessage());
        }
        return this;
      }

      @Override
      public Result<T> reduce(Supplier<Result<T>> supplier) {
        return this;
      }

      @Override
      public Stream<T> stream() {
        return Stream.of(arg);
      }
    };
  }

  /**
   * Convert an optional into a maybe
   *
   * <p>The empty optional is mapped to an empty maybe and an optional containing an item is mapped
   * to a maybe containing that same item
   */
  public static <T> Result<T> ofOptional(Optional<T> optional) {
    return optional.map(Result::of).orElseGet(Result::empty);
  }

  private Result() {}

  /** Combine two monads: if the receiver is non-empty, choose it, otherwise, choose the argument */
  public abstract Result<T> combine(Result<T> other);

  /**
   * Filter the contents of the monad
   *
   * <p>If the monad has a value and the predicate fails, an empty monad is returned. Otherwise, the
   * original empty/error monad is returned.
   */
  public abstract Result<T> filter(WhinyPredicate<T> predicate);

  /**
   * Filter the contents of the monad
   *
   * <p>If the monad has a value and the predicate fails, an error monad is returned. Otherwise, the
   * original empty/error monad is returned.
   */
  public abstract Result<T> filter(WhinyPredicate<T> predicate, String error);

  /** Apply the transformation to the monad */
  public abstract <R> Result<R> flatMap(WhinyFunction<T, Result<R>> func);

  /**
   * Conver the value in the monad to a stream.
   *
   * @param func a function that transforms the value in the monad into a stream
   * @return the stream provided by the transformation if the monad is non-empty, an empty stream if
   *     the monad is empty, or an {@link IllegalStateException} if the monad contains an error
   */
  public final <R> Stream<R> flatStream(WhinyFunction<? super T, Stream<R>> func) {
    return map(func).orElse(Stream.empty());
  }

  /**
   * Consume the value in the monad if there is one.
   *
   * <p>If the monad contains an error, an {@link IllegalStateException} is thrown.
   */
  public abstract void forEach(Consumer<? super T> consumer);

  /**
   * Retrieve the value in the monad.
   *
   * <p>If the monad is empty, null is returned. If the monad contains an error, an {@link
   * IllegalStateException} is thrown.
   */
  public abstract T get();

  /**
   * Manipulate the value in the monad.
   *
   * <p>If the monad is empty or contains an error, it is unaffected.
   */
  public abstract <R> Result<R> map(WhinyFunction<? super T, R> func);

  /**
   * Apply a transformation to the monad which returns an optional result
   *
   * <p>The empty optional is transformed to an empty monad and a non-empty optional is transformed
   * to a non-empty monad.
   */
  public abstract <R> Result<R> optionalMap(WhinyFunction<? super T, Optional<R>> func);

  /**
   * Apply a transformation to the monad which returns an optional result and, if that result is
   * empty, createFromValues an error monad
   */
  public abstract <R> Result<R> optionalMap(
      WhinyFunction<? super T, Optional<R>> func, String errorMessage);

  /**
   * Get the value in the monad, or a supplied value if empty.
   *
   * <p>If the monad contains an error, an {@link IllegalStateException} is thrown.
   *
   * @param other the alternate value to use
   */
  public abstract T orElse(T other);

  /**
   * Get the value in the monad, or a supplied value if empty.
   *
   * <p>If the monad contains an error, an {@link IllegalStateException} is thrown.
   *
   * @param other a generator of the alternate value to use
   */
  public abstract T orElseGet(Supplier<? extends T> other);

  /**
   * Get the value in the monad, or throw if empty.
   *
   * <p>If the monad contains an error, an {@link IllegalStateException} is thrown.
   *
   * @param error the error message to throw if the monad is empty
   */
  public abstract T orElseThrow(String error);

  /**
   * Get the result from the monad in any state.
   *
   * @param goodCase transform the value in the monad to the output type required. Typically {@link
   *     Function#identity()}
   * @param emptyMessage The error message to give when the monad is empty
   * @param errorCase transform an error message (either stored in the monad or provided by the
   *     previous parameter) in a value of the target type
   */
  public abstract <R> R orHandle(
      Function<? super T, ? extends R> goodCase,
      String emptyMessage,
      Function<String, ? extends R> errorCase);

  /**
   * View the current value in the monad if there is one
   *
   * <p>If the consumer throws an error, the monad is replaced by the error. If the monad is empty
   * or an error, the consumer is not called.
   */
  public abstract Result<T> peek(WhinyConsumer<? super T> consumer);

  /** Replace an empty monad with the value generated. */
  public abstract Result<T> reduce(Supplier<Result<T>> supplier);

  /**
   * Given a stream of possible monads, return the first monad that contains either an error or a
   * value
   */
  @SuppressWarnings("RedundantTypeArguments")
  public Result<T> reduce(Stream<Supplier<Result<T>>> stream) {
    return stream.<Result<T>>reduce(this, Result::reduce, Result::combine);
  }

  /**
   * Convert the monad to a stream.
   *
   * <p>If the monad contains an error, an {@link IllegalStateException} is thrown.
   *
   * @return an empty stream if the monad is empty, or a stream of one item if th monad contains a
   *     value
   */
  public abstract Stream<T> stream();
}
