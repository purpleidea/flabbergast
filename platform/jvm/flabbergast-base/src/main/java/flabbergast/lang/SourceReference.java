package flabbergast.lang;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Description of the current Flabbergast execution trace.
 *
 * <p>Since the Flabbergast execution trace is utterly alien to the underlying JVM, this object
 * records it such that it can be presented to the user when needed.
 */
public abstract class SourceReference {
  /** An empty trace */
  public static final SourceReference EMPTY =
      new SourceReference() {
        @Override
        public SourceReference caller() {
          return this;
        }

        boolean isTerminal() {
          return true;
        }

        @Override
        <T> void write(SourceReferenceRenderer<T> renderer, Map<SourceReference, T> seen) {
          // Do nothing.
        }
      };

  /**
   * A trace starting from some “special” (<i>i.e.</i>, external) place
   *
   * <p>This includes the REPL and constant-defined frames
   */
  public static SourceReference root(String message) {
    return new SourceReference() {
      @Override
      public SourceReference caller() {
        return EMPTY;
      }

      @Override
      <T> void write(SourceReferenceRenderer<T> renderer, Map<SourceReference, T> seen) {
        renderer.special(true, message);
      }
    };
  }

  private SourceReference() {}

  /**
   * A linear call in an execution trace
   *
   * @param message a helpful description of the operation at this entry
   * @param filename the source file of this entry
   * @param startLine the source line where the syntax for this entry starts
   * @param startColumn the source column where the syntax for this entry starts
   * @param endLine the source line where the syntax for this entry ends
   * @param endColumn the source column where the syntax for this entry ends
   */
  public SourceReference basic(
      String message, String filename, int startLine, int startColumn, int endLine, int endColumn) {
    final var caller = this;
    return new SourceReference() {
      @Override
      public SourceReference caller() {
        return caller;
      }

      @Override
      <T> void write(SourceReferenceRenderer<T> renderer, Map<SourceReference, T> seen) {
        if (seen.containsKey(this)) {
          renderer.backReference(seen.get(this));
          return;
        }
        seen.put(
            this,
            renderer.normal(
                caller.isTerminal(),
                filename,
                startLine,
                startColumn,
                endLine,
                endColumn,
                message));
        caller.write(renderer, seen);
      }
    };
  }

  /** Get the immediately calling source reference. */
  public abstract SourceReference caller();

  /**
   * Creates a Flabbergast reference from the JVM stack trace
   *
   * <p>To use this function, invoke it with the base Flabbergast source reference you want to graft
   * the JVM stack onto and a callback whose stack you want to capture. When the supplied supplier
   * is invoked, a new source reference will be created with all the frames in the JVM stack within
   * the callback will be added on top of the Flabbergast trace. The supplier may be used multiple
   * times and will produce a new stack for each point it is called on.
   *
   * <p>Calling the supplier after the function has returned will result in an exception. This
   * function can be safely nested, though this is not recommended since it will put a lot of
   * internal state in the Flabbergast trace..
   */
  public <T> T capture(Function<Supplier<SourceReference>, T> function) {
    final var state = new AtomicReference<>(this);
    final var depth = StackWalker.getInstance().walk(Stream::count);
    try {
      return function.apply(
          () ->
              StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                  .walk(
                      s -> {
                        final var caller = state.get();
                        if (caller == null) {
                          throw new IllegalStateException(
                              "Stack capturing callback invoked after context has been cleaned up.");
                        }
                        final var frames =
                            s.skip(1)
                                .collect(Collectors.toList())
                                .subList(0, Math.toIntExact(depth));
                        if (frames.isEmpty()) {
                          return this;
                        }
                        return new SourceReference() {
                          @Override
                          public SourceReference caller() {
                            return caller;
                          }

                          <S> void write(
                              SourceReferenceRenderer<S> renderer, Map<SourceReference, S> seen) {
                            if (seen.containsKey(this)) {
                              renderer.backReference(seen.get(this));
                              return;
                            }
                            for (var i = 0; i < frames.size(); i++) {
                              final var result =
                                  renderer.jvm(
                                      i == frames.size() - 1 && caller.isTerminal(), frames.get(i));
                              if (i == 0) {
                                seen.put(this, result);
                              }
                            }
                            caller.write(renderer, seen);
                          }
                        };
                      }));

    } finally {
      state.set(null);
    }
  }

  boolean isTerminal() {
    return false;
  }

  /**
   * An execution trace at the point of bifurcation
   *
   * @param message a helpful description of the operation at this entry
   * @param filename the source file of this entry
   * @param startLine the source line where the syntax for this entry starts
   * @param startColumn the source column where the syntax for this entry starts
   * @param endLine the source line where the syntax for this entry ends
   * @param endColumn the source column where the syntax for this entry ends
   * @param branch the secondary branch of this trace; for instantiation and amending, this is the
   *     source template's definition history
   */
  public SourceReference junction(
      String message,
      String filename,
      int startLine,
      int startColumn,
      int endLine,
      int endColumn,
      SourceReference branch) {
    final var caller = this;
    return new SourceReference() {
      @Override
      public SourceReference caller() {
        return caller;
      }

      @Override
      <T> void write(SourceReferenceRenderer<T> renderer, Map<SourceReference, T> seen) {
        if (seen.containsKey(this)) {
          renderer.backReference(seen.get(this));
          return;
        }
        seen.put(
            this,
            renderer.junction(
                caller.isTerminal(),
                filename,
                startLine,
                startColumn,
                endLine,
                endColumn,
                message,
                v -> branch.write(v, seen)));
        caller.write(renderer, seen);
      }
    };
  }

  /**
   * Write an execution trace to a file or console
   *
   * @param writer the text device to write to
   */
  public final void print(PrintWriter writer) {
    print(writer, "");
  }

  /**
   * Write an execution trace to a file or console
   *
   * @param writer the text device to write to
   * @param prefix a prefix to put at the beginning of every line
   */
  public final void print(PrintWriter writer, String prefix) {
    write(new ConsoleSourceReferenceRenderer(writer, new AtomicInteger(0), prefix));
  }

  /**
   * An execution trace for an entry that is in non-Flabbergast code
   *
   * <p>This is typically used when a new frame or template is generated by Java code that uses the
   * Flabbergast context
   *
   * @param message a helpful message about the location of the code
   */
  public SourceReference special(String message) {
    final var caller = this;
    return new SourceReference() {
      @Override
      public SourceReference caller() {
        return caller;
      }

      @Override
      <T> void write(SourceReferenceRenderer<T> renderer, Map<SourceReference, T> seen) {
        if (seen.containsKey(this)) {
          renderer.backReference(seen.get(this));
          return;
        }
        seen.put(this, renderer.special(caller.isTerminal(), message));
        caller.write(renderer, seen);
      }
    };
  }

  /**
   * An execution trace at the point of bifurcation inside a special operation
   *
   * @param message a helpful description of the operation at this entry
   * @param branch the secondary branch of this trace; for instantiation and amending, this is the
   *     source template's definition history
   */
  public SourceReference specialJunction(String message, SourceReference branch) {
    final var caller = this;
    return new SourceReference() {
      @Override
      public SourceReference caller() {
        return caller;
      }

      @Override
      <T> void write(SourceReferenceRenderer<T> renderer, Map<SourceReference, T> seen) {
        if (seen.containsKey(this)) {
          renderer.backReference(seen.get(this));
          return;
        }
        seen.put(
            this,
            renderer.specialJunction(caller.isTerminal(), message, v -> branch.write(v, seen)));
        caller.write(renderer, seen);
      }
    };
  }

  abstract <T> void write(SourceReferenceRenderer<T> renderer, Map<SourceReference, T> seen);

  /** Write the current stack trace. */
  public final <T> void write(SourceReferenceRenderer<T> renderer) {
    write(renderer, new HashMap<>());
  }
}
