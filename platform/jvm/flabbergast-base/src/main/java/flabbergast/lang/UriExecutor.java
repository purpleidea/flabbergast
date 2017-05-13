package flabbergast.lang;

/**
 * Limited execution service for {@link UriHandler} implementations
 *
 * <p>Since imports must be resolved to {@link Promise}, for any non-constant results, this
 * interface provides a way services to execute code in the running {@link Scheduler}.
 */
public interface UriExecutor {
  /**
   * Start executing a task
   *
   * @param definition the task to execute
   * @return a promise for the result of the task to be used by the caller
   */
  Promise<Any> launch(RootDefinition definition);

  /**
   * Emit an error during loading of a library.
   *
   * <p>This may be called many times.
   *
   * @param sourceReference the source trace in the called context
   * @param message an error message for the user
   */
  void error(SourceReference sourceReference, String message);
}
