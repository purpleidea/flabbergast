package flabbergast.lang;


/** Receive the results after computation is finished. */
public interface TaskResult extends FailureHandler {
  /**
   * The computation finished successfully
   *
   * @param result the result of the provided launchable
   */
  void succeeded(Any result);
}
