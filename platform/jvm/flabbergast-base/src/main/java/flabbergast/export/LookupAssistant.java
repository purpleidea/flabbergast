package flabbergast.export;

import flabbergast.lang.Any;
import flabbergast.lang.AnyConverter;
import flabbergast.lang.Context;
import flabbergast.lang.Definition;
import flabbergast.lang.Frame;
import flabbergast.lang.Future;
import flabbergast.lang.LookupHandler;
import flabbergast.lang.OverrideDefinition;
import flabbergast.lang.Promise;
import flabbergast.lang.SourceReference;
import flabbergast.lang.Template;
import flabbergast.lang.TypeErrorLocation;
import flabbergast.util.ConcurrentConsumer;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Performing multiple and varied lookups in order to bind a native function
 *
 * <p>Every native function bound in this way needs to have a class that will hold all the values
 * collected from the Flabbergast program. Once all the values are stored, the holding object will
 * have {@link Recipient#run(Future, SourceReference, Context)} called to compute the result and
 * then it must call {@link Future#complete(Object)}. There are easier to use methods provided in
 * {@link NativeBinding}, however, those methods may be insufficient under several scenarios:
 *
 * <ul>
 *   <li>the number of arguments is large
 *   <li>the return value is a {@link Frame} or {@link Template} that references the calling context
 *   <li>the function needs access to the calling context for reference or to evaluate {@link
 *       Promise} objects obtained from the calling context
 *   <li>the function needs asynchronous execution
 *   <li>the function needs complex error handling
 * </ul>
 *
 * This interface does not directly assist with conditional or chained lookups (<i>i.e.</i>,
 * performing lookups based on the values found by other lookups)
 *
 * @param <R> the type of the class holding the looked-up values
 */
public abstract class LookupAssistant<R extends LookupAssistant.Recipient> {

  /** A holding class that can be executed once its values have been looked up */
  public interface Recipient {
    /**
     * Begin to compute a result after arguments have been looked up
     *
     * @param future the future to which the value should be returned by {@link
     *     Future#complete(Object)}
     * @param sourceReference the calling source trace
     * @param context the calling lookup environment
     * @see LookupAssistant
     */
    void run(Future<Any> future, SourceReference sourceReference, Context context);
  }

  /**
   * Create a definition which performs the lookups provided
   *
   * @param <R> the type of the class holding the looked-up values
   * @param constructor createFromValues a new instance of the holder
   * @param savers the lookups to be performed and how to write the value into the holder
   */
  @SafeVarargs
  public static <R extends Recipient> Definition create(
      Supplier<R> constructor, LookupAssistant<R>... savers) {
    return create(constructor, Stream.of(savers));
  }

  /**
   * Create a definition which performs the lookups provided
   *
   * @param <R> the type of the class holding the looked-up values
   * @param constructor createFromValues a new instance of the holder
   * @param savers the lookups to be performed and how to write the value into the holder
   */
  public static <R extends Recipient> Definition create(
      Supplier<R> constructor, Stream<LookupAssistant<R>> savers) {
    final var saverList = savers.collect(Collectors.toList());
    return (future, sourceReference, context) ->
        () -> {
          final var target = constructor.get();
          ConcurrentConsumer.process(
              saverList,
              new ConcurrentConsumer<>() {
                @Override
                public void complete() {
                  target.run(future, sourceReference, context);
                }

                @Override
                public void process(LookupAssistant<R> item, int index, Runnable complete) {
                  item.start(target, future, sourceReference, context, complete);
                }
              });
        };
  }

  /**
   * Create an override definition which unboxes the original (overridden) value and performs the
   * lookup provided
   *
   * <p>In this case, the value being overridden is provided.
   *
   * @param <R> the type of the class holding the looked-up values
   * @param <T> the type of the original value to the override
   * @param converter convert the initial (overridden) value
   * @param constructor createFromValues a new instance of the holder for an initial value
   * @param savers the lookups to be performed and how to write the value into the holder
   */
  @SafeVarargs
  public static <R extends Recipient, T> OverrideDefinition create(
      AnyConverter<T> converter, Function<T, R> constructor, LookupAssistant<R>... savers) {
    return create(converter, constructor, Stream.of(savers));
  }

  /**
   * Create an override definition which unboxes the original (overridden) value and performs the
   * lookup provided
   *
   * <p>In this case, the value being overridden is provided.
   *
   * @param <R> the type of the class holding the looked-up values
   * @param <T> the type of the original value to the override
   * @param converter convert the initial (overridden) value
   * @param constructor createFromValues a new instance of the holder for an initial value
   * @param savers the lookups to be performed and how to write the value into the holder
   */
  public static <R extends Recipient, T> OverrideDefinition create(
      AnyConverter<T> converter, Function<T, R> constructor, Stream<LookupAssistant<R>> savers) {
    final var saverList = savers.collect(Collectors.toList());
    return (future, sourceReference, context, initial) ->
        () ->
            initial.accept(
                converter.asConsumer(
                    future,
                    sourceReference,
                    TypeErrorLocation.ORIGINAL,
                    value -> {
                      final var target = constructor.apply(value);
                      ConcurrentConsumer.process(
                          saverList,
                          new ConcurrentConsumer<>() {
                            @Override
                            public void complete() {
                              target.run(future, sourceReference, context);
                            }

                            @Override
                            public void process(
                                LookupAssistant<R> item, int index, Runnable complete) {
                              item.start(target, future, sourceReference, context, complete);
                            }
                          });
                    }));
  }

  /**
   * Lookup a value from Flabbergast and unbox it
   *
   * @param <R> the type of the class holding the looked-up values
   * @param <T> the type of the value being looked up
   * @param converter the converter for the type being looked up
   * @param writer a setter to write the value into the holding instance
   * @param names the Flabbergast identifiers to lookup (i.e., <tt>"x", "y"</tt> is equivalent to
   *     <tt>x.y</tt>)
   */
  public static <R extends Recipient, T> LookupAssistant<R> find(
      AnyConverter<T> converter, BiConsumer<R, T> writer, String... names) {
    return new LookupAssistant<>(names) {

      @Override
      void write(
          R target,
          Future<Any> future,
          SourceReference sourceReference,
          Context context,
          Any result,
          Runnable complete) {
        result.accept(
            converter.asConsumer(
                future,
                sourceReference,
                TypeErrorLocation.lookup(names),
                typedResult -> {
                  writer.accept(target, typedResult);
                  complete.run();
                }));
      }
    };
  }

  /**
   * Lookup a boxed value from Flabbergast
   *
   * @param writer a setter to write the value into the holding instance
   * @param names the Flabbergast identifiers to lookup (i.e., <tt>"x", "y"</tt> is equivalent to
   *     <tt>x.y</tt>)
   * @param <R> the type of the class holding the looked-up values
   */
  public static <R extends Recipient> LookupAssistant<R> find(
      BiConsumer<R, Any> writer, String... names) {
    return new LookupAssistant<>(names) {
      @Override
      void write(
          R target,
          Future<Any> future,
          SourceReference sourceReference,
          Context context,
          Any result,
          Runnable complete) {
        writer.accept(target, result);
        complete.run();
      }
    };
  }

  private final Definition lookup;

  private LookupAssistant(String... names) {
    lookup = LookupHandler.CONTEXTUAL.create(names);
  }

  final void start(
      R target,
      Future<Any> future,
      SourceReference sourceReference,
      Context context,
      Runnable complete) {
    future.launch(
        lookup,
        sourceReference,
        context,
        result -> write(target, future, sourceReference, context, result, complete));
  }

  abstract void write(
      R target,
      Future<Any> future,
      SourceReference sourceReference,
      Context context,
      Any result,
      Runnable complete);
}
