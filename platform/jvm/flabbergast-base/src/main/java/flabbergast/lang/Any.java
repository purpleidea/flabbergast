package flabbergast.lang;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A box holding on the the allowable Flabbergast types.
 *
 * <p>Every attribute in a {@link Frame} will be a {@link Promise} for a boxed value as will results
 * of {@link Definition}, {@link OverrideDefinition}, {@link RootDefinition}, and {@link
 * CollectorDefinition}. When required, the result ca be unboxed.
 */
public abstract class Any extends Promise<Any> implements Consumer<AnyConsumer> {

  private static class BinAny extends Any {

    private final byte[] value;

    public BinAny(byte[] value) {
      this.value = value;
    }

    @Override
    public void accept(AnyConsumer acceptor) {
      acceptor.accept(value);
    }

    @Override
    public <T> T apply(AnyFunction<T> function) {
      return function.apply(value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      BinAny binAny = (BinAny) o;
      return Arrays.equals(value, binAny.value);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
      return value.length + " bytes of unspeakable horror";
    }
  }

  private static class BooleanAny extends Any {

    private final boolean value;

    public BooleanAny(boolean value) {
      this.value = value;
    }

    @Override
    public void accept(AnyConsumer acceptor) {
      acceptor.accept(value);
    }

    @Override
    public <T> T apply(AnyFunction<T> function) {
      return function.apply(value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      BooleanAny that = (BooleanAny) o;
      return value == that.value;
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return value ? "True" : "False";
    }
  }

  private static class FloatAny extends Any {

    private final double value;

    public FloatAny(double value) {
      this.value = value;
    }

    @Override
    public void accept(AnyConsumer acceptor) {
      acceptor.accept(value);
    }

    @Override
    public <T> T apply(AnyFunction<T> function) {
      return function.apply(value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      FloatAny floatAny = (FloatAny) o;
      return Double.compare(floatAny.value, value) == 0;
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return Double.toString(value);
    }
  }

  private static class FrameAny extends Any {

    private final Frame value;

    public FrameAny(Frame value) {
      this.value = value;
    }

    @Override
    public void accept(AnyConsumer acceptor) {
      acceptor.accept(value);
    }

    @Override
    public <T> T apply(AnyFunction<T> function) {
      return function.apply(value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      FrameAny frameAny = (FrameAny) o;
      return value.equals(frameAny.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return "Frame " + value.id().toString();
    }
  }

  private static class IntAny extends Any {

    private final long value;

    public IntAny(long value) {
      this.value = value;
    }

    @Override
    public void accept(AnyConsumer acceptor) {
      acceptor.accept(value);
    }

    @Override
    public <T> T apply(AnyFunction<T> function) {
      return function.apply(value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      IntAny intAny = (IntAny) o;
      return value == intAny.value;
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return Long.toString(value);
    }
  }

  private static class LookupHandlerAny extends Any {

    private final LookupHandler value;

    public LookupHandlerAny(LookupHandler value) {
      this.value = value;
    }

    @Override
    public void accept(AnyConsumer acceptor) {
      acceptor.accept(value);
    }

    @Override
    public <T> T apply(AnyFunction<T> function) {
      return function.apply(value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LookupHandlerAny that = (LookupHandlerAny) o;
      return value.equals(that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return value.description();
    }
  }

  private static class StrAny extends Any {

    private final Str value;

    public StrAny(Str value) {
      this.value = value;
    }

    @Override
    public void accept(AnyConsumer acceptor) {
      acceptor.accept(value);
    }

    @Override
    public <T> T apply(AnyFunction<T> function) {
      return function.apply(value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      StrAny strAny = (StrAny) o;
      return value.equals(strAny.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return "Str: " + value.toString();
    }
  }

  private static class TemplateAny extends Any {

    private final Template value;

    public TemplateAny(Template value) {
      this.value = value;
    }

    @Override
    public void accept(AnyConsumer acceptor) {
      acceptor.accept(value);
    }

    @Override
    public <T> T apply(AnyFunction<T> function) {
      return function.apply(value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TemplateAny that = (TemplateAny) o;
      return value.equals(that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return "Template";
    }
  }

  /** The value for <tt>Null</tt> */
  public static final Any NULL =
      new Any() {
        @Override
        public void accept(AnyConsumer acceptor) {
          acceptor.accept();
        }

        @Override
        public <T> T apply(AnyFunction<T> function) {
          return function.apply();
        }

        @Override
        public String toString() {
          return "Null";
        }
      };

  /** Box a <tt>Bool</tt>. */
  public static Any of(boolean value) {
    return new BooleanAny(value);
  }

  /**
   * Box a <tt>Bool</tt>.
   *
   * @param value The value to store. If null, Flabbergast <tt>Null</tt> is substituted.
   */
  public static Any of(Boolean value) {
    return value == null ? NULL : new BooleanAny(value);
  }

  /**
   * Box a <tt>Bin</tt>.
   *
   * @param value The value to store. If null, Flabbergast <tt>Null</tt> is substituted.
   */
  public static Any of(byte[] value) {
    return value == null ? NULL : new BinAny(value);
  }

  /**
   * Box a <tt>Float</tt>.
   *
   * @param value The value to store.
   */
  public static Any of(double value) {
    return new FloatAny(value);
  }

  /**
   * Box a <tt>Float</tt>.
   *
   * @param value The value to store. If null, Flabbergast <tt>Null</tt> is substituted.
   */
  public static Any of(Double value) {
    return value == null ? NULL : new FloatAny(value);
  }

  /**
   * Box a <tt>Frame</tt>.
   *
   * @param value The value to store. If null, Flabbergast <tt>Null</tt> is substituted.
   */
  public static Any of(Frame value) {
    return value == null ? NULL : new FrameAny(value);
  }

  /**
   * Box a <tt>Int</tt>.
   *
   * @param value The value to store.
   */
  public static Any of(long value) {
    return new IntAny(value);
  }

  /**
   * Box a <tt>Int</tt>.
   *
   * @param value The value to store. If null, Flabbergast <tt>Null</tt> is substituted.
   */
  public static Any of(Integer value) {
    return value == null ? NULL : new IntAny(value);
  }

  /**
   * Box a <tt>Int</tt>.
   *
   * @param value The value to store. If null, Flabbergast <tt>Null</tt> is substituted.
   */
  public static Any of(Long value) {
    return value == null ? NULL : new IntAny(value);
  }

  /**
   * Box a <tt>LookupHandler</tt>.
   *
   * @param value The value to store. If null, Flabbergast <tt>Null</tt> is substituted.
   */
  public static Any of(LookupHandler value) {
    return value == null ? NULL : new LookupHandlerAny(value);
  }

  /**
   * Box a <tt>Str</tt>.
   *
   * @param value The value to store. If null, Flabbergast <tt>Null</tt> is substituted.
   */
  public static Any of(String value) {
    return value == null ? NULL : new StrAny(Str.from(value));
  }

  /**
   * Box a <tt>Str</tt>.
   *
   * @param value The value to store. If null, Flabbergast <tt>Null</tt> is substituted.
   */
  public static Any of(Str value) {
    return value == null ? NULL : new StrAny(value);
  }

  /**
   * Box a <tt>Template</tt>.
   *
   * @param value The value to store. If null, Flabbergast <tt>Null</tt> is substituted.
   */
  public static Any of(Template value) {
    return value == null ? NULL : new TemplateAny(value);
  }

  private Any() {}

  @Override
  public void accept(PromiseConsumer<? super Any> consumer) {
    consumer.accept(this);
  }

  @Override
  public <R> R apply(PromiseFunction<? super Any, R> function) {
    return function.apply(this);
  }

  /**
   * Convert the value in this box to a singular result type
   *
   * @param function a set of functions to convert each different possible input type into the
   *     result type
   * @param <T> the type of the result
   */
  public abstract <T> T apply(AnyFunction<T> function);

  @Override
  final void await(Future<?> waiter, Consumer<? super Any> consumer) {
    consumer.accept(this);
  }

  /**
   * Determine if this value is “falsy” or “falsey” in the JavaScript sense
   *
   * @return true if the item is Null, False, 0, NaN, an empty frame, or an empty string
   */
  public final boolean isFalsy() {
    return apply(
        new AnyFunction<>() {
          @Override
          public Boolean apply() {
            return true;
          }

          @Override
          public Boolean apply(boolean value) {
            return !value;
          }

          @Override
          public Boolean apply(byte[] value) {
            return value.length == 0;
          }

          @Override
          public Boolean apply(double value) {
            return value == 0 || Double.isNaN(value);
          }

          @Override
          public Boolean apply(Frame value) {
            return value.isEmpty();
          }

          @Override
          public Boolean apply(long value) {
            return value == 0;
          }

          @Override
          public Boolean apply(LookupHandler value) {
            return false;
          }

          @Override
          public Boolean apply(Str value) {
            return value.length() == 0;
          }

          @Override
          public Boolean apply(Template value) {
            return false;
          }
        });
  }

  /**
   * Convert the item in this box into a string
   *
   * @param future the future used to emit an error if this item is not convertible
   * @param sourceReference the calling trace to use for the error
   * @param consumer the callback to consume the resulting string
   */
  public final void toStr(
      Future<?> future, SourceReference sourceReference, Consumer<Str> consumer) {
    accept(
        new WhinyAnyConsumer() {
          @Override
          public void accept(boolean value) {
            consumer.accept(Str.from(value));
          }

          @Override
          public void accept(double value) {
            consumer.accept(Str.from(value));
          }

          @Override
          public void accept(long value) {
            consumer.accept(Str.from(value));
          }

          @Override
          public void accept(Str value) {
            consumer.accept(value);
          }

          @Override
          protected final void fail(String type) {
            future.error(
                sourceReference,
                String.format("Expected Bool or Float or Int or Str, but got %s.", type));
          }
        });
  }
}
