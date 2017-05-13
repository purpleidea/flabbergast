package flabbergast.lang;

import flabbergast.util.Pair;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Create a consumer of Flabbergast values that writes the output to the provided output consumers
 */
public class AnyConverter<T> {

  /**
   * A custom conversion rule
   *
   * @param <T> the result type from the conversion
   */
  public abstract static class ConversionRule<T> {
    private final TypeExpectation expectation;

    private ConversionRule(TypeExpectation expectation) {
      this.expectation = expectation;
    }

    abstract void set(CustomConversionFunction<T> customConversionFunction);
  }

  private static final List<TypeExpectation> STRING_TYPES =
      List.of(
          TypeExpectation.BOOL, TypeExpectation.FLOAT, TypeExpectation.INT, TypeExpectation.STR);

  private static List<TypeExpectation> allowedTypes(TypeExpectation type, boolean nullable) {
    return nullable ? List.of(type, TypeExpectation.NULL) : List.of(type);
  }

  private static List<TypeExpectation> allowedTypes(
      TypeExpectation type1, TypeExpectation type2, boolean nullable) {
    return nullable ? List.of(type1, type2, TypeExpectation.NULL) : List.of(type1, type2);
  }

  private static List<TypeExpectation> allowedTypes(
      Stream<TypeExpectation> types, boolean nullable) {
    return Stream.concat(types, nullable ? Stream.of(TypeExpectation.NULL) : Stream.empty())
        .collect(Collectors.toList());
  }

  /**
   * Create a converter that extracts a <tt>Bin</tt> value from Flabbergast
   *
   * @param nullable allow the Flabbergast <tt>Null</tt> value to be converted to a Java null value
   */
  public static AnyConverter<byte[]> asBin(boolean nullable) {
    return new AnyConverter<>(
        allowedTypes(TypeExpectation.BIN, nullable),
        new WhinyAnyFunction<>() {
          @Override
          public ConversionOperation<? extends byte[]> apply() {
            return nullable
                ? ConversionOperation.succeed(null)
                : ConversionOperation.fail(TypeExpectation.NULL);
          }

          @Override
          public ConversionOperation<? extends byte[]> apply(byte[] value) {
            return ConversionOperation.succeed(value);
          }
        });
  }

  /**
   * Create a converter that extracts a <tt>Bin</tt> value or a <tt>Str</tt> (or type which can be
   * coerced) converted to bytes in the charset specified
   *
   * @param nullable allow the Flabbergast <tt>Null</tt> value to be converted to a Java null value
   * @param charset the character set for converting string results
   */
  public static AnyConverter<byte[]> asBinOrStr(boolean nullable, Charset charset) {
    return new AnyConverter<>(
        allowedTypes(
            Stream.concat(Stream.of(TypeExpectation.BIN), STRING_TYPES.stream()), nullable),
        new WhinyAnyFunction<>() {
          @Override
          public ConversionOperation<? extends byte[]> apply() {
            return nullable
                ? ConversionOperation.succeed(null)
                : ConversionOperation.fail(TypeExpectation.NULL);
          }

          private ConversionOperation<? extends byte[]> apply(String str) {
            return ConversionOperation.succeed(str.getBytes(charset));
          }

          @Override
          public ConversionOperation<? extends byte[]> apply(boolean value) {
            return apply(value ? "True" : "False");
          }

          @Override
          public ConversionOperation<? extends byte[]> apply(double value) {
            return apply(Double.toString(value));
          }

          @Override
          public ConversionOperation<? extends byte[]> apply(long value) {
            return apply(Long.toString(value));
          }

          @Override
          public ConversionOperation<? extends byte[]> apply(Str value) {
            return apply(value.toString());
          }

          @Override
          public ConversionOperation<? extends byte[]> apply(byte[] value) {
            return ConversionOperation.succeed(value);
          }
        });
  }

  /**
   * Create a converter that extract a <tt>Bool</tt> value from Flabbergast
   *
   * @param nullable allow the Flabbergast <tt>Null</tt> value to be converted to a Java null value
   */
  public static AnyBidiConverter<Boolean> asBool(boolean nullable) {
    return new AnyBidiConverter<>(
        allowedTypes(TypeExpectation.BOOL, nullable),
        new WhinyAnyFunction<Boolean>() {
          @Override
          public ConversionOperation<? extends Boolean> apply() {
            return nullable
                ? ConversionOperation.succeed(null)
                : ConversionOperation.fail(TypeExpectation.NULL);
          }

          @Override
          public ConversionOperation<? extends Boolean> apply(boolean value) {
            return ConversionOperation.succeed(value);
          }
        }) {
      @Override
      public Any box(
          Future<?> future, SourceReference sourceReference, Context context, Boolean value) {
        return Any.of(value);
      }

      @Override
      public int compare(Boolean first, Boolean second) {
        return Boolean.compare(first, second);
      }
    };
  }

  /**
   * Create a converter that extract a <tt>Str</tt> value from Flabbergast containing one character
   * and convert that character to a Unicode codepoint
   */
  public static AnyBidiConverter<Integer> asCodepoint() {
    return new AnyBidiConverter<>(
        List.of(TypeExpectation.STR_1CHAR),
        new WhinyAnyFunction<Integer>() {
          @Override
          public ConversionOperation<? extends Integer> apply(Str value) {
            if (value.length() == 1) {
              return ConversionOperation.succeed(value.toString().codePointAt(0));
            } else {
              return ConversionOperation.fail(TypeExpectation.STR);
            }
          }
        }) {
      @Override
      public Any box(
          Future<?> future, SourceReference sourceReference, Context context, Integer value) {
        return Any.of(new String(new int[] {value}, 0, 1));
      }

      @Override
      public int compare(Integer left, Integer right) {
        return Integer.compare(left, right);
      }
    };
  }

  /**
   * Create a converter that extracts a time from Flabbergast
   *
   * <p>This can be a numeric value taken as seconds-since-the-epoch in UTC or a frame containing an
   * <tt>epoch</tt> value with the same.
   *
   * @param nullable allow the Flabbergast <tt>Null</tt> value to be converted to a Java null value
   */
  public static AnyBidiConverter<ZonedDateTime> asDateTime(boolean nullable) {
    return new AnyBidiConverter<>(
        Stream.of(
                TypeExpectation.FLOAT,
                TypeExpectation.INT,
                TypeExpectation.frame()
                    .attribute("epoch", TypeExpectation.FLOAT, TypeExpectation.INT),
                nullable ? TypeExpectation.NULL : null)
            .filter(Objects::nonNull)
            .collect(Collectors.toList()),
        new WhinyAnyFunction<ZonedDateTime>() {
          @Override
          public ConversionOperation<? extends ZonedDateTime> apply() {
            return nullable
                ? ConversionOperation.succeed(null)
                : ConversionOperation.fail(TypeExpectation.NULL);
          }

          @Override
          public ConversionOperation<? extends ZonedDateTime> apply(double value) {
            return emit(
                Instant.ofEpochSecond((long) value, (long) (value - (long) value) * 1_000_000));
          }

          @Override
          public ConversionOperation<? extends ZonedDateTime> apply(Frame value) {
            return ConversionOperation.extractProxy(value, ZonedDateTime.class, this::attribute);
          }

          @Override
          public ConversionOperation<? extends ZonedDateTime> apply(long value) {
            return emit(Instant.ofEpochSecond(value));
          }

          ConversionOperation<? extends ZonedDateTime> attribute(Frame value) {
            return ConversionOperation.attribute(
                value,
                Name.of("epoch"),
                new AnyConverter<>(List.of(TypeExpectation.FLOAT, TypeExpectation.INT), this));
          }

          private ConversionOperation<? extends ZonedDateTime> emit(Instant value) {
            return ConversionOperation.succeed(ZonedDateTime.ofInstant(value, ZoneId.of("Z")));
          }
        }) {
      @Override
      public Any box(
          Future<?> future, SourceReference sourceReference, Context context, ZonedDateTime value) {
        return Any.of(Frame.of(future, sourceReference, context, value));
      }

      @Override
      public int compare(ZonedDateTime left, ZonedDateTime right) {
        return left.compareTo(right);
      }
    };
  }

  /**
   * Create a converter that extracts a <tt>Float</tt> or <tt>Int</tt> value from Flabbergast
   *
   * @param nullable allow the Flabbergast <tt>Null</tt> value to be converted to a Java null value
   */
  public static AnyBidiConverter<Double> asFloat(boolean nullable) {
    return new AnyBidiConverter<>(
        allowedTypes(TypeExpectation.FLOAT, TypeExpectation.INT, nullable),
        new WhinyAnyFunction<Double>() {
          @Override
          public ConversionOperation<? extends Double> apply() {
            return nullable
                ? ConversionOperation.succeed(null)
                : ConversionOperation.fail(TypeExpectation.NULL);
          }

          @Override
          public ConversionOperation<? extends Double> apply(double value) {
            return ConversionOperation.succeed(value);
          }

          @Override
          public ConversionOperation<? extends Double> apply(long value) {
            return ConversionOperation.succeed((double) value);
          }
        }) {
      @Override
      public Any box(
          Future<?> future, SourceReference sourceReference, Context context, Double value) {
        return Any.of(value);
      }

      @Override
      public int compare(Double left, Double right) {
        return Double.compare(left, right);
      }
    };
  }

  /**
   * Create a converter that extracts a {@link Frame} value from Flabbergast
   *
   * @param nullable allow the Flabbergast <tt>Null</tt> value to be converted to a Java null value
   */
  public static AnyConverter<Frame> asFrame(boolean nullable) {
    return new AnyConverter<>(
        allowedTypes(TypeExpectation.FRAME, nullable),
        new WhinyAnyFunction<>() {
          @Override
          public ConversionOperation<? extends Frame> apply() {
            return nullable
                ? ConversionOperation.succeed(null)
                : ConversionOperation.fail(TypeExpectation.NULL);
          }

          @Override
          public ConversionOperation<? extends Frame> apply(Frame value) {
            return ConversionOperation.succeed(value);
          }
        });
  }

  /**
   * Create a converter that extracts a <tt>Int</tt> value from Flabbergast
   *
   * @param nullable allow the Flabbergast <tt>Null</tt> value to be converted to a Java null value
   */
  public static AnyBidiConverter<Long> asInt(boolean nullable) {
    return new AnyBidiConverter<>(
        allowedTypes(TypeExpectation.INT, nullable),
        new WhinyAnyFunction<Long>() {
          @Override
          public ConversionOperation<? extends Long> apply() {
            return nullable
                ? ConversionOperation.succeed(null)
                : ConversionOperation.fail(TypeExpectation.NULL);
          }

          @Override
          public ConversionOperation<? extends Long> apply(long value) {
            return ConversionOperation.succeed(value);
          }
        }) {
      @Override
      public Any box(
          Future<?> future, SourceReference sourceReference, Context context, Long value) {
        return Any.of(value);
      }

      @Override
      public int compare(Long left, Long right) {
        return Long.compare(left, right);
      }
    };
  }

  /**
   * Create a converter that extracts a {@link LookupHandler} value from Flabbergast
   *
   * @param nullable allow the Flabbergast <tt>Null</tt> value to be converted to a Java null value
   */
  public static AnyConverter<LookupHandler> asLookup(boolean nullable) {
    return new AnyConverter<>(
        allowedTypes(TypeExpectation.LOOKUP_HANDLER, nullable),
        new WhinyAnyFunction<>() {
          @Override
          public ConversionOperation<? extends LookupHandler> apply() {
            return nullable
                ? ConversionOperation.succeed(null)
                : ConversionOperation.fail(TypeExpectation.NULL);
          }

          @Override
          public ConversionOperation<? extends LookupHandler> apply(LookupHandler value) {
            return ConversionOperation.succeed(value);
          }
        });
  }

  /**
   * Create a converter that extracts an <tt>Int</tt> value or a <tt>Str</tt> value and converts it
   * to a {@link Name}
   *
   * @param nullable allow the Flabbergast <tt>Null</tt> value to be converted to a Java null value
   */
  public static AnyBidiConverter<Name> asName(boolean nullable) {
    return new AnyBidiConverter<>(
        allowedTypes(TypeExpectation.INT, TypeExpectation.STR, nullable),
        new WhinyAnyFunction<Name>() {
          @Override
          public ConversionOperation<? extends Name> apply() {
            return nullable
                ? ConversionOperation.succeed(null)
                : ConversionOperation.fail(TypeExpectation.NULL);
          }

          @Override
          public ConversionOperation<? extends Name> apply(long value) {
            return ConversionOperation.succeed(Name.of(value));
          }

          @Override
          public ConversionOperation<? extends Name> apply(Str value) {
            return ConversionOperation.succeed(Name.of(value));
          }
        }) {
      @Override
      public Any box(
          Future<?> future, SourceReference sourceReference, Context context, Name value) {
        return value.any();
      }

      @Override
      public int compare(Name left, Name right) {
        return left.compareTo(right);
      }
    };
  }

  /**
   * Create a converter that extracts two attributes from a frame and returns them as a pair
   *
   * @param firstConverter a converter for the first element in the pair
   * @param firstName the attribute name for the first element in the pair
   * @param secondConverter a converter for the second element in the pair
   * @param secondName the attribute name for the second element in the pair
   * @param <T> the type of the first element in the pair
   * @param <U> the type of the second element in the pair
   */
  public static <T, U> AnyConverter<Pair<T, U>> asPair(
      AnyConverter<T> firstConverter,
      String firstName,
      AnyConverter<U> secondConverter,
      String secondName) {
    return new AnyConverter<>(
        List.of(
            TypeExpectation.frame()
                .attribute(firstName, firstConverter.allowedTypes())
                .andAttribute(Name.of(secondName), secondConverter.allowedTypes())),
        new WhinyAnyFunction<>() {
          @Override
          public ConversionOperation<? extends Pair<T, U>> apply(Frame value) {
            return ConversionOperation.<T, U, Pair<T, U>>both(
                Pair::of,
                ConversionOperation.attribute(value, Name.of(firstName), firstConverter),
                ConversionOperation.attribute(value, Name.of(secondName), secondConverter));
          }
        });
  }

  /**
   * Create a converter that extracts a {@link Frame} from Flabbergast that is really a proxied Java
   * object
   *
   * @param nullable allow the Flabbergast <tt>Null</tt> value to be converted to a Java null value
   * @param type the type of the proxied object
   * @param specialLocation a description of how to find or create this special frame
   */
  public static <T> AnyConverter<T> asProxy(
      Class<T> type, boolean nullable, SpecialLocation specialLocation) {
    return new AnyConverter<>(
        allowedTypes(
            new TypeExpectation() {
              @Override
              public String toString() {
                return "Frame [" + specialLocation.toString() + "]";
              }
            },
            nullable),
        new WhinyAnyFunction<>() {
          @Override
          public ConversionOperation<? extends T> apply() {
            return nullable
                ? ConversionOperation.succeed(null)
                : ConversionOperation.fail(TypeExpectation.NULL);
          }

          @Override
          public ConversionOperation<? extends T> apply(Frame value) {
            return ConversionOperation.extractProxy(
                value, type, f -> ConversionOperation.<T>fail("Frame"));
          }
        });
  }

  /**
   * Create a converter that extracts a <tt>Str</tt> value from Flabbergast as a {@link Str}
   *
   * @param nullable allow the Flabbergast <tt>Null</tt> value to be converted to a Java null value
   */
  public static AnyBidiConverter<Str> asStr(boolean nullable) {
    return new AnyBidiConverter<>(
        allowedTypes(STRING_TYPES.stream(), nullable),
        new WhinyAnyFunction<Str>() {
          @Override
          public ConversionOperation<? extends Str> apply() {
            return nullable
                ? ConversionOperation.succeed(null)
                : ConversionOperation.fail(TypeExpectation.NULL);
          }

          @Override
          public ConversionOperation<? extends Str> apply(boolean value) {
            return ConversionOperation.succeed(Str.from(value));
          }

          @Override
          public ConversionOperation<? extends Str> apply(double value) {
            return ConversionOperation.succeed(Str.from(value));
          }

          @Override
          public ConversionOperation<? extends Str> apply(long value) {
            return ConversionOperation.succeed(Str.from(value));
          }

          @Override
          public ConversionOperation<? extends Str> apply(Str value) {
            return ConversionOperation.succeed(value);
          }
        }) {
      @Override
      public Any box(
          Future<?> future, SourceReference sourceReference, Context context, Str value) {
        return Any.of(value);
      }

      @Override
      public int compare(Str left, Str right) {
        return left.compareTo(right);
      }
    };
  }

  /**
   * Create a converter that extracts a <tt>Str</tt> value from Flabbergast and converts it to a
   * {@link String}
   *
   * @param nullable allow the Flabbergast <tt>Null</tt> value to be converted to a Java null value
   */
  public static AnyBidiConverter<String> asString(boolean nullable) {
    return new AnyBidiConverter<>(
        allowedTypes(STRING_TYPES.stream(), nullable),
        new WhinyAnyFunction<String>() {
          @Override
          public ConversionOperation<? extends String> apply() {
            return nullable
                ? ConversionOperation.succeed(null)
                : ConversionOperation.fail(TypeExpectation.NULL);
          }

          @Override
          public ConversionOperation<? extends String> apply(boolean value) {
            return ConversionOperation.succeed(Boolean.toString(value));
          }

          @Override
          public ConversionOperation<? extends String> apply(double value) {
            return ConversionOperation.succeed(Double.toString(value));
          }

          @Override
          public ConversionOperation<? extends String> apply(long value) {
            return ConversionOperation.succeed(Long.toString(value));
          }

          @Override
          public ConversionOperation<? extends String> apply(Str value) {
            return ConversionOperation.succeed(value.toString());
          }
        }) {
      @Override
      public Any box(
          Future<?> future, SourceReference sourceReference, Context context, String value) {
        return Any.of(value);
      }

      @Override
      public int compare(String left, String right) {
        return left.compareTo(right);
      }
    };
  }

  /**
   * Create a converter that extracts a {@link Template} value from Flabbergast
   *
   * @param nullable allow the Flabbergast <tt>Null</tt> value to be converted to a Java null value
   */
  public static AnyConverter<Template> asTemplate(boolean nullable) {
    return new AnyConverter<>(
        allowedTypes(TypeExpectation.TEMPLATE, nullable),
        new WhinyAnyFunction<>() {
          @Override
          public ConversionOperation<? extends Template> apply() {
            return nullable
                ? ConversionOperation.succeed(null)
                : ConversionOperation.fail(TypeExpectation.NULL);
          }

          @Override
          public ConversionOperation<? extends Template> apply(Template value) {
            return ConversionOperation.succeed(value);
          }
        });
  }

  /**
   * Create a custom converter for <tt>Bin</tt> data
   *
   * @param function a function to convert the data to the required output type
   * @param <T> the output conversion type
   */
  public static <T> ConversionRule<T> convertBin(
      Function<? super byte[], ConversionOperation<? extends T>> function) {
    return new ConversionRule<>(TypeExpectation.BIN) {
      @Override
      void set(CustomConversionFunction<T> customConversionFunction) {
        customConversionFunction.binConversion = function;
      }
    };
  }

  /**
   * Create a custom converter for <tt>Bool</tt> data
   *
   * @param function a function to convert the data to the required output type
   * @param <T> the output conversion type
   */
  public static <T> ConversionRule<T> convertBool(
      Function<? super Boolean, ConversionOperation<? extends T>> function) {
    return new ConversionRule<>(TypeExpectation.BOOL) {
      @Override
      void set(CustomConversionFunction<T> customConversionFunction) {
        customConversionFunction.booleanConversion = function;
      }
    };
  }

  /**
   * Create a custom converter for <tt>Float</tt> data
   *
   * @param function a function to convert the data to the required output type
   * @param <T> the output conversion type
   */
  public static <T> ConversionRule<T> convertFloat(
      DoubleFunction<ConversionOperation<? extends T>> function) {
    return new ConversionRule<>(TypeExpectation.FLOAT) {
      @Override
      void set(CustomConversionFunction<T> customConversionFunction) {
        customConversionFunction.floatConversion = function;
      }
    };
  }

  /**
   * Create a custom converter for <tt>Frame</tt> data
   *
   * @param function a function to convert the data to the required output type
   * @param <T> the output conversion type
   */
  public static <T> ConversionRule<T> convertFrame(
      Function<? super Frame, ConversionOperation<? extends T>> function) {
    return new ConversionRule<>(TypeExpectation.FRAME) {
      @Override
      void set(CustomConversionFunction<T> customConversionFunction) {
        customConversionFunction.frameConversion = function;
      }
    };
  }

  /** Create a custom converter for <tt>Frame</tt> data which resolves all the attributes */
  public static ConversionRule<Map<Name, Any>> convertFrameAll() {
    return new ConversionRule<>(TypeExpectation.FRAME) {
      @Override
      void set(CustomConversionFunction<Map<Name, Any>> customConversionFunction) {
        customConversionFunction.frameConversion = ConversionOperation::frame;
      }
    };
  }

  /**
   * Create a custom converter for <tt>Frame</tt> data which resolves all the attributes
   *
   * @param function a transformation to apply to the output
   * @param <T> the output conversion type
   */
  public static <T> ConversionRule<T> convertFrameAll(
      Function<? super Map<Name, Any>, T> function) {
    return new ConversionRule<>(TypeExpectation.FRAME) {
      @Override
      void set(CustomConversionFunction<T> customConversionFunction) {
        customConversionFunction.frameConversion =
            value -> ConversionOperation.frame(value).map(function);
      }
    };
  }

  /**
   * Create a custom converter for <tt>Frame</tt> data which converts all of the attributes
   *
   * @param converter the conversion to apply to the attribute values
   * @param <T> the output conversion type
   */
  public static <T> ConversionRule<Map<Name, T>> convertFrameAll(AnyConverter<T> converter) {
    return new ConversionRule<>(FrameTypeExpectation.frame().all(converter.allowedTypes())) {
      @Override
      void set(CustomConversionFunction<Map<Name, T>> customConversionFunction) {
        customConversionFunction.frameConversion =
            value -> ConversionOperation.frame(value, converter);
      }
    };
  }

  /**
   * Create a custom converter for <tt>Frame</tt> data which converts all of the attributes
   *
   * @param converter the conversion to apply to the attribute values
   * @param <T> the output conversion type for each attribute
   * @param <R> the output conversion type of the operation
   */
  public static <T, R> ConversionRule<R> convertFrameAll(
      AnyConverter<T> converter, Function<? super Map<Name, T>, R> function) {
    return new ConversionRule<>(FrameTypeExpectation.frame().all(converter.allowedTypes())) {
      @Override
      void set(CustomConversionFunction<R> customConversionFunction) {
        customConversionFunction.frameConversion =
            value -> ConversionOperation.frame(value, converter).map(function);
      }
    };
  }

  /**
   * Create a custom converter for <tt>Frame</tt> data that extracts the value of a particular
   * attribute
   *
   * @param name the attribute to select
   * @param converter the conversion to apply to the attribute
   * @param <T> the output conversion type
   */
  public static <T> ConversionRule<T> convertFrameAttribute(Name name, AnyConverter<T> converter) {
    return new ConversionRule<>(
        FrameTypeExpectation.frame().attribute(name, converter.allowedTypes())) {
      @Override
      void set(CustomConversionFunction<T> customConversionFunction) {
        customConversionFunction.frameConversion =
            value -> ConversionOperation.attribute(value, name, converter);
      }
    };
  }

  /**
   * Create a custom converter for <tt>Int</tt> data
   *
   * @param function a function to convert the data to the required output type
   * @param <T> the output conversion type
   */
  public static <T> ConversionRule<T> convertInt(
      LongFunction<ConversionOperation<? extends T>> function) {
    return new ConversionRule<>(TypeExpectation.INT) {
      @Override
      void set(CustomConversionFunction<T> customConversionFunction) {
        customConversionFunction.intConversion = function;
      }
    };
  }

  /**
   * Create a custom converter for <tt>LookupHadler</tt> data
   *
   * @param function a function to convert the data to the required output type
   * @param <T> the output conversion type
   */
  public static <T> ConversionRule<T> convertLookupHandler(
      Function<? super LookupHandler, ConversionOperation<? extends T>> function) {
    return new ConversionRule<>(TypeExpectation.LOOKUP_HANDLER) {
      @Override
      void set(CustomConversionFunction<T> customConversionFunction) {
        customConversionFunction.lookupHandlerConversion = function;
      }
    };
  }

  /**
   * Create a custom converter for <tt>Null</tt> data
   *
   * @param function a function to convert the data to the required output type
   * @param <T> the output conversion type
   */
  public static <T> ConversionRule<T> convertNull(
      Supplier<ConversionOperation<? extends T>> function) {
    return new ConversionRule<>(TypeExpectation.NULL) {
      @Override
      void set(CustomConversionFunction<T> customConversionFunction) {
        customConversionFunction.nullConversion = function;
      }
    };
  }

  /**
   * Create a custom converter for a <tt>Frame</tt> carrying a proxy
   *
   * @param type the type of the proxied object
   * @param location a description of how to find or create this special frame
   * @param <T> the output conversion type
   */
  public static <T> ConversionRule<T> convertProxyFrame(Class<T> type, SpecialLocation location) {
    return new ConversionRule<>(
        new TypeExpectation() {
          @Override
          public String toString() {
            return "Frame [" + location.toString() + "]";
          }
        }) {
      @Override
      void set(CustomConversionFunction<T> customConversionFunction) {
        customConversionFunction.frameConversion =
            value ->
                ConversionOperation.extractProxy(
                    value, type, f -> ConversionOperation.<T>fail(TypeExpectation.FRAME));
      }
    };
  }

  /**
   * Create a custom converter for <tt>Str</tt> data
   *
   * @param function a function to convert the data to the required output type
   * @param <T> the output conversion type
   */
  public static <T> ConversionRule<T> convertStr(
      Function<? super Str, ConversionOperation<? extends T>> function) {
    return new ConversionRule<>(TypeExpectation.STR) {
      @Override
      void set(CustomConversionFunction<T> customConversionFunction) {
        customConversionFunction.strConversion = function;
      }
    };
  }

  /**
   * Create a custom converter for <tt>Template</tt> data
   *
   * @param function a function to convert the data to the required output type
   * @param <T> the output conversion type
   */
  public static <T> ConversionRule<T> convertTemplate(
      Function<? super Template, ConversionOperation<? extends T>> function) {
    return new ConversionRule<>(TypeExpectation.TEMPLATE) {
      @Override
      void set(CustomConversionFunction<T> customConversionFunction) {
        customConversionFunction.templateConversion = function;
      }
    };
  }

  /**
   * Create a converter that consumes a frame where every value in the frame must match the provided
   * converter
   *
   * @param <T> the type of the inner values of the frame
   * @param anyConverter the anyConverter for the frame values
   * @param nullable whether the whole frame may be <tt>Null</tt>
   */
  public static <T> AnyConverter<Map<Name, T>> frameOf(
      AnyConverter<T> anyConverter, boolean nullable) {
    return new AnyConverter<>(
        allowedTypes(TypeExpectation.frame().all(anyConverter.allowedTypes()), nullable),
        new WhinyAnyFunction<>() {
          @Override
          public ConversionOperation<? extends Map<Name, T>> apply() {
            return nullable
                ? ConversionOperation.succeed(null)
                : ConversionOperation.fail(TypeExpectation.NULL);
          }

          @Override
          public ConversionOperation<? extends Map<Name, T>> apply(Frame value) {
            return ConversionOperation.frame(value, anyConverter);
          }
        });
  }

  /**
   * Create a converter that consumes a frame where every value is left as {@link Any}.
   *
   * @param nullable whether the whole frame may be <tt>Null</tt>
   */
  public static AnyConverter<Map<Name, Any>> frameOfAny(boolean nullable) {
    return new AnyConverter<>(
        allowedTypes(TypeExpectation.FRAME, nullable),
        new WhinyAnyFunction<>() {
          @Override
          public ConversionOperation<? extends Map<Name, Any>> apply() {
            return nullable
                ? ConversionOperation.succeed(null)
                : ConversionOperation.fail(TypeExpectation.NULL);
          }

          @Override
          public ConversionOperation<? extends Map<Name, Any>> apply(Frame value) {
            return ConversionOperation.frame(value);
          }
        });
  }

  /**
   * Convert all numeric types to a common result
   *
   * <p>This is not necessary if the target type is {@link #asFloat(boolean)}, since it will upgrade
   * integers automatically.
   *
   * @param longFunc the converter for integer type
   * @param doubleFunc the converter for floating point types
   * @param <T> the result type
   */
  public static <T> AnyConverter<T> numeric(
      LongFunction<T> longFunc, DoubleFunction<T> doubleFunc) {
    return of(
        AnyConverter.convertInt(v -> ConversionOperation.succeed(longFunc.apply(v))),
        convertFloat(v -> ConversionOperation.succeed(doubleFunc.apply(v))));
  }

  /**
   * Create a new converter from individual conversion processes for each input type
   *
   * @param conversionRules converters for each input type; if no converter is supplied for a
   *     particular type, then any attempt to convert that type will result in a type error in the
   *     calling Flabbergast program
   * @param <T> the result type of the conversion
   */
  @SafeVarargs
  public static <T> AnyConverter<T> of(ConversionRule<T>... conversionRules) {
    final var function = new CustomConversionFunction<T>();
    for (final ConversionRule<T> conversionRule : conversionRules) {
      conversionRule.set(function);
    }
    return new AnyConverter<>(
        Stream.of(conversionRules).map(c -> c.expectation).collect(Collectors.toList()), function);
  }
  /**
   * Create a new converter from individual conversion processes for each input type
   *
   * @param converters converters for each input type; if no converter is supplied for a particular
   *     type, then any attempt to convert that type will result in a type error in the calling
   *     Flabbergast program
   * @param <T> the result type of the conversion
   */
  public static <T> AnyConverter<T> of(Stream<ConversionRule<T>> converters) {
    final var function = new CustomConversionFunction<T>();
    return new AnyConverter<>(
        converters.peek(c -> c.set(function)).map(c -> c.expectation).collect(Collectors.toList()),
        function);
  }

  private final List<TypeExpectation> allowedTypes;
  private final AnyFunction<ConversionOperation<? extends T>> function;

  AnyConverter(
      List<TypeExpectation> allowedTypes, AnyFunction<ConversionOperation<? extends T>> function) {
    this.allowedTypes = allowedTypes;
    this.function = function;
  }

  /** Describe the types that this converter expects */
  final Stream<TypeExpectation> allowedTypes() {
    return allowedTypes.stream();
  }

  /**
   * Create a consumer for the conversion
   *
   * @param future the future in which the conversion will occur
   * @param sourceReference the caller of the conversion operation
   * @param typeErrorLocation a location that describes the point of the conversion operation
   * @param consumer the consumer of the converted value
   */
  public AnyConsumer asConsumer(
      Future<?> future,
      SourceReference sourceReference,
      TypeErrorLocation typeErrorLocation,
      Consumer<? super T> consumer) {
    return AnyFunction.compose(
        function,
        operation -> operation.resolve(future, sourceReference, this, typeErrorLocation, consumer));
  }

  /** Get the conversion operation for this converter */
  public final AnyFunction<ConversionOperation<? extends T>> function() {
    return function;
  }

  /**
   * Perform a transformation of the result of the conversion operation
   *
   * @param function the transformation to apply
   * @param <R> the new result type of the operation
   */
  public final <R> AnyConverter<R> thenApply(Function<? super T, R> function) {
    return new AnyConverter<>(
        allowedTypes,
        AnyFunction.transform(this.function, ConversionOperation.transform(function)));
  }
}
