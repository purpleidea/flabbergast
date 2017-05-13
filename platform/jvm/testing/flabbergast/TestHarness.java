package flabbergast;

import flabbergast.compiler.Compiler;
import flabbergast.compiler.SourceFormat;
import flabbergast.compiler.TargetFormat;
import flabbergast.compiler.support.Instantiator;
import flabbergast.interop.BuiltInLibraries;
import flabbergast.lang.Launchable;
import flabbergast.lang.TaskMaster;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class TestHarness {
  public static boolean doTest(
      Path file, boolean success, Compiler compiler, TaskMaster taskMaster) {
    final CheckResult check = new CheckResult();
    final Ptr<Launchable> launchable = new Ptr<>();
    compiler.compile(file).collect(new Instantiator(check, launchable::set));
    taskMaster.run(launchable.get(), check);
    final boolean pass = check.getSuccess() == success;
    System.err.printf(
        "%s %s %s\n", pass ? "----" : "FAIL", success ? "W" : "E", file.getFileName().toString());
    return pass;
  }

  public static void main(String[] args) {
    final TaskMaster taskMaster =
        TaskMaster.builder().addUriHandler(new BuiltInLibraries()).build();

    final Compiler compiler = Compiler.find(SourceFormat.FLABBERGAST, TargetFormat.JVM).get();
    final boolean success =
        Stream.of(Paths.get("test"), Paths.get("..", "..", "tests"))
            .flatMap(
                path ->
                    Stream.of(
                        new Pair<>(path.resolve("errors"), false),
                        new Pair<>(path.resolve("working"), false)))
            .filter(pair -> Files.exists(pair.getKey()))
            .flatMap(
                pair -> {
                  try (Stream<Path> files = Files.walk(pair.getKey())) {
                    return files.map(file -> new Pair<>(file, pair.getValue()));
                  } catch (final IOException e) {
                    return Stream.empty();
                  }
                })
            .filter(pair -> pair.getKey().endsWith(".o_0"))
            .allMatch(pair -> doTest(pair.getKey(), pair.getValue(), compiler, taskMaster));
    System.exit(success ? 0 : 1);
  }
}
