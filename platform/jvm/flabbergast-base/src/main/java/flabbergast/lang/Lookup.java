package flabbergast.lang;

import flabbergast.lang.Context.FrameAccessor;
import flabbergast.util.Pair;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base for lookups operations
 *
 * <p>All lookups in Flabbergast work by creating a grid with all frames (that the lookup can
 * investigate) along the horizontal and all the names that are part of the lookup along the
 * vertical. Lookup proceeds by trying to fill in the grid, filling each column top to bottom,
 * working from first column to last column.
 *
 * <p>For example, consider the following code
 *
 * <pre>
 *   b : {
 *     a: {
 *       x : { }
 *     }
 *   }
 *   x : { y : 3 }
 *   Lookup x.y In b.a
 * </pre>
 *
 * <p>This would create a table as follows. The numbers indicate the order in which operations are
 * called
 *
 * <table border=1>
 *   <tr><td></td><th>frame “b.a”</th><th>frame “b”</th><th>root frame</th></tr>
 *   <tr>
 *     <th><tt>x</tt></th>
 *     <td>1 {@link LookupExplorer#process Explorer.process} → {@link LookupNextOperation#finish finish}</td>
 *     <td>3 {@link LookupExplorer#process Explorer.process} → {@link LookupNextOperation#next next}</td>
 *     <td>4 {@link LookupExplorer#process Explorer.process} → {@link LookupNextOperation#finish finish}</td>
 *   </tr>
 *   <tr>
 *     <th><tt>y</tt></th>
 *     <td>2 {@link LookupExplorer#process Explorer.process} → {@link LookupNextOperation#next next}</td>
 *     <td></td>
 *     <td>5 {@link LookupExplorer#process Explorer.process} → {@link LookupNextOperation#finish finish}</td>
 *   </tr>
 *   <tr>
 *     <th>Collector</th>
 *     <td></td>
 *     <td></td>
 *     <td>6 {@link LookupSelector#accept(Any, LookupNextOperation) Collector.accept} → {@link LookupNextOperation#finish finish}</td>
 *   </tr>
 * </table>
 *
 * <p>Each column is headed by a frame in the lookup's {@link Context}. A {@link LookupExplorer} is
 * created for this column and the explorer is probed with each name in sequence. After each probe,
 * the explorer can choose to abandon the column by calling {@link LookupNextOperation#next()},
 * produce a value using {@link LookupNextOperation#finish(Any)}, or raise an error with {@link
 * LookupLastOperation#fail()}. The value produced for rows must ultimately be a frame (except the
 * last row, which can be any type); {@link LookupExplorer#map(Template, LookupOperation)} allows
 * converting this value before is is passed on to the next row, so it could a non-frame value into
 * a frame. If a non-frame value is passed to the next row, a type error will occur. If the explorer
 * reaches the last row of the column, the value found is passed to a {@link LookupSelector}. There
 * is only one selector instance for each lookup operation. The selector can choose to emit a value
 * to the caller or request that more columns be explored. This continues until the selector emits a
 * value, the selector raises an error, the explorer raises an error, or there are no more columns
 * to explore. If there are no more columns to explore, the selector can then choose to emit a value
 * or raise an error.
 */
public final class Lookup {
  private class FinalStep extends Step {
    public FinalStep(int frame, int syntheticFrame, Context.FrameAccessor sourceFrame) {
      super(nameCount() - 1, frame, syntheticFrame, sourceFrame);
    }

    @Override
    public void accept(Any value) {
      selector.accept(
          value,
          new LookupNextOperation() {
            @Override
            public void await(Promise<Any> promise, Consumer<Any> consumer) {
              future.await(promise, Lookup.this, consumer);
            }

            @Override
            public void fail() {
              future.error(Lookup.this);
            }

            @Override
            public void finish(Any result) {
              consumer.accept(result);
            }

            @Override
            public void next() {
              activateNext();
            }
          });
    }

    @Override
    public Step duplicate(LookupExplorer step, int syntheticFrame) {
      throw new UnsupportedOperationException("Cannot duplicate final step");
    }
  }

  private class IntermediateStep extends Step {
    private Frame resultFrame;
    private final LookupExplorer step;

    public IntermediateStep(
        LookupExplorer step,
        int name,
        int frame,
        int syntheticFrame,
        Context.FrameAccessor sourceFrame) {
      super(name, frame, syntheticFrame, sourceFrame);
      this.step = step;
    }

    @Override
    public void accept(Any result) {
      result.accept(
          new WhinyAnyConsumer() {
            @Override
            public void accept(Frame value) {
              resultFrame = value;

              step.process(
                  name(getName() + 1),
                  context.accessor(resultFrame),
                  getName(),
                  names.length - getName(),
                  new LookupForkOperation() {
                    @Override
                    public void await(Promise<Any> promise, Consumer<Any> consumer) {
                      future.await(promise, Lookup.this, consumer);
                    }

                    @Override
                    public void fail() {
                      future.error(Lookup.this);
                    }

                    @Override
                    public void finish(Any result) {
                      (getName() == nameCount() - 2
                              ? new FinalStep(
                                  getFrame(), getSyntheticFrame(), context.accessor(resultFrame))
                              : new IntermediateStep(
                                  step,
                                  getName() + 1,
                                  getFrame(),
                                  getSyntheticFrame(),
                                  context.accessor(resultFrame)))
                          .accept(result);
                    }

                    @Override
                    public void fork(Stream<Any> values) {
                      fork(values, Consumer::accept);
                    }

                    private <T> void fork(Stream<T> values, BiConsumer<Consumer<Any>, T> callback) {
                      final var items = values.collect(Collectors.toList());
                      if (items.isEmpty()) {
                        next();
                      } else {
                        for (var i = items.size() - 1; i > 1; i--) {
                          final var value = items.get(i);
                          final var freshExplorer = step.duplicate();
                          forks.offerLast(
                              () -> {
                                final var syntheticFrame = ++syntheticWidth;
                                knownSteps.addAll(
                                    knownSteps
                                        .stream()
                                        .filter(
                                            step -> step.getSyntheticFrame() == getSyntheticFrame())
                                        .map(
                                            step1 -> step1.duplicate(freshExplorer, syntheticFrame))
                                        .collect(Collectors.toList()));
                                callback.accept(
                                    (getName() == nameCount() - 2
                                        ? new FinalStep(
                                            getFrame(),
                                            syntheticFrame,
                                            context.accessor(resultFrame))
                                        : new IntermediateStep(
                                            freshExplorer,
                                            getName() + 1,
                                            getFrame(),
                                            syntheticFrame,
                                            context.accessor(resultFrame))),
                                    value);
                              });
                        }
                        callback.accept(this::finish, items.get(0));
                      }
                    }

                    @Override
                    public void forkPromises(Stream<Promise<Any>> values) {
                      fork(values, (consumer, promise) -> await(promise, consumer));
                    }

                    @Override
                    public void next() {
                      activateNext();
                    }
                  });
            }

            @Override
            protected void fail(String type) {
              future.error(Lookup.this, type);
            }
          });
    }

    @Override
    public Step duplicate(LookupExplorer step, int syntheticFrame) {
      final var result =
          new IntermediateStep(step, getName(), getFrame(), syntheticFrame, getSource());
      result.resultFrame = resultFrame;
      return result;
    }
  }

  /**
   * A lookup proceeds in a series of steps to get the value for a lookup
   *
   * <p>Other lookups should createFromValues attempts to indicate where in the grid they are
   * processing and the frame where the lookup is occurring.
   */
  private abstract class Step implements Consumer<Any> {
    private final int frame;
    private final int name;
    private final Context.FrameAccessor sourceFrame;
    private final int syntheticFrame;

    protected Step(int name, int frame, int syntheticFrame, Context.FrameAccessor sourceFrame) {
      this.name = name;
      this.frame = frame;
      this.syntheticFrame = syntheticFrame;
      this.sourceFrame = sourceFrame;
      knownSteps.add(this);
    }

    public abstract Step duplicate(LookupExplorer step, int syntheticFrame);

    protected final int getFrame() {
      return frame;
    }

    protected final int getName() {
      return name;
    }

    protected final FrameAccessor getSource() {
      return sourceFrame;
    }

    protected final int getSyntheticFrame() {
      return syntheticFrame;
    }
  }

  private final Consumer<? super Any> consumer;
  private final Context context;
  private final Deque<Runnable> forks = new ArrayDeque<>();
  private int frameIndex = 0;
  private final Context.FrameAccessor[] frames;
  private final Future<?> future;
  private final LookupHandler handler;
  private final LinkedList<Step> knownSteps = new LinkedList<>();
  /** The name components in the lookup expression. */
  private final Name[] names;

  private final LookupSelector selector;
  private final SourceReference sourceReference;
  private int syntheticWidth;

  /**
   * Create a new lookup event that will apply grid-driven rules to the lookup process
   *
   * @param handler the lookup handler that defines the semantics of this operation
   * @param future the future in which the lookup occurs
   * @param sourceReference the caller of the lookup
   * @param context the context in which lookup occurs
   * @param names the list of names to lookup
   * @param consumer the callback for the result of lookup, if successful
   */
  Lookup(
      LookupHandler handler,
      Future<?> future,
      SourceReference sourceReference,
      Context context,
      Name[] names,
      Consumer<? super Any> consumer) {
    selector = handler.selector().start(future, sourceReference, context);
    this.handler = handler;
    this.future = future;
    this.sourceReference = sourceReference;
    this.context = context;
    this.names = names;
    this.consumer = consumer;
    frames = context.stream().toArray(Context.FrameAccessor[]::new);
    syntheticWidth = frames.length;
  }

  private void activateNext() {
    if (!forks.isEmpty()) {
      forks.removeLast().run();
      return;
    }
    while (frameIndex < frames.length) {
      final var index = frameIndex++;
      final var rootAttempt =
          nameCount() == 1
              ? new FinalStep(index, syntheticWidth, frames[index])
              : new IntermediateStep(
                  handler.explorer().start(future, sourceReference, context),
                  0,
                  index,
                  syntheticWidth,
                  frames[index]);
      final var promise = frames[index].get(name(0));
      if (promise.isPresent()) {
        future.await(promise.get(), this, rootAttempt);
        return;
      }
    }
    selector.empty(
        new LookupLastOperation() {
          @Override
          public void await(Promise<Any> promise, Consumer<Any> consumer) {
            future.await(promise, Lookup.this, consumer);
          }

          @Override
          public void fail() {
            future.error(Lookup.this);
          }

          @Override
          public void finish(Any value) {
            consumer.accept(value);
          }
        });
  }

  /** Get the number of frames that can be searched */
  public int frameCount() {
    return syntheticWidth;
  }

  /** The future in which lookup is occurring */
  Future<?> future() {
    return future;
  }

  /**
   * Get the frame that was inspected at this position in the grid.
   *
   * @return the frame or empty if skipped/not started
   */
  public Optional<FrameAccessor> get(int name, int frame) {
    return knownSteps
        .stream()
        .filter(step -> step.getSyntheticFrame() == frame && step.getName() == name)
        .map(Step::getSource)
        .findFirst();
  }

  /** Get the lookup handler associated with this operation */
  public LookupHandler handler() {
    return handler;
  }

  /** Get the last frame that was actively searched and the attribute that was searched for */
  public Optional<Pair<FrameAccessor, Name>> last() {
    if (knownSteps.isEmpty()) {
      return Optional.empty();
    } else {
      final var last = knownSteps.peekLast();
      return Optional.of(Pair.of(last.getSource(), names[last.getName()]));
    }
  }

  /**
   * Get the name in this lookup
   *
   * @param index the identifier name (i.e., the row number)
   */
  public Name name(int index) {
    return names[index];
  }
  /** Get the number of names in this lookup */
  public int nameCount() {
    return names.length;
  }

  /** Get the names used in the lookup */
  public Stream<Name> names() {
    return Stream.of(names);
  }

  /** Get the Flabbergast source location for this lookup. */
  public SourceReference source() {
    return sourceReference;
  }

  /** Start the lookup process */
  void start() {
    activateNext();
  }

  /** Get the full expression for the lookup, as it would be written in Flabbergast. */
  public String syntax() {
    return names().map(Name::toString).collect(Collectors.joining("."));
  }
}
