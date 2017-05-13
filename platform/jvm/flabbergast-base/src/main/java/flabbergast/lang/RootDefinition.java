package flabbergast.lang;

/** The definition for a whole file or another definition after closure is completed */
public interface RootDefinition {
  /**
   * Prepare execution of the result
   *
   * @param future the future this definition should use to await other promises and then finally
   *     complete using {@link Future#complete(Object)}
   */
  Runnable launch(Future<Any> future);
}
