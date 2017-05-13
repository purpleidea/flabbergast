package flabbergast.cli;

import flabbergast.compiler.SourceLocation;
import flabbergast.util.Pair;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.cli.*;

public class MainDocumenter {
  public static void main(String[] args) {
    final var options = new Options();
    options.addOption("g", "github", true, "The URL to the GitHub version of these files.");
    options.addOption("o", "output", true, "The directory to place the docs.");
    options.addOption("h", "help", false, "Show this message and exit");
    final CommandLineParser clParser = new DefaultParser();
    final CommandLine result;

    try {
      result = clParser.parse(options, args);
    } catch (final ParseException e) {
      System.err.println(e.getMessage());
      System.exit(1);
      return;
    }

    if (result.hasOption('h')) {
      final var formatter = new HelpFormatter();
      System.err.println("Document a directory containing Flabbergast files.");
      formatter.printHelp("gnu", options);
      System.exit(1);
    }

    final String[] directories = result.getArgs();
    if (directories.length == 0) {
      System.err.println("I need some directories full of delicious source files to document.");
      System.exit(1);
      return;
    }
    final var output = Paths.get(result.getOptionValue('o', "."));
    final Compiler compiler = Compiler.find(SourceFormat.FLABBERGAST, TargetFormat.APIDOC).get();
    compiler.setGitHubUrl(result.getOptionValue('g'));
    final Set<String> brokenFiles = new HashSet<>();
    final BuildCollector collector =
        new BuildCollector() {

          @Override
          public void emitError(SourceLocation location, String error) {
            brokenFiles.add(location.getFileName());
          }

          @Override
          public void emitOutput(String fileName, byte[] data) {
            final var path =
                output.resolve(String.format("doc-%s.xml", fileName.replace('/', '-')));
            try {
              Files.createDirectories(path.getParent());
              Files.write(
                  path, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (final IOException e) {
              System.err.printf("Failed to write %s: %s\n", path, e.getMessage());
            }
          }

          @Override
          public void emitRoot(String fileName) {}

          @Override
          public void emitConflict(String error, Stream<Pair<SourceLocation, String>> locations) {
            locations.map(p -> p.first().getFileName()).forEach(brokenFiles::add);
          }
        };
    Stream.of(directories)
        .map(Paths::get)
        .flatMap(
            directory -> {
              try {
                return compiler.compile(directory, SourceFormat.FLABBERGAST.getExtension());
              } catch (final IOException e) {
                brokenFiles.add(directory.toString());
                return Stream.empty();
              }
            })
        .map(Pair::second)
        .forEach(artefact -> artefact.collect(collector));
    brokenFiles.forEach(file -> System.err.println("Failed to compile: " + file));
    System.exit(brokenFiles.isEmpty() ? 0 : 1);
  }
}
