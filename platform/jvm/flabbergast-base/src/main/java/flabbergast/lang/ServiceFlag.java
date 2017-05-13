package flabbergast.lang;

/** Indicate what kinds of URI handlers should be enabled. */
public enum ServiceFlag {
  /** There is a real live user we can interact with */
  INTERACTIVE,
  /**
   * Do not allow access to external resources
   *
   * <p>This includes any network activity, databases, or the file system. Resource files and
   * databases are permitted.
   */
  SANDBOXED
}
