package flabbergast.lang;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Describe an expected type of Frame when performing a conversion from a Flabbergast value into a
 * Java one
 */
abstract class FrameTypeExpectation extends TypeExpectation {

  static final FrameTypeExpectation START =
      new FrameTypeExpectation() {
        @Override
        public String toString() {
          return "Frame";
        }
      };

  FrameTypeExpectation() {}

  /**
   * Stipulate that all values in this frame must be of the types provided
   *
   * @param types the types for the attributes
   */
  public FrameTypeExpectation all(Stream<TypeExpectation> types) {
    final var base = toString();
    return new FrameTypeExpectation() {
      final String description =
          types
              .map(Object::toString)
              .sorted()
              .collect(Collectors.joining(" or ", String.format("%s with values [", base), "]"));

      @Override
      public String toString() {
        return description;
      }
    };
  }

  /**
   * Stipulate that this frame also has an attribute of the types specified
   *
   * @param name the name of the attribute
   * @param types the types permitted for that attribute
   */
  public FrameTypeExpectation andAttribute(Name name, Stream<TypeExpectation> types) {
    final var base = toString();
    return new FrameTypeExpectation() {
      final String description =
          types
              .map(Object::toString)
              .sorted()
              .collect(
                  Collectors.joining(
                      " or ", String.format("%s and “%s” (", base, name.toString()), ")"));

      @Override
      public String toString() {
        return description;
      }
    };
  }

  /**
   * Stipulate that this frame has an attribute of the types specified
   *
   * @param name the name of the attribute
   * @param types the types permitted for that attribute
   */
  public FrameTypeExpectation attribute(String name, Stream<TypeExpectation> types) {
    return attribute(Name.of(name), types);
  }

  /**
   * Stipulate that this frame has an attribute of the types specified
   *
   * @param name the name of the attribute
   * @param types the types permitted for that attribute
   */
  public FrameTypeExpectation attribute(Name name, Stream<TypeExpectation> types) {
    final var base = toString();
    return new FrameTypeExpectation() {
      final String description =
          types
              .map(Object::toString)
              .sorted()
              .collect(
                  Collectors.joining(
                      " or ", String.format("%s with “%s” (", base, name.toString()), ")"));

      @Override
      public String toString() {
        return description;
      }
    };
  }

  /**
   * Stipulate that this frame has an attribute of the types specified
   *
   * @param name the name of the attribute
   * @param types the types permitted for that attribute
   */
  public FrameTypeExpectation attribute(String name, TypeExpectation... types) {
    return attribute(name, Stream.of(types));
  }
}
