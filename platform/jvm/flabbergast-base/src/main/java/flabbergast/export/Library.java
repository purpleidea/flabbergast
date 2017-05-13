package flabbergast.export;

import flabbergast.lang.RootDefinition;
import java.time.Instant;

/** Base class for precompiled libraries created by the compiler */
public abstract class Library implements RootDefinition {
  private final String name;
  private final Instant timestamp;

  /**
   * Create a new instance of a pre-compiled library
   *
   * @param timestamp the modification time of the source file used to compile this library
   * @param name the trailing part of the URI for this library
   */
  public Library(long timestamp, String name) {
    this.timestamp = Instant.ofEpochMilli(timestamp);
    this.name = name;
  }

  /**
   * The trailing part of URI of the library as used in a <tt>From lib:</tt> expression
   * (<i>e.g.</i>, <tt>utils</tt>)
   */
  public final String name() {
    return name;
  }

  /** The last modification timestamp of the source file used for compilation */
  public final Instant timestamp() {
    return timestamp;
  }
}
