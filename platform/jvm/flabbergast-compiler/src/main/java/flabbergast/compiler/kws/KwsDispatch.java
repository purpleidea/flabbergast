package flabbergast.compiler.kws;

import flabbergast.compiler.Streamable;
import java.util.Optional;

/**
 * A dispatcher that will perform type-directed flow control based on the type in a box
 *
 * <p>Any paths not provided will result in a runtime type error in the Flabbergast program
 * generated
 *
 * @see KwsBlock#br_a(Object, KwsDispatch, Optional)
 * @param <V> the type of a value
 * @param <B> the type of a block
 */
public interface KwsDispatch<B, V> {

  /**
   * Create a path if the type is <tt>Bin</tt>
   *
   * @param target the block to transfer control to
   * @param captures any values that should be used to fill in the leading parameters to the block;
   *     the last one will be the value found in the box
   */
  void dispatchBin(B target, Streamable<V> captures);

  /**
   * Create a path if the type is <tt>Bool</tt>
   *
   * @param target the block to transfer control to
   * @param captures any values that should be used to fill in the leading parameters to the block;
   *     the last one will be the value found in the box
   */
  void dispatchBool(B target, Streamable<V> captures);

  /**
   * Create a path if the type is <tt>Float</tt>
   *
   * @param target the block to transfer control to
   * @param captures any values that should be used to fill in the leading parameters to the block;
   *     the last one will be the value found in the box
   */
  void dispatchFloat(B target, Streamable<V> captures);

  /**
   * Create a path if the type is <tt>Frame</tt>
   *
   * @param target the block to transfer control to
   * @param captures any values that should be used to fill in the leading parameters to the block;
   *     the last one will be the value found in the box
   */
  void dispatchFrame(B target, Streamable<V> captures);

  /**
   * Create a path if the type is <tt>Int</tt>
   *
   * @param target the block to transfer control to
   * @param captures any values that should be used to fill in the leading parameters to the block;
   *     the last one will be the value found in the box
   */
  void dispatchInt(B target, Streamable<V> captures);

  /**
   * Create a path if the type is <tt>LookupHandler</tt>
   *
   * @param target the block to transfer control to
   * @param captures any values that should be used to fill in the leading parameters to the block;
   *     the last one will be the value found in the box
   */
  void dispatchLookupHandler(B target, Streamable<V> captures);

  /**
   * Create a path if the type is <tt>Null</tt>
   *
   * @param target the block to transfer control to
   * @param captures any values that should be used to fill in the parameters to the block; since
   *     there no value in a null box, all arguments must be provided
   */
  void dispatchNull(B target, Streamable<V> captures);

  /**
   * Create a path if the type is <tt>Str</tt>
   *
   * @param target the block to transfer control to
   * @param captures any values that should be used to fill in the leading parameters to the block;
   *     the last one will be the value found in the box
   */
  void dispatchStr(B target, Streamable<V> captures);

  /**
   * Create a path if the type is <tt>Template</tt>
   *
   * @param target the block to transfer control to
   * @param captures any values that should be used to fill in the leading parameters to the block;
   *     the last one will be the value found in the box
   */
  void dispatchTemplate(B target, Streamable<V> captures);
}
