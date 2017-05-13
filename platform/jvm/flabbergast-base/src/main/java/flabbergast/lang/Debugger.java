package flabbergast.lang;

/** Callback to handle debugging a Flabbergast program */
public interface Debugger {
  /** The default debugger which does no debugging and returns the original value */
  Debugger RUN =
      (future, sourceReference, context, inner) -> future.launch(inner, future::complete);

  /**
   * Handle an interrupt at a breakpoint
   *
   * <p>The debugger must eventually release any future provided to it by either calling {@link
   * Future#error(SourceReference, String)} or calling {@link Future#complete(Object)}.
   *
   * @param future the future of the caller
   * @param sourceReference the source trace of the caller
   * @param context the context of the caller
   * @param normalReturnValue a task that would return the value of the expression
   */
  void handle(
      Future<Any> future,
      SourceReference sourceReference,
      Context context,
      RootDefinition normalReturnValue);
}
