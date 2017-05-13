package flabbergast.lang;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** An object that can provide attributes for creating frames and templates */
public abstract class AttributeSource {
  /**
   * Construct a new attribute for an item
   *
   * @param <T> the type of items being package into attributes
   */
  public interface Constructor<T> {

    /**
     * Construct an attribute
     *
     * @param name the name to use for the attribute
     * @param value the value to package
     * @return the constructed attribute
     */
    Attribute create(Name name, T value);
  }

  private static final Collector<Attribute, ?, AttributeSource> COLLECTOR =
      Collectors.collectingAndThen(
          Collectors.toList(),
          items ->
              new AttributeSource() {
                @Override
                Stream<Attribute> attributes() {
                  return items.stream();
                }

                @Override
                Context context(Context context) {
                  return context;
                }

                @Override
                Stream<Name> gatherers() {
                  return Stream.empty();
                }
              });
  /** An attribute source that contains nothing */
  public static final AttributeSource EMPTY =
      new AttributeSource() {
        @Override
        Stream<Attribute> attributes() {
          return Stream.empty();
        }

        @Override
        Context context(Context context) {
          return context;
        }

        @Override
        Stream<Name> gatherers() {
          return Stream.empty();
        }
      };

  /**
   * Convert a map of {@link Any} into an attribute source
   *
   * @param items the attributes
   */
  public static AttributeSource fromMapOfAny(Map<Name, ? extends Promise<Any>> items) {
    return items
        .entrySet()
        .stream()
        .map(e -> Attribute.of(e.getKey(), e.getValue()))
        .collect(COLLECTOR);
  }

  /**
   * Convert a map of {@link Definition} into an attribute source
   *
   * @param items the attributes
   */
  public static AttributeSource fromMapOfDefinition(Map<Name, ? extends Definition> items) {
    return items
        .entrySet()
        .stream()
        .map(e -> Attribute.of(e.getKey(), e.getValue()))
        .collect(COLLECTOR);
  }

  /**
   * Convert a stream of items into an attribute source, numbering them as if in a literal list
   *
   * @param <T> the type of the item
   * @param constructor a function to produce an attribute from each item
   * @param items the items to collect
   */
  public static <T> AttributeSource list(Constructor<T> constructor, Stream<T> items) {
    return items
        .map(
            new Function<T, Attribute>() {
              private long ordinal;

              @Override
              public Attribute apply(T item) {
                return constructor.create(Name.of(++ordinal), item);
              }
            })
        .collect(toSource());
  }

  /**
   * Convert a stream of {@link Any} into an attribute source, numbering them as if in a literal
   * list.
   *
   * @param items the values of the list
   */
  public static AttributeSource listOfAny(Stream<Any> items) {
    return list(Attribute::of, items);
  }

  /**
   * Convert a stream of {@link Definition} into an attribute source, numbering them as if in a
   * literal list.
   *
   * @param items the values of the list
   */
  public static AttributeSource listOfDefinition(Stream<Definition> items) {
    return list(Attribute::of, items);
  }

  /** Create a new attribute source from attribute literals */
  public static AttributeSource of(Attribute... attributes) {
    return Stream.of(attributes).collect(toSource());
  }

  /**
   * Create an attribute source from a collection of items
   *
   * @param attributes the collection to copy
   */
  public static AttributeSource of(Collection<Attribute> attributes) {
    return attributes.stream().collect(toSource());
  }

  /**
   * Merge a collection of builders performing all appropriate overrides
   *
   * @param builders the builders, in order from most-ancestral to most-amended
   */
  static Stream<Attribute> squash(Stream<AttributeSource> builders) {
    final var definitions = new TreeMap<Name, Attribute>();
    builders
        .flatMap(AttributeSource::attributes)
        .forEachOrdered(
            processor ->
                definitions.merge(
                    processor.name(),
                    processor,
                    (existing, replacement) -> replacement.override(existing)));
    return definitions.values().stream().filter(Objects::nonNull);
  }

  /** Stream collector to convert a stream of attributes into an attribute source */
  public static Collector<Attribute, ?, AttributeSource> toSource() {
    return COLLECTOR;
  }

  AttributeSource() {}

  /** Retrieve the attributes from this source */
  abstract Stream<Attribute> attributes();

  abstract Context context(Context context);

  /** Any gatherers associated with this source */
  abstract Stream<Name> gatherers();
}
