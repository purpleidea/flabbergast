package flabbergast.compiler.kws.codegen;

import static flabbergast.compiler.kws.codegen.LanguageType.LIBRARY_TYPE;

import flabbergast.compiler.ErrorCollector;
import flabbergast.export.Library;
import flabbergast.lang.Scheduler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

public final class JarBuilder extends JvmBuildCollector implements AutoCloseable {
  private static final String FLABBERGAST_MODULE_NAME =
      Library.class.getModule().getDescriptor().name();
  private static final String FLABBERGAST_MODULE_VERSION =
      Library.class.getModule().getDescriptor().rawVersion().orElse(null);
  private static final String JVM_MODULE_NAME = Object.class.getModule().getDescriptor().name();
  private static final String JVM_MODULE_VERSION =
      Object.class.getModule().getDescriptor().rawVersion().orElse(null);

  public static String moduleName(String libraryName) {
    return "flabbergast.library." + libraryName.replace('/', '.');
  }

  private final ZipOutputStream file;
  private final ClassVisitor moduleClass;
  private final ModuleVisitor moduleVisitor;
  private Exception storedException;

  public JarBuilder(
      OutputStream outputStream,
      String libraryName,
      Instant libraryModificationTime,
      String sourceFile,
      ErrorCollector errorCollector)
      throws IOException {
    super(
        errorCollector,
        "flabbergast/library/" + libraryName + "/",
        sourceFile,
        FunctionKind.library(libraryName, libraryModificationTime));
    final var moduleName = moduleName(libraryName);
    file = new ZipOutputStream(outputStream);

    moduleClass = createClass();
    moduleClass.visit(BC_VERSION, Opcodes.ACC_MODULE, "module-info", null, null, null);
    moduleVisitor = moduleClass.visitModule(moduleName, Opcodes.ACC_SYNTHETIC, null);
    moduleVisitor.visitRequire(JVM_MODULE_NAME, Opcodes.ACC_MANDATED, JVM_MODULE_VERSION);
    moduleVisitor.visitRequire(
        FLABBERGAST_MODULE_NAME, Opcodes.ACC_SYNTHETIC, FLABBERGAST_MODULE_VERSION);

    file.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
    final var manifest =
        "Manifest-Version: 1.0\n"
            + "Archiver-Version: Flabbergast JAR Builder\n"
            + "Created-By: Flabbergast "
            + Scheduler.VERSION
            + "\n"
            + "Built-By: "
            + System.getProperty("user.name")
            + "\n"
            + "Build-Jdk: "
            + Runtime.version().toString()
            + "\n";
    file.write(manifest.getBytes(StandardCharsets.UTF_8));
  }

  private void append(String className, byte[] contents) {
    try {
      final var entry = new ZipEntry(className + ".class");
      file.putNextEntry(entry);
      file.write(contents);
    } catch (IOException e) {
      if (storedException == null) {
        storedException = e;
      } else {
        storedException.addSuppressed(e);
      }
    }
  }

  @Override
  public ClassVisitor createClass() {
    return WritingClassVisitor.create(this::append);
  }

  @Override
  protected void finishOutput() throws Exception {
    if (storedException != null) throw storedException;
    moduleVisitor.visitProvide(LIBRARY_TYPE.getInternalName(), rootType().getInternalName());
    moduleVisitor.visitEnd();
    moduleClass.visitEnd();
    file.close();
  }
}
