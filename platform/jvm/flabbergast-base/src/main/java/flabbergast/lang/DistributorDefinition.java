package flabbergast.lang;

/**
 * The definition for flattening operation inside a fricassée
 *
 * <p>This is a computation that can be performed given the provided context
 */
public interface DistributorDefinition {

  /**
   * Prepare execution of the result in the provided context
   *
   * @param future the future this definition should use to await other promises and then finally
   *     complete using {@link Future#complete(Object)} with the fricassee chain that will be
   *     flattened
   * @param sourceReference the source location of the caller
   * @param context the context of the caller
   * @return a task that will be asynchronously executed to compute the result
   */
  Runnable invoke(Future<Fricassee> future, SourceReference sourceReference, Context context);
}
