package flabbergast.export;

import static flabbergast.lang.AttributeSource.toSource;

import flabbergast.lang.*;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Base class for building function-like templates that work over variadic <tt>args</tt>
 *
 * @param <T> the type of the values in the variadic arguments
 */
public abstract class BaseFrameTransformer<T> implements LookupAssistant.Recipient {
  /**
   * Prepare a new definition over a variadic <tt>args</tt>-using function-like template
   *
   * @param constructor constructor for new instances
   * @param <H> the type of the function-like template variable holder
   */
  @SafeVarargs
  public static <H extends BaseFrameTransformer<Any>> Definition create(
      Supplier<H> constructor, LookupAssistant<H>... savers) {
    return create(constructor, Stream.of(savers));
  }
  /**
   * Prepare a new definition over a variadic <tt>args</tt>-using function-like template
   *
   * @param anyConverter matcher for the values of the <tt>args</tt> frame
   * @param constructor constructor for new instances
   * @param <T> the type of the values of the <tt>args</tt> frame
   * @param <H> the type of the function-like template variable holder
   */

  @SafeVarargs
  public static <H extends BaseFrameTransformer<T>, T> Definition create(
      AnyConverter<T> anyConverter, Supplier<H> constructor, LookupAssistant<H>... savers) {
    return create(anyConverter, constructor, Stream.of(savers));
  }
  /**
   * Prepare a new definition over a variadic <tt>args</tt>-using function-like template
   *
   * @param anyConverter matcher for the values of the <tt>args</tt> frame
   * @param constructor constructor for new instances
   * @param <T> the type of the values of the <tt>args</tt> frame
   * @param <H> the type of the function-like template variable holder
   */
  public static <H extends BaseFrameTransformer<T>, T> Definition create(
      AnyConverter<T> anyConverter, Supplier<H> constructor, Stream<LookupAssistant<H>> savers) {
    return LookupAssistant.create(
        constructor,
        Stream.concat(
            Stream.of(
                LookupAssistant.find(
                    AnyConverter.frameOf(anyConverter, false),
                    BaseFrameTransformer::setArgs,
                    "args")),
            savers));
  }
  /**
   * Prepare a new definition over a variadic <tt>args</tt>-using function-like template
   *
   * @param constructor constructor for new instances
   * @param <H> the type of the function-like template variable holder
   */
  public static <H extends BaseFrameTransformer<Any>> Definition create(
      Supplier<H> constructor, Stream<LookupAssistant<H>> savers) {
    return LookupAssistant.create(
        constructor,
        Stream.concat(
            Stream.of(
                LookupAssistant.find(
                    AnyConverter.frameOfAny(false), BaseFrameTransformer::setArgs, "args")),
            savers));
  }

  /**
   * Prepare a new definition over a variadic override of an existing attribute
   *
   * @param supplier constructor for new instances
   * @param <H> the type of the variable holder
   */
  @SafeVarargs
  public static <H extends BaseFrameTransformer<Any>> OverrideDefinition createOverride(
      Supplier<H> supplier, LookupAssistant<H>... savers) {
    return createOverride(supplier, Stream.of(savers));
  }
  /**
   * Prepare a new definition over a variadic override of an existing attribute
   *
   * @param anyConverter matcher for the values of the overridden attribute
   * @param supplier constructor for new instances
   * @param <T> the type of the values of the frame being overridden
   * @param <H> the type of the variable holder
   */
  @SafeVarargs
  public static <H extends BaseFrameTransformer<T>, T> OverrideDefinition createOverride(
      AnyConverter<T> anyConverter, Supplier<H> supplier, LookupAssistant<H>... savers) {
    return createOverride(anyConverter, supplier, Stream.of(savers));
  }
  /**
   * Prepare a new definition over a variadic override of an existing attribute
   *
   * @param supplier constructor for new instances
   * @param <H> the type of the variable holder
   */
  public static <H extends BaseFrameTransformer<Any>> OverrideDefinition createOverride(
      Supplier<H> supplier, Stream<LookupAssistant<H>> savers) {
    return LookupAssistant.create(
        AnyConverter.frameOfAny(false),
        args -> {
          final var result = supplier.get();
          setArgs(result, args);
          return result;
        },
        Stream.concat(
            Stream.of(
                LookupAssistant.find(
                    AnyConverter.frameOfAny(false), BaseFrameTransformer::setArgs, "args")),
            savers));
  }
  /**
   * Prepare a new definition over a variadic override of an existing attribute
   *
   * @param anyConverter matcher for the values of the overridden attribute
   * @param supplier constructor for new instances
   * @param <T> the type of the values of the frame being overridden
   * @param <H> the type of the variable holder
   */
  public static <H extends BaseFrameTransformer<T>, T> OverrideDefinition createOverride(
      AnyConverter<T> anyConverter, Supplier<H> supplier, Stream<LookupAssistant<H>> savers) {
    return LookupAssistant.create(
        AnyConverter.frameOf(anyConverter, false),
        args -> {
          final var result = supplier.get();
          setArgs(result, args);
          return result;
        },
        Stream.concat(
            Stream.of(
                LookupAssistant.find(
                    AnyConverter.frameOf(anyConverter, false),
                    BaseFrameTransformer::setArgs,
                    "args")),
            savers));
  }

  public final void run(Future<Any> future, SourceReference sourceReference, Context context) {
    future.complete(
        Any.of(
            Frame.create(
                future,
                sourceReference,
                context,
                args.entrySet()
                    .stream()
                    .map(
                        entry ->
                            Attribute.of(
                                entry.getKey(),
                                (f, s, c) ->
                                    () -> apply(f, s, c, entry.getKey(), entry.getValue())))
                    .collect(toSource()))));
  }

  static <T> void setArgs(BaseFrameTransformer<T> instance, Map<Name, T> args) {
    instance.args = args;
  }

  /** Create a new function with empty arguments */
  public BaseFrameTransformer() {}

  private Map<Name, T> args;

  /**
   * Process one input value from the <tt>args</tt> frame
   *
   * @param future the future in which the input should be processed
   * @param sourceReference the caller's execution trace
   * @param context the evaluation context
   * @param name the attribute name from the frame
   * @param input the attribute value from the <tt>args</tt> frame
   */
  protected abstract void apply(
      Future<Any> future, SourceReference sourceReference, Context context, Name name, T input);
}
