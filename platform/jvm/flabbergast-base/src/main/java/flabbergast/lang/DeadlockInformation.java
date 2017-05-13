package flabbergast.lang;

import java.util.function.Supplier;
import java.util.stream.Stream;

/** Provide details about a deadlocked evaluation */
public interface DeadlockInformation {
  /**
   * Explore the connection graph and describe all the cycles causing deadlock
   *
   * @param cycleConsumer a provider of callbacks; one will be instantiated for each cycle detected
   */
  void describeCycles(Supplier<? extends DeadlockCycleConsumer> cycleConsumer);

  /** All in-flight lookups */
  Stream<Lookup> lookups();

  /** All in-flight non-lookup operations */
  Stream<WaitingOperation> waitingOperations();
}
