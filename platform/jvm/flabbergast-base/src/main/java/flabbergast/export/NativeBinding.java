package flabbergast.export;

import static flabbergast.lang.AttributeSource.toSource;

import flabbergast.lang.*;
import flabbergast.util.*;
import java.io.InputStream;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;

/**
 * Inject native functions and data into Flabbergast as function-like templates and boxed values,
 * respectively
 *
 * <p>To make use of this class, createFromValues a class implementing {@link UriService} exported
 * via the module system that returns the output of {@link #create(String, NativeBinding...)}. Every
 * bound value needs a URI associated with it. Create a library in Flabbergast that pulls in these
 * values using <tt>From native:x.y</tt> where <tt>x</tt> is the library name and <tt>y</tt> is an
 * attribute path in that library (<i>e.g.</i>, <tt>native:apache/zookeeper.get</tt>).
 */
public abstract class NativeBinding {
  private abstract static class Holder1<T> implements LookupAssistant.Recipient {
    public T first;
  }

  private abstract static class Holder2<T, S> extends Holder1<T> {
    public S second;
  }

  private abstract static class Holder3<T, S, U> extends Holder2<T, S> {
    public U third;
  }

  private abstract static class Holder4<T, S, U, V> extends Holder3<T, S, U> {
    public V fourth;
  }

  private static class NumericBiMapFunction extends BaseFrameTransformer<Any> {

    NumericCapture arg;

    @Override
    protected void apply(
        Future<Any> future,
        SourceReference sourceReference,
        Context context,
        Name name,
        Any input) {
      arg.run(future, sourceReference, input);
    }
  }

  private static final class NumericCapture {
    private final Any arg;
    private final WhinyFunction2<Double, Double, Any> doubleFunc;
    private final WhinyFunction2<Long, Long, Any> longFunc;

    private NumericCapture(
        WhinyFunction2<Long, Long, Any> longFunc,
        WhinyFunction2<Double, Double, Any> doubleFunc,
        Any arg) {
      this.longFunc = longFunc;
      this.doubleFunc = doubleFunc;
      this.arg = arg;
    }

    public void run(Future<Any> future, SourceReference sourceReference, Any input) {
      new LongBiConsumer() {
        @Override
        protected void accept(double left, double right) {
          try {
            future.complete(doubleFunc.apply(left, right));
          } catch (Exception e) {
            future.error(sourceReference, e.getMessage());
          }
        }

        @Override
        protected void accept(long left, long right) {
          try {
            future.complete(longFunc.apply(left, right));
          } catch (Exception e) {
            future.error(sourceReference, e.getMessage());
          }
        }
      }.dispatch(future, sourceReference, input, arg);
    }
  }

  private static class NumericMapFunction extends BaseFrameTransformer<WhinySupplier<Any>> {

    @Override
    protected void apply(
        Future<Any> future,
        SourceReference sourceReference,
        Context context,
        Name name,
        WhinySupplier<Any> input) {
      try {
        future.complete(input.get());
      } catch (Exception e) {
        future.error(sourceReference, e.getMessage());
      }
    }
  }

  /**
   * Convert a Java type into a Flabbergast-usable format
   *
   * <p>Null values are automatically converted to Flabbergast's <tt>Null</tt>
   *
   * @param <T> the Java type being converted
   */
  public abstract static class ResultType<T> {

    ResultType() {}

    abstract void pack(
        Future<Any> future, SourceReference sourceReference, Context context, T value);
  }
  /** Convert any boxed value */
  public static final ResultType<Any> ANY =
      new ResultType<>() {
        @Override
        void pack(Future<Any> future, SourceReference sourceReference, Context context, Any value) {
          future.complete(value == null ? Any.NULL : value);
        }
      };

  private static final TypeErrorLocation BASE_ERROR = TypeErrorLocation.lookup("base");
  private static final Definition BASE_LOOKUP = LookupHandler.CONTEXTUAL.create("base");
  /** Convert a byte array */
  public static final ResultType<byte[]> BIN =
      new ResultType<>() {
        @Override
        void pack(
            Future<Any> future, SourceReference sourceReference, Context context, byte[] value) {
          future.complete(Any.of(value));
        }
      };
  /** Convert a Boolean value */
  public static final ResultType<Boolean> BOOL =
      new ResultType<>() {
        @Override
        void pack(
            Future<Any> future, SourceReference sourceReference, Context context, Boolean value) {
          future.complete(Any.of(value));
        }
      };
  /** Convert an integer to a Unicode codepoint */
  public static final ResultType<Integer> CODEPOINT =
      new ResultType<>() {
        @Override
        void pack(
            Future<Any> future, SourceReference sourceReference, Context context, Integer value) {
          future.complete(value == null ? Any.NULL : Any.of(Str.fromCodepoint(value)));
        }
      };
  /** Convert a floating point value */
  public static final ResultType<Double> FLOAT =
      new ResultType<>() {
        @Override
        void pack(
            Future<Any> future, SourceReference sourceReference, Context context, Double value) {
          future.complete(Any.of(value));
        }
      };
  /** Convert a pre-constructed frame */
  public static final ResultType<Frame> FRAME =
      new ResultType<>() {
        @Override
        void pack(
            Future<Any> future, SourceReference sourceReference, Context context, Frame value) {
          future.complete(Any.of(value));
        }
      };
  /** Create a new frame from the attributes provided */
  public static final ResultType<AttributeSource> FRAME_BY_ATTRIBUTES =
      new ResultType<>() {
        @Override
        void pack(
            Future<Any> future,
            SourceReference sourceReference,
            Context context,
            AttributeSource value) {
          future.complete(
              value == null
                  ? Any.NULL
                  : Any.of(Frame.create(future, sourceReference, context, value)));
        }
      };

  /** Convert an integer */
  public static final ResultType<Long> INT =
      new ResultType<>() {
        @Override
        void pack(
            Future<Any> future, SourceReference sourceReference, Context context, Long value) {
          future.complete(Any.of(value));
        }
      };
  /** Convert a lookup handler */
  public static final ResultType<LookupHandler> LOOKUP_HANDLER =
      new ResultType<>() {
        @Override
        void pack(
            Future<Any> future,
            SourceReference sourceReference,
            Context context,
            LookupHandler value) {
          future.complete(Any.of(value));
        }
      };
  /** Convert a Flabbergast string */
  public static final ResultType<Str> STR =
      new ResultType<>() {
        @Override
        void pack(Future<Any> future, SourceReference sourceReference, Context context, Str value) {
          future.complete(Any.of(value));
        }
      };
  /** Convert a Java string */
  public static final ResultType<String> STRING =
      new ResultType<>() {
        @Override
        void pack(
            Future<Any> future, SourceReference sourceReference, Context context, String value) {
          future.complete(Any.of(value));
        }
      };
  /** Convert a pre-constructed template */
  public static final ResultType<Template> TEMPLATE =
      new ResultType<>() {
        @Override
        void pack(
            Future<Any> future, SourceReference sourceReference, Context context, Template value) {
          future.complete(Any.of(value));
        }
      };
  /** Convert a Java date time to a frame with the usual time-related attributes */
  public static final ResultType<ZonedDateTime> TIME =
      new ResultType<>() {
        @Override
        void pack(
            Future<Any> future,
            SourceReference sourceReference,
            Context context,
            ZonedDateTime value) {
          future.complete(
              value == null ? Any.NULL : Any.of(Frame.of(future, sourceReference, context, value)));
        }
      };
  /** Convert any XML document */
  public static final ResultType<Document> XML =
      new ResultType<>() {
        @Override
        void pack(
            Future<Any> future, SourceReference sourceReference, Context context, Document value) {
          if (value == null) {
            future.complete(Any.NULL);
          } else {
            future.launch(xml(value), sourceReference, context, future::complete);
          }
        }
      };

  /** Create a template that will return the value given by the supplier when invoked */
  public static <T> ResultType<Supplier<T>> asGeneratorTemplate(ResultType<T> resultType) {
    return new ResultType<>() {
      @Override
      void pack(
          Future<Any> future, SourceReference sourceReference, Context context, Supplier<T> value) {
        future.complete(
            value == null
                ? Any.NULL
                : Any.of(generatorTemplate(resultType, sourceReference, context, value)));
      }
    };
  }

  /**
   * Package a Java value into a frame using the frame proxying mechanism
   *
   * @param extractors the attributes to define in the frame
   * @param <T> the type being package
   * @see Frame#proxyOf(SourceReference, Context, Object, Stream)
   * @see ProxyAttribute
   */
  @SafeVarargs
  public static <T> ResultType<T> asProxy(ProxyAttribute<T>... extractors) {
    return new ResultType<>() {
      @Override
      void pack(Future<Any> future, SourceReference sourceReference, Context context, T value) {
        future.complete(
            value == null
                ? Any.NULL
                : Any.of(Frame.proxyOf(sourceReference, context, value, Stream.of(extractors))));
      }
    };
  }

  /**
   * Create a new template with the attributes provided
   *
   * @param source a reference to appear in stack traces
   */
  public static ResultType<AttributeSource> asTemplate(String source) {
    return new ResultType<>() {
      @Override
      void pack(
          Future<Any> future,
          SourceReference sourceReference,
          Context context,
          AttributeSource value) {
        future.complete(
            value == null
                ? Any.NULL
                : Any.of(new Template(sourceReference.special(source), context, value)));
      }
    };
  }

  /**
   * Create a template amending the provided template with the attributes provided
   *
   * @param source a reference to appear in stack traces
   */
  public static ResultType<Pair<Template, AttributeSource>> asTemplateOverride(String source) {
    return new ResultType<>() {
      @Override
      void pack(
          Future<Any> future,
          SourceReference sourceReference,
          Context context,
          Pair<Template, AttributeSource> value) {
        future.complete(
            value == null
                ? Any.NULL
                : Any.of(
                    new Template(
                        sourceReference.specialJunction(source, value.first().source()),
                        context,
                        Stream.empty(),
                        value.first(),
                        value.second())));
      }
    };
  }

  /**
   * Create a new interop binding for a library
   *
   * <p>All names will be bound as <tt>libraryname.bindingname</tt>. It is intended that every
   * Flabbergast library with native bindings had a matched interop library.
   *
   * @param libraryName the name of the library to appear in URLs and stack traces
   * @param bindings the bindings for this library
   */
  public static UriHandler create(String libraryName, NativeBinding... bindings) {
    return create(libraryName, Stream.of(bindings));
  }

  /**
   * Create a new interop binding for a library
   *
   * <p>All names will be bound as <tt>libraryname.bindingname</tt>. It is intended that every
   * Flabbergast library with native bindings had a matched interop library.
   *
   * @param libraryName the name of the library to appear in URLs and stack traces
   * @param bindings the bindings for this library
   */
  public static UriHandler create(String libraryName, Stream<NativeBinding> bindings) {
    return create(libraryName, 0, bindings);
  }

  /**
   * Create a new interop binding for a library
   *
   * <p>All names will be bound as <tt>libraryname.bindingname</tt>. It is intended that every
   * Flabbergast library with native bindings had a matched interop library.
   *
   * @param libraryName the name of the library to appear in URLs and stack traces
   * @param priority the priority of this handler relative to others
   * @param bindings the bindings for this library
   */
  public static UriHandler create(
      String libraryName, int priority, Stream<NativeBinding> bindings) {
    final var sourceReference = SourceReference.root("native:" + libraryName);
    return new UriHandler() {

      private final Map<String, Any> binding =
          bindings
              .map(
                  interop -> {
                    final var name = libraryName + "." + interop.name;
                    return Pair.of(name, interop.bind(name, sourceReference));
                  })
              .collect(Collectors.toMap(Pair::first, Pair::second));

      private final String source = "<" + libraryName + ">";

      @Override
      public String name() {
        return "library bindings for " + source;
      }

      @Override
      public int priority() {
        return priority;
      }

      @Override
      public final Result<Promise<Any>> resolveUri(UriExecutor executor, URI uri) {
        return Result.of(uri)
            .filter(x -> x.getScheme().equals("interop"))
            .map(URI::getSchemeSpecificPart)
            .map(binding::get);
      }
    };
  }

  /**
   * Convert a function with one argument to a definition
   *
   * <p>This definition will return the value of the function after looking up the parameter and
   * will handle any errors.
   *
   * @param resultType a converter to box the result
   * @param func The function to be called.
   * @param anyConverter The {@link AnyConverter} to unbox the Flabbergast value
   * @param parameter The Flabbergast name to lookup as the parameter
   */
  public static <T, R> Definition function(
      ResultType<R> resultType,
      WhinyFunction<? super T, ? extends R> func,
      AnyConverter<T> anyConverter,
      String parameter) {
    return LookupAssistant.create(
        () ->
            new Holder1<T>() {

              @Override
              public void run(
                  Future<Any> future, SourceReference sourceReference, Context context) {
                try {
                  resultType.pack(future, sourceReference, context, func.apply(first));
                } catch (final Exception e) {
                  future.error(sourceReference, e.getMessage());
                }
              }
            },
        LookupAssistant.find(anyConverter, (h, v) -> h.first = v, parameter));
  }

  /**
   * Convert a function with two argument to a definition
   *
   * <p>This definition will return the value of the function after looking up the parameter and
   * will handle any errors.
   *
   * @param resultType a converter to box the result
   * @param func The function to be called.
   * @param anyConverter1 The {@link AnyConverter} to unbox the Flabbergast value for the first
   *     parameter
   * @param parameter1 The Flabbergast name to lookup as the first parameter
   * @param anyConverter2 The {@link AnyConverter} to unbox the Flabbergast value as the second
   *     parameter
   * @param parameter2 The Flabbergast name to lookup as the second parameter
   */
  public static <T1, T2, R> Definition function(
      ResultType<R> resultType,
      WhinyFunction2<? super T1, ? super T2, ? extends R> func,
      AnyConverter<T1> anyConverter1,
      String parameter1,
      AnyConverter<T2> anyConverter2,
      String parameter2) {
    return LookupAssistant.create(
        () ->
            new Holder2<T1, T2>() {
              @Override
              public void run(
                  Future<Any> future, SourceReference sourceReference, Context context) {
                try {
                  resultType.pack(future, sourceReference, context, func.apply(first, second));
                } catch (final Exception e) {
                  future.error(sourceReference, e.getMessage());
                }
              }
            },
        LookupAssistant.find(anyConverter1, (h, x) -> h.first = x, parameter1),
        LookupAssistant.find(anyConverter2, (h, x) -> h.second = x, parameter2));
  }

  /**
   * Convert a function with three arguments to a definition
   *
   * <p>This definition will return the value of the function after looking up the parameter and
   * will handle any errors.
   *
   * @param resultType a converter to box the result
   * @param func The function to be called.
   * @param anyConverter1 The {@link AnyConverter} to unbox the Flabbergast value for the first
   *     parameter
   * @param parameter1 The Flabbergast name to lookup as the first parameter
   * @param anyConverter2 The {@link AnyConverter} to unbox the Flabbergast value as the second
   *     parameter
   * @param parameter2 The Flabbergast name to lookup as the second parameter
   * @param anyConverter3 The {@link AnyConverter} to unbox the Flabbergast value as the third
   *     parameter
   * @param parameter3 The Flabbergast name to lookup as the third parameter
   */
  public static <T1, T2, T3, R> Definition function(
      ResultType<R> resultType,
      WhinyFunction3<? super T1, ? super T2, ? super T3, ? extends R> func,
      AnyConverter<T1> anyConverter1,
      String parameter1,
      AnyConverter<T2> anyConverter2,
      String parameter2,
      AnyConverter<T3> anyConverter3,
      String parameter3) {
    return LookupAssistant.create(
        () ->
            new Holder3<T1, T2, T3>() {

              @Override
              public void run(
                  Future<Any> future, SourceReference sourceReference, Context context) {
                try {
                  resultType.pack(
                      future, sourceReference, context, func.apply(first, second, third));
                } catch (final Exception e) {
                  future.error(sourceReference, e.getMessage());
                }
              }
            },
        LookupAssistant.find(anyConverter1, (h, x) -> h.first = x, parameter1),
        LookupAssistant.find(anyConverter2, (h, x) -> h.second = x, parameter2),
        LookupAssistant.find(anyConverter3, (h, x) -> h.third = x, parameter3));
  }

  /**
   * Convert a function with four arguments to a definition
   *
   * <p>This definition will return the value of the function after looking up the parameter and
   * will handle any errors.
   *
   * @param resultType a converter to box the result
   * @param func The function to be called.
   * @param anyConverter1 The {@link AnyConverter} to unbox the Flabbergast value for the first
   *     parameter
   * @param parameter1 The Flabbergast name to lookup as the first parameter
   * @param anyConverter2 The {@link AnyConverter} to unbox the Flabbergast value as the second
   *     parameter
   * @param parameter2 The Flabbergast name to lookup as the second parameter
   * @param anyConverter3 The {@link AnyConverter} to unbox the Flabbergast value as the third
   *     parameter
   * @param parameter3 The Flabbergast name to lookup as the third parameter
   * @param anyConverter4 The {@link AnyConverter} to unbox the Flabbergast value as the fourth
   *     parameter
   * @param parameter4 The Flabbergast name to lookup as the fourth parameter
   */
  public static <T1, T2, T3, T4, R> Definition function(
      ResultType<R> resultType,
      WhinyFunction4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> func,
      AnyConverter<T1> anyConverter1,
      String parameter1,
      AnyConverter<T2> anyConverter2,
      String parameter2,
      AnyConverter<T3> anyConverter3,
      String parameter3,
      AnyConverter<T4> anyConverter4,
      String parameter4) {
    return LookupAssistant.create(
        () ->
            new Holder4<T1, T2, T3, T4>() {

              @Override
              public void run(
                  Future<Any> future, SourceReference sourceReference, Context context) {
                try {
                  resultType.pack(
                      future, sourceReference, context, func.apply(first, second, third, fourth));
                } catch (final Exception e) {
                  future.error(sourceReference, e.getMessage());
                }
              }
            },
        LookupAssistant.find(anyConverter1, (h, x) -> h.first = x, parameter1),
        LookupAssistant.find(anyConverter2, (h, x) -> h.second = x, parameter2),
        LookupAssistant.find(anyConverter3, (h, x) -> h.third = x, parameter3),
        LookupAssistant.find(anyConverter4, (h, x) -> h.fourth = x, parameter4));
  }

  /**
   * Create a definition that will return an arbitrary value when instantiated
   *
   * @param resultType a converter to box the result
   * @param next a callback to produce the next value when called
   */
  public static <T> Definition generator(ResultType<T> resultType, Supplier<T> next) {
    return (f, s, c) -> () -> resultType.pack(f, s, c, next.get());
  }

  /**
   * Create a function-like template that will return the value provided by the generator when the
   * template is instantiated.
   *
   * @param resultType a converter to box the result
   * @param sourceReference the caller's execution trace
   * @param context the calling context
   * @param next a callback to produce the next value when a template is instantiated
   */
  public static <T> Template generatorTemplate(
      ResultType<T> resultType,
      SourceReference sourceReference,
      Context context,
      Supplier<T> next) {
    return new Template(
        sourceReference,
        context,
        AttributeSource.of(Attribute.of("value", generator(resultType, next))));
  }

  /**
   * Create an -ifier function-like template
   *
   * <p>This is a function-like template that overrides attributes of an existing template, supplied
   * as the value <tt>base</tt> in the instantiation environment.
   *
   * @param builder the overrides to apply to the base template
   * @param filename a file name to include in the execution trace
   * @return a definition that will lookup <tt>base</tt> and apply the overrides to the result
   */
  public static Definition ifier(AttributeSource builder, String filename) {
    final var converter = AnyConverter.asTemplate(false);

    return (future, sourceReference, context) ->
        () ->
            future.launch(
                BASE_LOOKUP,
                sourceReference,
                context,
                converter.asConsumer(
                    future,
                    sourceReference,
                    BASE_ERROR,
                    template ->
                        future.complete(
                            Any.of(
                                new Template(
                                    sourceReference.specialJunction(filename, template.source()),
                                    context,
                                    Stream.empty(),
                                    template,
                                    builder)))));
  }

  private static <T, R> void map(
      Future<Any> future,
      SourceReference sourceReference,
      Context context,
      ResultType<R> resultType,
      WhinyFunction<? super T, ? extends R> func,
      Map<Name, T> input) {
    future.complete(
        Any.of(
            Frame.create(
                future,
                sourceReference,
                context,
                input
                    .entrySet()
                    .stream()
                    .map(
                        entry ->
                            Attribute.of(
                                entry.getKey(),
                                (f, s, c) ->
                                    () -> {
                                      try {
                                        resultType.pack(f, s, c, func.apply(entry.getValue()));
                                      } catch (final Exception e) {
                                        f.error(sourceReference, e.getMessage());
                                      }
                                    }))
                    .collect(toSource()))));
  }

  /**
   * Convert a function into a definition which does a “map” operation over variadic <tt>args</tt>
   *
   * @param resultType a converter to box the result
   * @param anyConverter The {@link AnyConverter} to unbox the Flabbergast value for each of the
   * @param func The function to be called.
   */
  public static <T, R> Definition mapFunction(
      ResultType<R> resultType,
      AnyConverter<T> anyConverter,
      WhinyFunction<? super T, ? extends R> func) {
    return LookupAssistant.create(
        () ->
            new Holder1<Map<Name, T>>() {
              @Override
              public void run(
                  Future<Any> future, SourceReference sourceReference, Context context) {
                map(future, sourceReference, context, resultType, func, first);
              }
            },
        LookupAssistant.find(
            AnyConverter.frameOf(anyConverter, false), (h, v) -> h.first = v, "args"));
  }

  /**
   * Convert a function into a definition which does a “map” operation over variadic <tt>args</tt>
   * with an additional argument that does not vary
   *
   * @param resultType a converter to box the result
   * @param anyConverter The {@link AnyConverter} to unbox the Flabbergast value for each of the
   *     values in <tt>args</tt>
   * @param func The function to be called.
   * @param parameterAnyConverter The {@link AnyConverter} to unbox the Flabbergast value for the
   *     fixed parameter
   * @param parameter The Flabbergast name to lookup as the fixed parameter
   */
  public static <T1, T2, R> Definition mapFunction(
      ResultType<R> resultType,
      AnyConverter<T1> anyConverter,
      WhinyFunction2<? super T1, ? super T2, ? extends R> func,
      AnyConverter<T2> parameterAnyConverter,
      String parameter) {
    return LookupAssistant.create(
        () ->
            new Holder2<Map<Name, T1>, T2>() {
              @Override
              public void run(
                  Future<Any> future, SourceReference sourceReference, Context context) {
                map(future, sourceReference, context, resultType, func.tailBind(second), first);
              }
            },
        LookupAssistant.find(
            AnyConverter.frameOf(anyConverter, false), (h1, v1) -> h1.first = v1, "args"),
        LookupAssistant.find(parameterAnyConverter, (h, v) -> h.second = v, parameter));
  }

  /**
   * Convert a function into a definition which does a “map” operation over variadic <tt>args</tt>
   * with two additional arguments that do not vary
   *
   * @param resultType a converter to box the result
   * @param anyConverter The {@link AnyConverter} to unbox the Flabbergast value for each of the
   *     values in <tt>args</tt>
   * @param func The function to be called.
   * @param parameter1AnyConverter The {@link AnyConverter} to unbox the Flabbergast value for the
   *     first fixed parameter
   * @param parameter1 The Flabbergast name to lookup as the first fixed parameter
   * @param parameter2AnyConverter The {@link AnyConverter} to unbox the Flabbergast value for the
   *     second fixed parameter
   * @param parameter2 The Flabbergast name to lookup as the second fixed parameter
   */
  public static <T1, T2, T3, R> Definition mapFunction(
      ResultType<R> resultType,
      AnyConverter<T1> anyConverter,
      WhinyFunction3<? super T1, ? super T2, ? super T3, ? extends R> func,
      AnyConverter<T2> parameter1AnyConverter,
      String parameter1,
      AnyConverter<T3> parameter2AnyConverter,
      String parameter2) {
    return LookupAssistant.create(
        () ->
            new Holder3<Map<Name, T1>, T2, T3>() {
              @Override
              public void run(
                  Future<Any> future, SourceReference sourceReference, Context context) {
                map(
                    future,
                    sourceReference,
                    context,
                    resultType,
                    func.tailBind(second, third),
                    first);
              }
            },
        LookupAssistant.find(
            AnyConverter.frameOf(anyConverter, false), (h1, v1) -> h1.first = v1, "args"),
        LookupAssistant.find(parameter1AnyConverter, (h, v) -> h.second = v, parameter1),
        LookupAssistant.find(parameter2AnyConverter, (h, v) -> h.third = v, parameter2));
  }

  /**
   * Convert a function into a definition which does a “map” operation over variadic <tt>args</tt>
   * with three additional arguments that do not vary
   *
   * @param resultType a converter to box the result
   * @param anyConverter The {@link AnyConverter} to unbox the Flabbergast value for each of the
   *     values in <tt>args</tt>
   * @param func The function to be called.
   * @param parameter1AnyConverter The {@link AnyConverter} to unbox the Flabbergast value for the
   *     first fixed parameter
   * @param parameter1 The Flabbergast name to lookup as the first fixed parameter
   * @param parameter2AnyConverter The {@link AnyConverter} to unbox the Flabbergast value for the
   *     second fixed parameter
   * @param parameter2 The Flabbergast name to lookup as the second fixed parameter
   * @param parameter3AnyConverter The {@link AnyConverter} to unbox the Flabbergast value for the
   *     third fixed parameter
   * @param parameter3 The Flabbergast name to lookup as the third fixed parameter
   */
  public static <T1, T2, T3, T4, R> Definition mapFunction(
      ResultType<R> resultType,
      AnyConverter<T1> anyConverter,
      WhinyFunction4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> func,
      AnyConverter<T2> parameter1AnyConverter,
      String parameter1,
      AnyConverter<T3> parameter2AnyConverter,
      String parameter2,
      AnyConverter<T4> parameter3AnyConverter,
      String parameter3) {
    return LookupAssistant.create(
        () ->
            new Holder4<Map<Name, T1>, T2, T3, T4>() {
              @Override
              public void run(
                  Future<Any> future, SourceReference sourceReference, Context context) {
                map(
                    future,
                    sourceReference,
                    context,
                    resultType,
                    func.tailBind(second, third, fourth),
                    first);
              }
            },
        LookupAssistant.find(
            AnyConverter.frameOf(anyConverter, false), (h1, v1) -> h1.first = v1, "args"),
        LookupAssistant.find(parameter1AnyConverter, (h, v) -> h.second = v, parameter1),
        LookupAssistant.find(parameter2AnyConverter, (h, v) -> h.third = v, parameter2),
        LookupAssistant.find(parameter3AnyConverter, (h, v) -> h.fourth = v, parameter3));
  }

  /**
   * Convert a function into a definition which does a “map” operation over the original value
   *
   * @param resultType a converter to box the result
   * @param anyConverter The {@link AnyConverter} to unbox the Flabbergast value for each of the
   * @param func The function to be called.
   */
  public static <T, R> OverrideDefinition mapOverride(
      ResultType<R> resultType,
      AnyConverter<T> anyConverter,
      WhinyFunction<? super T, ? extends R> func) {
    final var converter = AnyConverter.frameOf(anyConverter, false);
    return (future, sourceReference, context, original) ->
        () ->
            original.accept(
                converter.asConsumer(
                    future,
                    sourceReference,
                    TypeErrorLocation.ORIGINAL,
                    input -> map(future, sourceReference, context, resultType, func, input)));
  }

  /**
   * Convert a function into a definition which does a “map” operation over the original value of a
   * map operation with an additional argument that does not vary
   *
   * @param resultType a converter to box the result
   * @param anyConverter The {@link AnyConverter} to unbox the Flabbergast value for each of the
   *     values in <tt>args</tt>
   * @param func The function to be called.
   * @param parameterAnyConverter The {@link AnyConverter} to unbox the Flabbergast value for the
   *     fixed parameter
   * @param parameter The Flabbergast name to lookup as the fixed parameter
   */
  public static <T1, T2, R> OverrideDefinition mapOverride(
      ResultType<R> resultType,
      AnyConverter<T1> anyConverter,
      WhinyFunction2<? super T1, ? super T2, ? extends R> func,
      AnyConverter<T2> parameterAnyConverter,
      String parameter) {
    return LookupAssistant.create(
        AnyConverter.frameOf(anyConverter, false),
        args ->
            new Holder1<T2>() {

              @Override
              public void run(
                  Future<Any> future, SourceReference sourceReference, Context context) {
                map(future, sourceReference, context, resultType, func.tailBind(first), args);
              }
            },
        LookupAssistant.find(parameterAnyConverter, (h, v) -> h.first = v, parameter));
  }

  /**
   * Convert a function into a definition which does a “map” operation over the original value with
   * two additional arguments that do not vary
   *
   * @param resultType a converter to box the result
   * @param anyConverter The {@link AnyConverter} to unbox the Flabbergast value for each of the
   *     values in <tt>args</tt>
   * @param func The function to be called.
   * @param parameter1AnyConverter The {@link AnyConverter} to unbox the Flabbergast value for the
   *     first fixed parameter
   * @param parameter1 The Flabbergast name to lookup as the first fixed parameter
   * @param parameter2AnyConverter The {@link AnyConverter} to unbox the Flabbergast value for the
   *     second fixed parameter
   * @param parameter2 The Flabbergast name to lookup as the second fixed parameter parameter
   */
  public static <T1, T2, T3, R> OverrideDefinition mapOverride(
      ResultType<R> resultType,
      AnyConverter<T1> anyConverter,
      WhinyFunction3<? super T1, ? super T2, ? super T3, ? extends R> func,
      AnyConverter<T2> parameter1AnyConverter,
      String parameter1,
      AnyConverter<T3> parameter2AnyConverter,
      String parameter2) {
    return LookupAssistant.create(
        AnyConverter.frameOf(anyConverter, false),
        args ->
            new Holder2<T2, T3>() {
              @Override
              public void run(
                  Future<Any> future, SourceReference sourceReference, Context context) {
                map(
                    future,
                    sourceReference,
                    context,
                    resultType,
                    func.tailBind(first, second),
                    args);
              }
            },
        LookupAssistant.find(parameter1AnyConverter, (h, v) -> h.first = v, parameter1),
        LookupAssistant.find(parameter2AnyConverter, (h, v) -> h.second = v, parameter2));
  }

  /**
   * Convert a function into a definition which does a “map” operation over the original value with
   * third additional arguments that do not vary
   *
   * @param resultType a converter to box the result
   * @param anyConverter The {@link AnyConverter} to unbox the Flabbergast value for each of the
   *     values in <tt>args</tt>
   * @param func The function to be called.
   * @param parameter1AnyConverter The {@link AnyConverter} to unbox the Flabbergast value for the
   *     first fixed parameter
   * @param parameter1 The Flabbergast name to lookup as the first fixed parameter
   * @param parameter2AnyConverter The {@link AnyConverter} to unbox the Flabbergast value for the
   *     second fixed parameter
   * @param parameter2 The Flabbergast name to lookup as the second fixed parameter parameter *
   * @param parameter3AnyConverter The {@link AnyConverter} to unbox the Flabbergast value for the
   *     third fixed parameter
   * @param parameter3 The Flabbergast name to lookup as the third fixed parameter parameter
   */
  public static <T1, T2, T3, T4, R> OverrideDefinition mapOverride(
      ResultType<R> resultType,
      AnyConverter<T1> anyConverter,
      WhinyFunction4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> func,
      AnyConverter<T2> parameter1AnyConverter,
      String parameter1,
      AnyConverter<T3> parameter2AnyConverter,
      String parameter2,
      AnyConverter<T4> parameter3AnyConverter,
      String parameter3) {
    return LookupAssistant.create(
        AnyConverter.frameOf(anyConverter, false),
        args ->
            new Holder3<T2, T3, T4>() {
              @Override
              public void run(
                  Future<Any> future, SourceReference sourceReference, Context context) {
                map(
                    future,
                    sourceReference,
                    context,
                    resultType,
                    func.tailBind(first, second, third),
                    args);
              }
            },
        LookupAssistant.find(parameter1AnyConverter, (h, v) -> h.first = v, parameter1),
        LookupAssistant.find(parameter2AnyConverter, (h, v) -> h.second = v, parameter2),
        LookupAssistant.find(parameter3AnyConverter, (h, v) -> h.third = v, parameter3));
  }

  /**
   * Create a function-like template that causes a not-implemented error when instantiated.
   *
   * <p>This is used for adding bindings that are not available on all platforms.
   *
   * @param name the name of the attribute in this library
   */
  public static NativeBinding missing(String name) {
    return new NativeBinding(name) {
      @Override
      Any bind(String url, SourceReference sourceReference) {
        return Any.of(
            new Template(
                sourceReference,
                Context.EMPTY,
                AttributeSource.of(
                    Attribute.of(
                        "value",
                        Definition.error(
                            String.format(
                                "This platform has no implementation of “native:%s”.", url))))));
      }
    };
  }

  /**
   * Convert a single-argument function that has different behaviour for integers and floating point
   * numbers to a definition
   *
   * @param longFunc the integer-handling function
   * @param doubleFunc the floating point-handling function
   * @param parameterName the name of the parameter
   */
  public static Definition numberFunction(
      WhinyFunction<Long, Any> longFunc,
      WhinyFunction<Double, Any> doubleFunc,
      String parameterName) {
    return LookupAssistant.create(
        () ->
            new Holder1<WhinySupplier<Any>>() {
              @Override
              public void run(
                  Future<Any> future, SourceReference sourceReference, Context context) {

                try {
                  future.complete(first.get());
                } catch (Exception e) {
                  future.error(sourceReference, e.getMessage());
                }
              }
            },
        LookupAssistant.find(
            AnyConverter.numeric(longFunc::with, doubleFunc::with),
            (h, v) -> h.first = v,
            parameterName));
  }

  /**
   * Convert a two-argument function that has different behaviour for integers and floating point
   * numbers to a definition
   *
   * <p>If the user provides a combination of floating-point and integer numbers, the integers will
   * be coerced to floating-point numbers.
   *
   * @param longFunc the integer-handling function
   * @param doubleFunc the floating point-handling function
   * @param parameter1Name the name of the first parameter
   * @param parameter2Name the name of the second parameter
   */
  public static Definition numberFunction(
      WhinyFunction2<Long, Long, Any> longFunc,
      WhinyFunction2<Double, Double, Any> doubleFunc,
      String parameter1Name,
      String parameter2Name) {
    return LookupAssistant.create(
        () ->
            new Holder2<Any, Any>() {
              @Override
              public void run(
                  Future<Any> future, SourceReference sourceReference, Context context) {
                new LongBiConsumer() {

                  @Override
                  protected void accept(double left, double right) {
                    try {
                      future.complete(doubleFunc.apply(left, right));
                    } catch (Exception e) {
                      future.error(sourceReference, e.getMessage());
                    }
                  }

                  @Override
                  protected void accept(long left, long right) {
                    try {
                      future.complete(longFunc.apply(left, right));
                    } catch (Exception e) {
                      future.error(sourceReference, e.getMessage());
                    }
                  }
                }.dispatch(future, sourceReference, first, second);
              }
            },
        LookupAssistant.find((h, v) -> h.first = v, parameter1Name),
        LookupAssistant.find((h, v) -> h.second = v, parameter2Name));
  }

  /**
   * Convert a function,that has different behaviour for integers and floating point numbers, into a
   * definition which does a “map” operation over variadic <tt>args</tt>
   *
   * @param longFunc the integer-handling function
   * @param doubleFunc the floating point-handling function
   */
  public static Definition numberMapFunction(
      WhinyFunction<Long, Any> longFunc, WhinyFunction<Double, Any> doubleFunc) {
    return BaseFrameTransformer.create(
        AnyConverter.numeric(longFunc::with, doubleFunc::with), NumericMapFunction::new);
  }

  /**
   * Convert a function,that has different behaviour for integers and floating point numbers, into a
   * definition which does a “map” operation over variadic <tt>args</tt> with one additional
   * argument
   *
   * <p>If the user provides a combination of floating-point and integer numbers, the integers will
   * be coerced to floating-point numbers.
   *
   * @param longFunc the integer-handling function
   * @param doubleFunc the floating point-handling function
   * @param parameterName the name of the parameter
   */
  public static Definition numberMapFunction(
      WhinyFunction2<Long, Long, Any> longFunc,
      WhinyFunction2<Double, Double, Any> doubleFunc,
      String parameterName) {
    return BaseFrameTransformer.create(
        NumericBiMapFunction::new,
        LookupAssistant.find(
            (h, v) -> h.arg = new NumericCapture(longFunc, doubleFunc, v), parameterName));
  }

  /**
   * Convert a function,that has different behaviour for integers and floating point numbers, into a
   * definition which does a “map” operation over the original value
   *
   * @param longFunc the integer-handling function
   * @param doubleFunc the floating point-handling function
   */
  public static OverrideDefinition numberMapOverride(
      WhinyFunction<Long, Any> longFunc, WhinyFunction<Double, Any> doubleFunc) {
    return BaseFrameTransformer.createOverride(
        AnyConverter.numeric(longFunc::with, doubleFunc::with), NumericMapFunction::new);
  }

  /**
   * Convert a function,that has different behaviour for integers and floating point numbers, into a
   * definition which does a “map” operation over the original value
   *
   * <p>If the user provides a combination of floating-point and integer numbers, the integers will
   * be coerced to floating-point numbers.
   *
   * @param longFunc the integer-handling function
   * @param doubleFunc the floating point-handling function
   * @param parameterName the name of the parameter
   */
  public static OverrideDefinition numberMapOverride(
      WhinyFunction2<Long, Long, Any> longFunc,
      WhinyFunction2<Double, Double, Any> doubleFunc,
      String parameterName) {
    return BaseFrameTransformer.createOverride(
        NumericBiMapFunction::new,
        LookupAssistant.find(
            (h, v) -> h.arg = new NumericCapture(longFunc, doubleFunc, v), parameterName));
  }

  /**
   * Bind a fixed value
   *
   * @param name the name of the attribute in this library
   * @param value the value to bind
   */
  public static NativeBinding of(String name, Any value) {
    return new NativeBinding(name) {

      @Override
      Any bind(String url, SourceReference sourceReference) {
        return value;
      }
    };
  }

  /**
   * Bind a template with the attributes provided
   *
   * @param name The name of attribute in this library
   * @param source the attributes in the template
   */
  public static NativeBinding of(String name, AttributeSource source) {
    return new NativeBinding(name) {
      @Override
      Any bind(String url, SourceReference sourceReference) {
        return Any.of(new Template(sourceReference, Context.EMPTY, source));
      }
    };
  }

  /**
   * Bind a definition as a function-like template
   *
   * <p>Since there is a limit to the binding capabilities of the methods provided in this class, a
   * more complex function can be made by using {@link LookupAssistant} to perform the necessary
   * computation. This method will bind computation as the <tt>value</tt> attribute of a
   * function-like template.
   *
   * @param value usually, a reference to the constructor of the appropriate result of {@link
   *     LookupAssistant} that will be called when the template is instantiated and set the result's
   *     <tt>value</tt> attribue
   */
  public static NativeBinding of(String name, Definition value) {
    return of(name, AttributeSource.of(Attribute.of("value", value)));
  }

  /**
   * Bind a new lookup handler.
   *
   * @param name The name of attribute in this library
   * @param explorer the constructor for the handler's explorer
   * @param collector the constructor for the handler's collector
   */
  public static NativeBinding of(
      String name,
      LookupOperation<LookupExplorer> explorer,
      LookupOperation<LookupSelector> collector) {
    return of(name, Any.of(new LookupHandler(explorer, collector)));
  }

  /**
   * Bind an -ifier template that modifies the <tt>value</tt> attribute of a template provided by
   * <tt>base</tt>
   *
   * @param name The name of attribute in this library
   * @param override the override to apply tot the <tt>value</tt> attribute of the <tt>base</tt>
   *     template
   */
  public static NativeBinding of(String name, OverrideDefinition override) {
    return new NativeBinding(name) {
      @Override
      Any bind(String url, SourceReference sourceReference) {
        return Any.of(
            new Template(
                sourceReference,
                Context.EMPTY,
                AttributeSource.of(
                    Attribute.of(
                        "value",
                        ifier(AttributeSource.of(Attribute.of("value", override)), url)))));
      }
    };
  }

  /**
   * Convert a function with one argument to an override definition
   *
   * <p>This definition will return the value of the function after looking up the parameter and
   * will handle any errors.
   *
   * @param resultType a converter to box the result
   * @param anyConverter The {@link AnyConverter} to unbox the Flabbergast value
   * @param func The function to be called.
   */
  public static <T, R> OverrideDefinition override(
      ResultType<R> resultType,
      AnyConverter<T> anyConverter,
      WhinyFunction<? super T, ? extends R> func) {
    return LookupAssistant.create(
        anyConverter,
        arg ->
            (future, sourceReference, context) -> {
              try {
                resultType.pack(future, sourceReference, context, func.apply(arg));
              } catch (final Exception e) {
                future.error(sourceReference, e.getMessage());
              }
            });
  }

  /**
   * Convert a function with two argument to an override definition
   *
   * <p>This definition will return the value of the function after looking up the parameter and
   * will handle any errors.
   *
   * @param resultType a converter to box the result
   * @param originalAnyConverter The {@link AnyConverter} to unbox the Flabbergast value for the
   *     overriden value
   * @param func The function to be called.
   * @param anyConverter The {@link AnyConverter} to unbox the Flabbergast value as the second
   *     parameter
   * @param parameter The Flabbergast name to lookup as the second parameter
   */
  public static <T1, T2, R> OverrideDefinition override(
      ResultType<R> resultType,
      AnyConverter<T1> originalAnyConverter,
      WhinyFunction2<? super T1, ? super T2, ? extends R> func,
      AnyConverter<T2> anyConverter,
      String parameter) {
    return LookupAssistant.create(
        originalAnyConverter,
        arg ->
            new Holder1<T2>() {
              @Override
              public void run(
                  Future<Any> future, SourceReference sourceReference, Context context) {
                try {
                  resultType.pack(future, sourceReference, context, func.apply(arg, first));
                } catch (final Exception e) {
                  future.error(sourceReference, e.getMessage());
                }
              }
            },
        LookupAssistant.find(anyConverter, (h, x) -> h.first = x, parameter));
  }

  /**
   * Convert a function with three arguments to a definition
   *
   * <p>This definition will return the value of the function after looking up the parameter and
   * will handle any errors.
   *
   * @param resultType a converter to box the result
   * @param originalAnyConverter The {@link AnyConverter} to unbox the Flabbergast value for the
   *     overridden value
   * @param func The function to be called.
   * @param anyConverter1 The {@link AnyConverter} to unbox the Flabbergast value as the second
   *     parameter
   * @param parameter1 The Flabbergast name to lookup as the second parameter
   * @param anyConverter2 The {@link AnyConverter} to unbox the Flabbergast value as the third
   *     parameter
   * @param parameter2 The Flabbergast name to lookup as the third parameter
   */
  public static <T1, T2, T3, R> OverrideDefinition override(
      ResultType<R> resultType,
      AnyConverter<T1> originalAnyConverter,
      WhinyFunction3<? super T1, ? super T2, ? super T3, ? extends R> func,
      AnyConverter<T2> anyConverter1,
      String parameter1,
      AnyConverter<T3> anyConverter2,
      String parameter2) {
    return LookupAssistant.create(
        originalAnyConverter,
        arg ->
            new Holder2<T2, T3>() {

              @Override
              public void run(
                  Future<Any> future, SourceReference sourceReference, Context context) {
                try {
                  resultType.pack(future, sourceReference, context, func.apply(arg, first, second));
                } catch (final Exception e) {
                  future.error(sourceReference, e.getMessage());
                }
              }
            },
        LookupAssistant.find(anyConverter1, (h, x) -> h.first = x, parameter1),
        LookupAssistant.find(anyConverter2, (h, x) -> h.second = x, parameter2));
  }

  /**
   * Convert a function with four arguments to a definition
   *
   * <p>This definition will return the value of the function after looking up the parameter and
   * will handle any errors.
   *
   * @param resultType a converter to box the result
   * @param originalAnyConverter The {@link AnyConverter} to unbox the Flabbergast value for the
   *     overrideen value
   * @param func The function to be called.
   * @param anyConverter1 The {@link AnyConverter} to unbox the Flabbergast value as the second
   *     parameter
   * @param parameter1 The Flabbergast name to lookup as the second parameter
   * @param anyConverter2 The {@link AnyConverter} to unbox the Flabbergast value as the third
   *     parameter
   * @param parameter2 The Flabbergast name to lookup as the third parameter
   * @param anyConverter3 The {@link AnyConverter} to unbox the Flabbergast value as the fourth
   *     parameter
   * @param parameter3 The Flabbergast name to lookup as the fourth parameter
   */
  public static <T1, T2, T3, T4, R> OverrideDefinition override(
      ResultType<R> resultType,
      AnyConverter<T1> originalAnyConverter,
      WhinyFunction4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> func,
      AnyConverter<T2> anyConverter1,
      String parameter1,
      AnyConverter<T3> anyConverter2,
      String parameter2,
      AnyConverter<T4> anyConverter3,
      String parameter3) {
    return LookupAssistant.create(
        originalAnyConverter,
        arg ->
            new Holder3<T2, T3, T4>() {

              @Override
              public void run(
                  Future<Any> future, SourceReference sourceReference, Context context) {
                try {
                  resultType.pack(
                      future, sourceReference, context, func.apply(arg, first, second, third));
                } catch (final Exception e) {
                  future.error(sourceReference, e.getMessage());
                }
              }
            },
        LookupAssistant.find(anyConverter1, (h, x) -> h.first = x, parameter1),
        LookupAssistant.find(anyConverter2, (h, x) -> h.second = x, parameter2),
        LookupAssistant.find(anyConverter3, (h, x) -> h.third = x, parameter3));
  }

  /**
   * Convert an XML document into a nested set of template instantiations compatible with
   * <tt>lib:render/xml</tt>
   */
  public static Definition xml(Document document) {
    return xml(new DOMSource(document));
  }

  /**
   * Parse an XML document into a nested set of template instantiations compatible with
   * <tt>lib:render/xml</tt>
   */
  public static Definition xml(InputStream document) {
    return xml(new StreamSource(document));
  }

  /**
   * Convert an XML document into a nested set of template instantiations compatible with
   * <tt>lib:render/xml</tt>
   */
  public static Definition xml(Source documentSource) {
    try {
      final var converter = new XmlTemplateConverter();
      TransformerFactory.newDefaultInstance()
          .newTransformer()
          .transform(documentSource, new SAXResult(converter));
      return converter.result;
    } catch (TransformerException e) {
      return Definition.error(e.getMessage());
    }
  }

  private final String name;

  private NativeBinding(String name) {
    this.name = name;
  }

  abstract Any bind(String url, SourceReference sourceReference);
}
