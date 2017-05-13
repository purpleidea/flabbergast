package flabbergast.compiler.kws;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.kws.api.CorrectFactory;
import flabbergast.compiler.kws.codegen.*;
import flabbergast.compiler.kws.text.TextFactory;
import flabbergast.lang.Definition;
import flabbergast.lang.RootDefinition;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * An interface implemented by a class that wishes to emit bytecode
 *
 * <p>There are multiple implementations of the KWS bytecode compiler: an API usage checker, a JVM
 * bytecode generator, and a text dump generator. Implementors of this class can be fed into any of
 * these processes
 */
public interface KwsRenderer {
  /**
   * Check whether the client is using the API correctly
   *
   * <p>This mostly checks that values which are connected to functions are used in the appropriate
   * functions and that all blocks are terminated
   *
   * @param render the client to test
   * @param collector a sink for the errors to go to
   * @param isDefinition if true, the produced type is a definition rather than a root definition
   */
  static void checkApiUsage(KwsRenderer render, ErrorCollector collector, boolean isDefinition) {
    render.render(new CorrectFactory(collector, isDefinition));
  }

  /**
   * Produces a human-readable representation of the bytecode
   *
   * <p>This does no type checking or flow analysis, so the resulting bytecode maybe incorrect.
   *
   * @param render the client to pretty print
   * @param isDefinition if true, the produced type is a definition rather than a root definition
   * @param output a consumer of individual lines of output
   */
  static void prettyPrint(KwsRenderer render, boolean isDefinition, Consumer<String> output) {
    render.render(new TextFactory(isDefinition, output));
  }

  /**
   * Produce a JAR file with a JVM-compatible equivalent of the KWS bytecode for a library
   *
   * @param render client to convert
   * @param outputDirectory a directory to place the bytecode in
   * @param libraryName the Flabbergast name for the library being compiled
   * @param libraryModificationTime the last modification time of the source
   * @param sourceFile the path to the source (for debugging purposes)
   * @param errorCollector a sink for the errors to go to
   */
  static void generateJar(
      KwsRenderer render,
      Path outputDirectory,
      String libraryName,
      Instant libraryModificationTime,
      String sourceFile,
      ErrorCollector errorCollector)
      throws Exception {
    try (final var builder =
        new JarBuilder(
            Files.newOutputStream(
                outputDirectory.resolve(JarBuilder.moduleName(libraryName) + ".jar")),
            libraryName,
            libraryModificationTime,
            sourceFile,
            errorCollector)) {
      render.render(builder);
    }
  }
  /**
   * Produce a JAR file with a JVM-compatible equivalent of the KWS bytecode for a library
   *
   * @param render client to convert
   * @param libraryName the Flabbergast name for the library being compiled
   * @param libraryModificationTime the last modification time of the source
   * @param sourceFile the path to the source (for debugging purposes)
   * @param errorCollector a sink for the errors to go to
   */
  static byte[] generateJar(
      KwsRenderer render,
      String libraryName,
      Instant libraryModificationTime,
      String sourceFile,
      ErrorCollector errorCollector)
      throws Exception {
    final var output = new ByteArrayOutputStream();
    try (final var builder =
        new JarBuilder(output, libraryName, libraryModificationTime, sourceFile, errorCollector)) {
      render.render(builder);
    }
    return output.toByteArray();
  }

  /**
   * Produce a directory with a JVM-compatible <tt>.class</tt> files equivalent of the KWS bytecode
   * for a library
   *
   * @param renderer client to convert
   * @param outputDirectory a directory to place the bytecode in
   * @param libraryName the Flabbergast name for the library being compiled
   * @param libraryModificationTime the last modification time of the source
   * @param sourceFile the path to the source (for debugging purposes)
   * @param errorCollector a sink for the errors to go to
   */
  static void generateDirectory(
      KwsRenderer renderer,
      Path outputDirectory,
      String libraryName,
      Instant libraryModificationTime,
      String sourceFile,
      ErrorCollector errorCollector) {
    renderer.render(
        new FileSystemBuilder(
            outputDirectory, libraryName, libraryModificationTime, sourceFile, errorCollector));
  }

  /**
   * Create an object for the KWS bytecode that produces a root definition
   *
   * @param render client to convert
   * @param consumer the callback to invoke with the constructed object
   * @param sourceFile the path to the source (for debugging purposes)
   * @param collector a sink for the errors to go to
   */
  static void instantiate(
      KwsRenderer render,
      Consumer<? super RootDefinition> consumer,
      String sourceFile,
      ErrorCollector collector)
      throws Exception {
    try (final var builder =
        LoadingBuilder.instantiating(
            RootDefinition.class, collector, sourceFile, consumer, FunctionKind.ROOT_DEFINITION)) {
      render.render(builder);
    }
  }
  /**
   * Create an object for the KWS bytecode that produces a definition
   *
   * @param render client to convert
   * @param consumer the callback to invoke with the constructed object
   * @param sourceFile the path to the source (for debugging purposes)
   * @param collector a sink for the errors to go to
   */
  static void instantiateDefinition(
      KwsRenderer render,
      Consumer<? super Definition> consumer,
      String sourceFile,
      ErrorCollector collector)
      throws Exception {
    try (final var builder =
        LoadingBuilder.instantiating(
            Definition.class, collector, sourceFile, consumer, StandardFunctionKind.DEFINITION)) {
      render.render(builder);
    }
  }

  /**
   * Generate bytecode for this client
   *
   * <p>The client should be able to generate the same bytecode multiple times (<i>i.e.</i>, calling
   * this method should be repeatable)
   *
   * @param factory the factory to create bytecode
   * @param <V> the type of a value
   * @param <B> the type of a block
   * @param <D> the type of a dispatch
   * @param <F> the type of a function
   */
  <V, B extends KwsBlock<V, B, D>, D extends KwsDispatch<B, V>, F extends KwsFunction<V, B, D>>
      void render(KwsFactory<V, B, D, F> factory);
}
