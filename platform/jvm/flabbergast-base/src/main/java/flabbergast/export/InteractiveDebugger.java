package flabbergast.export;

import flabbergast.lang.*;
import flabbergast.lang.Future.ErrorHandler;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Create a debugger that will manage break points and signal when a new break point has been hit
 */
public abstract class InteractiveDebugger implements Debugger {
  /**
   * An active interruption event
   *
   * <p>This is a handle for the point in the execution where a debugging breakpoint was encountered
   */
  public final class InterruptionHandle implements Consumer<Any> {
    private boolean complete;
    private final Future<Any> future;
    private final long id = idGenerator.getAndIncrement();
    private final SourceReference sourceReference;
    private final Context context;
    private final RootDefinition normalReturnValue;

    private InterruptionHandle(
        Future<Any> future,
        SourceReference sourceReference,
        Context context,
        RootDefinition normalReturnValue) {
      this.future = future;
      this.sourceReference = sourceReference;
      this.context = context;
      this.normalReturnValue = normalReturnValue;
    }

    /**
     * Try to execute the supplied task in the calling context.
     *
     * <p>If the task raises an error, the {@link #errorHandler(InterruptionHandle) error handler}
     * will be invoked.
     *
     * @param task the task to execute
     * @param consumer the output handler
     */
    public void attempt(RootDefinition task, Consumer<? super Any> consumer) {
      future.reschedule(task.launch(future.inner(consumer, errorHandler(this))));
    }

    /**
     * Return the provided result to the caller and continue
     *
     * <p>This handle will no longer be valid. Attempting to call this method multiple times will
     * result in an exception.
     *
     * @param result the value to return to the caller
     */
    @Override
    public void accept(Any result) {
      if (complete) {
        throw new IllegalStateException(
            "Attempted to complete a breakpoint that was already completed.");
      }
      points.remove(this);
      complete = true;
      future.complete(result);
    }

    /** The context of the caller */
    public Context context() {
      return context;
    }

    /** The execution trace of the caller */
    public SourceReference sourceReference() {
      return sourceReference;
    }

    /** A unique ID for this event in the scope of this debugger instance. */
    public long id() {
      return id;
    }

    /** A definition that returns the value as specified in the code */
    public RootDefinition normalReturnValue() {
      return normalReturnValue;
    }

    /** Copmute the normal return value and return it to the caller */
    public void resume() {
      future.launch(normalReturnValue, future::complete);
    }
  }

  private final AtomicLong idGenerator = new AtomicLong();
  private final Set<InterruptionHandle> points = ConcurrentHashMap.newKeySet();

  /** Get all active interruption events */
  protected final Stream<InterruptionHandle> active() {
    return points.stream();
  }

  /** Cancel all outstanding interruptions by raising an error and failing to return a value. */
  public final void clear() {
    points.forEach(point -> point.future.error(SourceReference.EMPTY, "Cancelled by debugger"));
  }

  @Override
  public final void handle(
      Future<Any> future,
      SourceReference sourceReference,
      Context context,
      RootDefinition normalReturnValue) {
    var point = new InterruptionHandle(future, sourceReference, context, normalReturnValue);
    points.add(point);
    interrupt(point);
  }

  /**
   * Create an error handler for an interruption
   *
   * <p>This may be called multiple times and the resulting error handler may never be used
   *
   * @param interruptionHandle the interruption event that initiated the computation
   */
  protected abstract ErrorHandler errorHandler(InterruptionHandle interruptionHandle);

  /**
   * Handle a new event that needs user interaction
   *
   * @param point the event that needs service
   */
  protected abstract void interrupt(InterruptionHandle point);
}
