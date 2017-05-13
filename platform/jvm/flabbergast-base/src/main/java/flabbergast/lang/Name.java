package flabbergast.lang;

import java.text.Collator;
import java.util.Objects;
import java.util.function.*;
import java.util.regex.Pattern;

/**
 * A name for an attribute in a frame or template
 *
 * <p>Names in Flabbergast can be strings, conforming to a “valid identifier” or an integral value.
 */
public abstract class Name implements Comparable<Name> {
  private static final class OrdinalName extends Name {
    private final long ordinal;

    private OrdinalName(long ordinal) {
      this.ordinal = ordinal;
    }

    @Override
    public void accept(NameConsumer consumer) {
      consumer.accept(ordinal);
    }

    @Override
    public Any any() {
      return Any.of(ordinal);
    }

    @Override
    public <T> T apply(NameFunction<? extends T> function) {
      return function.apply(ordinal);
    }

    @Override
    public <T> T apply(Name other, NameBiFunction<T> function) {
      return other.apply(ordinal, function);
    }

    @Override
    <T> T apply(String name, NameBiFunction<T> function) {
      return function.apply(name, ordinal);
    }

    @Override
    <T> T apply(long ordinal, NameBiFunction<T> function) {
      return function.apply(ordinal, this.ordinal);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final var that = (OrdinalName) o;
      return ordinal == that.ordinal;
    }

    @Override
    public int hashCode() {
      return Objects.hash(ordinal);
    }

    @Override
    public boolean test(boolean stringTest, boolean ordinalTest) {
      return ordinalTest;
    }

    @Override
    public String toString() {
      return "Attribute(" + ordinal + ")";
    }
  }

  private static final class StringName extends Name {
    private final String name;

    private StringName(String name) {
      this.name = name;
    }

    @Override
    public void accept(NameConsumer consumer) {
      consumer.accept(name);
    }

    @Override
    public Any any() {
      return Any.of(name);
    }

    @Override
    public <T> T apply(NameFunction<? extends T> function) {
      return function.apply(name);
    }

    @Override
    public <T> T apply(Name other, NameBiFunction<T> function) {
      return other.apply(name, function);
    }

    @Override
    <T> T apply(String name, NameBiFunction<T> function) {
      return function.apply(name, this.name);
    }

    @Override
    <T> T apply(long ordinal, NameBiFunction<T> function) {
      return function.apply(ordinal, name);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final var that = (StringName) o;
      return name.equals(that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }

    @Override
    public boolean test(boolean stringTest, boolean ordinalTest) {
      return stringTest;
    }

    @Override
    public String toString() {
      if (IDENTIFIER.test(name)) {
        return name;
      } else {
        final var buffer = new StringBuilder();
        buffer.append("Attribute(\"");
        for (var i = 0; i < name.length(); i++) {
          if (name.charAt(i) == '"') {
            buffer.append("\\\"");
          } else if (name.charAt(i) == '\\') {
            buffer.append("\\\"");
          } else if (Character.isISOControl(name.charAt(i))) {
            buffer.append(String.format("\\x%02x", (int) name.charAt(i)));
          } else {
            buffer.append(name.charAt(i));
          }
        }
        buffer.append("\")");
        return buffer.toString();
      }
    }
  }

  private static final NameBiFunction<Integer> COMPARATOR =
      new NameBiFunction<>() {
        @Override
        public Integer apply(long left, long right) {
          return Long.compare(left, right);
        }

        @Override
        public Integer apply(long left, String right) {
          return 1;
        }

        @Override
        public Integer apply(String left, long right) {
          return -1;
        }

        @Override
        public Integer apply(String left, String right) {
          return Collator.getInstance().compare(left, right);
        }
      };
  private static final Predicate<String> IDENTIFIER =
      Pattern.compile("^[a-z][a-zA-Z0-9_]*$").asMatchPredicate();

  /** Create a name from a string value */
  public static Name of(Str name) {
    return of(name.toString());
  }

  /** Create a name from a string value */
  public static Name of(String name) {
    return new StringName(name);
  }

  /** Create a name from an integral value */
  public static Name of(long ordinal) {
    return new OrdinalName(ordinal);
  }

  private Name() {}

  /**
   * Access the value in this name
   *
   * @param consumer the consumer that will be invoked with the value
   */
  public abstract void accept(NameConsumer consumer);

  /** Convert this name into a boxed value */
  public abstract Any any();

  /**
   * Extract this name into a result
   *
   * @param function a transformation to create a value for this name
   * @param <T> the return type of the transformation
   */
  public abstract <T> T apply(NameFunction<? extends T> function);

  abstract <T> T apply(String name, NameBiFunction<T> function);

  abstract <T> T apply(long ordinal, NameBiFunction<T> function);

  /**
   * Extract two names and produce a new value based on the combination
   *
   * @param other the other name to unpack
   * @param function the processor to compute a result for the unpacked values
   * @param <T> the return type of the processor
   */
  public abstract <T> T apply(Name other, NameBiFunction<T> function);

  /** Order two names in the standard Flabbergast sort order for attribute names */
  @Override
  public final int compareTo(Name other) {
    return apply(other, COMPARATOR);
  }

  /**
   * Check what type the value is
   *
   * @param stringTest the value to return if a string
   * @param ordinalTest the value to return if an integer
   */
  public abstract boolean test(boolean stringTest, boolean ordinalTest);
}
