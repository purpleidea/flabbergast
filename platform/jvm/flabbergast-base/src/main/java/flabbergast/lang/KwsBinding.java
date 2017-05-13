package flabbergast.lang;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** A binding injecting a Java function as a KWS function */
public final class KwsBinding {
  private static final MethodHandle BICONSUMER_ACCEPT;
  private static final MethodHandle CONSUMER_ACCEPT;
  private static final MethodHandle FUNCTION_APPLY;
  private static final MethodHandle SUPPLIER_GET;

  static {
    try {
      final var lookup = MethodHandles.lookup();
      BICONSUMER_ACCEPT =
          lookup.findVirtual(
              BiConsumer.class,
              "accept",
              MethodType.methodType(void.class, Object.class, Object.class));
      CONSUMER_ACCEPT =
          lookup.findVirtual(
              Consumer.class, "accept", MethodType.methodType(void.class, Object.class));
      FUNCTION_APPLY =
          lookup.findVirtual(
              Function.class, "apply", MethodType.methodType(Object.class, Object.class));
      SUPPLIER_GET = lookup.findVirtual(Supplier.class, "get", MethodType.methodType(Object.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Create a KWS external binding using a method handle
   *
   * <p>Note that the call site must take {@link Future} and {@link SourceReference} as its first
   * two arguments, {@link Consumer} as its last argument, and return void.
   *
   * @param name the name the function should be bound to; if already in use, this will replace the
   *     previous definition
   * @param returnType the type that the consumer should expect to receive
   * @param handle the method handle of the function; any other parameter types are inferred from
   *     the method handle's type
   */
  public static KwsBinding of(String name, Class<?> returnType, MethodHandle handle) {
    return new KwsBinding(name, returnType, handle);
  }

  /**
   * Create a KWS external binding using a supplier
   *
   * @param name the name the function should be bound to; if already in use, this will replace the
   *     previous definition
   * @param returnType the type that the consumer should expect to receive
   * @param supplier a callback that will produce the value when invoked
   * @param <R> the return type
   */
  public static <R> KwsBinding of(
      String name, Class<R> returnType, Supplier<? extends R> supplier) {
    return new KwsBinding(
        name,
        returnType,
        MethodHandles.dropArguments(
            MethodHandles.foldArguments(CONSUMER_ACCEPT, 1, SUPPLIER_GET.bindTo(supplier)),
            0,
            Future.class,
            SourceReference.class));
  }

  /**
   * Bind an asynchronous parametereless function
   *
   * @param name the name the function should be bound to; if already in use, this will replace the
   *     previous definition
   * @param returnType the type that the consumer should expect to receive
   * @param supplier a callback that will send the value to the consumer when invoked; it may be
   *     asynchoronous
   * @param <R> the return type
   */
  public static <R> KwsBinding of(
      String name, Class<R> returnType, Consumer<? super Consumer<? super R>> supplier) {
    return new KwsBinding(
        name,
        returnType,
        MethodHandles.dropArguments(
            CONSUMER_ACCEPT
                .bindTo(supplier)
                .asType(MethodType.methodType(void.class, Consumer.class)),
            0,
            Future.class,
            SourceReference.class));
  }

  /**
   * Create a KWS external binding using a function
   *
   * @param name the name the function should be bound to; if already in use, this will replace the
   *     previous definition
   * @param returnType the type that the consumer should expect to receive
   * @param function a callback that will produce the value when invoked
   * @param <R> the return type
   * @param <T> the parameter type
   */
  public static <T, R> KwsBinding of(
      String name,
      Class<R> returnType,
      Class<T> parameterType,
      Function<? super T, ? extends R> function) {
    return new KwsBinding(
        name,
        returnType,
        MethodHandles.dropArguments(
            MethodHandles.foldArguments(CONSUMER_ACCEPT, 1, FUNCTION_APPLY.bindTo(function))
                .asType(MethodType.methodType(Object.class, parameterType)),
            0,
            Future.class,
            SourceReference.class));
  }

  /**
   * Create a KWS external binding using an asynchronous function
   *
   * @param name the name the function should be bound to; if already in use, this will replace the
   *     previous definition
   * @param returnType the type that the consumer should expect to receive
   * @param function a callback that will send the value to the consumer when invoked; it may be
   *     asynchronous
   * @param <R> the return type
   * @param <T> the parameter type
   */
  public static <T, R> KwsBinding of(
      String name,
      Class<R> returnType,
      Class<T> parameterType,
      BiConsumer<Consumer<? super R>, ? super T> function) {
    return new KwsBinding(
        name,
        returnType,
        MethodHandles.dropArguments(
            BICONSUMER_ACCEPT
                .bindTo(function)
                .asType(MethodType.methodType(void.class, Consumer.class, parameterType)),
            0,
            Future.class,
            SourceReference.class));
  }

  private final MethodHandle handle;
  private final String name;
  private final Class<?> returnType;

  private KwsBinding(String name, Class<?> returnType, MethodHandle handle) {
    this.name = name;
    this.returnType = returnType;
    this.handle = handle;
    if (handle.type().parameterCount() < 3
        || !handle.type().returnType().equals(void.class)
        || !handle.type().parameterType(0).equals(Future.class)
        || !handle.type().parameterType(1).equals(SourceReference.class)
        || !handle.type().parameterType(2).equals(Consumer.class)) {

      throw new IllegalArgumentException(
          String.format(
              "Method type %s must return void, take Future, SourceReference, and Consumer<%s> as the first parameters",
              handle.type(), returnType));
    }
  }

  MethodHandle handle() {
    return handle;
  }

  /** The name that will be available to import */
  public String name() {
    return name;
  }

  /** The return type of the binding */
  public Class<?> returnType() {
    return returnType;
  }
}
