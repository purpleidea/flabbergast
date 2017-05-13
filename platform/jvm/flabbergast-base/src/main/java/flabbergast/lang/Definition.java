package flabbergast.lang;

/**
 * The definition for an attribute in a Frame
 *
 * <p>This is a computation that can be performed given the provided context
 */
public interface Definition {
  /**
   * Create a closure over a definition
   *
   * @param definition the definition to close over
   * @param sourceReference the source location of the caller
   * @param context the context of the caller
   */
  static RootDefinition bind(
      Definition definition, SourceReference sourceReference, Context context) {
    return future -> definition.invoke(future, sourceReference, context);
  }

  /**
   * Create a definition that returns the same value every time
   *
   * @param value the value to return
   */
  static Definition constant(Any value) {
    return (f, c, s) -> () -> f.complete(value);
  }

  /** Create a definition that will always return an error. */
  static Definition error(String message) {
    return (future, sourceReference, context) -> () -> future.error(sourceReference, message);
  }

  /**
   * Create a closure over a definition that discards the callers context and uses the provided
   * contest
   *
   * @param definition the definition to close over
   * @param sourceReference the source location of the caller
   * @param context the context of the caller
   */
  static Definition seal(
      Definition definition,
      String filename,
      int startLine,
      int startColumn,
      int endLine,
      int endColumn,
      SourceReference sourceReference,
      Context context) {
    return (f, s, c) ->
        definition.invoke(
            f,
            s.junction(
                "sealed definition",
                filename,
                startLine,
                startColumn,
                endLine,
                endColumn,
                sourceReference),
            context);
  }

  /**
   * Prepare execution of the result in the provided context
   *
   * @param future the future this definition should use to await other promises and then finally
   *     complete using {@link Future#complete(Object)}
   * @param sourceReference the source location of the caller
   * @param context the context of the caller
   * @return a task that will be asynchronously executed to compute the result
   */
  Runnable invoke(Future<Any> future, SourceReference sourceReference, Context context);
}
