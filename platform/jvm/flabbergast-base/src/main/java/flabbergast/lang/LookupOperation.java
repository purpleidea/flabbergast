package flabbergast.lang;

/**
 * A supplier that also includes some description of the value being produced
 *
 * @param <T> the type of the results being returned by this supplier
 */
public interface LookupOperation<T> {
  /**
   * Create a supplier which produces the same value every time
   *
   * @param description the description to be provided
   * @param value a constant value to be provided
   * @param <T> the type of the results being returned by this supplier
   */
  static <T> LookupOperation<T> of(String description, T value) {
    return new LookupOperation<>() {
      @Override
      public String description() {
        return description;
      }

      @Override
      public T start(Future<?> future, SourceReference sourceReference, Context context) {
        return value;
      }
    };
  }

  /** A human-readable description of this operation */
  String description();

  /**
   * Create a new instance of this lookup operation
   *
   * @param future the future in which the lookup is happening
   * @param sourceReference the caller of the lookup
   * @param context the context in which the lookup is searching
   */
  T start(Future<?> future, SourceReference sourceReference, Context context);
}
