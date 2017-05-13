package flabbergast.lang;

/** A handler for the output of a step in a lookup operation */
public interface LookupNextOperation extends LookupLastOperation {
  /**
   * Indicate that this column has reached a non-critical failure where it should be discarded and
   * the next column evaluated.
   */
  void next();
}
