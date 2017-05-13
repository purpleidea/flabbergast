package flabbergast.cli;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.LanguageGrammar;
import flabbergast.compiler.Program;
import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.kws.KwsRenderer;
import flabbergast.export.PrintNormalOutput;
import flabbergast.lang.*;
import flabbergast.util.Pair;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import picocli.CommandLine;

@CommandLine.Command(
    name = "print",
    description =
        "Evaluates a Flabbergast script and allows interactive command evaluation and debugging",
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class)
public class MainPrinter implements Callable<Integer> {
  private static class PrintResultHandler extends ConsoleFailureHandler implements TaskResult {
    private final Consumer<Any> showResult;
    private final Consumer<ConsoleInteractiveState> state;

    private PrintResultHandler(Consumer<ConsoleInteractiveState> state, Consumer<Any> showResult) {
      this.state = state;
      this.showResult = showResult;
    }

    @Override
    public void succeeded(Any result) {
      showResult.accept(result);
      result.accept(
          new WhinyAnyConsumer() {
            @Override
            public void accept(Frame value) {
              state.accept(new ConsoleInteractiveState(Frame.EMPTY, Optional.empty()));
            }

            @Override
            protected void fail(String type) {
              System.err.println(
                  String.format("Got %s from script. This shouldn't be possible.", type));
            }
          });
    }

    @Override
    protected PrintWriter writer() {
      return new PrintWriter(System.err);
    }
  }

  @CommandLine.Option(
      names = {"-i", "--interactive"},
      description =
          "Open a shell after evaluating the input script; this is the default if no script is provided")
  public boolean interactive;

  @CommandLine.Option(
      names = {"-o", "--output"},
      description = "Write output to file instead of standard output.")
  public Path output;

  @CommandLine.Option(
      names = {"-s", "--sandbox"},
      description = "Do not allow network/disk access")
  public boolean sandboxed;

  @CommandLine.Parameters(description = "The script to evaluate", arity = "0..1")
  public Path script;

  public Integer call() {
    final var taskMaster =
        Scheduler.builder()
            .add(
                (script != null ? script.toAbsolutePath().getParent() : Paths.get("."))
                    .resolve("lib"))
            .defaultPaths()
            .defaultUriServices()
            .rule(ServiceFlag.INTERACTIVE, interactive)
            .rule(ServiceFlag.SANDBOXED, sandboxed)
            .build();
    if (script == null && output != null) {
      System.err.println("Output file specified, but no script to generate output. Ignoring.");
    }
    var exitCode = new AtomicInteger();
    try {

      final var state =
          new AtomicReference<>(new ConsoleInteractiveState(Frame.EMPTY, Optional.empty()));
      if (script != null) {
        final ErrorCollector collector =
            new ErrorCollector() {
              @Override
              public void emitConflict(
                  String error, Stream<Pair<SourceLocation, String>> locations) {
                System.err.println(error);
                locations.forEach(
                    pair -> System.err.printf("\t%s: %s\n", pair.first(), pair.second()));
              }

              @Override
              public void emitError(SourceLocation location, String error) {
                System.err.printf("%s: %s\n", location, error);
              }
            };
        final var printer =
            output == null
                ? PrintNormalOutput.TO_STANDARD_OUTPUT
                : PrintNormalOutput.toFile(output, System.err::println);

        Program.compile(
                LanguageGrammar.automatic(),
                Files.readString(script, StandardCharsets.UTF_8),
                script.toString(),
                collector)
            .finish()
            .ifPresent(
                renderer -> {
                  try {
                    KwsRenderer.instantiate(
                        renderer,
                        root ->
                            taskMaster.run(
                                root,
                                new PrintResultHandler(
                                    state::set,
                                    result -> {
                                      printer.accept(result);
                                      exitCode.set(printer.successful() ? 0 : 1);
                                    })),
                        script.toString(),
                        collector);
                  } catch (Exception e) {
                    e.printStackTrace();
                    exitCode.set(1);
                  }
                });
      }
      if (interactive || script == null) {
        // TODO REPL
      }
    } catch (Exception e) {
      e.printStackTrace();
      exitCode.set(1);
    }
    return exitCode.get();
  }
}
