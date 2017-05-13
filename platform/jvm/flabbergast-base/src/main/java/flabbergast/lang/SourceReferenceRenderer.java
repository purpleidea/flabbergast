package flabbergast.lang;

import java.util.function.Consumer;

/**
 * Interface to write an execution trace to output
 *
 * @param <T> a type used for creating cross-references
 */
public interface SourceReferenceRenderer<T> {
  /** Cross reference an entry that has already been displayed */
  void backReference(T reference);

  /**
   * Write an execution where the trace bifurcates
   *
   * @param terminal whether there is no caller of this entry
   * @param filename the source file of this entry
   * @param startLine the source line where the syntax for this entry starts
   * @param startColumn the source column where the syntax for this entry starts
   * @param endLine the source line where the syntax for this entry ends
   * @param endColumn the source column where the syntax for this entry ends
   * @param message a helpful description of the operation at this entry
   * @param branch the secondary branch of this trace; for instantiation and amending, this is the
   *     source template's definition history
   * @return a marker for this entry in the output
   */
  T junction(
      boolean terminal,
      String filename,
      int startLine,
      int startColumn,
      int endLine,
      int endColumn,
      String message,
      Consumer<SourceReferenceRenderer<T>> branch);

  /**
   * Write an execution trace captured from the JVM
   *
   * @param terminal whether there is no caller of this entry
   * @param frame the JVM stack frame woven into the Flabbergast trace
   * @return a marker for this entry in the output
   */
  T jvm(boolean terminal, StackWalker.StackFrame frame);

  /**
   * Write an execution trace which proceeds linearly through Flabbergast code
   *
   * @param terminal whether there is no caller of this entry
   * @param filename the source file of this entry
   * @param startLine the source line where the syntax for this entry starts
   * @param startColumn the source column where the syntax for this entry starts
   * @param endLine the source line where the syntax for this entry ends
   * @param endColumn the source column where the syntax for this entry ends
   * @param message a helpful description of the operation at this entry
   * @return a marker for this entry in the output
   */
  T normal(
      boolean terminal,
      String filename,
      int startLine,
      int startColumn,
      int endLine,
      int endColumn,
      String message);

  /**
   * Write an execution trace for a special operation
   *
   * @param terminal whether there is no caller of this entry
   * @param message a message describing the operation
   * @return a marker for this entry in the output
   */
  T special(boolean terminal, String message);

  /**
   * Write an execution where the trace bifurcates inside a special operation
   *
   * @param terminal whether there is no caller of this entry
   * @param message a helpful description of the operation at this entry
   * @param branch the secondary branch of this trace; for instantiation and amending, this is the
   *     source template's definition history
   * @return a marker for this entry in the output
   */
  T specialJunction(boolean terminal, String message, Consumer<SourceReferenceRenderer<T>> branch);
}
