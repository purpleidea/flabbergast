package flabbergast.lang;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Describe a location for finding special {@link Frame} values when converting Flabbergast types to
 * Java types
 */
public abstract class SpecialLocation {
  /**
   * Get the location for a Flabbergast library
   *
   * @param parts the components of the path to find the library (e.g., <tt>"cncf",
   *     "kubernetes"</tt> for <tt>lib:cncf/kubernetes</tt>)
   */
  public static SpecialLocation library(String... parts) {
    return new SpecialLocation() {
      private final String location = String.format("From lib:%s", String.join("/", parts));

      @Override
      public String toString() {
        return location;
      }
    };
  }

  /**
   * Get the location for an imported URI
   *
   * @param schema the schema portion of the URI
   */
  public static SpecialLocation uri(String schema) {
    return new SpecialLocation() {
      private final String location = String.format("From %s:...", schema);

      @Override
      public String toString() {
        return location;
      }
    };
  }

  private SpecialLocation() {}

  /** Describe a template that was amended */
  public SpecialLocation amended() {
    final var location = "template amended from " + toString();
    return new SpecialLocation() {
      @Override
      public String toString() {
        return location;
      }
    };
  }

  /**
   * Describe any value in this context
   *
   * <p>For instance, suppose there is a library which provides several functions that maybe used.
   * This would describe that path to the user. This of this as a wildcard attribute name
   */
  public final SpecialLocation any() {
    final var location = String.format("any from %s", toString());
    return new SpecialLocation() {
      @Override
      public String toString() {
        return location;
      }
    };
  }

  /**
   * Describe a path of attributes, as if by lookup
   *
   * @param names the attribute names
   */
  public final SpecialLocation attributes(String... names) {
    final var location = String.format("(%s).%s", toString(), String.join(".", names));
    return new SpecialLocation() {
      @Override
      public String toString() {
        return location;
      }
    };
  }

  /**
   * Describe a set of possible attributes
   *
   * <p>For instance, supposed a library provides several functions that might be useful. This would
   * allow listing them. This is a restricted version of {@link #any()}
   *
   * @param names the possible attribute names
   */
  public final SpecialLocation choose(String... names) {
    final var location =
        Stream.of(names).collect(Collectors.joining("” or “", "one of “", "” from " + toString()));
    return new SpecialLocation() {
      @Override
      public String toString() {
        return location;
      }
    };
  }

  /** Describe a template that was instantiated */
  public SpecialLocation instantiated() {
    final var location = "instantiated template from " + toString();
    return new SpecialLocation() {
      @Override
      public String toString() {
        return location;
      }
    };
  }
  /** Describe a function-like template that was called */
  public SpecialLocation invoked() {
    final var location = "return value of function-like template from " + toString();
    return new SpecialLocation() {
      @Override
      public String toString() {
        return location;
      }
    };
  }
}
