package flabbergast.util;

import java.util.function.Predicate;

/** Identical to {@link Predicate}, but can throw an exception */
public interface WhinyPredicate<T> {
  /**
   * Check if the input satisfies the predicate
   *
   * @param arg the input to be checked
   */
  boolean test(T arg) throws Exception;
}
