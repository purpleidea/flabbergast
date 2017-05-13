package flabbergast.lang;

import java.util.stream.Stream;

/**
 * The resulting value of an accumulation operation.
 *
 * <p>The result contains two entities: the value, accessible by {@link #value()}, and the attribute
 * builders, accessible by using this object in building a new frame or template.
 */
public final class Accumulator extends AttributeSource {

  /**
   * Create a new accumulation result.
   *
   * @param value the value to be associated with the reduce chain
   * @param attributes the attributes to be emitted
   */
  public static Accumulator of(Any value, Attribute... attributes) {
    return new Accumulator(value, AttributeSource.of(attributes));
  }

  /**
   * Create a new accumulation result.
   *
   * @param value the value to be associated with the reduce chain
   * @param attributes the attributes to be emitted
   */
  public static Accumulator of(Any value, AttributeSource attributes) {
    return new Accumulator(value, attributes);
  }
  /**
   * Create a new accumulation result.
   *
   * @param value the value to be associated with the reduce chain
   * @param sources the attributes to be emitted
   */
  public static Accumulator of(Any value, AttributeSource... sources) {
    return new Accumulator(
        value,
        Stream.of(sources)
            .flatMap(AttributeSource::attributes)
            .collect(AttributeSource.toSource()));
  }

  private final AttributeSource attributes;
  private final Any value;

  private Accumulator(Any value, AttributeSource attributes) {
    this.value = value;
    this.attributes = attributes;
  }

  @Override
  Stream<Attribute> attributes() {
    return attributes.attributes();
  }

  @Override
  Context context(Context context) {
    return context;
  }

  @Override
  Stream<Name> gatherers() {
    return Stream.empty();
  }

  /** The value associated with the result */
  public Any value() {
    return value;
  }
}
