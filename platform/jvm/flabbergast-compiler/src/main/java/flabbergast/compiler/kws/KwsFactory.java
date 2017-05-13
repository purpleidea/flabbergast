package flabbergast.compiler.kws;

import flabbergast.compiler.SourceLocation;
import java.util.stream.Stream;

/**
 * A root for creating all the functions in a library file
 *
 * @param <V> the type of a value
 * @param <B> the type of a block
 * @param <D> the type of a dispatch
 * @param <F> the type of a function
 */
public interface KwsFactory<
    V, B extends KwsBlock<V, B, D>, D extends KwsDispatch<B, V>, F extends KwsFunction<V, B, D>> {
  /**
   * Create a fricassée accumulator function
   *
   * @param location the source location where the function is defined
   * @param name the name of the function
   * @param export whether the function should be exported into the global namespace
   * @param entryBlockName the name of the entry block
   * @param captures the type and attributes of any captures
   */
  F createAccumulator(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures);

  /**
   * Create a fricassée collector function
   *
   * @param location the source location where the function is defined
   * @param name the name of the function
   * @param export whether the function should be exported into the global namespace
   * @param entryBlockName the name of the entry block
   * @param captures the type and attributes of any captures
   */
  F createCollector(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures);
  /**
   * Create an attribute definition function
   *
   * @param location the source location where the function is defined
   * @param name the name of the function
   * @param export whether the function should be exported into the global namespace
   * @param entryBlockName the name of the entry block
   * @param captures the type and attributes of any captures
   */
  F createDefinition(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures);
  /**
   * Create a fricassée distributor function
   *
   * @param location the source location where the function is defined
   * @param name the name of the function
   * @param export whether the function should be exported into the global namespace
   * @param entryBlockName the name of the entry block
   * @param captures the type and attributes of any captures
   */
  F createDistributor(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures);

  /**
   * Create a function for the entry point in to the file
   *
   * <p>This must be called exactly once
   *
   * @param location the source location describing the file
   * @param name the name of the function (mostly irrelevant)
   * @param entryBlockName the name of the entry block
   */
  F createFile(SourceLocation location, String name, String entryBlockName);
  /**
   * Create an override attribute function
   *
   * @param location the source location where the function is defined
   * @param name the name of the function
   * @param export whether the function should be exported into the global namespace
   * @param entryBlockName the name of the entry block
   * @param captures the type and attributes of any captures
   */
  F createOverride(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures);
}
