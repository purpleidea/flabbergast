package flabbergast.util;

/** Represents a predicate (boolean-valued function) of two long arguments. */
public interface LongBiPredicate {
  /**
   * Evaluates this predicate on the given arguments.
   *
   * @param left the first argument
   * @param right the second argument
   * @return true if the input arguments match the predicate, otherwise false
   */
  boolean test(long left, long right);
}
