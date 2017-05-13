package flabbergast.compiler.kws;

import java.util.stream.Stream;

/**
 * A KWS function being built
 *
 * @param <V> the type of a value
 * @param <B> the type of a block
 * @param <D> the type of a dispatch
 */
public interface KwsFunction<V, B extends KwsBlock<V, B, D>, D extends KwsDispatch<B, V>> {
  /**
   * Generate a call to this function
   *
   * @param block the target block in which the call will take place
   * @param captures the values for the captures to this function; this must match the number and
   *     type of the captures
   * @return a value holding a reference to this function with the provided captures
   */
  V access(B block, Stream<V> captures);

  /**
   * Get the value of a capture
   *
   * @param index the zero-based index of the capture
   * @return the value of this capture
   */
  V capture(int index);

  /** The number of captures this function requires */
  int captures();

  /**
   * Create a new basic block for inside the function
   *
   * @param name the name of the basic block
   * @param parameterTypes the types (and attributes) of the parameters to this block
   */
  B createBlock(String name, Stream<KwsType> parameterTypes);

  /**
   * Get the basic block first called when the function is invoked externally
   *
   * <p>The parameters of the entry block are predetermined by the function's type
   */
  B entryBlock();

  /**
   * Indicate the function is complete
   *
   * <p>This must be called to correctly emit the function output and all blocks must have reached a
   * terminal instruction.
   */
  void finish();

  /** Get the result type of the function */
  ResultType result();
}
