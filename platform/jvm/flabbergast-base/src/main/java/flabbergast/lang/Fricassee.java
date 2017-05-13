package flabbergast.lang;

import flabbergast.util.ConcurrentConsumer;
import flabbergast.util.ConcurrentMapper;
import flabbergast.util.Pair;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** The implementation of Flabbergast's fricassée operations */
public abstract class Fricassee extends Thunkerator {

  private interface EmptyHandler {
    void finish(
        Future<?> future,
        SourceReference sourceReference,
        Context context,
        Consumer<? super Any> consumer);
  }

  interface FetchDefinition {
    void fetch(
        Future<?> future,
        SourceReference sourceReference,
        long ordinal,
        Name name,
        Consumer<Attribute> output);
  }

  interface GroupConsumer {
    void accept(
        Future<?> future,
        SourceReference sourceReference,
        Context context,
        SourceReference callingSourceReference,
        Context callingContext,
        Runnable complete);

    void finish(
        Future<?> future,
        SourceReference callingSourceReference,
        Context callingContext,
        Runnable complete);
  }

  private interface TailAction {
    void end();

    void next(SourceReference source, Context context);

    void skip();
  }

  private abstract static class BlockTransform extends Fricassee {
    private enum State {
      UNINITIALIZED,
      ENTERED,
      FINISHED
    }

    private List<Pair<SourceReference, Context>> cache;
    private final Deque<ThunkeratorConsumer> consumers = new ConcurrentLinkedDeque<>();
    private final Fricassee input;
    private State state = State.UNINITIALIZED;

    protected BlockTransform(Fricassee input) {
      this.input = input;
    }

    @Override
    public Context context() {
      return input.context();
    }

    @Override
    public final void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
      State currentState;
      synchronized (this) {
        currentState = state;
        switch (state) {
          case UNINITIALIZED:
            state = State.ENTERED;
            consumers.push(thunkerator);
            break;
          case ENTERED:
            consumers.push(thunkerator);
            break;
          case FINISHED:
            break;
        }
      }
      switch (currentState) {
        case UNINITIALIZED:
          input.iterator(
              future,
              new ThunkeratorConsumer() {
                private final List<Pair<SourceReference, Context>> input = new ArrayList<>();

                @Override
                public void end() {
                  process(
                      future,
                      input,
                      result -> {
                        synchronized (BlockTransform.this) {
                          cache = result;
                          state = State.FINISHED;
                        }
                        ThunkeratorConsumer consumer;
                        while ((consumer = consumers.poll()) != null) {
                          launch(consumer);
                        }
                      });
                }

                @Override
                public void next(
                    SourceReference sourceReference, Context context, Thunkerator next) {
                  input.add(Pair.of(sourceReference, context));
                  next.iterator(future, this);
                }
              });
          break;
        case ENTERED:
          break;
        case FINISHED:
          launch(thunkerator);
          break;
      }
    }

    private void launch(ThunkeratorConsumer thunkeratorConsumer) {
      launch(cache, 0, thunkeratorConsumer);
    }

    protected abstract void process(
        Future<?> future,
        List<Pair<SourceReference, Context>> input,
        Consumer<List<Pair<SourceReference, Context>>> resultHandler);
  }

  private static class DiscardingConsumer implements ThunkeratorConsumer {
    private final Future<?> future;
    private final long remaining;
    private final ThunkeratorConsumer thunkerator;

    public DiscardingConsumer(ThunkeratorConsumer thunkerator, long remaining, Future<?> future) {
      this.thunkerator = thunkerator;
      this.remaining = remaining;
      this.future = future;
    }

    @Override
    public void end() {
      thunkerator.end();
    }

    @Override
    public void next(SourceReference sourceReference, Context context, Thunkerator next) {
      if (remaining > 0) {
        next.iterator(future, new DiscardingConsumer(thunkerator, remaining - 1, future));
      } else {
        thunkerator.next(sourceReference, context, next);
      }
    }
  }

  private static class GroupBy extends BlockTransform {

    private final SourceReference callingSourceReference;
    private final Map<Name, CollectorDefinition> collections = new HashMap<>();
    private final GroupConsumer consumer;
    private final List<Pair<SourceReference, Context>> output = new ArrayList<>();

    protected GroupBy(
        Fricassee input, SourceReference sourceReference, FricasseeGrouper... groupers) {
      super(input);
      callingSourceReference = sourceReference;
      Function<AttributeSource, GroupConsumer> constructor =
          attributes ->
              new GroupConsumer() {
                private final List<Pair<SourceReference, Context>> items = new ArrayList<>();

                @Override
                public void accept(
                    Future<?> future,
                    SourceReference sourceReference,
                    Context context,
                    SourceReference callingSourceReference,
                    Context callingContext,
                    Runnable complete) {
                  items.add(Pair.of(sourceReference, context));
                  complete.run();
                }

                @Override
                public void finish(
                    Future<?> future,
                    SourceReference callingSourceReference,
                    Context callingContext,
                    Runnable complete) {
                  final var frame =
                      Frame.create(
                          future,
                          sourceReference,
                          context(),
                          false,
                          Stream.empty(),
                          attributes,
                          collections
                              .entrySet()
                              .stream()
                              .map(
                                  entry ->
                                      new Attribute.CollectorAttribute(
                                          entry.getKey(), entry.getValue(), items))
                              .collect(AttributeSource.toSource()));

                  output.add(Pair.of(frame.source(), frame.context()));
                  complete.run();
                }
              };
      for (var i = groupers.length - 1; i >= 0; i--) {
        constructor = groupers[i].prepare(collections, constructor);
      }
      consumer = constructor.apply(AttributeSource.EMPTY);
    }

    @Override
    protected void process(
        Future<?> future,
        List<Pair<SourceReference, Context>> input,
        Consumer<List<Pair<SourceReference, Context>>> resultHandler) {
      ConcurrentConsumer.iterate(
          input.iterator(),
          new ConcurrentConsumer<>() {
            @Override
            public void complete() {
              consumer.finish(
                  future, callingSourceReference, context(), () -> resultHandler.accept(output));
            }

            @Override
            public void process(
                Pair<SourceReference, Context> current, int index, Runnable complete) {
              consumer.accept(
                  future,
                  current.first(),
                  current.second(),
                  callingSourceReference,
                  context(),
                  () -> future.reschedule(complete));
            }
          });
    }
  }

  private abstract static class TerminalSink {
    private final Future<?> future;
    private Thunkerator input;

    protected TerminalSink(Fricassee input, Future<?> future) {
      this.input = input;
      this.future = future;
    }

    protected abstract void emit();

    public Future<?> future() {
      return future;
    }

    protected abstract void process(SourceReference sourceReference, Context context);

    protected final void pullNext() {
      future.reschedule(
          () ->
              input.iterator(
                  future,
                  new ThunkeratorConsumer() {
                    @Override
                    public void end() {
                      emit();
                    }

                    @Override
                    public void next(
                        SourceReference sourceReference, Context context, Thunkerator next) {
                      input = next;
                      process(sourceReference, context);
                    }
                  }));
    }
  }

  private abstract static class Transform extends Fricassee {
    private class TransformConsumer implements ThunkeratorConsumer {
      private final Runnable end;
      private final Future<?> future;
      private final BiConsumer<SourceReference, Context> nextConsumer;

      public TransformConsumer(
          Runnable end, Future<?> future, BiConsumer<SourceReference, Context> nextConsumer) {
        this.end = end;
        this.future = future;
        this.nextConsumer = nextConsumer;
      }

      @Override
      public void end() {
        end.run();
      }

      @Override
      public void next(
          SourceReference sourceReference, Context context, Thunkerator nextThunkerator) {
        process(
            future,
            sourceReference,
            context,
            new TailAction() {
              @Override
              public void end() {
                end.run();
              }

              @Override
              public void next(SourceReference source, Context context) {
                current = nextThunkerator;
                nextConsumer.accept(source, context);
              }

              @Override
              public void skip() {
                nextThunkerator.iterator(future, TransformConsumer.this);
              }
            });
      }
    }

    private Thunkerator current;
    private final Fricassee input;
    private final WrappedThunkerator thunk =
        new WrappedThunkerator(
            ((future, end, nextConsumer) ->
                current.iterator(future, new TransformConsumer(end, future, nextConsumer))));

    protected Transform(Fricassee input) {
      current = this.input = input;
    }

    @Override
    public Context context() {
      return input.context();
    }

    @Override
    public final void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
      thunk.iterator(future, thunkerator);
    }

    protected abstract void process(
        Future<?> future, SourceReference sourceReference, Context context, TailAction action);
  }

  private abstract static class Yielder implements Definition {

    private volatile boolean exhausted;

    private final Definition getter;

    private Thunkerator input;

    private final Deque<Future<Any>> queue = new ConcurrentLinkedDeque<>();

    public Yielder(Thunkerator input, Definition getter) {
      this.input = input;
      this.getter = getter;
    }

    protected abstract void empty(Future<Any> future, SourceReference callerSourceReference);

    @Override
    public Runnable invoke(Future<Any> future, SourceReference callerSource, Context context) {
      if (exhausted) {
        return () -> empty(future, callerSource);
      }
      final boolean push;
      synchronized (this) {
        push = queue.isEmpty();
        queue.add(future);
      }
      if (push) {
        return () ->
            input.iterator(
                future,
                new ThunkeratorConsumer() {
                  @Override
                  public void end() {
                    exhausted = true;
                    while (!queue.isEmpty()) {
                      empty(queue.poll(), callerSource);
                    }
                  }

                  @Override
                  public void next(
                      SourceReference sourceReference, Context context, Thunkerator next) {
                    input = next;
                    final var waiter = queue.poll();
                    if (waiter == null) {
                      throw new IllegalStateException(
                          "Yield has received input, but no future is waiting for it.");
                    }
                    future.reschedule(getter.invoke(future, sourceReference, context));
                    final var nextWaiter = queue.peek();
                    if (nextWaiter != null) {
                      nextWaiter.reschedule(() -> input.iterator(nextWaiter, this));
                    }
                  }
                });
      } else {
        return () -> {};
      }
    }
  }

  private static final EmptyHandler NULL_EMPTY =
      (future, sourceReference, context, consumer) -> consumer.accept(Any.NULL);

  /** Concatenate the output of multiple fricassée sources */
  public static Fricassee concat(Context context, Fricassee... chains) {
    return new Fricassee() {
      class ArrayThunkerator extends Thunkerator {
        class ProxyThunkeratorConsumer implements ThunkeratorConsumer {

          private final Future<?> future;
          private final ThunkeratorConsumer thunkerator;

          public ProxyThunkeratorConsumer(Future<?> future, ThunkeratorConsumer thunkerator) {
            this.future = future;
            this.thunkerator = thunkerator;
          }

          @Override
          public void end() {
            new ArrayThunkerator(index + 1).iterator(future, thunkerator);
          }

          @Override
          public void next(SourceReference sourceReference, Context context, Thunkerator next) {
            thunkerator.next(
                sourceReference,
                context,
                new Thunkerator() {
                  @Override
                  void iterator(Future<?> future, ThunkeratorConsumer nextConsumer) {
                    next.iterator(future, new ProxyThunkeratorConsumer(future, nextConsumer));
                  }
                });
          }
        }

        private final int index;

        ArrayThunkerator(int index) {
          this.index = index;
        }

        @Override
        public void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
          if (index < chains.length) {
            chains[index].iterator(future, new ProxyThunkeratorConsumer(future, thunkerator));
          } else {
            thunkerator.end();
          }
        }
      }

      @Override
      Context context() {
        return context;
      }

      @Override
      public void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
        new ArrayThunkerator(0).iterator(future, thunkerator);
      }
    };
  }

  /** Create a <tt>For Each</tt> fricassée source */
  public static Fricassee forEach(
      Frame source,
      Context context,
      String filename,
      int startLine,
      int startColumn,
      int endLine,
      int endColumn,
      SourceReference sourceReference) {
    return new Fricassee() {
      private final Iterator<Name> iterator = source.names().iterator();
      private int ordinal = 1;
      private final Thunkerator thunk =
          new WrappedThunkerator(
              (future, end, next) -> {
                if (iterator.hasNext()) {
                  final var attribute = iterator.next();
                  source
                      .get(attribute)
                      .ifPresentOrElse(
                          promise ->
                              future.await(
                                  promise,
                                  sourceReference,
                                  String.format("Attribute “%s” in “Each” clause", attribute),
                                  new WhinyAnyConsumer() {
                                    @Override
                                    public void accept(Frame frame) {
                                      next.accept(makeSourceReference(), context.prepend(frame));
                                    }

                                    @Override
                                    protected void fail(String type) {
                                      future.error(
                                          makeSourceReference(),
                                          String.format(
                                              "In “Each” clause, attribute “%s” is %s, but Frame was expected.",
                                              attribute, type));
                                    }

                                    private SourceReference makeSourceReference() {
                                      return sourceReference.junction(
                                          String.format(
                                              "fricassée “Each” iteration %d “%s”",
                                              ordinal++, attribute),
                                          filename,
                                          startLine,
                                          startColumn,
                                          endLine,
                                          endColumn,
                                          source.source());
                                    }
                                  }),
                          end);
                } else {
                  end.run();
                }
              });

      @Override
      Context context() {
        return context;
      }

      @Override
      public void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
        thunk.iterator(future, thunkerator);
      }
    };
  }

  /**
   * Create a <tt>Generate</tt> fricassée source
   *
   * @param initial the seed value
   * @param accumulator the definition to compute the next value from the current one
   */
  public static Fricassee generate(
      String filename,
      int startLine,
      int startColumn,
      int endLine,
      int endColumn,
      SourceReference sourceReference,
      Context context,
      Any initial,
      AccumulatorDefinition accumulator) {
    return new Fricassee() {
      private long count;
      private Any current = initial;
      private final WrappedThunkerator thunk =
          new WrappedThunkerator(
              (future, end, next) ->
                  future.launch(
                      accumulator,
                      sourceReference,
                      context,
                      current,
                      accumulation -> {
                        current = accumulation.value();
                        final var output =
                            Frame.create(
                                future,
                                sourceReference.basic(
                                    String.format("Fricassée generator iteration %d", ++count),
                                    filename,
                                    startLine,
                                    startColumn,
                                    endLine,
                                    endColumn),
                                context,
                                false,
                                Stream.empty(),
                                accumulation);
                        next.accept(output.source(), context.forFrame(output));
                      }));

      @Override
      Context context() {
        return context;
      }

      @Override
      public void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
        thunk.iterator(future, thunkerator);
      }
    };
  }

  static void launch(
      List<Pair<SourceReference, Context>> items,
      int index,
      ThunkeratorConsumer thunkeratorConsumer) {
    if (index < items.size()) {
      thunkeratorConsumer.next(
          items.get(index).first(),
          items.get(index).second(),
          new Thunkerator() {
            @Override
            void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
              launch(items, index + 1, thunkerator);
            }
          });
    } else {
      thunkeratorConsumer.end();
    }
  }

  /**
   * Create a lookup handler that processes the collected values through a fricassée operation
   *
   * @param name the name to bind the value to in the operation
   * @param context the context in which the fricassée should be evaluated
   * @param definition the definition that will resolve the lookup-driven fricassée chain to a value
   */
  public static LookupHandler lookupHandler(
      Str name, Context context, CollectorDefinition definition) {
    return new LookupHandler(
        LookupExplorer.EXACT, LookupSelector.fricassee(Name.of(name), context, definition));
  }

  /** Create a new zipping operation for the start of a fricassée chain */
  public static Fricassee zip(
      boolean intersect,
      Context context,
      String filename,
      int startLine,
      int startColumn,
      int endLine,
      int endColumn,
      SourceReference sourceReference,
      FricasseeZipper... zippers) {
    final var names = new TreeSet<Name>();
    final var sources = new ArrayList<FetchDefinition>();
    final var frameConsumer =
        intersect
            ? new Consumer<Frame>() {
              private boolean first = true;

              @Override
              public void accept(Frame frame) {
                if (first) {
                  frame.addNamesTo(names);
                  first = false;
                } else {
                  frame.intersectNames(names);
                }
              }
            }
            : new Consumer<Frame>() {
              @Override
              public void accept(Frame frame) {
                frame.addNamesTo(names);
              }
            };
    for (final var zipper : zippers) {
      zipper.prepare(sources, frameConsumer);
    }

    return new Fricassee() {
      private final Iterator<Name> iterator = names.iterator();
      private int ordinal;
      final Thunkerator thunk =
          new WrappedThunkerator(
              ((future, end, next) -> {
                if (iterator.hasNext()) {
                  final var attribute = iterator.next();
                  ordinal++;
                  final var junctionReference =
                      sourceReference.basic(
                          String.format("fricassée iteration %d “%s”", ordinal, attribute),
                          filename,
                          startLine,
                          startColumn,
                          endLine,
                          endColumn);
                  ConcurrentMapper.process(
                      sources,
                      new ConcurrentMapper<FetchDefinition, Attribute>() {

                        @Override
                        public void emit(List<Attribute> builder) {
                          next.accept(
                              junctionReference,
                              Frame.create(
                                      future,
                                      junctionReference,
                                      context(),
                                      AttributeSource.of(builder))
                                  .context());
                        }

                        @Override
                        public void process(
                            FetchDefinition source, int index, Consumer<Attribute> output) {
                          source.fetch(future, junctionReference, ordinal, attribute, output);
                        }
                      });

                } else {
                  end.run();
                }
              }));

      @Override
      Context context() {
        return context;
      }

      @Override
      public void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
        thunk.iterator(future, thunkerator);
      }
    };
  }

  Fricassee() {}

  /**
   * Create an <tt>Accumulate</tt> clause
   *
   * @param initial the initial value set in the <tt>With</tt> clause
   * @param accumulator an definition that calculates the next value
   */
  public final Fricassee accumulate(Any initial, AccumulatorDefinition accumulator) {
    return new Transform(this) {
      private Any currentValue = initial;

      @Override
      protected void process(
          Future<?> future, SourceReference sourceReference, Context context, TailAction action) {
        future.launch(
            accumulator,
            sourceReference,
            context,
            currentValue,
            result -> {
              currentValue = result.value();
              action.next(
                  sourceReference,
                  Frame.create(future, sourceReference, context, false, Stream.empty(), result)
                      .context());
            });
      }
    };
  }

  abstract Context context();

  /**
   * Count the number of items being fricasséed
   *
   * @param future the scheduler for all the operations in this fricassée chain
   * @param consumer a callback that receives the count
   */
  public final void count(
      Future<?> future, SourceReference sourceReference, Consumer<Long> consumer) {
    new TerminalSink(this, future) {
      long count;

      @Override
      protected void emit() {
        consumer.accept(count);
      }

      @Override
      public void process(SourceReference sourceReference, Context context) {
        count++;
        pullNext();
      }
    }.pullNext();
  }

  /**
   * Discard the items from the beginning of the chain
   *
   * @param count the number of items to discard
   */
  public final Fricassee drop(long count) {
    final var input = this;
    return new Fricassee() {
      @Override
      Context context() {
        return input.context();
      }

      @Override
      public void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
        input.iterator(future, new DiscardingConsumer(thunkerator, count, future));
      }
    };
  }

  /**
   * Discard items from the end of the chain
   *
   * @param count the number of items to discard
   */
  public final Fricassee dropLast(long count) {
    return new BlockTransform(this) {
      @Override
      protected void process(
          Future<?> future,
          List<Pair<SourceReference, Context>> input,
          Consumer<List<Pair<SourceReference, Context>>> resultHandler) {
        resultHandler.accept(
            input.size() > count ? input.subList(0, (int) (input.size() - count)) : input);
      }
    };
  }

  /**
   * Remove items from the fricassée operation until a condition is satisfied
   *
   * @param condition the clause to decide whether to keep an item or skip; it must return
   *     <tt>Bool</tt> or an error will occur
   */
  public final Fricassee dropWhile(Definition condition) {
    return new Transform(this) {
      private boolean allow = false;

      @Override
      protected void process(
          Future<?> future, SourceReference sourceReference, Context context, TailAction action) {
        if (allow) {
          action.next(sourceReference, context);
          return;
        }

        future.launch(
            condition,
            sourceReference,
            context,
            new WhinyAnyConsumer() {

              @Override
              public void accept(boolean filter) {
                if (filter) {
                  action.skip();
                } else {
                  allow = true;
                  action.next(sourceReference, context);
                }
              }

              @Override
              protected void fail(String type) {
                future.error(
                    sourceReference,
                    String.format(
                        "In “DropWhile clause, result is %s, but Bool was expected.", type));
              }
            });
      }
    };
  }

  private void first(
      Future<?> future,
      SourceReference callerSource,
      Definition definition,
      EmptyHandler emptyHandler,
      Consumer<? super Any> consumer) {
    new TerminalSink(this, future) {

      @Override
      protected void emit() {
        emptyHandler.finish(future, callerSource, context(), consumer);
      }

      @Override
      public void process(SourceReference sourceReference, Context context) {
        future.launch(definition, sourceReference, context, consumer);
      }
    }.pullNext();
  }

  /**
   * Get the first value from the fricassée
   *
   * <p>If multiple values are matched, the remainder are ignored.
   *
   * @param future the scheduler for all the operations in this fricassée chain
   * @param definition the value to return
   * @param defaultDefinition the value to use if the fricassée is empty
   */
  public final void first(
      Future<?> future,
      SourceReference sourceReference,
      Definition definition,
      Definition defaultDefinition,
      Consumer<? super Any> consumer) {
    first(future, sourceReference, definition, of(defaultDefinition), consumer);
  }

  /**
   * Get the first value from the fricassée
   *
   * <p>If multiple values are present, the remainder are ignored. If no values are available, null
   * is returned.
   *
   * @param future the scheduler for all the operations in this fricassée chain
   * @param definition the value to return
   */
  public final void firstOrNull(
      Future<?> future,
      SourceReference sourceReference,
      Definition definition,
      Consumer<? super Any> consumer) {
    first(future, sourceReference, definition, NULL_EMPTY, consumer);
  }

  /**
   * Create a fricassée <tt>Flatten</tt> operation
   *
   * @param distributor a definition that can produce a fricassée to iterate over for each item
   */
  public final Fricassee flatten(DistributorDefinition distributor) {

    final var input = this;
    return new Fricassee() {
      class FlattenThunkeratorConsumer implements ThunkeratorConsumer {
        private final Runnable end;
        private final Future<?> future;
        private final BiConsumer<SourceReference, Context> next;

        public FlattenThunkeratorConsumer(
            Future<?> future, Runnable end, BiConsumer<SourceReference, Context> next) {
          this.future = future;
          this.end = end;
          this.next = next;
        }

        @Override
        public void end() {
          outerNext.iterator(
              future,
              new ThunkeratorConsumer() {
                @Override
                public void end() {
                  end.run();
                }

                @Override
                public void next(
                    SourceReference sourceReference, Context context, Thunkerator givenNext) {
                  outerNext = givenNext;
                  future.launch(
                      distributor,
                      sourceReference,
                      context,
                      fricassee -> fricassee.iterator(future, FlattenThunkeratorConsumer.this));
                }
              });
        }

        @Override
        public void next(SourceReference sourceReference, Context context, Thunkerator givenNext) {
          innerNext = givenNext;
          next.accept(sourceReference, context);
        }
      }

      private Thunkerator innerNext =
          new Thunkerator() {
            @Override
            void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
              thunkerator.end();
            }
          };
      private Thunkerator outerNext = input;
      private final Thunkerator thunk =
          new WrappedThunkerator(
              ((future, end, next) ->
                  innerNext.iterator(future, new FlattenThunkeratorConsumer(future, end, next))));

      @Override
      Context context() {
        return input.context();
      }

      @Override
      public void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
        thunk.iterator(future, thunkerator);
      }
    };
  }

  /** Starts a grouping operation */
  public final Fricassee groupBy(SourceReference sourceReference, FricasseeGrouper... groupers) {
    return new GroupBy(this, sourceReference, groupers);
  }

  /**
   * A <tt>Let</tt> clause that defines the supplied values in the existing context
   *
   * @param attributes the new attributes to define
   */
  public final Fricassee let(Attribute... attributes) {
    return new Transform(this) {
      private final AttributeSource source = AttributeSource.of(attributes);

      @Override
      protected void process(
          Future<?> future, SourceReference sourceReference, Context context, TailAction action) {
        action.next(
            sourceReference,
            Frame.create(future, sourceReference, context, false, Stream.empty(), source)
                .context());
      }
    };
  }

  private EmptyHandler of(Definition definition) {
    return (future, sourceReference, context, consumer) ->
        future.launch(definition, sourceReference, context, consumer);
  }

  private <T> Fricassee orderBy(
      AnyBidiConverter<T> anyConverter, boolean ascending, Definition definition) {
    return new BlockTransform(this) {

      @Override
      protected void process(
          Future<?> future,
          List<Pair<SourceReference, Context>> input,
          Consumer<List<Pair<SourceReference, Context>>> resultHandler) {
        ConcurrentMapper.process(
            input,
            new ConcurrentMapper<
                Pair<SourceReference, Context>, Pair<T, Pair<SourceReference, Context>>>() {

              @Override
              public void emit(List<Pair<T, Pair<SourceReference, Context>>> output) {
                final var itemComparator =
                    Comparator.<Pair<T, Pair<SourceReference, Context>>, T>comparing(
                        Pair::first, anyConverter);
                resultHandler.accept(
                    output
                        .stream()
                        .sorted(ascending ? itemComparator : itemComparator.reversed())
                        .map(Pair::second)
                        .collect(Collectors.toList()));
              }

              @Override
              public void process(
                  Pair<SourceReference, Context> current,
                  int index,
                  Consumer<Pair<T, Pair<SourceReference, Context>>> output) {
                future.launch(
                    definition,
                    current.first(),
                    current.second(),
                    AnyFunction.compose(
                        anyConverter.function(),
                        operation ->
                            operation.resolve(
                                future,
                                current.first(),
                                anyConverter,
                                TypeErrorLocation.ORDER_BY,
                                key -> {
                                  output.accept(Pair.of(key, current));
                                })));
              }
            });
      }
    };
  }

  /**
   * Reorder the attributes based on a boolean value.
   *
   * <p>This is order preserving. That is, if two values have the same sort key, the will appear in
   * the original order.
   *
   * @param definition a definition that computes the sort key. If the sort key is not of the
   *     correct type, an error occurs
   * @param ascending if true, order the result ascending (false through true), false to order
   *     descending
   */
  public final Fricassee orderByBool(boolean ascending, Definition definition) {
    return orderBy(AnyConverter.asBool(false), ascending, definition);
  }

  /**
   * Reorder the attributes based on a floating-point value.
   *
   * <p>NaN is considered the largest value.
   *
   * <p>This is order preserving. That is, if two values have the same sort key, the will appear in
   * the original order.
   *
   * @param definition a definition that computes the sort key. If the sort key is not of the
   *     correct type, an error occurs
   * @param ascending if true, order the result ascending (-Inf through +Inf), false to order
   *     descending
   */
  public final Fricassee orderByFloat(boolean ascending, Definition definition) {
    return orderBy(AnyConverter.asFloat(false), ascending, definition);
  }

  /**
   * Reorder the attributes based on an integer value.
   *
   * <p>This is order preserving. That is, if two values have the same sort key, the will appear in
   * the original order.
   *
   * @param clause a definition that computes the sort key. If the sort key is not of the correct
   *     type, an error occurs
   * @param ascending if true, order the result ascending (IntMin through IntMax), false to order
   *     descending
   */
  public final Fricassee orderByInt(boolean ascending, Definition clause) {
    return orderBy(AnyConverter.asInt(false), ascending, clause);
  }

  /**
   * Reorder the attributes based on a string value.
   *
   * <p>This sorting is lexicographical for the current locale.
   *
   * <p>This is order preserving. That is, if two values have the same sort key, the will appear in
   * the original order.
   *
   * @param definition a definition that computes the sort key. If the sort key is not of the
   *     correct type, an error occurs
   * @param ascending if true, order the result ascending, false to order descending; this is
   *     locale-dependant
   */
  public final Fricassee orderByStr(boolean ascending, Definition definition) {
    return orderBy(AnyConverter.asStr(false), ascending, definition);
  }

  /**
   * Reduce the fricassée values to a single value
   *
   * @param future the scheduler for all the operations in this fricassée chain
   * @param initial the initial value to use (i.e., the <tt>With</tt> clause
   * @param reducer an definition that calculates the next value
   * @param consumer a callback that receives the last output of the reducer, or the initial value
   *     if the fricassée had no values
   */
  public final void reduce(
      Future<?> future,
      SourceReference sourceReference,
      Any initial,
      OverrideDefinition reducer,
      Consumer<? super Any> consumer) {
    new TerminalSink(this, future) {
      private Any currentValue = initial;

      @Override
      protected void emit() {
        consumer.accept(currentValue);
      }

      @Override
      public void process(SourceReference sourceReference, Context context) {
        future.launch(
            reducer,
            sourceReference,
            context,
            currentValue,
            result -> {
              currentValue = result;
              pullNext();
            });
      }
    }.pullNext();
  }

  /** Reverse the order of the items in the fricassée operation */
  public final Fricassee reverse() {
    return new BlockTransform(this) {
      @Override
      protected void process(
          Future<?> future,
          List<Pair<SourceReference, Context>> input,
          Consumer<List<Pair<SourceReference, Context>>> output) {
        Collections.reverse(input);
        output.accept(input);
      }
    };
  }

  /**
   * Do a cumulative-reduce the fricassée values to a frame
   *
   * @param future the scheduler for all the operations in this fricassée chain
   * @param initial the initial value to use (i.e., the <tt>With</tt> clause
   * @param reducer an definition that calculates the next value
   * @param consumer a callback that receives the complete frame
   */
  public final void scan(
      Future<?> future,
      SourceReference sourceReference,
      Any initial,
      OverrideDefinition reducer,
      Consumer<Frame> consumer) {
    new TerminalSink(this, future) {
      private Any currentValue = initial;
      private final List<Any> results = new ArrayList<>();

      @Override
      protected void emit() {
        consumer.accept(
            Frame.create(
                future,
                sourceReference,
                context(),
                AttributeSource.list(Attribute::of, results.stream())));
      }

      @Override
      public void process(SourceReference sourceReference, Context context) {
        future.launch(
            reducer,
            sourceReference,
            context,
            currentValue,
            result -> {
              currentValue = result;
              results.add(result);
              pullNext();
            });
      }
    }.pullNext();
  }

  /** Randomly shuffle the contexts in the chain */
  public final Fricassee shuffle() {
    return new BlockTransform(this) {

      @Override
      protected void process(
          Future<?> future,
          List<Pair<SourceReference, Context>> input,
          Consumer<List<Pair<SourceReference, Context>>> output) {
        Collections.shuffle(input);
        output.accept(input);
      }
    };
  }

  private void single(
      Future<?> future,
      SourceReference callerSource,
      Definition definition,
      EmptyHandler emptyHandler,
      Consumer<? super Any> consumer) {
    new TerminalSink(this, future) {
      Any currentValue;

      @Override
      protected void emit() {
        if (currentValue == null) {
          emptyHandler.finish(future, callerSource, context(), consumer);
        } else {
          consumer.accept(currentValue);
        }
      }

      @Override
      public void process(SourceReference sourceReference, Context context) {
        if (currentValue != null) {
          future().error(callerSource, "Multiple values for “Single” fricassée operation.");
          return;
        }
        future.launch(
            definition,
            sourceReference,
            context,
            result -> {
              currentValue = result;
              pullNext();
            });
      }
    }.pullNext();
  }

  /**
   * Get a single value from the fricassée
   *
   * <p>If multiple values are matched, an error occurs.
   *
   * @param future the scheduler for all the operations in this fricassée chain
   * @param definition the value to return
   * @param defaultDefinition the value to use if the fricassée is empty
   */
  public final void single(
      Future<?> future,
      SourceReference sourceReference,
      Definition definition,
      Definition defaultDefinition,
      Consumer<? super Any> consumer) {
    single(future, sourceReference, definition, of(defaultDefinition), consumer);
  }

  /**
   * Get a single value from the fricassée
   *
   * <p>If multiple values are present, an error occurs. If no values are available, null is
   * returned.
   *
   * @param future the scheduler for all the operations in this fricassée chain
   * @param definition the value to return
   */
  public final void singleOrNull(
      Future<?> future,
      SourceReference sourceReference,
      Definition definition,
      Consumer<? super Any> consumer) {
    single(future, sourceReference, definition, NULL_EMPTY, consumer);
  }

  /**
   * Keep items from the beginning of the chain
   *
   * @param count the number of items to keep
   */
  public final Fricassee take(long count) {
    final var input = this;
    return new Fricassee() {
      @Override
      Context context() {
        return input.context();
      }

      private void iterate(
          Thunkerator iterator, long remaining, Future<?> future, ThunkeratorConsumer thunkerator) {
        if (remaining > 0) {
          iterator.iterator(
              future,
              new ThunkeratorConsumer() {
                @Override
                public void end() {
                  thunkerator.end();
                }

                @Override
                public void next(
                    SourceReference sourceReference, Context context, Thunkerator next) {
                  thunkerator.next(
                      sourceReference,
                      context,
                      new Thunkerator() {
                        @Override
                        void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
                          iterate(next, remaining - 1, future, thunkerator);
                        }
                      });
                }
              });
        } else {
          thunkerator.end();
        }
      }

      @Override
      public void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
        iterate(input, count, future, thunkerator);
      }
    };
  }

  /**
   * Keep items from the end of the chain
   *
   * @param count the number of items to keep
   */
  public final Fricassee takeLast(long count) {
    return new BlockTransform(this) {
      @Override
      protected void process(
          Future<?> future,
          List<Pair<SourceReference, Context>> input,
          Consumer<List<Pair<SourceReference, Context>>> resultHandler) {
        resultHandler.accept(
            input.size() > count
                ? input.subList((int) (input.size() - count), input.size())
                : input);
      }
    };
  }

  /**
   * Keep items from the fricassée operation while a condition is satisfied
   *
   * @param condition the clause to decide whether to keep an item or skip; it must return
   *     <tt>Bool</tt> or an error will occur
   */
  public final Fricassee takeWhile(Definition condition) {
    return new Transform(this) {

      @Override
      protected void process(
          Future<?> future, SourceReference sourceReference, Context context, TailAction action) {
        future.launch(
            condition,
            sourceReference,
            context,
            new WhinyAnyConsumer() {

              @Override
              public void accept(boolean filter) {
                if (filter) {
                  action.next(sourceReference, context);
                } else {
                  action.end();
                }
              }

              @Override
              protected void fail(String type) {
                future.error(
                    sourceReference,
                    String.format(
                        "In “TakeWhile” clause, result is %s, but Bool was expected.", type));
              }
            });
      }
    };
  }

  /**
   * Collect the items of the fricassée operation into a new frame with user-defined attribute names
   *
   * @param future the scheduler for all the operations in this fricassée chain
   * @param sourceReference the source reference that will be used to createFromValues the frame
   * @param nameDefinition a definition to compute the attribute name of each item in the frame; it
   *     must return <tt>Int</tt> or <tt>Str</tt> and be a valid symbol name
   * @param valueDefinition a definition to compute the attribute value
   */
  public final void toFrame(
      Future<?> future,
      SourceReference sourceReference,
      Definition nameDefinition,
      Definition valueDefinition,
      Consumer<Frame> consumer) {
    new TerminalSink(this, future) {
      private final Map<Name, Any> builder = new TreeMap<>();

      @Override
      protected void emit() {
        consumer.accept(
            Frame.create(
                future(), sourceReference, context(), AttributeSource.fromMapOfAny(builder)));
      }

      @Override
      public void process(SourceReference sourceReference, Context context) {
        future.launch(
            nameDefinition,
            sourceReference,
            context,
            new WhinyAnyConsumer() {
              @Override
              public void accept(long id) {
                getValue(Name.of(id));
              }

              @Override
              public void accept(Str id) {
                getValue(Name.of(id));
              }

              @Override
              protected void fail(String type) {
                future()
                    .error(
                        sourceReference,
                        String.format(
                            "In “Select” (named), attribute name is %s, but Str or Int was expected.",
                            type));
              }

              private void getValue(Name attribute) {

                if (builder.containsKey(attribute)) {
                  future()
                      .error(
                          sourceReference,
                          String.format(
                              "In “Select” (named), duplicate attribute name “%s” in fricassée result.",
                              attribute));
                  return;
                }

                future()
                    .launch(
                        valueDefinition,
                        sourceReference,
                        context,
                        value -> {
                          builder.put(attribute, value);
                          pullNext();
                        });
              }
            });
      }
    }.pullNext();
  }

  /**
   * Collect the items of the fricassée operation into a new frame with automatically generated
   * attribute names
   *
   * @param future the scheduler for all the operations in this fricassée chain
   * @param sourceReference the source reference that will be used to createFromValues the frame
   * @param definition a definition to compute the attribute value
   */
  public final void toList(
      Future<?> future,
      SourceReference sourceReference,
      Definition definition,
      Consumer<Frame> consumer) {
    new TerminalSink(this, future) {
      private final List<Any> builder = new ArrayList<>();

      @Override
      protected void emit() {
        consumer.accept(
            Frame.create(
                future, sourceReference, context(), AttributeSource.listOfAny(builder.stream())));
      }

      @Override
      public void process(SourceReference sourceReference, Context context) {
        future()
            .launch(
                definition,
                sourceReference,
                context,
                value -> {
                  builder.add(value);
                  pullNext();
                });
      }
    }.pullNext();
  }

  private void univalued(
      Future<?> future,
      SourceReference callerSource,
      Definition definition,
      EmptyHandler emptyHandler,
      Consumer<? super Any> consumer) {
    new TerminalSink(this, future) {
      Any currentValue;

      @Override
      protected void emit() {
        if (currentValue == null) {
          emptyHandler.finish(future, callerSource, context(), consumer);
        } else {
          consumer.accept(currentValue);
        }
      }

      @Override
      public void process(SourceReference sourceReference, Context context) {
        future.launch(
            definition,
            sourceReference,
            context,
            result -> {
              if (currentValue == null) {
                currentValue = result;
              } else if (!currentValue.equals(result)) {
                future()
                    .error(
                        callerSource,
                        "Multiple different values for “Univalued” fricassée operation.");
                return;
              }
              pullNext();
            });
      }
    }.pullNext();
  }

  /**
   * Get a single value (or many identical values) from the fricassée
   *
   * <p>If multiple values are matched, an error occurs.
   *
   * @param future the scheduler for all the operations in this fricassée chain
   * @param definition the value to return
   * @param defaultDefinition the value to use if the fricassée is empty
   */
  public final void univalued(
      Future<?> future,
      SourceReference sourceReference,
      Definition definition,
      Definition defaultDefinition,
      Consumer<? super Any> consumer) {
    univalued(future, sourceReference, definition, of(defaultDefinition), consumer);
  }

  /**
   * Get a single value (or many identical values) from the fricassée
   *
   * <p>If multiple values are present, an error occurs. If no values are available, null is
   * returned.
   *
   * @param future the scheduler for all the operations in this fricassée chain
   * @param definition the value to return
   */
  public final void univaluedOrNull(
      Future<?> future,
      SourceReference sourceReference,
      Definition definition,
      Consumer<? super Any> consumer) {
    univalued(future, sourceReference, definition, NULL_EMPTY, consumer);
  }

  /**
   * Conditionally remove some items from the fricassée operation
   *
   * @param condition the clause to decide whether to keep an item; it must return <tt>Bool</tt> or
   *     an error will occur
   */
  public final Fricassee where(Definition condition) {
    return new Transform(this) {
      @Override
      protected void process(
          Future<?> future, SourceReference sourceReference, Context context, TailAction action) {
        future.launch(
            condition,
            sourceReference,
            context,
            new WhinyAnyConsumer() {

              @Override
              public void accept(boolean filter) {
                if (filter) {
                  action.next(sourceReference, context);
                } else {
                  action.skip();
                }
              }

              @Override
              protected void fail(String type) {
                future.error(
                    sourceReference,
                    String.format("In “Where” clause, result is %s, but Bool was expected.", type));
              }
            });
      }
    };
  }

  /**
   * Return a definition that, when instantiated, will have an attribute with the next value in the
   * chain.
   *
   * <p>If there are no more items in the chain, an error occurs.
   *
   * @param definition the definition to extract the result
   */
  public final Definition yield(SourceReference sourceReference, Definition definition) {
    return new Yielder(this, definition) {

      @Override
      protected void empty(Future<Any> future, SourceReference callerSourceReference) {
        future.error(
            callerSourceReference.specialJunction("fricassée yield", sourceReference),
            "Fricassée chain is empty.");
      }
    };
  }

  /**
   * Return a definition that, when instantiated, will have an attribute with the next value in the
   * chain.
   *
   * @param definition the definition to extract the result
   * @param empty a value to return when the chain is empty
   */
  public final Definition yield(SourceReference sourceReference, Definition definition, Any empty) {
    return new Yielder(this, definition) {

      @Override
      protected void empty(Future<Any> future, SourceReference callerSourceReference) {
        future.complete(empty);
      }
    };
  }
}
