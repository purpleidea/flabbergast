package flabbergast.interop;

import flabbergast.lang.*;
import flabbergast.util.Result;
import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

/** Provide introspection about the current VM */
final class HandlerCurrentInformation implements UriHandler {
  private static final Map<String, Any> INFORMATION =
      Map.ofEntries(
          entry("login", System.getProperty("user.name")),
          entry("directory", Paths.get(".").toAbsolutePath().toString()),
          Map.entry("version", Any.of(Scheduler.VERSION)),
          entry("machine/directory_separator", File.separator),
          entry("machine/name", System.getProperty("os.name")),
          entry("machine/path_separator", File.pathSeparator),
          entry("machine/line_ending", String.format("%n")),
          entry("vm/name", "JVM"),
          entry("vm/vendor", System.getProperty("java.vendor")),
          entry("vm/version", System.getProperty("java.version")));

  private static Map.Entry<String, Any> entry(String key, String value) {
    return Map.entry(key, Any.of(value));
  }

  private final Map<String, Any> information;

  HandlerCurrentInformation(Predicate<ServiceFlag> flags) {
    information = new TreeMap<>(INFORMATION);
    information.put("interactive", Any.of(flags.test(ServiceFlag.INTERACTIVE)));
    if (flags.test(ServiceFlag.SANDBOXED)) {
      information.put("login", Any.of("nobody"));
      information.put("directory", Any.of("."));
    }
  }

  @Override
  public String name() {
    return "current information";
  }

  @Override
  public int priority() {
    return 0;
  }

  @Override
  public Result<Promise<Any>> resolveUri(UriExecutor runner, URI uri) {
    return Result.of(uri)
        .filter(x -> x.getScheme().equals("current"))
        .map(URI::getSchemeSpecificPart)
        .map(information::get);
  }
}
