package flabbergast.lang;

/**
 * Provide details about a cycle of deadlocked lookups
 *
 * <p>This interface will be called multiple times for each edge in a deadlock cycle. The order of
 * the method calls indicates the order of dependency on the items in the cycle. If only one method
 * is called, then the cycle is a self-reference.
 */
public interface DeadlockCycleConsumer {
  /** A lookup is detected in the cycle */
  void accept(Lookup lookup);
  /** An in-flight non-lookup operation is detected in the cycle */
  void accept(WaitingOperation waitingOperation);

  /** All items in the cycle have been visited */
  void finish();
}
