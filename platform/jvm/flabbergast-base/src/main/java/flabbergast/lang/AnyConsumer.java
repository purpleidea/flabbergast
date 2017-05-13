package flabbergast.lang;

/** Unbox a boxed value ({@link Any}) into the matching native type for the Flabbergast type. */
public interface AnyConsumer {

  /** Receive a <tt>Null</tt> value. */
  void accept();

  /** Receive a <tt>Bool</tt> value. */
  void accept(boolean value);

  /** Receive a <tt>Bin</tt> value. */
  void accept(byte[] value);

  /** Receive a <tt>Float</tt> value. */
  void accept(double value);

  /** Receive a <tt>Frame</tt> value. */
  void accept(Frame value);

  /** Receive an <tt>Int</tt> value. */
  void accept(long value);

  /** Receive a <tt>LookupHandler</tt> value. */
  void accept(LookupHandler value);

  /** Receive a <tt>Str</tt> value. */
  void accept(Str value);

  /** Receive a <tt>Template</tt> value. */
  void accept(Template value);
}
