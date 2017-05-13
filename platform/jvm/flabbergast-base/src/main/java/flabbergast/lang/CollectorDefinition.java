package flabbergast.lang;

/** A function that can be invoked to collect result in fricassée <tt>Group</tt> operations */
public interface CollectorDefinition {
  /**
   * Create a partial closure of a collector definition by biding the fricassée chain
   *
   * @param definition the collector definition to bind
   * @param fricassee the fricassée chain to use
   * @return a definition with partial closure
   */
  static Definition bind(CollectorDefinition definition, Fricassee fricassee) {
    return (f, s, c) -> definition.invoke(f, s, c, fricassee);
  }
  /**
   * Begin processing the inner collection operation
   *
   * @param future the future this collector should use to await other promises and then finally
   *     complete using {@link Future#complete(Object)}
   * @param sourceReference the source of the calling code; this will include references to the
   *     fricassée operation
   * @param context the lookup context of the calling code
   * @param chain an initial fricassée node that iterates of the contents of the group to be
   *     collected
   * @return a task that will be asynchronously executed to compute the result
   */
  Runnable invoke(
      Future<Any> future, SourceReference sourceReference, Context context, Fricassee chain);
}
