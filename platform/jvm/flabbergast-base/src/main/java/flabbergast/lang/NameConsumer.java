package flabbergast.lang;

/** Consume an attribute name, which may contain either a string or a number. */
public interface NameConsumer {
  /**
   * Consume an number.
   *
   * @param ordinal the attribute number
   */
  void accept(long ordinal);

  /**
   * Consume a string
   *
   * @param name the attribute name
   */
  void accept(String name);
}
