package flabbergast.lang;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Write an execution trace to a text device */
final class ConsoleSourceReferenceRenderer implements SourceReferenceRenderer<Integer> {

  private final AtomicInteger line;
  private final String prefix;
  private final PrintWriter writer;

  ConsoleSourceReferenceRenderer(PrintWriter writer, AtomicInteger line, String prefix) {
    this.writer = writer;
    this.line = line;
    this.prefix = prefix;
  }

  @Override
  public void backReference(Integer reference) {
    writer.print(prefix);
    writer.print("┊ ");
    writer.print("Previously mentioned ");
    writer.print(line.get() - reference);
    writer.println(" lines ago");
    line.incrementAndGet();
  }

  @Override
  public Integer junction(
      boolean isTerminal,
      String filename,
      int startLine,
      int startColumn,
      int endLine,
      int endColumn,
      String message,
      Consumer<SourceReferenceRenderer<Integer>> branch) {
    writer.print(prefix);
    writer.print(isTerminal ? "└─┬ " : "├─┬ ");
    writer.print(filename);
    writer.print(": ");
    writer.print(startLine);
    writer.print(":");
    writer.print(startColumn);
    writer.print("-");
    writer.print(endLine);
    writer.print(":");
    writer.print(endColumn);
    writer.print(": ");
    writer.println(message);
    final var current = line.incrementAndGet();
    branch.accept(
        new ConsoleSourceReferenceRenderer(writer, line, prefix + (isTerminal ? "  " : "│ ")));
    return current;
  }

  @Override
  public Integer jvm(boolean isTerminal, StackWalker.StackFrame frame) {
    writer.print(prefix);
    writer.print(isTerminal ? "└ " : "├ ");

    if (frame.getFileName() != null) {
      writer.print(frame.getFileName());
      if (frame.getLineNumber() >= 0) {
        writer.print(":");
        writer.print(frame.getLineNumber());
      }
      writer.print(": ");
    }
    writer.print(frame.getClassName());
    writer.print(".");
    writer.print(frame.getMethodName());
    writer.println(frame.getMethodType());
    return line.incrementAndGet();
  }

  @Override
  public Integer normal(
      boolean isTerminal,
      String filename,
      int startLine,
      int startColumn,
      int endLine,
      int endColumn,
      String message) {
    writer.print(prefix);
    writer.print(isTerminal ? "└ " : "├ ");
    writer.print(filename);
    writer.print(": ");
    writer.print(startLine);
    writer.print(":");
    writer.print(startColumn);
    writer.print("-");
    writer.print(endLine);
    writer.print(":");
    writer.print(endColumn);
    writer.print(": ");
    writer.println(message);
    return line.incrementAndGet();
  }

  @Override
  public Integer special(boolean isTerminal, String message) {
    writer.print(prefix);
    writer.print(isTerminal ? "└ " : "├ ");
    writer.print("<");
    writer.print(message);
    writer.println(">");
    return line.incrementAndGet();
  }

  @Override
  public Integer specialJunction(
      boolean isTerminal, String message, Consumer<SourceReferenceRenderer<Integer>> branch) {
    writer.print(prefix);
    writer.print(isTerminal ? "└─┬ " : "├─┬ ");
    writer.print(" <");
    writer.println(message);
    writer.println(">");
    final var current = line.incrementAndGet();
    branch.accept(
        new ConsoleSourceReferenceRenderer(writer, line, prefix + (isTerminal ? "  " : "│ ")));
    return current;
  }
}
