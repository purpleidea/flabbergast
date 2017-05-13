package flabbergast.lang;

import java.util.stream.Stream;

/** Interface for REPL to implement so that compiled commands can use it */
public interface InteractiveState extends FailureHandler {
  /** Get the currently selected frame used as the basis for most interaction */
  Frame current();

  /**
   * Set the currently selected frame
   *
   * <p>This should not affect {@link #root()}
   *
   * @param currentFrame the frame to use
   */
  void current(Frame currentFrame);

  /**
   * Report that an error occurred with the last command
   *
   * <p>This is unique to commands, so it has no source context
   *
   * @param message the error message
   */
  void error(String message);

  /** The user no longer wishes to continue this session */
  void quit();

  /**
   * The original frame for this interactive session
   *
   * <p>Normally, this is the frame associated with the script being evaluated or {@link
   * Frame#EMPTY}
   */
  Frame root();

  /**
   * Display a result value
   *
   * @param value the value to display
   */
  void show(Any value);

  /**
   * List a collection of names based on the current context
   *
   * @param names the names
   */
  void showNames(Stream<Name> names);

  /**
   * Display an execution trace
   *
   * @param source the trace to display
   */
  void showTrace(SourceReference source);
}
