package flabbergast.lang;

/**
 * The accumulator definition for an attribute in a fricassée named Accumulate/Scan-like contexts.
 *
 * <p>This is a computation that can be performed given the provided context and an existing value.
 */
public interface AccumulatorDefinition {
  /**
   * Prepare execution of the result in the provided context with a previous value
   *
   * @param future the future this definition should use to await other promises and then finally
   *     complete using {@link Future#complete(Object)}
   * @param sourceReference the source location of the caller
   * @param context the context of the caller
   * @param previous the previous value from the operation chain
   * @return a task that will be asynchronously executed to compute the result
   */
  Runnable invoke(
      Future<Accumulator> future, SourceReference sourceReference, Context context, Any previous);
}
