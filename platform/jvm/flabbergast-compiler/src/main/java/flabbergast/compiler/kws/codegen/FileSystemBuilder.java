package flabbergast.compiler.kws.codegen;

import flabbergast.compiler.ErrorCollector;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.objectweb.asm.ClassVisitor;

public final class FileSystemBuilder extends JvmBuildCollector implements AutoCloseable {

  private final Path outputDirectory;
  private Exception storedException;

  public FileSystemBuilder(
      Path outputDirectory,
      String libraryName,
      Instant libraryModificationTime,
      String sourceFile,
      ErrorCollector errorCollector) {
    super(
        errorCollector,
        "flabbergast/library/" + libraryName + "/",
        sourceFile,
        FunctionKind.library(libraryName, libraryModificationTime));
    this.outputDirectory = outputDirectory;
  }

  private void append(String className, byte[] contents) {
    try {
      Files.write(
          outputDirectory.resolve(className.replace('/', File.separatorChar) + ".class"), contents);
    } catch (IOException e) {
      if (storedException == null) {
        storedException = e;
      } else {
        storedException.addSuppressed(e);
      }
    }
  }

  @Override
  protected void finishOutput() throws Exception {
    if (storedException != null) throw storedException;
  }

  @Override
  public ClassVisitor createClass() {
    return WritingClassVisitor.create(this::append);
  }
}
