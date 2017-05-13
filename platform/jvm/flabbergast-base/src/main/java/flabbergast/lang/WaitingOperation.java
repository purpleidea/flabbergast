package flabbergast.lang;

/**
 * Describes a non-lookup operation that is waiting for a value to be available.
 *
 * <p>These may participate in deadlocked evaluation cycles.
 */
public interface WaitingOperation {
  /** A human-friendly description of the operation */
  String description();

  /** The location in the source where the operation originated */
  SourceReference source();
}
