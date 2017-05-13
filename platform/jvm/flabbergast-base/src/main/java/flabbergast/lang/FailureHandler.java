package flabbergast.lang;

import flabbergast.util.Pair;
import java.util.Optional;
import java.util.stream.Stream;

/** Receive the results after computation is finished. */
public interface FailureHandler {
  /**
   * There is a circular lookup in the program resulting in deadlock.
   *
   * <p>This may also occur during interactive sessions where a task depends on errors from a
   * previous evaluation cycle.
   */
  void deadlocked(DeadlockInformation information);

  /**
   * One of more errors occurred.
   *
   * @param errors the errors that prevented successful computation
   * @param lookupErrors the lookups in error that prevented successful computation
   */
  void error(
      Stream<Pair<SourceReference, String>> errors,
      Stream<Pair<Lookup, Optional<String>>> lookupErrors);

  /** An internal error occurred manipulating the scheduler. */
  void failed(Exception e);
}
