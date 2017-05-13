package flabbergast.compiler.cli;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.kws.codegen.JarBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class BuildJarCache {
  private static final Pattern VALID_FILE_NAME = Pattern.compile("^\\p{Alnum}*.o_0$");
  private static final Pattern VALID_NAME = Pattern.compile("^\\p{Alnum}*$");
  private final ErrorCollector errorCollector;

  protected BuildJarCache(ErrorCollector errorCollector) {
    this.errorCollector = errorCollector;
  }

  protected abstract void badFile(Path f);

  public boolean process(Path root) throws IOException {
    Set<Path> existingJars;

    try (final var files = Files.walk(root, 1, FileVisitOption.FOLLOW_LINKS)) {
      existingJars =
          files
              .filter(f -> f.getFileName().toString().endsWith(".jar"))
              .collect(Collectors.toSet());
    }
    try (final var files = Files.walk(root, FileVisitOption.FOLLOW_LINKS)) {
      boolean buildOk =
          files
              .filter(Files::isRegularFile)
              .reduce(
                  true,
                  (ok, f) -> {
                    for (final var component : f.subpath(0, f.getNameCount() - 1)) {
                      if (!VALID_NAME.matcher(component.toString()).matches()) {
                        badFile(f);
                        return false;
                      }
                    }
                    if (!VALID_FILE_NAME.matcher(f.getFileName().toString()).matches()) {
                      badFile(f);
                      return false;
                    }
                    final var libraryName =
                        f.relativize(root)
                            .toString()
                            .replaceAll("\\.o_0$", "")
                            .replace(File.separatorChar, '/');
                    final var jarFile = root.resolve(JarBuilder.moduleName(libraryName) + ".jar");
                    existingJars.remove(jarFile);
                    try {
                      if (Files.exists(jarFile)
                          && Files.getLastModifiedTime(f)
                                  .compareTo(Files.getLastModifiedTime(jarFile))
                              < 1) {
                        return ok;
                      }

                      try (final var collector =
                          new JarBuilder(
                              root,
                              libraryName,
                              Files.getLastModifiedTime(f).toInstant(),
                              f.toString(),
                              errorCollector)) {
                        // TODO compile
                        return ok;
                      }
                    } catch (Exception e) {
                      e.printStackTrace();
                      return false;
                    }
                  },
                  (a, b) -> a && b);
      for (final var existingJar : existingJars) {
        Files.delete(existingJar);
      }
      return buildOk;
    }
  }
}
