package flabbergast.lang;

import java.util.Comparator;
import java.util.List;

/**
 * Provides a way to convert back and forth between a boxed {@link Any} value and a corresponding
 * unboxed value for the sortable value types
 *
 * @param <T> the Java type being boxed and unboxed
 */
public abstract class AnyBidiConverter<T> extends AnyConverter<T> implements Comparator<T> {

  AnyBidiConverter(
      List<TypeExpectation> allowedTypes, AnyFunction<ConversionOperation<? extends T>> function) {
    super(allowedTypes, function);
  }

  /** Convert a value to its boxed representation. */
  public abstract Any box(
      Future<?> future, SourceReference sourceReference, Context context, T value);
}
