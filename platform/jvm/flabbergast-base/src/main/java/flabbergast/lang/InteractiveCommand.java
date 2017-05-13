package flabbergast.lang;

import flabbergast.util.Pair;
import java.util.Optional;
import java.util.stream.Stream;

/** A command that can be used from an interactive session */
public interface InteractiveCommand {
  /** Common commands that take no arguments */
  enum Standard implements InteractiveCommand {
    /** Go to the original frame */
    HOME(
        "Home", "Change the current frame (This) to the file-level frame of the script provided.") {

      @Override
      public void run(Scheduler master, InteractiveState state) {
        state.current(state.root());
      }
    },
    /** List public attribute names */
    LIST("Ls", "List the names of the attributes in the current frame (This).") {

      @Override
      public void run(Scheduler master, InteractiveState state) {
        state.showNames(state.current().names());
      }
    },
    /** List public and private names */
    LIST_PRIVATE(
        "LsAll",
        "List all the names of the attributes in the current frame (This) including private attributes.") {
      @Override
      public void run(Scheduler master, InteractiveState state) {
        state.showNames(state.current().namesPrivate());
      }
    },
    /** Quit the interactive shell */
    QUIT("Quit", "Stop with the Flabbergast.") {
      @Override
      public void run(Scheduler master, InteractiveState state) {
        state.quit();
      }
    },
    /** Display the execution trace */
    TRACE("Trace", "Print the execution trace for the current frame (This).") {
      @Override
      public void run(Scheduler master, InteractiveState state) {
        state.showTrace(state.current().source());
      }
    },
    /** Go to the container */
    UP("Up", "Change the current frame to the containing frame (Container).") {
      @Override
      public void run(Scheduler master, InteractiveState state) {
        state.current(state.current().container());
      }
    };

    private final String description;
    private final String syntax;

    Standard(String syntax, String description) {
      this.syntax = syntax;
      this.description = description;
    }

    /** A human-readable description of this command for help text or tool tips */
    public String description() {
      return description;
    }

    /** The thing the user user should type to get this command */
    public String syntax() {
      return syntax;
    }
  }

  /** A command which first needs to evaluate a Flabbergast expression */
  abstract class WithExpression implements InteractiveCommand {
    private final boolean allowPrivate;
    private final Definition definition;

    /**
     * Create a new interactive command that will evaluate an expression first
     *
     * @param allowPrivate whether this command should be allowed to see private values (true) or
     *     only public values (false)
     * @param definition the expression to evaluate
     */
    protected WithExpression(boolean allowPrivate, Definition definition) {
      this.allowPrivate = allowPrivate;
      this.definition = definition;
    }

    /**
     * Execute the command with the resulting value from the provided expression
     *
     * @param value the resulting value; it may contain any Flabbergast type
     * @param state the interactive state to manipulate
     */
    protected abstract void run(Any value, InteractiveState state);

    public final void run(Scheduler master, InteractiveState state) {
      master.run(
          future ->
              () ->
                  future.launch(
                      definition,
                      SourceReference.root("command prompt"),
                      allowPrivate
                          ? state.current().context()
                          : Context.EMPTY.forFrame(state.current()),
                      future::complete),
          new TaskResult() {

            @Override
            public void deadlocked(DeadlockInformation info) {
              state.deadlocked(info);
            }

            @Override
            public void error(
                Stream<Pair<SourceReference, String>> errors,
                Stream<Pair<Lookup, Optional<String>>> lookupErrors) {
              state.error(errors, lookupErrors);
            }

            @Override
            public void failed(Exception e) {
              state.failed(e);
            }

            @Override
            public void succeeded(Any result) {
              run(result, state);
            }
          });
    }
  }

  /**
   * Change current frame (<tt>This</tt>) to supplied frame.
   *
   * @param definition an expression to evaluate to find the frame
   */
  static InteractiveCommand go(Definition definition) {
    return new WithExpression(false, definition) {
      @Override
      protected void run(Any value, InteractiveState state) {
        value.accept(
            new WhinyAnyConsumer() {

              @Override
              public void accept(Frame value) {
                state.current(value);
              }

              @Override
              public void accept(Template value) {
                state.current(value.container());
              }

              @Override
              protected void fail(String type) {
                state.error(String.format("“Go” got %s, but expects Frame or Template.", type));
              }
            });
      }
    };
  }

  /**
   * Change the current frame (<tt>This</tt>) to some other containing frame.
   *
   * @param count the number of parent frames; if larger than the number of available parent frames,
   *     it will stop at the highest frame
   */
  static InteractiveCommand up(int count) {
    return (future, state) -> {
      var current = state.current();
      for (var c = count; c > 0; c--) {
        current = current.container();
      }
      state.current(current);
    };
  }

  /**
   * Perform an interactive command
   *
   * @param master the task master to run the command in
   * @param state the interactive state to manipulate
   */
  void run(Scheduler master, InteractiveState state);
}
