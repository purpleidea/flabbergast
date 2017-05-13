package flabbergast.cli;

import flabbergast.compiler.LanguageGrammar;
import flabbergast.export.PrintInspectionOutput;
import flabbergast.lang.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.history.DefaultHistory;

public final class ConsoleInteractiveState extends ConsoleFailureHandler
    implements InteractiveState {
  private Frame current;
  private final DefaultHistory history = new DefaultHistory();
  private int lineNumber;
  private final LineReader reader;
  private final Frame root;
  private boolean running = true;

  public ConsoleInteractiveState(Frame root, Optional<Path> scriptPath) {
    this.root = root;
    current = root;
    reader =
        LineReaderBuilder.builder()
            .appName("Flabbergast")
            .history(history)
            .completer(
                new AggregateCompleter(
                    Arrays.asList(
                        new StringsCompleter(
                            LanguageGrammar.automatic().keywords().collect(Collectors.toList())),
                        new AttributeCompleter(this::current))))
            .build();
  }

  @Override
  public Frame current() {
    return current;
  }

  @Override
  public void current(Frame currentFrame) {
    current = currentFrame;
  }

  @Override
  public void error(String message) {
    reader.printAbove(message);
  }

  private void printColumns(List<String> collection) {
    final var screenWidth = reader.getTerminal().getWidth();
    final var maxWidth = collection.stream().mapToInt(String::length).max().orElse(0);
    if (maxWidth > screenWidth) {
      collection.forEach(reader.getTerminal().writer()::println);
      return;
    }
    final var columns = screenWidth / (maxWidth + 2);
    final var rows = collection.size() / columns + 1;
    for (var r = 0; r < rows; r++) {
      for (var c = 0; c < columns; c++) {
        final var index = r + c * rows;
        if (index < collection.size()) {
          final var s = collection.get(index);
          reader.getTerminal().writer().print(s);
          for (var space = maxWidth + 2 - s.length(); space >= 0; space--) {
            reader.getTerminal().writer().print(' ');
          }
        }
      }
      reader.getTerminal().writer().println();
    }
  }

  @Override
  public void quit() {
    running = false;
  }

  @Override
  public Frame root() {
    return root;
  }

  public void run() throws IOException {
    while (running) {
      compiler
          .compile(
              "line" + (++lineNumber),
              new LineSource() {
                private boolean first = true;

                @Override
                public boolean hasMore() {
                  return false;
                }

                @Override
                public String pull(String outstandingKeyword) {
                  if (!first && outstandingKeyword == null) {
                    return null;
                  }
                  first = false;
                  return reader.readLine(
                      outstandingKeyword == null ? "‽ " : outstandingKeyword + "‽ ");
                }
              })
          .collect(collector);
    }
    history.save();
  }

  @Override
  public void show(Any result) {
    new PrintInspectionOutput() {
      @Override
      protected void write(String string) {
        reader.getTerminal().writer().print(string);
      }
    }.print(result);
  }

  @Override
  public void showNames(Stream<Name> names) {
    printColumns(names.map(Object::toString).collect(Collectors.toList()));
  }

  @Override
  public void showTrace(SourceReference source) {
    source.print(reader.getTerminal().writer());
  }

  @Override
  protected PrintWriter writer() {
    return reader.getTerminal().writer();
  }
}
