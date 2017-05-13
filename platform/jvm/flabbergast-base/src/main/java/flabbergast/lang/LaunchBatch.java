package flabbergast.lang;

/** Starts a collection of tasks simultaneously */
abstract class LaunchBatch {
  /** Create a new batch */
  LaunchBatch() {}
  /** Start all tasks that have been previously added via {@link #launch(RootDefinition)} */
  public abstract void execute();

  /**
   * Create a promise for a task, but do not launch it.
   *
   * <p>The task will be queued, but not started until {@link #execute()} is invoked. If this method
   * is called after, the task will be launched immediately.
   *
   * @param definition the task to be launched
   * @return a promise for the unstarted task
   */
  public abstract Promise<Any> launch(RootDefinition definition);

  /** The future that owns this batch. */
  public abstract Future<?> owner();
}
