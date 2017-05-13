package flabbergast.lang;

import flabbergast.util.Pair;
import java.util.function.Function;

/**
 * Extract a value from the a Java object being proxied through a Flabbergast frame and convert it
 * to a visible Flabbergast attribute.
 */
public final class ProxyAttribute<T> {
  /** Create an attribute for a proxied object. */
  public static <T> ProxyAttribute<T> extract(String name, Function<T, Any> extract) {
    return new ProxyAttribute<>(name, extract);
  }
  /** Create an attribute of type <tt>Bool</tt> for a proxied object. */
  public static <T> ProxyAttribute<T> extractBool(String name, Function<T, Boolean> extract) {
    return new ProxyAttribute<>(name, extract.andThen(Any::of));
  }

  /** Create an attribute of type <tt>Float</tt> for a proxied object. */
  public static <T> ProxyAttribute<T> extractFloat(String name, Function<T, Double> extract) {
    return new ProxyAttribute<>(name, extract.andThen(Any::of));
  }

  /** Create an attribute of type <tt>Frame</tt> for a proxied object. */
  public static <T> ProxyAttribute<T> extractFrame(String name, Function<T, Frame> extract) {
    return new ProxyAttribute<>(name, extract.andThen(Any::of));
  }

  /** Create an attribute of type <tt>Int</tt> for a proxied object. */
  public static <T> ProxyAttribute<T> extractInt(String name, Function<T, Long> extract) {
    return new ProxyAttribute<>(name, extract.andThen(Any::of));
  }

  /** Create an attribute of type <tt>Str</tt> for a proxied object. */
  public static <T> ProxyAttribute<T> extractStr(String name, Function<T, String> extract) {
    return new ProxyAttribute<>(name, extract.andThen(Any::of));
  }
  /** Create a fixed attribute for a proxied object. */
  public static <T> ProxyAttribute<T> fixed(String name, Any value) {
    return new ProxyAttribute<>(name, x -> value);
  }

  private final Function<T, Any> extract;
  private final String name;

  /**
   * Create a new extractor
   *
   * @param name the name of the attribute in the generated frame
   * @param extract a function to extract the value from backing object
   */
  ProxyAttribute(String name, Function<T, Any> extract) {
    this.name = name;
    this.extract = extract;
  }

  Pair<Name, Any> apply(T src) {
    return Pair.of(Name.of(name), extract.apply(src));
  }
}
