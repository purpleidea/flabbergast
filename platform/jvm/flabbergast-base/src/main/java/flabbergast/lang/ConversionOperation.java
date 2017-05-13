package flabbergast.lang;

import flabbergast.util.ConcurrentMapper;
import flabbergast.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The resulting action from an attempt to convert a Flabbergast value to a Java consumer
 *
 * @param <T> the result type of the conversion
 */
public abstract class ConversionOperation<T> {
  /**
   * Get the attribute of a frame and convert it, or produce an type error for the missing attribute
   *
   * @param frame the frame to search
   * @param name the attribute to extract
   * @param converter the converter for the attribute value
   * @param <T> the type of the result
   */
  public static <T> ConversionOperation<? extends T> attribute(
      Frame frame, Name name, AnyConverter<T> converter) {
    return attribute(
        frame, name, converter, f -> ConversionOperation.<T>fail(TypeExpectation.FRAME));
  }
  /**
   * Get the attribute of a frame and convert it, or perform an alternate operation of the attribute
   * is missing
   *
   * @param frame the frame to search
   * @param name the attribute to extract
   * @param converter the converter for the attribute value
   * @param alternate the operation to perform if the frame does not contain the attribute
   * @param <T> the type of the result
   */
  public static <T> ConversionOperation<? extends T> attribute(
      Frame frame,
      Name name,
      AnyConverter<T> converter,
      Function<Frame, ConversionOperation<? extends T>> alternate) {
    return frame
        .get(name)
        .<ConversionOperation<? extends T>>map(
            attribute ->
                new ConversionOperation<>() {
                  @Override
                  public void resolve(
                      Future<?> future,
                      SourceReference sourceReference,
                      AnyConverter expectedTypes,
                      TypeErrorLocation typeErrorLocation,
                      Consumer<? super T> consumer) {
                    future.await(
                        attribute,
                        sourceReference,
                        String.format("Attribute “%s” in frame %s", name, frame.id()),
                        v ->
                            v.apply(converter.function())
                                .resolve(
                                    future,
                                    sourceReference,
                                    converter,
                                    typeErrorLocation,
                                    consumer));
                  }
                })
        .orElseGet(() -> alternate.apply(frame));
  }
  /**
   * Perform two operations and combine the result
   *
   * @param function the function to combine the results
   * @param first the first conversion operation
   * @param second the second conversion operation
   * @param <T> the result type of the first operation
   * @param <U> the result type of the second operation
   * @param <R> the result type of the combined operation
   */
  public static <T, U, R> ConversionOperation<? extends R> both(
      BiFunction<T, U, R> function,
      ConversionOperation<? extends T> first,
      ConversionOperation<? extends U> second) {
    return new ConversionOperation<>() {
      @Override
      void resolve(
          Future<?> future,
          SourceReference sourceReference,
          AnyConverter expectedTypes,
          TypeErrorLocation typeErrorLocation,
          Consumer<? super R> consumer) {
        final var interlock = new AtomicInteger(2);
        final var firstResult = new AtomicReference<T>();
        final var secondResult = new AtomicReference<U>();
        first.resolve(
            future,
            sourceReference,
            expectedTypes,
            typeErrorLocation,
            f -> {
              firstResult.set(f);
              if (interlock.decrementAndGet() == 0) {
                consumer.accept(function.apply(f, secondResult.get()));
              }
            });
        second.resolve(
            future,
            sourceReference,
            expectedTypes,
            typeErrorLocation,
            s -> {
              secondResult.set(s);
              if (interlock.decrementAndGet() == 0) {
                consumer.accept(function.apply(firstResult.get(), s));
              }
            });
      }
    };
  }

  /**
   * Extract a proxied Java object from a frame
   *
   * @param frame the frame to check
   * @param type the Java type of the object
   * @param alternate a handler if the frame does not contain an object (or an object of the right
   *     type)
   * @param <T> the type of the proxied Java object
   */
  public static <T> ConversionOperation<? extends T> extractProxy(
      Frame frame, Class<T> type, Function<Frame, ConversionOperation<? extends T>> alternate) {
    return frame
        .extractProxy(type)
        .<ConversionOperation<? extends T>>map(ConversionOperation::succeed)
        .orElseGet(() -> alternate.apply(frame));
  }

  /**
   * Raise a type mismatch error.
   *
   * <p>This is preferrable to using a generic error since the transformation context will provide
   * additional details about the operation in progress.
   *
   * @param type the Flabbergast type that was found
   * @param <T> the type of the result of the operation
   */
  static <T> ConversionOperation<? extends T> fail(TypeExpectation type) {
    return new ConversionOperation<>() {
      @Override
      public void resolve(
          Future<?> future,
          SourceReference sourceReference,
          AnyConverter expectedTypes,
          TypeErrorLocation typeErrorLocation,
          Consumer<? super T> consumer) {
        future.error(sourceReference, typeErrorLocation.render(type.toString(), expectedTypes));
      }
    };
  }

  /**
   * Fail the conversion operation with a custom error
   *
   * @param message the error to report
   * @param <T> the type of the result
   */
  public static <T> ConversionOperation<? extends T> fail(String message) {
    return new ConversionOperation<>() {
      @Override
      public void resolve(
          Future<?> future,
          SourceReference sourceReference,
          AnyConverter expectedTypes,
          TypeErrorLocation typeErrorLocation,
          Consumer<? super T> consumer) {
        future.error(sourceReference, message);
      }
    };
  }

  /**
   * Convert all attributes in a frame into a new value
   *
   * @param frame the frame to scan
   * @param converter the converter for the attribute values in the frame
   * @param <T> the type of the result for each attribute
   */
  public static <T> ConversionOperation<Map<Name, T>> frame(
      Frame frame, AnyConverter<T> converter) {
    return new ConversionOperation<>() {
      public void resolve(
          Future<?> future,
          SourceReference sourceReference,
          AnyConverter expectedTypes,
          TypeErrorLocation typeErrorLocation,
          Consumer<? super Map<Name, T>> consumer) {
        frame.forAll(
            future,
            sourceReference,
            new Frame.AttributeConsumer<Pair<Name, ConversionOperation<? extends T>>>() {

              @Override
              public Pair<Name, ConversionOperation<? extends T>> accept(
                  Name name, long ordinal, Any value) {
                return Pair.of(name, value.apply(converter.function()));
              }

              @Override
              public void complete(List<Pair<Name, ConversionOperation<? extends T>>> results) {
                ConcurrentMapper.process(
                    results,
                    new ConcurrentMapper<
                        Pair<Name, ConversionOperation<? extends T>>, Pair<Name, T>>() {
                      @Override
                      public void emit(List<Pair<Name, T>> output) {
                        consumer.accept(
                            output.stream().collect(Collectors.toMap(Pair::first, Pair::second)));
                      }

                      @Override
                      public void process(
                          Pair<Name, ConversionOperation<? extends T>> item,
                          int index,
                          Consumer<Pair<Name, T>> output) {
                        item.second()
                            .resolve(
                                future,
                                sourceReference,
                                converter,
                                typeErrorLocation,
                                result -> output.accept(Pair.of(item.first(), result)));
                      }
                    });
              }

              @Override
              public String describe(Name name) {
                return String.format("Attribute “%s” from frame %s", name, frame.id());
              }
            });
      }
    };
  }

  /**
   * Wait for all promises in a frame to be resolved to boxed values
   *
   * @param frame the frame to scan
   */
  public static ConversionOperation<Map<Name, Any>> frame(Frame frame) {
    return new ConversionOperation<>() {
      @Override
      public void resolve(
          Future<?> future,
          SourceReference sourceReference,
          AnyConverter expectedTypes,
          TypeErrorLocation typeErrorLocation,
          Consumer<? super Map<Name, Any>> consumer) {

        frame.forAll(
            future,
            sourceReference,
            new Frame.AttributeConsumer<Pair<Name, Any>>() {
              @Override
              public Pair<Name, Any> accept(Name name, long ordinal, Any value) {
                return Pair.of(name, value);
              }

              @Override
              public void complete(List<Pair<Name, Any>> results) {
                consumer.accept(
                    results.stream().collect(Collectors.toMap(Pair::first, Pair::second)));
              }

              @Override
              public String describe(Name name) {
                return String.format("Attribute “%s” in frame %s", name, frame.id());
              }
            });
      }
    };
  }

  /**
   * Return a value as the result of the operation
   *
   * @param value the value to return
   * @param <T> the type of the result
   */
  public static <T> ConversionOperation<? extends T> succeed(T value) {
    return new ConversionOperation<>() {
      @Override
      public void resolve(
          Future<?> future,
          SourceReference sourceReference,
          AnyConverter expectedTypes,
          TypeErrorLocation typeErrorLocation,
          Consumer<? super T> consumer) {
        consumer.accept(value);
      }
    };
  }

  /**
   * Create a mapping that will apply a function to the output of an operation
   *
   * @param function the function to apply
   * @param <T> the original result type of the operation
   * @param <R> the new result type of the operation
   */
  public static <T, R>
      Function<ConversionOperation<? extends T>, ConversionOperation<? extends R>> transform(
          Function<? super T, R> function) {
    return input -> input.map(function);
  }

  private ConversionOperation() {}

  /**
   * Transform the result type of the operation to a different operation
   *
   * @param function the transformation to apply
   * @param <R> the new result type
   */
  public final <R> ConversionOperation<R> flatMap(
      Function<? super T, ConversionOperation<R>> function) {
    final var input = this;
    return new ConversionOperation<>() {
      @Override
      public void resolve(
          Future<?> future,
          SourceReference sourceReference,
          AnyConverter expectedTypes,
          TypeErrorLocation typeErrorLocation,
          Consumer<? super R> consumer) {
        input.resolve(
            future,
            sourceReference,
            expectedTypes,
            typeErrorLocation,
            value ->
                function
                    .apply(value)
                    .resolve(future, sourceReference, expectedTypes, typeErrorLocation, consumer));
      }
    };
  }

  /**
   * Transform the result type of the operation
   *
   * @param function the transformation to apply
   * @param <R> the new result type
   */
  public final <R> ConversionOperation<R> map(Function<? super T, R> function) {
    final var input = this;
    return new ConversionOperation<>() {
      @Override
      public void resolve(
          Future<?> future,
          SourceReference sourceReference,
          AnyConverter expectedTypes,
          TypeErrorLocation typeErrorLocation,
          Consumer<? super R> consumer) {
        input.resolve(
            future,
            sourceReference,
            expectedTypes,
            typeErrorLocation,
            value -> consumer.accept(function.apply(value)));
      }
    };
  }

  /**
   * Perform the operation in the provided context
   *
   * @param future the future in which the operation is being performed
   * @param sourceReference the calling execution trance
   * @param expectedTypes the converter for the types expected
   * @param typeErrorLocation a description of the caller necessary to produce a useful error
   *     message if a type error is raised
   * @param consumer the callback in the case of a successful result
   */
  abstract void resolve(
      Future<?> future,
      SourceReference sourceReference,
      AnyConverter expectedTypes,
      TypeErrorLocation typeErrorLocation,
      Consumer<? super T> consumer);
}
