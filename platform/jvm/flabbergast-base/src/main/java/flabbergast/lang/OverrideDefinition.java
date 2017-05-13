package flabbergast.lang;

/**
 * The override definition for an attribute in a frame.
 *
 * <p>This is a computation that can be performed given the provided context and an existing value.
 */
public interface OverrideDefinition {

  /**
   * Create a closure over an override definition
   *
   * @param sourceReference the source location of the caller
   * @param context the context of the caller
   * @param original the anonymous initial value from the underlying operation this has overridden
   */
  static RootDefinition bind(
      OverrideDefinition definition,
      SourceReference sourceReference,
      Context context,
      Any original) {
    return future -> definition.invoke(future, sourceReference, context, original);
  }

  /**
   * Create a closure over an override definition
   *
   * @param original the anonymous initial value from the underlying operation this has overridden
   */
  static Definition bind(OverrideDefinition definition, Any original) {
    return (future, sourceReference, context) ->
        definition.invoke(future, sourceReference, context, original);
  }

  /**
   * Create a closure over an override definition that discards the callers context and uses the
   * provided contest
   *
   * @param sourceReference the source location of the caller
   * @param context the context of the caller
   */
  static OverrideDefinition seal(
      OverrideDefinition definition,
      String filename,
      int startLine,
      int startColumn,
      int endLine,
      int endColumn,
      SourceReference sourceReference,
      Context context) {
    return (f, s, c, o) ->
        definition.invoke(
            f,
            s.junction(
                "sealed override definition",
                filename,
                startLine,
                startColumn,
                endLine,
                endColumn,
                sourceReference),
            context,
            o);
  }

  /**
   * Prepare execution of the result in the provided context with an initial anonymous value
   *
   * @param future the future this definition should use to await other promises and then finally
   *     complete using {@link Future#complete(Object)}
   * @param sourceReference the source location of the caller
   * @param context the context of the caller
   * @param original the anonymous initial value from the underlying operation this has overridden
   * @return a task that will be asynchronously executed to compute the result
   */
  Runnable invoke(
      Future<Any> future, SourceReference sourceReference, Context context, Any original);
}
