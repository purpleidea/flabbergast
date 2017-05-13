package flabbergast.lang;

import java.lang.invoke.MethodHandle;
import java.util.function.Consumer;

/**
 * The handler for an asynchronously evaluated computation.
 *
 * <p>Evaluating Flabbergast attribute values requires non-linear program flow due to the lookup
 * semantics. For each value to be computed, the scheduler creates a {@link Future} that serves as a
 * write-only interface and a matched {@link Promise} that serves to read the value.
 *
 * <p>The computation will be given a fresh future that it can use it signal that is finished
 * (either with a value or an error), wait on the value of other computations (via their {@link
 * Promise} instances), start new computations, and schedule work to be completed.
 */
public abstract class Future<T> {

  /** Collect errors during a debugged operation */
  public interface ErrorHandler {

    /**
     * Handle a lookup that raised a non-type error
     *
     * @param lookup the lookup that failed
     */
    void handle(Lookup lookup);
    /**
     * Handle a lookup that encountered an incorrect type
     *
     * @param lookup the lookup that failed
     * @param failType the incorrect type found
     */
    void handle(Lookup lookup, String failType);

    /**
     * Handle a non-lookup error
     *
     * @param sourceReference the source where the error occurred
     * @param message the error message
     */
    void handle(SourceReference sourceReference, String message);
  }

  private static class InnerFuture<T> extends Future<T> {
    private final Consumer<? super T> consumer;
    private final Future<?> original;

    private InnerFuture(Future<?> original, Consumer<? super T> consumer) {
      this.original = original;
      this.consumer = consumer;
    }

    @Override
    void await(Promise<Any> promise, Lookup lookup, Consumer<? super Any> consumer) {
      original.await(promise, lookup, consumer);
    }

    @Override
    public void await(
        Promise<Any> promise,
        SourceReference sourceReference,
        String description,
        Consumer<? super Any> consumer) {
      original.await(promise, sourceReference, description, consumer);
    }

    @Override
    MethodHandle bind(Scheduler.FunctionDescriptor function) {
      return original.bind(function);
    }

    @Override
    public void complete(T result) {
      consumer.accept(result);
    }

    @Override
    public void debug(
        RootDefinition normalReturnValue,
        SourceReference sourceReference,
        Context context,
        Consumer<? super Any> next) {
      original.debug(normalReturnValue, sourceReference, context, next);
    }

    @Override
    public void error(SourceReference sourceReference, String message) {
      original.error(sourceReference, message);
    }

    @Override
    void export(String name, Class<?> returnType, MethodHandle handle) {
      original.export(name, returnType, handle);
    }

    @Override
    public void external(
        String uri, SourceReference sourceReference, Consumer<? super Any> target) {
      original.external(uri, sourceReference, target);
    }

    @Override
    public LaunchBatch launch() {
      return original.launch();
    }

    @Override
    public Promise<Any> launch(RootDefinition definition) {
      return original.launch(definition);
    }

    @Override
    Scheduler.State.Inflight<?> real() {
      return original.real();
    }

    @Override
    public void reschedule(Runnable runnable) {
      original.reschedule(runnable);
    }

    @Override
    Runnable waitEdge(Future<?> callee) {
      return original.waitEdge(callee);
    }
  }

  Future() {}

  /**
   * Get the value of a promise during a lookup.
   *
   * <p>Wait for a promise to be evaluated, if it isn't already, and provide result to a consumer
   * associated with a lookup. This tracks the lookup as part of a potential deadlock cycle.
   */
  abstract void await(Promise<Any> promise, Lookup lookup, Consumer<? super Any> consumer);

  /**
   * Get the value of a promise with debugging information.
   *
   * <p>Waits for a promise to be evaluated, if it isn't already, and provide the result to a
   * consumer. If the promise cannot be evaluated, due to an error or deadlock, the consumer is
   * never invoked and the debugging information provided is shown to the user.
   */
  public abstract void await(
      Promise<Any> promise,
      SourceReference sourceReference,
      String description,
      Consumer<? super Any> consumer);
  /**
   * Get the value of a promise with debugging information.
   *
   * <p>Waits for a promise to be evaluated, if it isn't already, and provide the result to a
   * consumer. If the promise cannot be evaluated, due to an error or deadlock, the consumer is
   * never invoked and the debugging information provided is shown to the user.
   */
  public final void await(
      Promise<Any> promise,
      SourceReference sourceReference,
      String description,
      AnyConsumer consumer) {
    await(promise, sourceReference, description, v -> v.accept(consumer));
  }

  /** Find the method handle associated with a KWS function */
  abstract MethodHandle bind(Scheduler.FunctionDescriptor descriptor);

  /**
   * Return a value for this future.
   *
   * <p>Once this method is invoked, no further methods can be invoked on this future.
   */
  public abstract void complete(T result);

  /**
   * Trigger a break point in the debugger
   *
   * <p>This works on a definition with appropriate context
   */
  public final void debug(
      Definition definition,
      SourceReference sourceReference,
      Context context,
      Consumer<? super Any> consumer) {
    debug(
        future -> definition.invoke(future, sourceReference, context),
        sourceReference,
        context,
        consumer);
  }

  /**
   * Trigger a break point in the debugger
   *
   * @param normalReturnValue a task that would return the value if the debugger were inactive
   * @param sourceReference the source trace of the caller
   * @param context the context of the caller
   * @param next the callback for the resulting value
   */
  public abstract void debug(
      RootDefinition normalReturnValue,
      SourceReference sourceReference,
      Context context,
      Consumer<? super Any> next);

  /**
   * Raise an error during lookup.
   *
   * <p>Displays an error that happened during a lookup.
   *
   * @param lookup the lookup where the error occurred
   * @see #error(SourceReference,String)
   */
  void error(Lookup lookup) {
    error(
        lookup.source(),
        String.format("Undefined name “%s”. Lookup was as follows:", lookup.syntax()));
  }
  /**
   * Raise an error during lookup.
   *
   * <p>Displays an error that happened during a lookup.
   *
   * @param lookup the lookup where the error occurred
   * @param failType the erroneous type that prevent lookup from proceeding or null if no more
   *     frames were available in the context.
   * @see #error(SourceReference,String)
   */
  void error(Lookup lookup, String failType) {
    error(
        lookup.source(),
        String.format(
            "Unexpected type %s while resolving name “%s”. Lookup was as follows:",
            failType, lookup.syntax()));
  }

  /**
   * Report an error during execution of the program.
   *
   * <p>Once an error has been raised in a future, it cannot complete, or schedule more tasks. More
   * errors can be raised though.
   */
  public abstract void error(SourceReference sourceReference, String message);

  /** Install a method handle to a KWS function */
  abstract void export(String name, Class<?> returnType, MethodHandle handle);

  /**
   * Resolve a URI.
   *
   * <p>Load the value for a URI. If the URI does not exist, the callback will never be invoked.
   */
  public abstract void external(
      String uri, SourceReference sourceReference, Consumer<? super Any> target);

  /**
   * Create a new future that runs a callback when complete
   *
   * <p>Any errors in the new future will bubble up into the current future
   *
   * @param consumer the callback to handle the result
   * @param <X> the type of the result
   */
  final <X> Future<X> inner(Consumer<? super X> consumer) {
    return new InnerFuture<>(this, consumer);
  }

  /**
   * Create a new future that runs a callback when complete with error interception
   *
   * @param consumer the callback to handle the result
   * @param errorHandler a callback to handle any errors generated by this future
   * @param <X> the type of the result
   */
  public final <X> Future<X> inner(Consumer<? super X> consumer, ErrorHandler errorHandler) {
    return new InnerFuture<>(this, consumer) {
      @Override
      void error(Lookup lookup, String failType) {
        errorHandler.handle(lookup, failType);
      }

      @Override
      void error(Lookup lookup) {
        errorHandler.handle(lookup);
      }

      @Override
      public void error(SourceReference sourceReference, String message) {
        errorHandler.handle(sourceReference, message);
      }
    };
  }

  /**
   * Launch a number of tasks simultaneously.
   *
   * <p>This is most useful for {@link Frame} creation, where all the attributes must be evaluated
   * simultaneously
   */
  abstract LaunchBatch launch();

  /**
   * Evaluate an accumulator definition in a child future.
   *
   * <p>This evaluates the definition in a future that, if it errors, will display the error and
   * never invoke the callback. If it completes, the callback will receive the result. Multiple
   * tasks can be schedule simultaneously, but concurrency control is the responsibility of the
   * caller.
   */
  public final void launch(
      AccumulatorDefinition definition,
      SourceReference sourceReference,
      Context context,
      Any previous,
      Consumer<? super Accumulator> consumer) {
    reschedule(
        definition.invoke(new InnerFuture<>(this, consumer), sourceReference, context, previous));
  }

  /**
   * Evaluate a collector definition in a child future.
   *
   * <p>This evaluates the definition in a future that, if it errors, will display the error and
   * never invoke the callback. If it completes, the callback will receive the result. Multiple
   * tasks can be schedule simultaneously, but concurrency control is the responsibility of the
   * caller.
   */
  public void launch(
      CollectorDefinition definition,
      SourceReference sourceReference,
      Context context,
      Fricassee chain,
      Consumer<? super Any> consumer) {
    reschedule(
        definition.invoke(new InnerFuture<>(this, consumer), sourceReference, context, chain));
  }
  /**
   * Evaluate a collector definition in a child future.
   *
   * <p>This evaluates the definition in a future that, if it errors, will display the error and
   * never invoke the callback. If it completes, the callback will receive the result. Multiple
   * tasks can be schedule simultaneously, but concurrency control is the responsibility of the
   * caller.
   */
  public void launch(
      CollectorDefinition definition,
      SourceReference sourceReference,
      Context context,
      Fricassee chain,
      AnyConsumer consumer) {
    launch(definition, sourceReference, context, chain, v -> v.accept(consumer));
  }
  /**
   * Evaluate a definition in a child future.
   *
   * <p>This evaluates the definition in a future that, if it errors, will display the error and
   * never invoke the callback. If it completes, the callback will receive the result. This is a
   * light-weight version of {@link #launch(RootDefinition)}. Multiple tasks can be schedule
   * simultaneously, but concurrency control is the responsibility of the caller.
   */
  public final void launch(
      Definition definition,
      SourceReference sourceReference,
      Context context,
      Consumer<? super Any> consumer) {
    reschedule(definition.invoke(new InnerFuture<>(this, consumer), sourceReference, context));
  }
  /**
   * Evaluate a definition in a child future.
   *
   * <p>This evaluates the definition in a future that, if it errors, will display the error and
   * never invoke the callback. If it completes, the callback will receive the result. This is a
   * light-weight version of {@link #launch(RootDefinition)}. Multiple tasks can be schedule
   * simultaneously, but concurrency control is the responsibility of the caller.
   */
  public final void launch(
      Definition definition,
      SourceReference sourceReference,
      Context context,
      AnyConsumer consumer) {
    launch(definition, sourceReference, context, v -> v.accept(consumer));
  }
  /**
   * Evaluate a definition in a child future.
   *
   * <p>This evaluates the definition in a future that, if it errors, will display the error and
   * never invoke the callback. If it completes, the callback will receive the result. This is a
   * light-weight version of {@link #launch(RootDefinition)}. Multiple tasks can be schedule
   * simultaneously, but concurrency control is the responsibility of the caller.
   */
  public final void launch(
      DistributorDefinition definition,
      SourceReference sourceReference,
      Context context,
      Consumer<? super Fricassee> consumer) {
    reschedule(definition.invoke(new InnerFuture<>(this, consumer), sourceReference, context));
  }

  /**
   * Launch a new task.
   *
   * <p>Start a task and receive a promise for the result of that task.
   */
  abstract Promise<Any> launch(RootDefinition definition);
  /**
   * Launch a new task.
   *
   * <p>Start a task and receive a promise for the result of that task.
   */
  public final void launch(RootDefinition definition, Consumer<? super Any> consumer) {
    reschedule(definition.launch(new InnerFuture<>(this, consumer)));
  }
  /**
   * Launch a new task.
   *
   * <p>Start a task and receive a promise for the result of that task.
   */
  public final void launch(RootDefinition definition, AnyConsumer consumer) {
    launch(definition, v -> v.accept(consumer));
  }

  /**
   * Evaluate an override definition in a child future.
   *
   * <p>This evaluates the definition in a future that, if it errors, will display the error and
   * never invoke the callback. If it completes, the callback will receive the result. This is a
   * light-weight version of {@link #launch(RootDefinition)}. Multiple tasks can be schedule
   * simultaneously, but concurrency control is the responsibility of the caller.
   */
  public final void launch(
      OverrideDefinition definition,
      SourceReference sourceReference,
      Context context,
      Any original,
      Consumer<? super Any> consumer) {
    reschedule(
        definition.invoke(new InnerFuture<>(this, consumer), sourceReference, context, original));
  }
  /**
   * Evaluate an override definition in a child future.
   *
   * <p>This evaluates the definition in a future that, if it errors, will display the error and
   * never invoke the callback. If it completes, the callback will receive the result. This is a
   * light-weight version of {@link #launch(RootDefinition)}. Multiple tasks can be schedule
   * simultaneously, but concurrency control is the responsibility of the caller.
   */
  public final void launch(
      OverrideDefinition definition,
      SourceReference sourceReference,
      Context context,
      Any original,
      AnyConsumer consumer) {
    launch(definition, sourceReference, context, original, v -> v.accept(consumer));
  }

  abstract Scheduler.State.Inflight<?> real();

  /**
   * Schedule an unboxing to be done
   *
   * <p>This executes {@link Any#accept(Object)} after rescheduling.
   *
   * @param value the value to unbox
   * @param consumer the consumer to handle the different cases
   */
  public final void reschedule(Any value, AnyConsumer consumer) {
    reschedule(() -> value.accept(consumer));
  }

  /**
   * Schedule work to be done.
   *
   * <p>This may be executed on any live future. If the future is dead, the request will be ignored.
   * The task will be scheduled for execution and executed when resources are available. Multiple
   * tasks can be scheduled simultaneously, but concurrency control is the responsibility of the
   * caller.
   *
   * <p>In an effort to keep the JVM stack short, callbacks are invoked on empty stacks, so work is
   * permitted in the callback. If parallelism is required or the task needs a trampoline, this
   * method may be used.
   */
  public abstract void reschedule(Runnable runnable);

  abstract Runnable waitEdge(Future<?> callee);
}
