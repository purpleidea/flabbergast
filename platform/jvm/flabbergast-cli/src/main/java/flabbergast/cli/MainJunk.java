package flabbergast.cli;

import flabbergast.compiler.*;
import flabbergast.compiler.cli.BuildJarCache;
import flabbergast.export.PrintNormalOutput;
import flabbergast.lang.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.cli.*;

public class MainJunk {
  public static void main(String[] args) {
    final var options = new Options();
    options.addOption("C", "rebuild-cache", true, "Rebuild a precompiled libraries from sources.");
    options.addOption(
        "i",
        "interactive",
        true,
        "Provide an interactive prompt (after running a script, if provided).");
    options.addOption("o", "output", true, "Write output to file instead of standard output.");
    options.addOption("p", "no-precomp", false, "Do not use precompiled libraries.");
    options.addOption("s", "sandbox", false, "Do not allow network/disk access.");
    options.addOption("h", "help", false, "Show this message and exit.");
    final CommandLineParser argParser = new DefaultParser();
    CommandLine result;

    try {
      result = argParser.parse(options, args);
    } catch (final ParseException e) {
      System.err.println(e.getMessage());
      System.exit(1);
      return;
    }

    if (result.hasOption('h')) {
      final var formatter = new HelpFormatter();
      System.err.println("Run a Flabbergast file and display the “value” attribute.");
      formatter.printHelp("gnu", options);
      System.exit(1);
    }

    if (result.hasOption('C')) {
      if (Stream.of('i', 'o', 'p', 's').anyMatch(result::hasOption)) {
        System.err.println("Cache rebuild is incompatible with other options.");
        System.exit(1);
        return;
      }
      BuildJarCache cacheBuilder =
          new BuildJarCache(ErrorCollector.toStandardError()) {
            @Override
            protected void badFile(Path f) {
              System.err.printf("Unable to rebuild file: %s\n", f);
            }
          };
      for (var pathName : result.getArgList().isEmpty() ? List.of(".") : result.getArgList()) {
        var path = Paths.get(pathName);
        if (Files.isDirectory(path)) {
          try {
            if (!cacheBuilder.process(path)) {
              System.err.printf("Compilation failure: %s\n", path);
            }
          } catch (IOException e) {
            System.err.printf("Failed to process directory %s: %s\n", pathName, e.getMessage());
          }
        }
      }
    }

    final var rules = EnumSet.noneOf(ServiceFlag.class);
    final boolean interactive = result.hasOption('i');
    if (interactive) {
      rules.add(ServiceFlag.INTERACTIVE);
    }
    if (result.hasOption('p')) {
      rules.add(ServiceFlag.PRECOMPILED);
    }
    if (result.hasOption('s')) {
      rules.add(ServiceFlag.SANDBOXED);
    }
    final String[] files = result.getArgs();
    if (files.length > 1) {
      System.err.println("No more than one Flabbergast script must be given.");
      System.exit(1);
      return;
    }
    final var root = new Ptr<Frame>(Frame.EMPTY);
    if (!interactive && files.length == 0) {
      System.err.println("One Flabbergast script must be given.");
      System.exit(1);
      return;
    }

    var relevantPath =
        files.length > 0 ? Paths.get(files[0]).toAbsolutePath().getParent() : Paths.get(".");
    final var resourceFinder =
        new ResourcePathFinder(
            Stream.concat(
                Stream.of(relevantPath.resolve("lib")), ResourcePathFinder.defaultPaths()));

    final var compilerLoader =
        new DynamicCompiler(SourceFormat.FLABBERGAST, ErrorCollector.toStandardError());
    compilerLoader.setFinder(resourceFinder);

    final Scheduler scheduler =
        Scheduler.builder()
            .addUriHandler(compilerLoader)
            .addAllUriHandlers(resourceFinder, rules)
            .build();
    final Compiler compiler =
        SourceFormat.find(files[0])
            .optionalMap(format -> Compiler.find(format, TargetFormat.JVM))
            .orElseThrow("Cannot find compiler for this file.");
    final PrintNormalOutput printer =
        result.hasOption('o')
            ? new PrintNormalOutput.ToFile(result.getOptionValue('o'))
            : new PrintNormalOutput.ToStandardOut();
    final var launchable = new Ptr<RootDefinition>();
    compiler
        .compile(Paths.get(files[0]))
        .collect(new Instantiator(ErrorCollector.toStandardError(), launchable::set));
    scheduler.run(
        launchable.get(),
        new ConsoleFailureHandler() {

          @Override
          protected void print(String str) {
            System.err.print(str);
          }

          @Override
          protected void println(String str) {
            System.err.println(str);
          }

          @Override
          public void succeeded(Any result) {
            result.accept(printer);
          }
        });
    System.exit(printer.successful() ? 0 : 1);
  }
}
