package flabbergast.lang;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Describe the context in which a type mismatch error is happening. */
public final class TypeErrorLocation {
  static final TypeErrorLocation BUCKETS = new TypeErrorLocation(" in “Buckets” of “Group”");
  static final TypeErrorLocation ADJACENT = new TypeErrorLocation(" in “Adjacent” of “Group”");
  static final TypeErrorLocation CROSSTAB = new TypeErrorLocation(" in “CrossTab” of “Group”");
  static final TypeErrorLocation ORDER_BY = new TypeErrorLocation(" in “Order By”");
  /** Describe the original value when doing a named override or template override */
  public static final TypeErrorLocation ORIGINAL = new TypeErrorLocation(" for original value");
  /** Describe something very vague and unclear */
  public static final TypeErrorLocation UNKNOWN =
      new TypeErrorLocation(" in indescribable location");

  static final TypeErrorLocation WINDOW = new TypeErrorLocation(" for window in “Group”");

  static TypeErrorLocation by(Name name) {
    return new TypeErrorLocation(String.format(" for “%s” in “By” of “Group”", name));
  }

  /**
   * Describe a type error which occurs during a lookup operation
   *
   * @param names the attribute names being looked up
   */
  public static TypeErrorLocation lookup(Name... names) {
    return new TypeErrorLocation(
        Stream.of(names).map(Object::toString).collect(Collectors.joining(".", " for “", "”")));
  }
  /**
   * Describe a type error which occurs during a lookup operation
   *
   * @param names the attribute names being looked up
   */
  public static TypeErrorLocation lookup(String... names) {
    return new TypeErrorLocation(Stream.of(names).collect(Collectors.joining(".", " for “", "”")));
  }

  private final String target;

  private TypeErrorLocation(String target) {
    this.target = target;
  }

  final String render(String actual, AnyConverter<?> expected) {
    return String.format(
        "Expected %s%s, but got %s.",
        expected.allowedTypes().map(Object::toString).sorted().collect(Collectors.joining()),
        target,
        actual);
  }
}
