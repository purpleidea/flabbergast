package flabbergast.cli;

import flabbergast.lang.*;
import flabbergast.lang.Context.FrameAccessor;
import flabbergast.util.Pair;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.fusesource.jansi.Ansi;

/** A task output meant for interactive TTYs */
public abstract class ConsoleFailureHandler implements FailureHandler {
  protected abstract PrintWriter writer();

  @Override
  public final void deadlocked(DeadlockInformation information) {
    writer().print(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).toString());
    writer().println("Circular evaluation detected.");
    writer().print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
    final var cycleCount = new AtomicInteger();
    information.describeCycles(
        () ->
            new DeadlockCycleConsumer() {
              private final List<Lookup> lookups = new ArrayList<>();
              private final List<WaitingOperation> operations = new ArrayList<>();

              @Override
              public void accept(Lookup lookup) {
                lookups.add(lookup);
              }

              @Override
              public void accept(WaitingOperation waitingOperation) {
                operations.add(waitingOperation);
              }

              @Override
              public void finish() {
                writer().print(" === CYCLE #");
                writer().print(cycleCount.incrementAndGet());
                writer().println(" ===");

                for (final var lookup : lookups) {
                  writer()
                      .print(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).toString());
                  writer().print("Lookup for “");
                  writer()
                      .print(lookup.names().map(Object::toString).collect(Collectors.joining(".")));
                  writer().println("” blocked. Lookup initiated at:");
                  writer().print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
                  lookup.source().print(writer(), "  ");
                  writer()
                      .print(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).toString());
                  lookup
                      .last()
                      .ifPresentOrElse(
                          last -> {
                            writer().print(" is waiting for “");
                            writer().print(last.second());
                            writer().println("” in frame defined at:");
                            writer().print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
                            last.first().frame().source().print(writer(), "  ");
                          },
                          () -> {
                            writer().println("” not yet started.");
                            writer().print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
                          });
                }
                for (final var operation : operations) {
                  writer()
                      .print(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).toString());
                  writer().print(operation.description());
                  writer().println("” blocked. Initiated at:");
                  writer().print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
                  operation.source().print(writer(), "  ");
                }
              }
            });
  }

  @Override
  public final void error(
      Stream<Pair<SourceReference, String>> errors,
      Stream<Pair<Lookup, Optional<String>>> lookupErrors) {
    errors.forEach(
        Pair.consume(
            (reference, message) -> {
              writer().print(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).toString());
              writer().println(message);
              writer().print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
              reference.print(writer(), "  ");
            }));

    lookupErrors.forEach(
        Pair.consume(
            (lookup, failType) -> {
              writer().print(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).toString());
              failType.ifPresentOrElse(
                  type -> {
                    writer().print("Non-frame type ");
                    writer().print(type);
                    writer().print(" while resolving name “");
                    writer()
                        .print(
                            lookup.names().map(Object::toString).collect(Collectors.joining(".")));
                    writer().println("”. Lookup was as follows:");
                  },
                  () -> {
                    writer().print("Undefined name “");
                    writer()
                        .print(
                            lookup.names().map(Object::toString).collect(Collectors.joining(".")));
                    writer().println("”. Lookup was as follows:");
                  });
              writer().print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
              final var colWidth =
                  IntStream.concat(
                          IntStream.of((int) Math.log10(lookup.frameCount()) + 1, 3),
                          lookup.names().mapToInt(n -> n.toString().length()))
                      .max()
                      .orElse(10);
              lookup
                  .names()
                  .forEachOrdered(
                      name -> {
                        writer().print("│");
                        writer().print(pad(name.toString(), colWidth));
                      });
              writer().println("│");
              IntStream.range(0, lookup.nameCount())
                  .mapToObj(
                      i ->
                          (i == 0 ? "├" : "┼")
                              + String.join("", Collections.nCopies(colWidth, "─")))
                  .forEachOrdered(writer()::print);
              writer().println("┤");
              final var knownFrames = new HashMap<FrameAccessor, String>();
              final var frameList = new ArrayList<FrameAccessor>();
              final var nullText = pad("│ ", colWidth + 2);
              for (var frameIt = 0; frameIt < lookup.frameCount(); frameIt++) {
                for (var name = 0; name < lookup.nameCount(); name++) {
                  lookup
                      .get(name, frameIt)
                      .ifPresentOrElse(
                          frame -> {
                            if (!knownFrames.containsKey(frame)) {
                              frameList.add(frame);
                              knownFrames.put(
                                  frame, pad(Integer.toString(frameList.size()), colWidth));
                            }
                            writer().print("│");
                            writer().print(knownFrames.get(frame));
                          },
                          () -> writer().print(nullText));
                }
                writer().println("│");
              }
              writer().print(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).toString());
              writer().println("Lookup happened here:");
              writer().print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
              lookup.source().print(writer(), "  ");
              for (var it = 0; it < frameList.size(); it++) {
                writer()
                    .print(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).toString());
                writer().print("Frame ");
                writer().print(it + 1);
                writer().println(" defined:");
                writer().print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
                frameList.get(it).frame().source().print(writer(), "  ");
              }
            }));
  }

  @Override
  public final void failed(Exception e) {
    writer().print(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).toString());
    writer().print(e.getClass().getName());
    writer().print(": ");
    writer().println(e.getMessage());
    writer().print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
    for (final var stack : e.getStackTrace()) {
      writer().print("  ");
      writer().print(stack.getClassName());
      writer().print("#");
      writer().print(stack.getMethodName());
      if (stack.getLineNumber() > 0) {
        writer().print(":");
        writer().print(stack.getLineNumber());
      }
      writer().println("");
    }
  }

  private String pad(String str, int length) {
    return String.format("%1$-" + length + "s", str);
  }
}
