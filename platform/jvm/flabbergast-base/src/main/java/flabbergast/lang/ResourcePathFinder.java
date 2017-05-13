package flabbergast.lang;

import flabbergast.util.Result;
import java.io.File;
import java.lang.module.ModuleFinder;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Search locations on disk for resources or libraries. */
public final class ResourcePathFinder {
  private static final boolean IS_MACOS =
      System.getProperty("os.name").toLowerCase().contains("mac")
          || System.getProperty("os.name").toLowerCase().contains("darwin");
  private static final boolean IS_WINDOWS =
      System.getProperty("os.name").toLowerCase().contains("windows");
  private static final Pattern PATH_SEPARATOR = Pattern.compile(Pattern.quote(File.pathSeparator));

  /**
   * Get the standard search paths
   *
   * <p>This includes the directory where Flabbergast is installed, directories in the
   * <tt>FLABBERGAST_PATH</tt> environment variable, and the user's private storage directory.
   *
   * @see #environmentPaths()
   * @see #userPaths()
   * @see #selfPath()
   * @see #systemPaths()
   */
  public static Stream<Path> defaultPaths() {
    return Stream.of(environmentPaths(), userPaths(), selfPath().stream(), systemPaths())
        .flatMap(Function.identity());
  }

  /** Get all the paths specified in the <tt>FLABBERGAST_PATH</tt> environment variable */
  public static Stream<Path> environmentPaths() {
    return Result.of(System.getenv("FLABBERGAST_PATH"))
        .flatStream(PATH_SEPARATOR::splitAsStream)
        .filter(s -> !s.isBlank())
        .map(Paths::get);
  }

  /**
   * Get the path to the libraries co-installed with the executing Flabbergast instance, if
   * possible.
   */
  public static Optional<Path> selfPath() {
    try {
      return Optional.of(
          Paths.get(
              Frame.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(),
              "..",
              "..",
              "flabbergast",
              "lib"));
    } catch (final URISyntaxException e) {
      return Optional.empty();
    }
  }

  /**
   * Get Flabbergast library paths in common system locations
   *
   * <p>This may overlap with {@link #selfPath()}
   */
  static Stream<Path> systemPaths() {
    return IS_WINDOWS
        ? Stream.empty()
        : Stream.of(
            Paths.get("/usr/share/flabbergast/lib"), Paths.get("/usr/local/lib/flabbergast/lib"));
  }

  /**
   * Get all possible Flabbergast paths in a user's home directory
   *
   * <p>This considers a number of environment variables in making this decision. The best choice is
   * present first, so selecting the first one is desirable for writing values.
   */
  public static Stream<Path> userPaths() {
    return (IS_WINDOWS
            ? Stream.of(
                System.getenv("LOCALAPPDATA"),
                System.getenv("APPDATA"),
                System.getProperty("user.home")
                    + File.separator
                    + "Local Settings"
                    + File.separator
                    + "ApplicationData")
            : Stream.of(
                System.getenv("XDG_DATA_HOME"),
                IS_MACOS
                    ? System.getProperty("user.home")
                        + File.separator
                        + "Library"
                        + File.separator
                        + "flabbergast"
                    : System.getProperty("user.home")
                        + File.separator
                        + ".local"
                        + File.separator
                        + "share"
                        + File.separator
                        + "flabbergast"))
        .filter(Objects::nonNull)
        .map(root -> Paths.get(root).resolve("flabbergast"));
  }

  private final ModuleLayer layer;
  private final List<Path> paths;

  ResourcePathFinder(Stream<Path> paths) {
    this.paths = paths.collect(Collectors.toList());
    layer =
        ModuleLayer.boot()
            .defineModulesWithOneLoader(
                ModuleLayer.boot()
                    .configuration()
                    .resolveAndBind(
                        ModuleFinder.of(this.paths.toArray(Path[]::new)),
                        ModuleFinder.of(),
                        Set.of()),
                ClassLoader.getSystemClassLoader());
  }

  /**
   * Locate a file.
   *
   * @param basename the base name of the file
   * @param extensions the extensions possible for the file; at least one must be provided
   */
  public Optional<File> find(String basename, String... extensions) {
    return paths
        .stream()
        .flatMap(path -> Stream.of(extensions).map(extension -> path.resolve(basename + extension)))
        .map(Path::toFile)
        .filter(File::canRead)
        .findFirst();
  }

  /**
   * Find service loader providers using the paths specified to find services in JAR files
   *
   * @param clazz the class to be discover
   * @param <T> the type of the services to discover
   * @return all services found
   */
  public <T> Stream<ServiceLoader.Provider<T>> findProviders(Class<T> clazz) {
    return ServiceLoader.load(layer, clazz).stream();
  }
}
